package io.shulie.takin.cloud.biz.checker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import io.shulie.takin.adapter.api.entrypoint.check.CloudCheckApi;
import io.shulie.takin.adapter.api.entrypoint.resource.CloudResourceApi;
import io.shulie.takin.adapter.api.model.request.check.ResourceCheckRequest;
import io.shulie.takin.adapter.api.model.request.resource.ResourceLockRequest;
import io.shulie.takin.adapter.api.model.response.resource.ResourceLockResponse;
import io.shulie.takin.cloud.biz.notify.PodStatusNotifyProcessor.PodStatus;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.biz.service.strategy.StrategyConfigService;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.common.utils.CloudPluginUtils;
import io.shulie.takin.cloud.ext.content.enginecall.StrategyConfigExt;
import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EngineResourceChecker implements CloudStartConditionChecker {

    public static final String RESOURCE_STATUS = "status";
    public static final String RESOURCE_POD_NUM = "podNum";
    public static final String RESOURCE_SUCCESS_NUM = "successNum";
    public static final String RESOURCE_MESSAGE = "message";
    public static final String RESOURCE_END_TIME = "endTime";

    @Resource
    private CloudCheckApi cloudCheckApi;

    @Resource
    private StrategyConfigService strategyConfigService;

    @Resource
    private CloudSceneManageService cloudSceneManageService;

    @Resource
    private CloudResourceApi cloudResourceApi;

    @Resource
    private RedisClientUtils redisClientUtils;

    @Value("${pressure.node.start.expireTime:30}")
    private Integer pressureNodeStartExpireTime;

    @Override
    public CheckResult preCheck(Long sceneId, String resourceId) throws TakinCloudException {
        if (StringUtils.isBlank(resourceId)) {
            return firstCheck(sceneId);
        }
        return getResourceStatus(resourceId);
    }

    private CheckResult firstCheck(Long sceneId) {
        try {
            SceneManageWrapperOutput sceneData = cloudSceneManageService.getSceneManage(sceneId, null);
            StrategyConfigExt config = getStrategy();
            sceneData.setStrategy(config);
            ResourceCheckRequest request = new ResourceCheckRequest();
            request.setCpu(config.getCpuNum());
            request.setMemory(config.getMemorySize());
            request.setPod(sceneData.getIpNum());
            cloudCheckApi.checkResources(request);
            // 锁定资源：异步接口，每个pod启动成功都会回调一次回调接口
            String resourceId = lockResource(sceneData);
            return getResourceStatus(resourceId);
        } catch (Exception e) {
            return CheckResult.fail(type(), e.getMessage());
        }
    }

    private CheckResult getResourceStatus(String resourceId) {
        String statusKey = getResourceStatusKey(resourceId);
        int status = Integer.parseInt(String.valueOf(redisClientUtils.hmget(statusKey, RESOURCE_STATUS)));
        String message = String.valueOf(redisClientUtils.hmget(statusKey, RESOURCE_STATUS));
        if (status == PodStatus.FAIL.ordinal()) {
            // 失败时，删除对应缓存
            redisClientUtils.del(clearCacheKey(resourceId).toArray(new String[0]));
        }
        return new CheckResult(type(), status, message);
    }

    private String lockResource(SceneManageWrapperOutput sceneData) {
        StrategyConfigExt strategy = sceneData.getStrategy();
        ResourceLockRequest request = new ResourceLockRequest();
        request.setCpu(strategy.getCpuNum());
        request.setMemory(strategy.getMemorySize());
        request.setPod(sceneData.getIpNum());
        ResourceLockResponse lockResponse = cloudResourceApi.lockResource(request);
        String resourceId = lockResponse.getResourceId();
        cache(sceneData, resourceId);
        return resourceId;
    }

    private void cache(SceneManageWrapperOutput sceneData, String resourceId) {
        // 缓存压测场景-资源Id关系
        redisClientUtils.hmset(getResourceMappingCacheKey(), String.valueOf(sceneData.getId()), resourceId);
        // 缓存资源和租户环境关系
        redisClientUtils.setString(getResourceTenantCacheKey(resourceId),
            CloudPluginUtils.getTenantId() + "$" + CloudPluginUtils.getEnvCode());
        // 缓存资源锁定状态和pod数
        Map<String, Object> param = new HashMap<>(8);
        param.put(RESOURCE_STATUS, CheckStatus.PENDING.ordinal());
        param.put(RESOURCE_POD_NUM, sceneData.getIpNum());
        param.put(RESOURCE_SUCCESS_NUM, 0);
        param.put(RESOURCE_MESSAGE, "");
        param.put(RESOURCE_END_TIME, System.currentTimeMillis() + pressureNodeStartExpireTime * 1000);
        redisClientUtils.hmset(getResourceStatusKey(resourceId), param);
    }

    private StrategyConfigExt getStrategy() {
        StrategyConfigExt config = strategyConfigService.getCurrentStrategyConfig();
        if (config == null) {
            throw new RuntimeException("未配置策略");
        }
        return config;
    }

    public static List<String> clearCacheKey(String resourceId) {
        return Arrays.asList(getResourceStatusKey(resourceId), getResourceMappingCacheKey(), getResourceTenantCacheKey(resourceId));
    }

    public static String getResourceStatusKey(String resourceId) {
        ContextExt context = CloudPluginUtils.getContext();
        return String.format("pressure:resource:locking:%s:%s:%s", context.getTenantId(), context.getEnvCode(), resourceId);
    }

    public static String getResourceMappingCacheKey() {
        ContextExt context = CloudPluginUtils.getContext();
        return String.format("pressure:resource:mapping:%s:%s", context.getTenantId(), context.getEnvCode());
    }

    public static String getResourceTenantCacheKey(String resourceId) {
        return String.format("pressure:resource:tenant:%s", resourceId);
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
