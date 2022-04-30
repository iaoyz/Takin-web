package io.shulie.takin.web.biz.checker;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.adapter.api.entrypoint.resource.CloudResourceApi;
import io.shulie.takin.adapter.api.model.request.resource.ResourceCheckRequest;
import io.shulie.takin.adapter.api.model.request.resource.ResourceLockRequest;
import io.shulie.takin.adapter.api.model.request.resource.ResourceUnLockRequest;
import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.config.AppConfig;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.service.async.CloudAsyncService;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneTaskService;
import io.shulie.takin.cloud.biz.service.strategy.StrategyConfigService;
import io.shulie.takin.cloud.biz.utils.DataUtils;
import io.shulie.takin.cloud.common.bean.scenemanage.SceneManageQueryOptions;
import io.shulie.takin.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.takin.cloud.common.bean.task.TaskResult;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.dao.report.ReportDao;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskDAO;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskEntity;
import io.shulie.takin.cloud.data.model.mysql.ReportEntity;
import io.shulie.takin.cloud.data.param.report.ReportUpdateParam;
import io.shulie.takin.cloud.ext.content.enginecall.StrategyConfigExt;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.eventcenter.annotation.IntrestFor;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class EngineResourceChecker extends AbstractIndicators implements StartConditionChecker {

    @Resource
    private StrategyConfigService strategyConfigService;

    @Resource
    private CloudResourceApi cloudResourceApi;

    @Resource
    private CloudSceneManageService cloudSceneManageService;

    @Resource
    private CloudSceneTaskService cloudSceneTaskService;

    @Resource
    private RedisClientUtils redisClientUtils;

    @Value("${pressure.node.start.expireTime:30}")
    private Integer pressureNodeStartExpireTime;

    @Resource
    private AppConfig appConfig;

    @Resource
    private CloudAsyncService cloudAsyncService;

    @Resource
    private PressureTaskDAO pressureTaskDAO;

    @Resource
    private ReportDao reportDao;

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
            cloudResourceApi.check(request);

            // 锁定资源：异步接口，每个pod启动成功都会回调一次回调接口
            String resourceId = lockResource(context);
            context.setResourceId(resourceId);
            afterLocking(context);
            refresh(resourceId);
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
        Object redisStatus = redisClientUtils.hmget(statusKey, PressureStartCache.RESOURCE_STATUS);
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
        request.setCallbackUrl(DataUtils.mergeUrl(appConfig.getConsole(),
            EntrypointUrl.join(EntrypointUrl.MODULE_ENGINE_CALLBACK,
                EntrypointUrl.METHOD_ENGINE_CALLBACK_TASK_RESULT_NOTIFY)));
        return cloudResourceApi.lock(request);
    }

    private void refresh(String resourceId) {
        Executors.newScheduledThreadPool(1).schedule(() -> {
            Event event = new Event();
            ResourceContext context = new ResourceContext();
            context.setResourceId(resourceId);
            event.setExt(context);
            event.setEventName(PressureStartCache.CHECK_SUCCESS_EVENT);
            callStartSuccess(event);
        }, 15, TimeUnit.SECONDS);
    }

    private void afterLocking(StartConditionCheckerContext context) {
        associateResource(context);
        SceneManageWrapperOutput sceneData = context.getSceneData();
        cloudSceneManageService.updateSceneLifeCycle(
            UpdateStatusBean.build(context.getSceneId(), context.getReportId(), sceneData.getTenantId())
                .checkEnum(SceneManageStatusEnum.WAIT, SceneManageStatusEnum.FAILED, SceneManageStatusEnum.STOP,
                    SceneManageStatusEnum.FORCE_STOP)
                .updateEnum(SceneManageStatusEnum.RESOURCE_LOCKING).build());
        initCache(context);
        // cloudAsyncService.checkPodStartedTask(context);
    }

    private void initCache(StartConditionCheckerContext context) {
        SceneManageWrapperOutput sceneData = context.getSceneData();
        // 缓存资源锁定状态和pod数
        Map<String, Object> param = new HashMap<>(32);
        param.put(PressureStartCache.RESOURCE_STATUS, CheckStatus.PENDING.ordinal());
        param.put(PressureStartCache.RESOURCE_POD_NUM, sceneData.getIpNum());
        param.put(PressureStartCache.RESOURCE_END_TIME, System.currentTimeMillis() + pressureNodeStartExpireTime * 1000);
        param.put(PressureStartCache.TENANT_ID, sceneData.getTenantId());
        param.put(PressureStartCache.ENV_CODE, sceneData.getEnvCode());
        param.put(PressureStartCache.SCENE_ID, String.valueOf(sceneData.getId()));
        param.put(PressureStartCache.JMETER_STOP, 0);
        param.put(PressureStartCache.HEARTBEAT_TIME, 0);
        param.put(PressureStartCache.PRESSURE_TASK_ID, context.getTaskId());
        param.put(PressureStartCache.REPORT_ID, context.getReportId());
        param.put(PressureStartCache.UNIQUE_KEY, context.getUniqueKey());
        redisClientUtils.hmset(PressureStartCache.getResourceKey(context.getResourceId()), param);
        redisClientUtils.hmset(PressureStartCache.getSceneResourceKey(sceneData.getId()),
            PressureStartCache.RESOURCE_ID, context.getResourceId());
    }

    private StrategyConfigExt getStrategy() {
        StrategyConfigExt config = strategyConfigService.getCurrentStrategyConfig();
        if (config == null) {
            throw new RuntimeException("未配置策略");
        }
        return config;
    }

    public static List<String> clearCacheKey(String resourceId, Long sceneId) {
        return Arrays.asList(PressureStartCache.getResourceKey(resourceId), PressureStartCache.getResourcePodKey(resourceId),
            PressureStartCache.getResourceJmeterKey(resourceId), PressureStartCache.getSceneResourceKey(sceneId));
    }

    @IntrestFor(event = PressureStartCache.CHECK_FAIL_EVENT, order = 1)
    public void expireCache(Event event) {
        ResourceContext context = (ResourceContext)event.getExt();
        String resourceId = context.getResourceId();
        if (StringUtils.isNotBlank(resourceId)) {
            redisClientUtils.expire(PressureStartCache.getResourceKey(resourceId), 30);
            redisClientUtils.expire(PressureStartCache.getResourcePodKey(resourceId), 30);
            ResourceUnLockRequest request = new ResourceUnLockRequest();
            request.setResourceId(resourceId);
            cloudResourceApi.unLock(request);
        }
    }

    @IntrestFor(event = PressureStartCache.CHECK_SUCCESS_EVENT, order = 0)
    public void callStartSuccess(Event event) {
        ResourceContext ext = (ResourceContext)event.getExt();
        String resourceId = ext.getResourceId();
        String resourceKey = PressureStartCache.getResourceKey(resourceId);
        if (redisClientUtils.hasKey(resourceKey)) {
            redisClientUtils.hmset(resourceKey, PressureStartCache.RESOURCE_STATUS, CheckStatus.SUCCESS.ordinal());
        }
        redisClientUtils.delete(PressureStartCache.getErrorMessageKey(resourceId));
    }

    @IntrestFor(event = PressureStartCache.LACK_POD_RESOURCE_EVENT)
    public void lackPodResource(Event event) {
        log.info("pod resource不足");
        ResourceContext context = (ResourceContext)event.getExt();
        failed(context.getResourceId(), context.getMessage());

        Event failEvent = new Event();
        failEvent.setEventName(PressureStartCache.CHECK_FAIL_EVENT);
        failEvent.setExt(context);
        eventCenterTemplate.doEvents(failEvent);
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
            redisClientUtils.delete(PressureStartCache.getResourceKey(resource));
            failed(resource, "取消压测");
            clearCache(resource, sceneId);
            ResourceUnLockRequest request = new ResourceUnLockRequest();
            request.setResourceId(resource);
            cloudResourceApi.unLock(request);
        }
    }

    @IntrestFor(event = "finished")
    public void clearResourceCache(Event event) {
        log.info("删除resource缓存");
        TaskResult taskResult = (TaskResult)event.getExt();
        clearCache(taskResult.getResourceId(), taskResult.getSceneId());
    }

    private void failed(String resourceId, String message) {
        String resourceKey = PressureStartCache.getResourceKey(resourceId);
        if (redisClientUtils.hasKey(resourceKey)) {
            redisClientUtils.hmset(resourceKey, PressureStartCache.RESOURCE_STATUS, CheckStatus.FAIL.ordinal());
        }
        redisClientUtils.setString(PressureStartCache.getErrorMessageKey(resourceId), message, 30, TimeUnit.SECONDS);
    }

    private void clearCache(String resourceId, Long sceneId) {
        redisClientUtils.del(clearCacheKey(resourceId, sceneId).toArray(new String[0]));
    }

    private void fillContextIfNecessary(StartConditionCheckerContext context) {
        if (context.getSceneData() == null) {
            SceneManageQueryOptions options = new SceneManageQueryOptions();
            options.setIncludeBusinessActivity(true);
            options.setIncludeScript(true);
            SceneManageWrapperOutput sceneManage = cloudSceneManageService.getSceneManage(context.getSceneId(), options);
            context.setSceneData(sceneManage);
            context.setSceneId(sceneManage.getId());
            context.setTenantId(sceneManage.getTenantId());
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

    @Override
    public String type() {
        return "resource";
    }

    @Override
    public int getOrder() {
        return 4;
    }
}
