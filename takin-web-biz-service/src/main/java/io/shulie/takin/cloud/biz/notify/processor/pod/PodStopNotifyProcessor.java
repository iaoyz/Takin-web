package io.shulie.takin.cloud.biz.notify.processor.pod;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import org.springframework.stereotype.Component;

@Component
public class PodStopNotifyProcessor extends AbstractIndicators implements CloudNotifyProcessor<PodStopNotifyParam> {

    @Override
    public CallbackType type() {
        return CallbackType.RESOURCE_EXAMPLE_STOP;
    }

    @Override
    public String process(PodStopNotifyParam context) {
        // 暂不处理
        return "";
    }
}
