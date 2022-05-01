package io.shulie.takin.cloud.biz.notify;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import io.shulie.takin.cloud.biz.notify.processor.jmeter.PressureErrorNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.jmeter.PressureHeartbeatNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.jmeter.PressureStartNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.jmeter.PressureStopNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.pod.PodErrorNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.pod.PodStartNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.pod.PodStopNotifyParam;
import io.shulie.takin.cloud.biz.notify.processor.sla.SlaNotifyParam;
import lombok.Data;

@Data
@JsonTypeInfo(use = Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PodStartNotifyParam.class, name = "101"),
    @JsonSubTypes.Type(value = PodStopNotifyParam.class, name = "102"),
    @JsonSubTypes.Type(value = PodErrorNotifyParam.class, name = "103"),
    @JsonSubTypes.Type(value = PressureHeartbeatNotifyParam.class, name = "200"),
    @JsonSubTypes.Type(value = PressureStartNotifyParam.class, name = "201"),
    @JsonSubTypes.Type(value = PressureStopNotifyParam.class, name = "202"),
    @JsonSubTypes.Type(value = PressureErrorNotifyParam.class, name = "203"),
    @JsonSubTypes.Type(value = SlaNotifyParam.class, name = "301"),
})
public class CloudNotifyParam {

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date time;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date callbackTime;
    private String sign;
    private CallbackType type;
}
