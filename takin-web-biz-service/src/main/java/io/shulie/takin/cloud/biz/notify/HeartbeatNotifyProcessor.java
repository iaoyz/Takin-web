package io.shulie.takin.cloud.biz.notify;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.common.beans.response.ResponseResult;
import org.springframework.stereotype.Component;

@Component
public class HeartbeatNotifyProcessor extends AbstractIndicators implements CloudNotifyProcessor {

    @Override
    public String type() {
        return "heartbeat";
    }

    @Override
    public ResponseResult<?> process(NotifyContext context) {
        String resourceId = context.getResourceId();
        if (heartbeatTimeout(resourceId)) {
            finalFailed(getResourceContext(resourceId), "心跳超时");
        }
        return ResponseResult.success();
    }
}
