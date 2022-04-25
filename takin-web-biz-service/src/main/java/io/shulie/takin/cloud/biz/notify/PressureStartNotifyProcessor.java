package io.shulie.takin.cloud.biz.notify;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;

import com.pamirs.takin.cloud.entity.dao.report.TReportMapper;
import com.pamirs.takin.cloud.entity.domain.entity.report.Report;
import com.pamirs.takin.cloud.entity.domain.vo.scenemanage.SceneManageStartRecordVO;
import io.shulie.takin.cloud.biz.cache.SceneTaskStatusCache;
import io.shulie.takin.cloud.biz.checker.EngineResourceChecker;
import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.service.async.CloudAsyncService;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.takin.cloud.common.bean.task.TaskResult;
import io.shulie.takin.cloud.common.constants.SceneTaskRedisConstants;
import io.shulie.takin.cloud.common.constants.ScheduleConstants;
import io.shulie.takin.cloud.common.enums.PressureSceneEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneRunTaskStatusEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.dao.report.ReportDao;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.eventcenter.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class PressureStartNotifyProcessor extends AbstractIndicators implements CloudNotifyProcessor {

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

    @Value("${pressure.node.start.expireTime:30}")
    private Integer pressureNodeStartExpireTime;

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
            case START:
                processStarted(context);
                break;
            case STOP:
                processStopped(context);
                break;
            default:
                break;
        }
        return ResponseResult.success();
    }

    private void processStarted(NotifyContext context) {
        String resourceId = context.getResourceId();
        String resourceKey = EngineResourceChecker.getResourceKey(resourceId);
        Map<Object, Object> resource = redisClientUtils.hmget(resourceKey);
        if (CollectionUtils.isEmpty(resource)) {
            return;
        }
        Long count = redisClientUtils.hIncrBy(resourceKey, EngineResourceChecker.JMETER_STARTED, 1);
        Long tenantId = Long.valueOf(String.valueOf(resource.get(EngineResourceChecker.TENANT_ID)));
        Long sceneId = Long.valueOf(String.valueOf(resource.get(EngineResourceChecker.SCENE_ID)));
        Long reportId = Long.valueOf(String.valueOf(resource.get(EngineResourceChecker.REPORT_ID)));
        Long podNumber = Long.valueOf(String.valueOf(resource.get(EngineResourceChecker.RESOURCE_POD_NUM)));
        long endTime = Long.parseLong(String.valueOf(resource.get(EngineResourceChecker.RESOURCE_END_TIME)));
        if (endTime < System.currentTimeMillis() && count < podNumber) {
            // 记录停止原因
            // 补充停止原因
            //设置缓存，用以检查压测场景启动状态 lxr 20210623
            String k8sPodKey = String.format(SceneTaskRedisConstants.PRESSURE_NODE_ERROR_KEY + "%s_%s", sceneId, reportId);
            redisClientUtils.hmset(k8sPodKey, SceneTaskRedisConstants.PRESSURE_NODE_START_ERROR,
                String.format("节点没有在设定时间【%s】s内启动，计划启动节点个数【%s】,实际启动节点个数【%s】,"
                    + "导致压测停止", pressureNodeStartExpireTime, podNumber, count));
            callStop(sceneId, reportId, resourceId, tenantId);
            return;
        }
        long time = context.getTime().getTime();
        String engineName = ScheduleConstants.getEngineName(sceneId, reportId, tenantId);
        setMin(engineName + ScheduleConstants.FIRST_SIGN, time);
        if (count != null && count == 1) {
            cloudSceneManageService.updateSceneLifeCycle(UpdateStatusBean.build(sceneId, reportId, tenantId)
                .checkEnum(SceneManageStatusEnum.PRESSURE_NODE_RUNNING)
                .updateEnum(SceneManageStatusEnum.ENGINE_RUNNING)
                .build());
            notifyStart(sceneId, reportId, time);
            cacheTryRunTaskStatus(sceneId, reportId, tenantId, SceneRunTaskStatusEnum.RUNNING);
        }
    }

    private void processStopped(NotifyContext context) {
        String resourceKey = EngineResourceChecker.getResourceKey(context.getResourceId());
        Map<Object, Object> resource = redisClientUtils.hmget(resourceKey);
        if (CollectionUtils.isEmpty(resource)) {
            return;
        }
        long time = context.getTime().getTime();
        Long sceneId = Long.valueOf(String.valueOf(resource.get(EngineResourceChecker.SCENE_ID)));
        Long reportId = Long.valueOf(String.valueOf(resource.get(EngineResourceChecker.REPORT_ID)));
        Long tenantId = Long.valueOf(String.valueOf(resource.get(EngineResourceChecker.TENANT_ID)));
        Long podNumber = Long.valueOf(String.valueOf(resource.get(EngineResourceChecker.RESOURCE_POD_NUM)));
        String engineName = ScheduleConstants.getEngineName(sceneId, reportId, tenantId);
        String taskKey = getPressureTaskKey(sceneId, reportId, tenantId);
        Long count = redisClientUtils.hIncrBy(resourceKey, EngineResourceChecker.JMETER_STOP, 1);
        if (count != null && count.equals(podNumber)) {
            setLast(last(taskKey), ScheduleConstants.LAST_SIGN);
            setMax(engineName + ScheduleConstants.LAST_SIGN, time);
            // 删除临时标识
            redisClientUtils.del(ScheduleConstants.TEMP_LAST_SIGN + engineName);
            // 压测停止
            notifyEnd(sceneId, reportId, time, tenantId);
        }
    }

    private void cacheTryRunTaskStatus(Long sceneId, Long reportId, Long customerId, SceneRunTaskStatusEnum status) {
        taskStatusCache.cacheStatus(sceneId, reportId, status);
        Report report = tReportMapper.selectByPrimaryKey(reportId);
        if (Objects.nonNull(report) && !Objects.equals(report.getPressureType(), PressureSceneEnum.FLOW_DEBUG.getCode())
            && !Objects.equals(report.getPressureType(), PressureSceneEnum.INSPECTION_MODE.getCode())
            && status.getCode() == SceneRunTaskStatusEnum.RUNNING.getCode()) {
            cloudAsyncService.updateSceneRunningStatus(sceneId, reportId, customerId);
        }
    }

    private void notifyStart(Long sceneId, Long reportId, long startTime) {
        log.info("场景[{}]压测任务开始，更新报告[{}]开始时间[{}]", sceneId, reportId, startTime);
        reportDao.updateReportStartTime(reportId, new Date(startTime));
    }

    private void notifyEnd(Long sceneId, Long reportId, long endTime, Long tenantId) {
        log.info("场景[{}]压测任务已完成,更新结束时间{}", sceneId, reportId);
        // 刷新任务状态的Redis缓存
        taskStatusCache.cacheStatus(sceneId, reportId, SceneRunTaskStatusEnum.ENDED);
        // 更新压测场景状态  压测引擎运行中,压测引擎停止压测 ---->压测引擎停止压测
        cloudSceneManageService.updateSceneLifeCycle(UpdateStatusBean.build(sceneId, reportId, tenantId)
            .checkEnum(SceneManageStatusEnum.ENGINE_RUNNING, SceneManageStatusEnum.STOP)
            .updateEnum(SceneManageStatusEnum.STOP)
            .build());
        reportDao.updateReportEndTime(reportId, new Date(endTime));
    }

    private void callStop(Long sceneId, Long taskId, String resourceId, Long tenantId) {
        // 汇报失败
        cloudSceneManageService.reportRecord(SceneManageStartRecordVO.build(sceneId,
            taskId, tenantId).success(false).errorMsg("").build());
        // 清除 SLA配置 清除PushWindowDataScheduled 删除pod job configMap  生成报告拦截 状态拦截
        Event event = new Event();
        event.setEventName("finished");
        TaskResult result = new TaskResult(sceneId, taskId, tenantId);
        result.setResourceId(resourceId);
        event.setExt(result);
        eventCenterTemplate.doEvents(event);
    }

    public enum JmeterStatus {
        START,
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
