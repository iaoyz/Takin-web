package io.shulie.takin.cloud.biz.notify.processor.jmeter;

import io.shulie.takin.cloud.biz.notify.CloudNotifyParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PressureErrorNotifyParam extends CloudNotifyParam {

    private JobExampleErrorInfo data;
}
