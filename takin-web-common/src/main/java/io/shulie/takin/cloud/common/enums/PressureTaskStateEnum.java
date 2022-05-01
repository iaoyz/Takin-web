package io.shulie.takin.cloud.common.enums;

public enum PressureTaskStateEnum {
    INITIALIZED,//初始化
    CHECKED,
    RESOURCE_LOCKING,
    RESOURCE_LOCK_FAILED,
    STARTING,//启动中
    ALIVE,//启动完成
    PRESSURING,//压测中
    UNUSUAL,//异常
    STOPPING,//停止中
    INACTIVE,//停止
}
