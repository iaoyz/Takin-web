package io.shulie.takin.cloud.common.enums;

public enum PressureTaskStateEnum {
    INITIALIZED,//初始化
    STARTING,//启动中
    ALIVE,//启动完成
    PRESSURING,//压测中
    UNUSUAL,//异常
    STOPPING,//停止中
    INACTIVE,//停止
    RESOURCE_LOCKING
}
