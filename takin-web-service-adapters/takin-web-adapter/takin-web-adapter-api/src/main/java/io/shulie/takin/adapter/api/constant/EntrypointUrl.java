package io.shulie.takin.adapter.api.constant;

/**
 * 入口配置
 *
 * @author 张天赐
 */
@SuppressWarnings("SpellCheckingInspection")
public class EntrypointUrl {
    public static String join(String... path) {
        return BASIC + "/"
            + String.join("/", path);
    }

    /**
     * 基础路径
     */
    public final static String BASIC = "/api";

    /**
     * 模块 - 通用接口
     */
    public final static String MODULE_COMMON = "common";
    public final static String METHOD_COMMON_CONFIG = "getCloudConfigurationInfos";

    /**
     * 模块 - 场景
     */
    private final static String MODULE_SCENE = "scene";

    /**
     * 模块 - 场景任务
     */
    public final static String MODULE_SCENE_TASK = MODULE_SCENE + "/task";
    public final static String METHOD_SCENE_TASK_START = "start";
    public final static String METHOD_SCENE_TASK_STOP = "stop";
    public final static String METHOD_SCENE_TASK_BOLT_STOP = "bolt/stop";
    public final static String METHOD_SCENE_TASK_CHECK_TASK = "checkStartStatus";
    public final static String METHOD_SCENE_TASK_UPDATE_TPS = "updateSceneTaskTps";
    public final static String METHOD_SCENE_TASK_ADJUST_TPS = "queryAdjustTaskTps";
    public final static String METHOD_SCENE_TASK_START_FLOW_DEBUG = "startFlowDebugTask";
    public final static String METHOD_SCENE_TASK_START_INSPECT = "startInspectTask";
    public final static String METHOD_SCENE_TASK_STOP_INSPECT = "stopInspectTask";
    public final static String METHOD_SCENE_TASK_FORCE_STOP_INSPECT = "forceStopTask";
    public final static String METHOD_SCENE_TASK_START_TRY_RUN = "startTryRunTask";
    public final static String METHOD_SCENE_TASK_CHECK_STATUS = "checkTaskStatus";
    public final static String METHOD_SCENE_TASK_CHECK_JOB_STATUS = "checkJobStatus";
    public final static String METHOD_SCENE_TASK_START_PRE_CHECK = "preCheck";
    public final static String METHOD_SCENE_TASK_CALL_BACK_TO_WRITE_BALANCE = "writeBalance";

    /**
     * 模块 - 资源
     */
    public final static String MODULE_RESOURCE = "resource";
    public final static String METHOD_RESOURCE_PHYSICAL = "physical/all";
    public final static String METHOD_RESOURCE_MACHINE = "machine/task";
    public final static String METHOD_RESOURCE_LOCK = "lock";
    public final static String METHOD_RESOURCE_UNLOCK = "unlock";

    /**
     * 模块 - 校验
     */
    public final static String MODULE_CHECK = "check";
    public final static String METHOD_CHECK_FILE = "files";
    public final static String METHOD_CHECK_RESOURCES = "resources";
    public final static String METHOD_CHECK_ENV = "env";

    /**
     * 模块 - 压测
     */
    public final static String MODULE_RRESSURE = "pressure";
    public final static String METHOD_RRESSURE_START = "task/start";
    public final static String METHOD_RRESSURE_STOP = "task/stop";
    public final static String METHOD_RRESSURE_MODIFY = "task/modify";
}
