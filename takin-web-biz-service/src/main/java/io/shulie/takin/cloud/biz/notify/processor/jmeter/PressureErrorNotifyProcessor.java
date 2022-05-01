package io.shulie.takin.cloud.biz.notify.processor.jmeter;

import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import org.springframework.stereotype.Component;

@Component
public class PressureErrorNotifyProcessor implements CloudNotifyProcessor<PressureErrorNotifyParam> {

    @Override
    public CallbackType type() {
        return CallbackType.JMETER_ERROR;
    }

    @Override
    public void process(PressureErrorNotifyParam param) {

    }
}
