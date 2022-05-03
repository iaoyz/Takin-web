package io.shulie.takin.cloud.common.enums;

import java.util.HashMap;
import java.util.Map;

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
    ;

    private static final Map<Integer, PressureTaskStateEnum> START = new HashMap<>(8);
    private static final Map<Integer, PressureTaskStateEnum> RUNNING = new HashMap<>(8);

    public static boolean starting(int code) {
        return START.containsKey(code);
    }

    public static boolean running(int code) {
        return RUNNING.containsKey(code);
    }

    static {

        START.put(RESOURCE_LOCK_FAILED.ordinal(), RESOURCE_LOCKING);
        START.put(STARTING.ordinal(), STARTING);
        START.put(ALIVE.ordinal(), ALIVE);

        RUNNING.put(PRESSURING.ordinal(), PRESSURING);
    }
}
