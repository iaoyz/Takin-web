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
     * 模块 - 引擎
     */
    private final static String MODULE_ENGINE = "engine";
    /**
     * 模块 - 引擎回调
     */
    public final static String MODULE_ENGINE_CALLBACK = MODULE_ENGINE + "/callback";
    public final static String METHOD_ENGINE_CALLBACK_TASK_RESULT_NOTIFY = "";

    /**
     * 模块 - 资源
     */
    public final static String MODULE_RESOURCE = "resource";
    public final static String METHOD_RESOURCE_PHYSICAL = "physical/all";
    public final static String METHOD_RESOURCE_MACHINE = "machine/task";
    public final static String METHOD_RESOURCE_LOCK = "lock";

    /**
     * 模块 - 校验
     */
    public final static String MODULE_CHECK = "check";
    public final static String METHOD_CHECK_RESOURCES = "resources";
    public final static String METHOD_CHECK_ENV = "env";

    /**
     * 模块 - 压测
     */
    public final static String MODULE_RRESSURE = "pressure";
    public final static String METHOD_RRESSURE_START = "task/start";
    public final static String METHOD_RRESSURE_STOP = "task/stop";
}
