package io.shulie.takin.cloud.biz.notify.processor.pod;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import org.springframework.stereotype.Component;

@Component
public class PodErrorNotifyProcessor extends AbstractIndicators implements CloudNotifyProcessor<PodErrorNotifyParam> {

    @Override
    public CallbackType type() {
        return CallbackType.RESOURCE_EXAMPLE_ERROR;
    }

    @Override
    public void process(PodErrorNotifyParam context) {
        ResourceExampleErrorInfo data = context.getData();
        callStopEventIfNecessary(String.valueOf(data.getResourceId()), data.getErrorMessage());
    }
}
