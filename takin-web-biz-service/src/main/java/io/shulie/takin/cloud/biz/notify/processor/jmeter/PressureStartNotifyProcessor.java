package io.shulie.takin.cloud.biz.notify.processor.jmeter;

import java.util.Date;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.cache.SceneTaskStatusCache;
import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.biz.service.async.CloudAsyncService;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.takin.cloud.common.constants.ScheduleConstants;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneRunTaskStatusEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.dao.report.ReportDao;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.cloud.model.callback.basic.JobExample;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PressureStartNotifyProcessor extends AbstractIndicators
    implements CloudNotifyProcessor<PressureStartNotifyParam> {

    @Resource
    private RedisClientUtils redisClientUtils;

    @Resource
    protected CloudSceneManageService cloudSceneManageService;

    @Resource
    private ReportDao reportDao;

    @Resource
    private CloudAsyncService cloudAsyncService;

    @Resource
    private SceneTaskStatusCache taskStatusCache;
    @Override
    public CallbackType type() {
        return CallbackType.JMETER_START;
    }

    @Override
    public void process(PressureStartNotifyParam param) {
        processStartSuccess(param);
    }

    private void processStartSuccess(PressureStartNotifyParam context) {
        JobExample data = context.getData();
        String resourceId = String.valueOf(data.getResourceId());
        String podId = String.valueOf(data.getJobExampleId());
        ResourceContext resourceContext = getResourceContext(resourceId);
        if (resourceContext == null) {
            return;
        }
        if (!redisClientUtils.hasKey(RedisClientUtils.getLockPrefix(PressureStartCache.getStopFlag(resourceContext.getSceneId(), resourceId)))) {
            redisClientUtils.setSetValue(PressureStartCache.getResourceJmeterSuccessKey(resourceId), podId);
            Long tenantId = resourceContext.getTenantId();
            Long sceneId = resourceContext.getSceneId();
            Long reportId = resourceContext.getReportId();
            String engineName = ScheduleConstants.getEngineName(sceneId, reportId, tenantId);
            setMin(engineName + ScheduleConstants.FIRST_SIGN, context.getTime().getTime());
            if (Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(PressureStartCache.getJmeterStartFirstKey(resourceId), podId))) {
                cloudSceneManageService.updateSceneLifeCycle(UpdateStatusBean.build(sceneId, reportId, tenantId)
                    .checkEnum(SceneManageStatusEnum.PRESSURE_NODE_RUNNING)
                    .updateEnum(SceneManageStatusEnum.ENGINE_RUNNING)
                    .build());
                notifyStart(resourceContext, context.getTime().getTime());
                cacheTryRunTaskStatus(resourceContext);
            }
        }
    }

    private void cacheTryRunTaskStatus(ResourceContext context) {
        Long sceneId = context.getSceneId();
        Long reportId = context.getReportId();
        taskStatusCache.cacheStatus(sceneId, reportId, SceneRunTaskStatusEnum.RUNNING);
    }

    private void notifyStart(ResourceContext context, long startTime) {
        cloudAsyncService.checkJmeterHeartbeatTask(context);
        Long sceneId = context.getSceneId();
        Long reportId = context.getReportId();
        log.info("场景[{}]压测任务开始，更新报告[{}]开始时间[{}]", sceneId, reportId, startTime);
        reportDao.updateReportStartTime(reportId, new Date(startTime));
    }
}
