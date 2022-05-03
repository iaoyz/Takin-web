package io.shulie.takin.cloud.biz.notify.processor.pod;

import io.shulie.takin.cloud.biz.notify.CloudNotifyParam;
import io.shulie.takin.cloud.model.callback.basic.JobExample;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PodHeartbeatNotifyParam extends CloudNotifyParam {

    private JobExample data;
}
