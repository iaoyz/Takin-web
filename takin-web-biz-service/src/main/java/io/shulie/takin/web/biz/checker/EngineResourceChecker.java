package io.shulie.takin.web.biz.checker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.adapter.api.entrypoint.check.CloudCheckApi;
import io.shulie.takin.adapter.api.entrypoint.resource.CloudResourceApi;
import io.shulie.takin.adapter.api.model.request.check.ResourceCheckRequest;
import io.shulie.takin.adapter.api.model.request.resource.ResourceLockRequest;
import io.shulie.takin.adapter.api.model.response.resource.ResourceLockResponse;
import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.config.AppConfig;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
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
import io.shulie.takin.cloud.data.model.mysql.PressureTaskEntity;
import io.shulie.takin.cloud.data.model.mysql.ReportEntity;
import io.shulie.takin.cloud.ext.content.enginecall.StrategyConfigExt;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.eventcenter.annotation.IntrestFor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import static io.shulie.takin.web.biz.checker.CompositeStartConditionChecker.CHECK_FAIL_EVENT;
import static io.shulie.takin.web.biz.checker.CompositeStartConditionChecker.CHECK_SUCCESS_EVENT;
import static io.shulie.takin.web.biz.checker.CompositeStartConditionChecker.LACK_POD_RESOURCE;

@Slf4j
@Component
public class EngineResourceChecker extends AbstractIndicators implements StartConditionChecker {

    public static final String RESOURCE_STATUS = "status";
    public static final String RESOURCE_POD_NUM = "podNum";
    public static final String RESOURCE_MESSAGE = "message";
    public static final String RESOURCE_END_TIME = "endTime";
    public static final String TENANT_ID = "tenant_id";
    public static final String ENV_CODE = "env_code";
    public static final String SCENE_ID = "scene_id";
    public static final String REPORT_ID = "report_id";
    public static final String PRESSURE_TASK_ID = "pressure_task_id";
    public static final String JMETER_STARTED = "jmeter_started";
    public static final String JMETER_STOP = "jmeter_stopped";
    public static final String HEARTBEAT_TIME = "heartbeat_time";

    @Resource
    private StrategyConfigService strategyConfigService;

    @Resource
    private CloudResourceApi cloudResourceApi;

    @Resource
    private CloudCheckApi cloudCheckApi;

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
    @Qualifier("redisTemplate")
    private RedisTemplate redisTemplate;

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
            request.setCpu(config.getCpuNum());
            request.setMemory(config.getMemorySize());
            request.setPod(sceneData.getIpNum());
            //cloudCheckApi.checkResources(request);

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
        String statusKey = getResourceKey(resourceId);
        Object redisStatus = redisClientUtils.hmget(statusKey, RESOURCE_STATUS);
        if (redisStatus == null) {
            return new CheckResult(type(), CheckStatus.FAIL.ordinal(), resourceId, "未找到启动中的任务");
        }
        int status = Integer.parseInt(String.valueOf(redisStatus));
        String message = String.valueOf(redisClientUtils.hmget(statusKey, RESOURCE_MESSAGE));
        return new CheckResult(type(), status, resourceId, message);
    }

    private String lockResource(StartConditionCheckerContext context) {
        SceneManageWrapperOutput sceneData = context.getSceneData();
        String sceneRunningKey = getSceneResourceLockingKey(sceneData.getId());
        if (!Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(sceneRunningKey, 1))) {
            throw new IllegalStateException("该场景正在启动压测中");
        }
        try {
            StrategyConfigExt strategy = sceneData.getStrategy();
            ResourceLockRequest request = new ResourceLockRequest();
            request.setCpu(strategy.getCpuNum());
            request.setMemory(strategy.getMemorySize());
            request.setPod(sceneData.getIpNum());
            request.setCallBackUrl(DataUtils.mergeUrl(appConfig.getConsole(),
                EntrypointUrl.join(EntrypointUrl.MODULE_ENGINE_CALLBACK,
                    EntrypointUrl.METHOD_ENGINE_CALLBACK_TASK_RESULT_NOTIFY)));
            //ResourceLockResponse lockResponse = cloudResourceApi.lockResource(request);
            //String resourceId = lockResponse.getResourceId();
            String resourceId = "test-resource";
            return resourceId;
        } catch (Exception e) {
            redisTemplate.delete(sceneRunningKey);
            throw e;
        }
    }

    private void refresh(String resourceId) {
        Executors.newScheduledThreadPool(1).schedule(() -> {
            Event event = new Event();
            ResourceContext context = new ResourceContext();
            context.setResourceId(resourceId);
            event.setExt(context);
            event.setEventName(CHECK_SUCCESS_EVENT);
            callStartSuccess(event);
        }, 15, TimeUnit.SECONDS);
    }

    private void afterLocking(StartConditionCheckerContext context) {
        SceneManageWrapperOutput sceneData = context.getSceneData();
        cloudSceneManageService.updateSceneLifeCycle(
            UpdateStatusBean.build(context.getSceneId(), context.getReportId(), sceneData.getTenantId())
                .checkEnum(SceneManageStatusEnum.WAIT, SceneManageStatusEnum.FAILED, SceneManageStatusEnum.STOP,
                    SceneManageStatusEnum.FORCE_STOP)
                .updateEnum(SceneManageStatusEnum.STARTING).build());
        initCache(context);
    }

    private void initCache(StartConditionCheckerContext context) {
        SceneManageWrapperOutput sceneData = context.getSceneData();
        // 缓存资源锁定状态和pod数
        Map<String, Object> param = new HashMap<>(32);
        param.put(RESOURCE_STATUS, CheckStatus.PENDING.ordinal());
        param.put(RESOURCE_POD_NUM, sceneData.getIpNum());
        param.put(RESOURCE_MESSAGE, "");
        param.put(RESOURCE_END_TIME, System.currentTimeMillis() + pressureNodeStartExpireTime * 1000);
        param.put(TENANT_ID, sceneData.getTenantId());
        param.put(ENV_CODE, sceneData.getEnvCode());
        param.put(SCENE_ID, String.valueOf(sceneData.getId()));
        param.put(JMETER_STARTED, 0);
        param.put(JMETER_STOP, 0);
        param.put(HEARTBEAT_TIME, 0);
        param.put(PRESSURE_TASK_ID, context.getTaskId());
        param.put(REPORT_ID, context.getReportId());
        redisClientUtils.hmset(getResourceKey(context.getResourceId()), param);
    }

    private StrategyConfigExt getStrategy() {
        StrategyConfigExt config = strategyConfigService.getCurrentStrategyConfig();
        if (config == null) {
            throw new RuntimeException("未配置策略");
        }
        return config;
    }

    public static List<String> clearCacheKey(String resourceId) {
        return Arrays.asList(getResourceKey(resourceId), getResourcePodKey(resourceId));
    }

    public static String getResourceKey(String resourceId) {
        return String.format("pressure:resource:locking:%s", resourceId);
    }

    // 启动成功的pod实例名称存入该key
    public static String getResourcePodKey(String resourceId) {
        return String.format("pressure:resource:pod:%s", resourceId);
    }

    public static String getSceneResourceLockingKey(Long sceneId) {
        return "scene:pressure:resource:locking:" + sceneId;
    }

    @IntrestFor(event = CHECK_FAIL_EVENT)
    public void expireCache(Event event) {
        ResourceContext context = (ResourceContext)event.getExt();
        String resourceId = context.getResourceId();
        if (StringUtils.isNotBlank(resourceId)) {
            redisClientUtils.expire(getResourceKey(resourceId), 15);
            redisClientUtils.expire(getResourcePodKey(resourceId), 15);
            redisClientUtils.delete(EngineResourceChecker.getSceneResourceLockingKey(context.getSceneId()));
        }
    }

    @IntrestFor(event = CHECK_SUCCESS_EVENT)
    public void callStartSuccess(Event event) {
        ResourceContext ext = (ResourceContext)event.getExt();
        Map<String, Object> param = new HashMap<>(4);
        param.put(EngineResourceChecker.RESOURCE_STATUS, CheckStatus.SUCCESS.ordinal());
        redisClientUtils.hmset(EngineResourceChecker.getResourceKey(ext.getResourceId()), param);
    }


    @IntrestFor(event = LACK_POD_RESOURCE)
    public void lackPodResource(Event event) {
        log.info("pod resource不足");
        ResourceContext context = (ResourceContext)event.getExt();
        Map<String, Object> param = new HashMap<>(4);
        param.put(EngineResourceChecker.RESOURCE_STATUS, CheckStatus.FAIL.ordinal());
        param.put(EngineResourceChecker.RESOURCE_MESSAGE, "压力机资源不足");
        redisClientUtils.hmset(EngineResourceChecker.getResourceKey(context.getResourceId()), param);

        Event failEvent = new Event();
        failEvent.setEventName(CHECK_FAIL_EVENT);
        failEvent.setExt(context);
        eventCenterTemplate.doEvents(event);
    }

    @IntrestFor(event = "finished")
    public void clearResourceCache(Event event) {
        log.info("删除resource缓存");
        TaskResult taskResult = (TaskResult)event.getExt();
        clearCache(taskResult.getResourceId());
    }

    private void clearCache(String resourceId) {
        redisClientUtils.del(clearCacheKey(resourceId).toArray(new String[0]));
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

    @Override
    public String type() {
        return "resource";
    }

    @Override
    public int getOrder() {
        return 4;
    }
}
