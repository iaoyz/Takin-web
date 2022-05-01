package io.shulie.takin.cloud.biz.notify.processor.sla;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.shulie.takin.cloud.biz.notify.CloudNotifyParam;
import io.shulie.takin.cloud.model.callback.Sla.SlaInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("301")
public class SlaNotifyParam extends CloudNotifyParam {

    private SlaInfo data;
}
