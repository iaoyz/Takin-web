package io.shulie.takin.cloud.common.enums;

public enum PressureTaskStateEnum {
    RESOURCE_LOCKING,
    /**
     * 检测完成
     */
    CHECK_SUCCESS,
    /**
     * 启动中
     */
    STARTING,
    /**
     * 压测中
     */
    PRESSURE_TESTING,
    /**
     * 压测停止
     */
    PRESSURE_TEST_STOPPED
}
