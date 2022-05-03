package io.shulie.takin.cloud.biz.notify.processor.pod;

import io.shulie.takin.cloud.biz.notify.CloudNotifyParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PodErrorNotifyParam extends CloudNotifyParam {

    private ResourceExampleErrorInfo data;
}
