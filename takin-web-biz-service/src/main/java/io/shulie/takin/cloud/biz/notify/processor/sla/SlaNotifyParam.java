package io.shulie.takin.cloud.biz.notify.processor.sla;

import io.shulie.takin.cloud.biz.notify.CloudNotifyParam;
import io.shulie.takin.cloud.model.callback.Sla.SlaInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SlaNotifyParam extends CloudNotifyParam {

    private SlaInfo data;
}
