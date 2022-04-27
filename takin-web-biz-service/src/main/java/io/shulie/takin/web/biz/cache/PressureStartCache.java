package io.shulie.takin.web.biz.cache;

public abstract class PressureStartCache {

    // 缓存
    public static final String RESOURCE_STATUS = "status";
    public static final String RESOURCE_POD_NUM = "podNum";
    public static final String RESOURCE_END_TIME = "endTime";
    public static final String TENANT_ID = "tenant_id";
    public static final String ENV_CODE = "env_code";
    public static final String SCENE_ID = "scene_id";
    public static final String REPORT_ID = "report_id";
    public static final String PRESSURE_TASK_ID = "pressure_task_id";
    public static final String JMETER_STARTED = "jmeter_started";
    public static final String JMETER_STOP = "jmeter_stopped";
    public static final String HEARTBEAT_TIME = "heartbeat_time";
    public static final String RESOURCE_ID = "resourceId";
    public static final String UNIQUE_KEY = "unique_key";

    // 事件
    public static final String CHECK_FAIL_EVENT = "check_failed";
    public static final String CHECK_SUCCESS_EVENT = "check_success";
    public static final String LACK_POD_RESOURCE_EVENT = "lack_pod_resource";
    public static final String PRE_STOP_EVENT = "pre_stop";
    public static final String UNLOCK_FLOW = "unlock_flow";

    public static String getResourceKey(String resourceId) {
        return String.format("pressure:resource:locking:%s", resourceId);
    }

    public static String getErrorMessageKey(String resourceId) {
        return String.format("pressure:resource:message:%s", resourceId);
    }

    // 启动成功的pod实例名称存入该key
    public static String getResourcePodKey(String resourceId) {
        return String.format("pressure:resource:pod:%s", resourceId);
    }

    // 启动成功的jmeter实例名称存入该key
    public static String getResourceJmeterKey(String resourceId) {
        return String.format("pressure:resource:jmeter:%s", resourceId);
    }

    public static String getSceneResourceLockingKey(Long sceneId) {
        return String.format("pressure:scene:locking:%s", sceneId);
    }

    public static String getSceneResourceKey(Long sceneId) {
        return String.format("pressure:scene:resource:%s", sceneId);
    }
}
