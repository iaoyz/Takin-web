package io.shulie.takin.cloud.biz.notify.processor.jmeter;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import org.springframework.stereotype.Component;

@Component
public class PressureErrorNotifyProcessor extends AbstractIndicators
    implements CloudNotifyProcessor<PressureErrorNotifyParam> {

    @Override
    public void process(PressureErrorNotifyParam param) {
        processError(param);
    }

    private void processError(PressureErrorNotifyParam context) {
        JobExampleErrorInfo data = context.getData();
        String resourceId = String.valueOf(data.getResourceId());
        ResourceContext resourceContext = getResourceContext(resourceId);
        if (resourceContext == null) {
            return;
        }
        callStopEventIfNecessary(String.valueOf(data.getResourceId()), data.getErrorMessage());
    }

    @Override
    public CallbackType type() {
        return CallbackType.JMETER_ERROR;
    }
}
