package io.shulie.takin.cloud.biz.notify.processor.jmeter;

import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import org.springframework.stereotype.Component;

@Component
public class PressureStopNotifyProcessor implements CloudNotifyProcessor<PressureStopNotifyParam> {

    @Override
    public CallbackType type() {
        return CallbackType.JMETER_STOP;
    }

    @Override
    public void process(PressureStopNotifyParam param) {

    }
}
