package io.shulie.takin.cloud.biz.notify.processor.sla;

import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import org.springframework.stereotype.Component;

@Component
public class SlaNotifyProcessor implements CloudNotifyProcessor<SlaNotifyParam> {

    @Override
    public CallbackType type() {
        return CallbackType.SLA;
    }

    @Override
    public void process(SlaNotifyParam param) {

    }
}
