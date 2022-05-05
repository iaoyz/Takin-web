package io.shulie.takin.cloud.data.util;

import java.util.Arrays;
import java.util.List;

import io.shulie.takin.cloud.common.redis.RedisClientUtils;

public abstract class PressureStartCache {

    // 缓存
    public static final String CHECK_STATUS = "check_status";
    public static final String RESOURCE_POD_NUM = "pod_num";
    public static final String TENANT_ID = "tenant_id";
    public static final String ENV_CODE = "env_code";
    public static final String SCENE_ID = "scene_id";
    public static final String REPORT_ID = "report_id";
    public static final String TASK_ID = "task_id";
    public static final String RESOURCE_ID = "resource_Id";
    public static final String UNIQUE_KEY = "unique_key";
    public static final String JOB_ID = "pressure_task_id";

    // 事件
    public static final String CHECK_FAIL_EVENT = "check_failed";
    public static final String CHECK_SUCCESS_EVENT = "check_success";
    public static final String PRE_STOP_EVENT = "pre_stop";
    public static final String UNLOCK_FLOW = "unlock_flow";
    public static final String START_FAILED = "start_fail";
    public static final String PRESSURE_END = "pressure_end";

    public static String getResourceKey(String resourceId) {
        return String.format("pressure:resource:locking:%s", resourceId);
    }

    public static String getErrorMessageKey(String resourceId) {
        return String.format("pressure:resource:message:%s", resourceId);
    }

    // 启动成功的pod实例名称存入该key
    public static String getResourcePodSuccessKey(String resourceId) {
        return String.format("pressure:resource:pod:%s", resourceId);
    }

    // 启动成功的jmeter实例名称存入该key
    public static String getResourceJmeterSuccessKey(String resourceId) {
        return String.format("pressure:resource:jmeter:success:%s", resourceId);
    }

    // 停止的jmeter实例名称存入该key
    public static String getResourceJmeterStopKey(String resourceId) {
        return String.format("pressure:resource:jmeter:stop:%s", resourceId);
    }

    public static String getSceneResourceLockingKey(Long sceneId) {
        return String.format("pressure:scene:locking:%s", sceneId);
    }

    public static String getSceneResourceKey(Long sceneId) {
        return String.format("pressure:scene:resource:%s", sceneId);
    }

    public static String getScenePreStopKey(Long sceneId, String resourceId) {
        return String.format("pressure:scene:pre_stop:%s:%s", sceneId, resourceId);
    }

    public static String getStopFlag(Long sceneId, String resourceId) {
        return String.format("pressure:scene:stop:%s:%s", sceneId, resourceId);
    }

    public static String getJmeterHeartbeatKey(Long sceneId) {
        return String.format("pressure:scene:heartbeat:jmeter:%s", sceneId);
    }

    public static String getPodHeartbeatKey(Long sceneId) {
        return String.format("pressure:scene:heartbeat:pod:%s", sceneId);
    }

    public static String getPodStartFirstKey(String resourceId) {
        return String.format("pressure:resource:start:pod:first:%s", resourceId);
    }

    public static String getJmeterStartFirstKey(String resourceId) {
        return String.format("pressure:resource:start:jmeter:first:%s", resourceId);
    }

    public static String getJmeterErrorFirstKey(String resourceId) {
        return String.format("pressure:resource:stop:jmeter:first:%s", resourceId);
    }

    public static String getFlowDebugKey(Long sceneId) {
        return String.format("pressure:scene:flow_debug:%s", sceneId);
    }

    public static String getInspectKey(Long sceneId) {
        return String.format("pressure:scene:inspect:%s", sceneId);
    }

    public static String getTryRunKey(Long sceneId) {
        return String.format("pressure:scene:try_run:%s", sceneId);
    }

    public static List<String> clearCacheKey(String resourceId, Long sceneId) {
        return Arrays.asList(PressureStartCache.getResourceKey(resourceId),
            PressureStartCache.getResourcePodSuccessKey(resourceId),
            PressureStartCache.getPodHeartbeatKey(sceneId),
            PressureStartCache.getPodStartFirstKey(resourceId),
            PressureStartCache.getResourceJmeterSuccessKey(resourceId),
            PressureStartCache.getResourceJmeterStopKey(resourceId),
            PressureStartCache.getJmeterHeartbeatKey(sceneId),
            PressureStartCache.getJmeterStartFirstKey(resourceId),
            PressureStartCache.getSceneResourceLockingKey(sceneId),
            PressureStartCache.getSceneResourceKey(sceneId),
            PressureStartCache.getScenePreStopKey(sceneId, resourceId),
            PressureStartCache.getStopFlag(sceneId, resourceId),
            PressureStartCache.getSceneResourceKey(sceneId),
            PressureStartCache.getFlowDebugKey(sceneId),
            PressureStartCache.getInspectKey(sceneId),
            PressureStartCache.getTryRunKey(sceneId),
            PressureStartCache.getErrorMessageKey(resourceId),
            RedisClientUtils.getLockPrefix(PressureStartCache.getJmeterErrorFirstKey(resourceId)),
            RedisClientUtils.getLockPrefix(PressureStartCache.getStopFlag(sceneId, resourceId)),
            RedisClientUtils.getLockPrefix(PressureStartCache.getSceneResourceLockingKey(sceneId))
        );
    }
}
