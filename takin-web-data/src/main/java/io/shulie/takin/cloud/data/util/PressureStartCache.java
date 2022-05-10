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
    public static final String JOB_ID = "job_id";

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
        return String.format("pressure:resource:pod:success:%s", resourceId);
    }

    // 启动成功的jmeter实例名称存入该key,stop 时会移除
    public static String getResourceJmeterSuccessKey(String resourceId) {
        return String.format("pressure:resource:jmeter:success:%s", resourceId);
    }

    // 场景启动锁，保证场景不能同时启动多次
    public static String getSceneResourceLockingKey(Long sceneId) {
        return String.format("pressure:scene:locking:%s", sceneId);
    }

    // 场景Id为key保存的相关信息
    public static String getSceneResourceKey(Long sceneId) {
        return String.format("pressure:scene:resource:%s", sceneId);
    }

    // 场景提前取消压测标识
    public static String getScenePreStopKey(Long sceneId, String resourceId) {
        return String.format("pressure:scene:pre_stop:%s:%s", sceneId, resourceId);
    }

    // 压测停止标识：jmeter停止、jmeter异常、pod异常、jmeter启动超时、pod启动超时、jmeter心跳超时、pod心跳超时
    public static String getStopFlag(Long sceneId, String resourceId) {
        return String.format("pressure:scene:stop:%s:%s", sceneId, resourceId);
    }

    // jmeter心跳时间
    public static String getJmeterHeartbeatKey(String resourceId) {
        return String.format("pressure:resource:jmeter:heartbeat:%s", resourceId);
    }

    // pod心跳时间
    public static String getPodHeartbeatKey(String resourceId) {
        return String.format("pressure:resource:pod:heartbeat:%s", resourceId);
    }

    // 第一个启动的pod id
    public static String getPodStartFirstKey(String resourceId) {
        return String.format("pressure:resource:pod:first:start:%s", resourceId);
    }

    // 第一个启动的jmeter id
    public static String getJmeterStartFirstKey(String resourceId) {
        return String.format("pressure:resource:jmeter:first:start:%s", resourceId);
    }

    // 第一个异常的jmeter id
    public static String getPodErrorFirstKey(String resourceId) {
        return String.format("pressure:resource:pod:first:error:%s", resourceId);
    }

    // 第一个停止的pod id
    public static String getPodStopFirstKey(String resourceId) {
        return String.format("pressure:resource:pod:first:stop:%s", resourceId);
    }

    // 第一个异常的jmeter id
    public static String getJmeterErrorFirstKey(String resourceId) {
        return String.format("pressure:resource:jmeter:first:error:%s", resourceId);
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

    public static String getFinishReportStepKey(String resourceId) {
        return String.format("pressure:resource:finish_step:%s", resourceId);
    }

    // 释放流量key
    public static String getReleaseFlowKey(Long reportId) {
        return String.format("pressure:resource:flow:unlock:%s", reportId);
    }

    // 锁定流量key
    public static String getLockFlowKey(Long reportId) {
        return String.format("pressure:resource:flow:lock:%s", reportId);
    }

    // 报告相关数据补充key
    public static String getInitActivityKey(Long reportId) {
        return String.format("pressure:resource:activity:%s", reportId);
    }

    public static List<String> clearCacheKey(String resourceId, Long sceneId) {
        return Arrays.asList(PressureStartCache.getResourceKey(resourceId),
            PressureStartCache.getResourcePodSuccessKey(resourceId),
            PressureStartCache.getPodHeartbeatKey(resourceId),
            PressureStartCache.getPodStartFirstKey(resourceId),
            PressureStartCache.getResourceJmeterSuccessKey(resourceId),
            PressureStartCache.getJmeterHeartbeatKey(resourceId),
            PressureStartCache.getJmeterStartFirstKey(resourceId),
            PressureStartCache.getSceneResourceLockingKey(sceneId),
            PressureStartCache.getSceneResourceKey(sceneId),
            PressureStartCache.getScenePreStopKey(sceneId, resourceId),
            PressureStartCache.getSceneResourceKey(sceneId),
            PressureStartCache.getFlowDebugKey(sceneId),
            PressureStartCache.getInspectKey(sceneId),
            PressureStartCache.getTryRunKey(sceneId),
            PressureStartCache.getErrorMessageKey(resourceId),
            RedisClientUtils.getLockPrefix(PressureStartCache.getPodStopFirstKey(resourceId)),
            RedisClientUtils.getLockPrefix(PressureStartCache.getJmeterErrorFirstKey(resourceId)),
            RedisClientUtils.getLockPrefix(PressureStartCache.getPodErrorFirstKey(resourceId)),
            RedisClientUtils.getLockPrefix(PressureStartCache.getStopFlag(sceneId, resourceId)),
            RedisClientUtils.getLockPrefix(PressureStartCache.getSceneResourceLockingKey(sceneId)
            )
        );
    }
}
