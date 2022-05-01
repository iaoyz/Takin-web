package io.shulie.takin.cloud.biz.notify.processor.jmeter;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.model.callback.basic.JobExample;
import org.springframework.stereotype.Component;

@Component
public class PressureHeartbeatNotifyProcessor extends AbstractIndicators
    implements CloudNotifyProcessor<PressureHeartbeatNotifyParam> {

    @Override
    public CallbackType type() {
        return CallbackType.JMETER_HEARTBEAT;
    }

    @Override
    public void process(PressureHeartbeatNotifyParam param) {
        JobExample data = param.getData();
        String resourceId = String.valueOf(data.getResourceId());
        if (heartbeatTimeout(resourceId)) {
            //finalFailed(getResourceContext(resourceId), "心跳超时");
        }
    }
}
