package io.shulie.takin.cloud.biz.notify.processor.jmeter;

import java.util.Objects;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.cache.SceneTaskStatusCache;
import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.takin.cloud.common.bean.task.TaskResult;
import io.shulie.takin.cloud.common.constants.ScheduleConstants;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneRunTaskStatusEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.dao.report.ReportDao;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.cloud.model.callback.basic.JobExample;
import io.shulie.takin.eventcenter.Event;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PressureStopNotifyProcessor extends AbstractIndicators
    implements CloudNotifyProcessor<PressureStopNotifyParam> {

    @Resource
    private RedisClientUtils redisClientUtils;
    @Resource
    private SceneTaskStatusCache taskStatusCache;
    @Resource
    private ReportDao reportDao;

    @Override
    public void process(PressureStopNotifyParam param) {
        processStopped(param);
    }

    private void processStopped(PressureStopNotifyParam context) {
        JobExample data = context.getData();
        String resourceId = String.valueOf(data.getResourceId());
        ResourceContext resourceContext = getResourceContext(resourceId);
        if (resourceContext == null) {
            return;
        }
        stopFinish(context, resourceContext);
    }

    private void stopFinish(PressureStopNotifyParam context, ResourceContext resourceContext) {
        long time = context.getTime().getTime();
        String podId = String.valueOf(context.getData().getJobExampleId());
        Long tenantId = resourceContext.getTenantId();
        Long sceneId = resourceContext.getSceneId();
        Long reportId = resourceContext.getReportId();
        String resourceId = resourceContext.getResourceId();
        Long stoppedCount = redisClientUtils.setSetValueAndReturnCount(PressureStartCache.getResourceJmeterStopKey(resourceId), podId);
        String engineName = ScheduleConstants.getEngineName(sceneId, reportId, tenantId);
        String taskKey = getPressureTaskKey(sceneId, reportId, tenantId);
        if (Objects.equals(stoppedCount,
            redisClientUtils.getSetSize(PressureStartCache.getResourceJmeterSuccessKey(resourceId)))) {
            setLast(last(taskKey), ScheduleConstants.LAST_SIGN);
            setMax(engineName + ScheduleConstants.LAST_SIGN, time);
            // 删除临时标识
            redisClientUtils.del(ScheduleConstants.TEMP_LAST_SIGN + engineName);
            // 压测停止
            notifyEnd(resourceContext, context);
        }
    }

    private void notifyEnd(ResourceContext context, PressureStopNotifyParam param) {
        Long sceneId = context.getSceneId();
        Long reportId = context.getReportId();
        Long tenantId = context.getTenantId();
        log.info("场景[{}]压测任务已完成,更新结束时间{}", sceneId, reportId);
        // 刷新任务状态的Redis缓存
        taskStatusCache.cacheStatus(sceneId, reportId, SceneRunTaskStatusEnum.ENDED);
        // 更新压测场景状态  压测引擎运行中,压测引擎停止压测 ---->压测引擎停止压测
        cloudSceneManageService.updateSceneLifeCycle(UpdateStatusBean.build(sceneId, reportId, tenantId)
            .checkEnum(SceneManageStatusEnum.ENGINE_RUNNING, SceneManageStatusEnum.STOP)
            .updateEnum(SceneManageStatusEnum.STOP)
            .build());
        reportDao.updateReportEndTime(reportId, param.getTime());

        // 清除 SLA配置  生成报告拦截 状态拦截
        Event event = new Event();
        event.setEventName("finished");
        TaskResult result = new TaskResult(sceneId, reportId, tenantId);
        result.setResourceId(context.getResourceId());
        event.setExt(result);
        eventCenterTemplate.doEvents(event);

        // 清理缓存
        Event otherEvent = new Event();
        otherEvent.setEventName(PressureStartCache.PRESSURE_END);
        otherEvent.setExt(context);
        eventCenterTemplate.doEvents(otherEvent);
    }

    @Override
    public CallbackType type() {
        return CallbackType.JMETER_STOP;
    }
}
