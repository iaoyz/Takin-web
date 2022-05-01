package io.shulie.takin.cloud.biz.notify;

import java.util.Date;
import java.util.Objects;

import javax.annotation.Resource;

import com.pamirs.takin.cloud.entity.dao.report.TReportMapper;
import com.pamirs.takin.cloud.entity.domain.entity.report.Report;
import io.shulie.takin.cloud.biz.cache.SceneTaskStatusCache;
import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.service.async.CloudAsyncService;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.takin.cloud.common.bean.task.TaskResult;
import io.shulie.takin.cloud.common.constants.ScheduleConstants;
import io.shulie.takin.cloud.common.enums.PressureSceneEnum;
import io.shulie.takin.cloud.common.enums.PressureTaskStateEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneRunTaskStatusEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.TaskStatusEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.dao.report.ReportDao;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskVarietyDAO;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskVarietyEntity;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PressureStartNotifyProcessor extends AbstractIndicators implements CloudNotifyProcessor {

    private static final String STARTED_FIRST_JMETER = "pressure:resource:jmeter:first:start:%s";

    @Resource
    private RedisClientUtils redisClientUtils;

    @Resource
    protected CloudSceneManageService cloudSceneManageService;

    @Resource
    private ReportDao reportDao;

    @Resource
    private TReportMapper tReportMapper;

    @Resource
    private CloudAsyncService cloudAsyncService;

    @Resource
    private SceneTaskStatusCache taskStatusCache;

    @Resource
    private PressureTaskVarietyDAO pressureTaskVarietyDAO;

    @Override
    public String type() {
        return "pressure";
    }

    @Override
    public ResponseResult<?> process(NotifyContext context) {
        JmeterStatus status = JmeterStatus.of(context.getStatus());
        if (Objects.isNull(status)) {
            return ResponseResult.success();
        }
        switch (status) {
            case START_SUCCESS:
                processStartSuccess(context);
                break;
            case START_FAIL:
                processStartFail(context);
                break;
            case STOP:
                processStopped(context);
                break;
            default:
                break;
        }
        return ResponseResult.success();
    }

    private void processStartSuccess(NotifyContext context) {
        String resourceId = context.getResourceId();
        if (redisClientUtils.hasKey(PressureStartCache.getResourceJmeterFailKey(resourceId))) {
            return;
        }
        ResourceContext resourceContext = getResourceContext(resourceId);
        if (resourceContext == null) {
            return;
        }
        redisClientUtils.setSetValue(PressureStartCache.getResourceJmeterSuccessKey(context.getResourceId()), context.getPodId());
        Long tenantId = resourceContext.getTenantId();
        Long sceneId = resourceContext.getSceneId();
        Long reportId = resourceContext.getReportId();
        String engineName = ScheduleConstants.getEngineName(sceneId, reportId, tenantId);
        setMin(engineName + ScheduleConstants.FIRST_SIGN, context.getTime().getTime());
        if (Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(String.format(STARTED_FIRST_JMETER, resourceId), 1))) {
            cloudSceneManageService.updateSceneLifeCycle(UpdateStatusBean.build(sceneId, reportId, tenantId)
                .checkEnum(SceneManageStatusEnum.PRESSURE_NODE_RUNNING)
                .updateEnum(SceneManageStatusEnum.ENGINE_RUNNING)
                .build());
            notifyStart(resourceContext, context.getTime().getTime());
            cacheTryRunTaskStatus(resourceContext, SceneRunTaskStatusEnum.RUNNING);
        }
    }

    private void processStartFail(NotifyContext context) {
        String resourceId = context.getResourceId();
        ResourceContext resourceContext = getResourceContext(resourceId);
        if (resourceContext == null) {
            return;
        }
        redisClientUtils.setSetValue(PressureStartCache.getResourceJmeterFailKey(resourceId), context.getPodId());
        String message = context.getMessage();
        Long sceneId = resourceContext.getSceneId();
        Long reportId = resourceContext.getReportId();
        Long tenantId = resourceContext.getTenantId();
        String engineName = ScheduleConstants.getEngineName(sceneId, reportId, tenantId);
        String tempFailSign = ScheduleConstants.TEMP_FAIL_SIGN + engineName;
        redisClientUtils.increment(tempFailSign, 1);
        // finalFailed(resourceContext, message);
    }

    private void processStopped(NotifyContext context) {
        String resourceId = context.getResourceId();
        ResourceContext resourceContext = getResourceContext(resourceId);
        if (resourceContext == null) {
            return;
        }
        Long stoppedCount = redisClientUtils.setSetValueAndReturnCount(PressureStartCache.getResourceJmeterStopKey(resourceId),
            context.getPodId());
        long time = context.getTime().getTime();
        Long tenantId = resourceContext.getTenantId();
        Long sceneId = resourceContext.getSceneId();
        Long reportId = resourceContext.getReportId();
        String engineName = ScheduleConstants.getEngineName(sceneId, reportId, tenantId);
        String taskKey = getPressureTaskKey(sceneId, reportId, tenantId);
        if (Objects.equals(stoppedCount, redisClientUtils.getSetSize(PressureStartCache.getResourcePodSuccessKey(resourceId)))) {
            setLast(last(taskKey), ScheduleConstants.LAST_SIGN);
            setMax(engineName + ScheduleConstants.LAST_SIGN, time);
            // 删除临时标识
            redisClientUtils.del(ScheduleConstants.TEMP_LAST_SIGN + engineName);
            // 压测停止
            notifyEnd(resourceContext);
        }
    }

    private void notifyStartEvent(ResourceContext resourceContext, NotifyContext context) {
        TaskResult result = new TaskResult();
        result.setSceneId(resourceContext.getSceneId());
        result.setTaskId(resourceContext.getReportId());
        result.setTenantId(resourceContext.getTenantId());
        result.setMsg(context.getMessage());
        result.setStatus(TaskStatusEnum.STARTED);
        Event event = new Event();
        event.setEventName("started");
        event.setExt(result);
        eventCenterTemplate.doEvents(event);
    }

    private void cacheTryRunTaskStatus(ResourceContext context , SceneRunTaskStatusEnum status) {
        Long sceneId = context.getSceneId();
        Long reportId = context.getReportId();
        Long tenantId = context.getTenantId();
        taskStatusCache.cacheStatus(sceneId, reportId, status);
        Report report = tReportMapper.selectByPrimaryKey(reportId);
        if (Objects.nonNull(report) && !Objects.equals(report.getPressureType(), PressureSceneEnum.FLOW_DEBUG.getCode())
            && !Objects.equals(report.getPressureType(), PressureSceneEnum.INSPECTION_MODE.getCode())
            && status.getCode() == SceneRunTaskStatusEnum.RUNNING.getCode()) {
            cloudAsyncService.updateSceneRunningStatus(sceneId, reportId, tenantId);
        }
    }

    private void notifyStart(ResourceContext context, long startTime) {
        Long sceneId = context.getSceneId();
        Long reportId = context.getReportId();
        log.info("场景[{}]压测任务开始，更新报告[{}]开始时间[{}]", sceneId, reportId, startTime);
        reportDao.updateReportStartTime(reportId, new Date(startTime));
        pressureTaskVarietyDAO.save(PressureTaskVarietyEntity.of(context.getPressureTaskId(),
            PressureTaskStateEnum.STARTING.ordinal()));
    }

    private void notifyEnd(ResourceContext context) {
        Long sceneId = context.getSceneId();
        Long reportId = context.getReportId();
        Long tenantId = context.getTenantId();
        Long endTime = context.getEndTime();
        String resourceId = context.getResourceId();
        log.info("场景[{}]压测任务已完成,更新结束时间{}", sceneId, reportId);
        // 刷新任务状态的Redis缓存
        taskStatusCache.cacheStatus(sceneId, reportId, SceneRunTaskStatusEnum.ENDED);
        // 更新压测场景状态  压测引擎运行中,压测引擎停止压测 ---->压测引擎停止压测
        cloudSceneManageService.updateSceneLifeCycle(UpdateStatusBean.build(sceneId, reportId, tenantId)
            .checkEnum(SceneManageStatusEnum.ENGINE_RUNNING, SceneManageStatusEnum.STOP)
            .updateEnum(SceneManageStatusEnum.STOP)
            .build());
        reportDao.updateReportEndTime(reportId, new Date(endTime));

        // 清除 SLA配置  生成报告拦截 状态拦截
        // TODO：需要确认是否在此位置
        Event event = new Event();
        event.setEventName("finished");
        TaskResult result = new TaskResult(sceneId, reportId, tenantId);
        result.setResourceId(resourceId);
        result.setSceneId(sceneId);
        event.setExt(result);
        eventCenterTemplate.doEvents(event);
    }

    public enum JmeterStatus {
        START_SUCCESS,
        // TODO：不一定可以收集到
        START_FAIL,
        STOP;

        public static JmeterStatus of(Integer status) {
            if (Objects.isNull(status)) {
                return null;
            }
            for (JmeterStatus value : values()) {
                if (value.ordinal() == status) {
                    return value;
                }
            }
            return null;
        }
    }
}
