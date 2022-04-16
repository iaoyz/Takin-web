package io.shulie.takin.web.biz.checker;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import io.shulie.takin.adapter.api.entrypoint.pressure.PressureTaskApi;
import io.shulie.takin.adapter.api.entrypoint.resource.CloudResourceApi;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStopReq;
import io.shulie.takin.adapter.api.model.request.resource.ResourceCheckRequest;
import io.shulie.takin.adapter.api.model.request.resource.ResourceLockRequest;
import io.shulie.takin.adapter.api.model.request.resource.ResourceUnLockRequest;
import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.config.AppConfig;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.notify.StopEventSource;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.service.async.CloudAsyncService;
import io.shulie.takin.cloud.biz.service.report.CloudReportService;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneTaskService;
import io.shulie.takin.cloud.biz.service.strategy.StrategyConfigService;
import io.shulie.takin.cloud.common.bean.scenemanage.SceneManageQueryOptions;
import io.shulie.takin.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.takin.cloud.common.bean.task.TaskResult;
import io.shulie.takin.cloud.common.constants.ReportConstants;
import io.shulie.takin.cloud.common.constants.SceneTaskRedisConstants;
import io.shulie.takin.cloud.common.enums.PressureTaskStateEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneRunTaskStatusEnum;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.dao.report.ReportDao;
import io.shulie.takin.cloud.data.dao.scene.manage.SceneManageDAO;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskDAO;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskEntity;
import io.shulie.takin.cloud.data.model.mysql.ReportEntity;
import io.shulie.takin.cloud.data.model.mysql.SceneManageEntity;
import io.shulie.takin.cloud.data.param.report.ReportUpdateParam;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.cloud.ext.content.enginecall.StrategyConfigExt;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.eventcenter.annotation.IntrestFor;
import io.shulie.takin.web.ext.util.WebPluginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class EngineResourceChecker extends AbstractIndicators implements StartConditionChecker {

    @Resource
    private StrategyConfigService strategyConfigService;
    @Resource
    private CloudResourceApi cloudResourceApi;
    @Resource
    private PressureTaskApi pressureTaskApi;
    @Resource
    private CloudSceneManageService cloudSceneManageService;
    @Resource
    private CloudSceneTaskService cloudSceneTaskService;
    @Resource
    private RedisClientUtils redisClientUtils;
    @Resource
    private AppConfig appConfig;
    @Resource
    private CloudAsyncService cloudAsyncService;
    @Resource
    private PressureTaskDAO pressureTaskDAO;
    @Resource
    private ReportDao reportDao;
    @Resource
    private CloudReportService cloudReportService;
    @Resource
    private SceneManageDAO sceneManageDAO;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public CheckResult check(StartConditionCheckerContext context) throws TakinCloudException {
        String resourceId = context.getResourceId();
        return StringUtils.isBlank(resourceId) ? firstCheck(context) : getResourceStatus(resourceId);
    }

    private CheckResult firstCheck(StartConditionCheckerContext context) {
        try {
            fillContextIfNecessary(context);
            initTaskAndReportIfNecessary(context);
            SceneManageWrapperOutput sceneData = context.getSceneData();
            StrategyConfigExt config = getStrategy();
            sceneData.setStrategy(config);
            ResourceCheckRequest request = new ResourceCheckRequest();
            request.setCpu(config.getCpuNum().toPlainString());
            request.setMemory(config.getMemorySize().toPlainString());
            request.setNumber(sceneData.getIpNum());
            request.setLimitCpu(config.getLimitCpuNum().toPlainString());
            request.setLimitMemory(config.getLimitMemorySize().toPlainString());
            Boolean checkResult = cloudResourceApi.check(request);
            if (!Boolean.TRUE.equals(checkResult)) {
                return CheckResult.fail(type(), "压力机资源不足");
            }
            pressureTaskDAO.updateStatus(context.getTaskId(), PressureTaskStateEnum.CHECKED, null);
            // 锁定资源：异步接口，每个pod启动成功都会回调一次回调接口
            String resourceId = lockResource(context);
            context.setResourceId(resourceId);
            afterLocking(context);
            return getResourceStatus(resourceId);
        } catch (Exception e) {
            return CheckResult.fail(type(), e.getMessage());
        }
    }

    private CheckResult getResourceStatus(String resourceId) {
        Object message = redisClientUtils.getObject(PressureStartCache.getErrorMessageKey(resourceId));
        if (message != null) {
            return new CheckResult(type(), CheckStatus.FAIL.ordinal(), String.valueOf(message));
        }
        String statusKey = PressureStartCache.getResourceKey(resourceId);
        Object redisStatus = redisClientUtils.hmget(statusKey, PressureStartCache.CHECK_STATUS);
        if (redisStatus == null) {
            return new CheckResult(type(), CheckStatus.FAIL.ordinal(), resourceId, "未找到启动中的任务");
        }
        int status = Integer.parseInt(String.valueOf(redisStatus));
        return new CheckResult(type(), status, resourceId, null);
    }

    private String lockResource(StartConditionCheckerContext context) {
        SceneManageWrapperOutput sceneData = context.getSceneData();
        StrategyConfigExt strategy = sceneData.getStrategy();
        ResourceLockRequest request = new ResourceLockRequest();
        request.setCpu(strategy.getCpuNum());
        request.setMemory(strategy.getMemorySize());
        request.setNumber(sceneData.getIpNum());
        request.setCallbackUrl(appConfig.getCallbackUrl());
        return cloudResourceApi.lock(request);
    }

    private void afterLocking(StartConditionCheckerContext context) {
        associateResource(context);
        initCache(context);
        SceneManageWrapperOutput sceneData = context.getSceneData();
        cloudSceneManageService.updateSceneLifeCycle(
            UpdateStatusBean.build(context.getSceneId(), context.getReportId(), sceneData.getTenantId())
                .checkEnum(SceneManageStatusEnum.WAIT, SceneManageStatusEnum.FAILED, SceneManageStatusEnum.STOP,
                    SceneManageStatusEnum.FORCE_STOP)
                .updateEnum(SceneManageStatusEnum.RESOURCE_LOCKING).build());
        pressureTaskDAO.updateStatus(context.getTaskId(), PressureTaskStateEnum.RESOURCE_LOCKING, null);
        cloudAsyncService.checkPodStartedTask(context);
    }

    private void initCache(StartConditionCheckerContext context) {
        SceneManageWrapperOutput sceneData = context.getSceneData();
        Map<String, Object> param = new HashMap<>(32);
        param.put(PressureStartCache.CHECK_STATUS, CheckStatus.PENDING.ordinal());
        param.put(PressureStartCache.RESOURCE_POD_NUM, sceneData.getIpNum());
        param.put(PressureStartCache.TENANT_ID, sceneData.getTenantId());
        param.put(PressureStartCache.ENV_CODE, sceneData.getEnvCode());
        param.put(PressureStartCache.SCENE_ID, String.valueOf(sceneData.getId()));
        param.put(PressureStartCache.TASK_ID, context.getTaskId());
        param.put(PressureStartCache.REPORT_ID, context.getReportId());
        param.put(PressureStartCache.UNIQUE_KEY, context.getUniqueKey());
        redisClientUtils.hmset(PressureStartCache.getResourceKey(context.getResourceId()), param);

        Map<String, Object> sceneParam = new HashMap<>(4);
        sceneParam.put(PressureStartCache.REPORT_ID, context.getReportId());
        sceneParam.put(PressureStartCache.TASK_ID, context.getTaskId());
        sceneParam.put(PressureStartCache.RESOURCE_ID, context.getResourceId());
        sceneParam.put(PressureStartCache.UNIQUE_KEY, context.getUniqueKey());
        redisClientUtils.hmset(PressureStartCache.getSceneResourceKey(sceneData.getId()), sceneParam);
    }

    private StrategyConfigExt getStrategy() {
        StrategyConfigExt config = strategyConfigService.getCurrentStrategyConfig();
        if (config == null) {
            throw new RuntimeException("未配置策略");
        }
        return config;
    }

    @IntrestFor(event = PressureStartCache.CHECK_FAIL_EVENT, order = 0)
    public void expireCache(Event event) {
        ResourceContext context = (ResourceContext)event.getExt();
        String resourceId = context.getResourceId();
        if (StringUtils.isNotBlank(resourceId)) {
            redisClientUtils.del(PressureStartCache.getResourceKey(resourceId),
                PressureStartCache.getResourcePodSuccessKey(resourceId),
                PressureStartCache.getPodStartFirstKey(resourceId));
            ResourceUnLockRequest request = new ResourceUnLockRequest();
            request.setResourceId(resourceId);
            cloudResourceApi.unLock(request);
        }
        releaseFlow(context.getReportId());
    }

    @IntrestFor(event = PressureStartCache.CHECK_SUCCESS_EVENT, order = 0)
    public void callStartSuccess(Event event) {
        ResourceContext ext = (ResourceContext)event.getExt();
        String resourceId = ext.getResourceId();
        String resourceKey = PressureStartCache.getResourceKey(resourceId);
        if (redisClientUtils.hasKey(resourceKey)) {
            redisClientUtils.hmset(resourceKey, PressureStartCache.CHECK_STATUS, CheckStatus.SUCCESS.ordinal());
        }
        redisClientUtils.delete(PressureStartCache.getErrorMessageKey(resourceId));
        pressureTaskDAO.updateStatus(ext.getTaskId(), PressureTaskStateEnum.STARTING, null);
    }

    @IntrestFor(event = PressureStartCache.PRE_STOP_EVENT, order = 1)
    public void callPreStop(Event event) {
        ResourceContext context = (ResourceContext)event.getExt();
        Long sceneId = context.getSceneId();
        String resource = context.getResourceId();
        if (StringUtils.isBlank(resource)) {
            Object resourceId = redisClientUtils.hmget(PressureStartCache.getSceneResourceKey(sceneId), PressureStartCache.RESOURCE_ID);
            if (Objects.nonNull(resource)) {
                resource = String.valueOf(resourceId);
            }
        }
        if (StringUtils.isNotBlank(resource)) {
            failed(context);
            clearCache(resource, sceneId);
            unLockResource(context.getResourceId());
        }
        releaseFlow(context.getReportId());
    }

    @IntrestFor(event = PressureStartCache.START_FAILED)
    public void callStartFail(Event event) {
        StopEventSource source = (StopEventSource)event.getExt();
        String message = source.getMessage();
        ResourceContext context = source.getContext();
        setTryRunTaskInfo(context.getSceneId(), context.getReportId(), context.getTenantId(), message);
        // 此处手动停止的也会插入此状态
        pressureTaskDAO.updateStatus(context.getTaskId(), PressureTaskStateEnum.UNUSUAL, message);
        if (!source.isPressureRunning()) {
            preStartFail(source);
        } else {
            pressureTaskDAO.updateStatus(context.getTaskId(), PressureTaskStateEnum.STOPPING, message);
            stopJob(context.getJobId());
            // 如果没有启动的jmeter节点，直接结束
            if (!redisClientUtils.hasKey(PressureStartCache.getJmeterStartFirstKey(context.getResourceId()))) {
                preStartFail(source);
            }
        }
    }

    @IntrestFor(event = PressureStartCache.PRESSURE_END)
    public void pressureEnd(Event event) {
        ResourceContext context = (ResourceContext)event.getExt();
        Long sceneId = context.getSceneId();
        Long reportId = context.getReportId();
        clearCache(context.getResourceId(), sceneId);
        String statusKey = String.format(SceneTaskRedisConstants.SCENE_TASK_RUN_KEY + "%s_%s", sceneId, reportId);
        stringRedisTemplate.opsForHash().put(
            statusKey, SceneTaskRedisConstants.SCENE_RUN_TASK_STATUS_KEY, SceneRunTaskStatusEnum.ENDED.getText());
        pressureTaskDAO.updateStatus(context.getTaskId(), PressureTaskStateEnum.INACTIVE, null);
    }

    // 启动前失败，即启动异常
    private void preStartFail(StopEventSource source) {
        String message = source.getMessage();
        ResourceContext context = source.getContext();
        Long reportId = context.getReportId();
        Long sceneId = context.getSceneId();
        // 状态 更新 失败状态
        clearCache(context.getResourceId(), sceneId);
        cloudReportService.updateReportFeatures(reportId, ReportConstants.FINISH_STATUS, ReportConstants.PRESSURE_MSG, message);
        reportDao.updateReportEndTime(reportId, new Date());
        SceneManageEntity sceneManage = new SceneManageEntity() {{
            setId(sceneId);
            setLastPtTime(new Date());
            setUpdateTime(new Date());
            setStatus(SceneManageStatusEnum.FAILED.getValue());
        }};
        // --->update 失败状态
        sceneManageDAO.getBaseMapper().updateById(sceneManage);
        pressureTaskDAO.updateStatus(context.getTaskId(), PressureTaskStateEnum.INACTIVE, null);
        unLockResource(context.getResourceId());
        releaseFlow(context.getReportId());
    }


    private void failed(ResourceContext context) {
        String resourceId = context.getResourceId();
        String resourceKey = PressureStartCache.getResourceKey(resourceId);
        if (redisClientUtils.hasKey(resourceKey)) {
            redisClientUtils.hmset(resourceKey, PressureStartCache.CHECK_STATUS, CheckStatus.FAIL.ordinal());
        }
        redisClientUtils.del(PressureStartCache.getResourcePodSuccessKey(resourceId),
            PressureStartCache.getPodStartFirstKey(resourceId),
            PressureStartCache.getPodHeartbeatKey(resourceId));
        redisClientUtils.setString(PressureStartCache.getErrorMessageKey(resourceId), context.getMessage(), 15, TimeUnit.SECONDS);
    }

    private void clearCache(String resourceId, Long sceneId) {
        redisClientUtils.del(PressureStartCache.clearCacheKey(resourceId, sceneId).toArray(new String[0]));
    }

    private void fillContextIfNecessary(StartConditionCheckerContext context) {
        if (Objects.isNull(context.getSceneData())) {
            SceneManageQueryOptions options = new SceneManageQueryOptions();
            options.setIncludeBusinessActivity(true);
            options.setIncludeScript(true);
            SceneManageWrapperOutput sceneManage = cloudSceneManageService.getSceneManage(context.getSceneId(), options);
            context.setSceneData(sceneManage);
            context.setSceneId(sceneManage.getId());
            context.setTenantId(sceneManage.getTenantId());
        }
        if (Objects.isNull(context.getInput())) {
            SceneTaskStartInput input = new SceneTaskStartInput();
            input.setOperateId(WebPluginUtils.traceUserId());
            context.setInput(input);
        }
    }

    private void initTaskAndReportIfNecessary(StartConditionCheckerContext context) {
        if (!context.isInitTaskAndReport()) {
            SceneManageWrapperOutput sceneData = context.getSceneData();
            SceneTaskStartInput input = context.getInput();
            PressureTaskEntity pressureTask = cloudSceneTaskService.initPressureTask(sceneData, input);
            ReportEntity report = cloudSceneTaskService.initReport(sceneData, input, pressureTask);
            context.setTaskId(pressureTask.getId());
            context.setReportId(report.getId());
        }
        context.setInitTaskAndReport(true);
    }

    private void associateResource(StartConditionCheckerContext context) {
        PressureTaskEntity entity = new PressureTaskEntity();
        entity.setId(context.getTaskId());
        entity.setResourceId(context.getResourceId());
        entity.setGmtUpdate(new Date());
        pressureTaskDAO.updateById(entity);

        ReportUpdateParam param = new ReportUpdateParam();
        param.setId(context.getReportId());
        param.setResourceId(context.getResourceId());
        reportDao.updateReport(param);
    }

    private void unLockResource(String resourceId) {
        ResourceUnLockRequest request = new ResourceUnLockRequest();
        request.setResourceId(resourceId);
        cloudResourceApi.unLock(request);
    }

    private void stopJob(Long jobId) {
        PressureTaskStopReq req = new PressureTaskStopReq();
        req.setJobId(jobId);
        try {
            pressureTaskApi.stop(req);
        } catch (Exception e) {
            log.error("压力引擎停止异常", e);
        }
    }

    //释放流量
    private void releaseFlow(Long reportId) {
        if (Objects.nonNull(reportId)) {
            Event flowEvent = new Event();
            flowEvent.setEventName(PressureStartCache.UNLOCK_FLOW);
            TaskResult taskResult = new TaskResult();
            taskResult.setTaskId(reportId);
            flowEvent.setExt(taskResult);
            eventCenterTemplate.doEvents(flowEvent);
        }
    }

    @Override
    public String type() {
        return "resource";
    }

    @Override
    public int getOrder() {
        return 4;
    }
}
