package io.shulie.takin.cloud.biz.notify.processor.sla;

import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.biz.service.sla.SlaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SlaNotifyProcessor implements CloudNotifyProcessor<SlaNotifyParam> {

    @Autowired
    private SlaService slaService;

    @Override
    public CallbackType type() {
        return CallbackType.SLA;
    }

    @Override
    public void process(SlaNotifyParam param) {
        slaService.detection(param.getData());
    }
}
