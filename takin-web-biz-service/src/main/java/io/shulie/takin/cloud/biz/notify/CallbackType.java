package io.shulie.takin.cloud.biz.notify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CallbackType {
    RESOURCE_EXAMPLE_HEARTBEAT(100, "资源实例(Pod)心跳"),
    RESOURCE_EXAMPLE_START(101, "资源实例启动"),
    RESOURCE_EXAMPLE_STOP(102, "资源实例停止"),
    RESOURCE_EXAMPLE_ERROR(103, "资源实例异常"),
    JMETER_HEARTBEAT(200, "JMeter心跳"),
    JMETER_START(201, "JMeter启动"),
    JMETER_STOP(202, "JMeter停止"),
    JMETER_ERROR(203, "JMeter异常"),
    SLA(301, "触发SLA");

    @JsonValue
    private final Integer code;
    private final String description;
    private static final Map<Integer, CallbackType> TYPE_MAPPING = new HashMap<>();

    @JsonCreator
    public static CallbackType of(Integer code) {
        return TYPE_MAPPING.get(code);
    }

    static {
        Arrays.stream(values()).forEach(t -> TYPE_MAPPING.put(t.getCode(), t));
    }

    public Integer getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }

    CallbackType(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}