package io.shulie.takin.cloud.common.constants;

/**
 * @author xr.l
 * @date 2021-05-10
 */
public class SceneTaskRedisConstants {
    /**
     * 压力节点 问题
     */
    public static final String PRESSURE_NODE_ERROR_KEY = "pressure_node_error_key:";

    /**
     * 场景启动key
     */
    public static final String SCENE_TASK_RUN_KEY = "scene_run_task:";

    /**
     * 场景启动状态key
     */
    public static final String SCENE_RUN_TASK_STATUS_KEY = "scene_run_status";

    /**
     * 场景启动错误信息key
     */
    public static final String SCENE_RUN_TASK_ERROR = "scene_run_error";

    /**
     * 压力节点启动 错误
     */
    public static final String PRESSURE_NODE_START_ERROR = "pressure_node_start_error";

    /**
     * 请求流量明细上传记录KEY
     */
    public static final String PRESSURE_TEST_LOG_UPLOAD_RECORD = "pressure_test_log_upload_record";

    /**
     * 引擎启动回传事件文件名KEY
     */
    public static final String CURRENT_PTL_FILE_NAME_SYSTEM_PROP_KEY = "__CURRENT_PTL_FILE_NAME__";

}
