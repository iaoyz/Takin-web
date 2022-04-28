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
    public final static String BASIC = "";

    /**
     * 模块 - 通用接口
     */
    public final static String MODULE_COMMON = "common";
    public final static String METHOD_COMMON_CONFIG = "getCloudConfigurationInfos";


    /**
     * 模块 - 引擎
     */
    private final static String MODULE_ENGINE = "engine";
    /**
     * 模块 - 引擎回调
     */
    public final static String MODULE_ENGINE_CALLBACK = MODULE_ENGINE + "/callback";
    // 改的的话需要调整ee中的agentInterceptor
    public final static String METHOD_ENGINE_CALLBACK_TASK_RESULT_NOTIFY = "";

    /**
     * 模块 - 资源
     */
    public final static String MODULE_RESOURCE = "resource";
    public final static String METHOD_RESOURCE_MACHINE = "machine/task";
    public final static String METHOD_RESOURCE_LOCK = "lock?callbackUrl=%s";
    public final static String METHOD_RESOURCE_UNLOCK = "unlock";
    public final static String MODULE_RESOURCE_CHECK = "check";

    /**
     * 模块 - 任务
     */
    public final static String MODULE_RRESSURE = "job";
    public final static String METHOD_RRESSURE_START = "start";
    public final static String METHOD_RRESSURE_STOP = "stop";
    public final static String METHOD_RRESSURE_MODIFY = "config/modify";
    public final static String METHOD_RRESSURE_PARAMS = "config/get";

    /**
     * 调度器
     */
    public final static String MODULE_WATCHMAN = "watchman";
    public final static String MATHOD_WATCHMAN_STATUS = "status";
}
