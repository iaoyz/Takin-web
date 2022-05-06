package io.shulie.takin.cloud.biz.notify.processor.pod;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import org.springframework.stereotype.Component;

@Component
public class PodErrorNotifyProcessor extends AbstractIndicators implements CloudNotifyProcessor<PodErrorNotifyParam> {

    @Resource
    private RedisClientUtils redisClientUtils;

    @Override
    public CallbackType type() {
        return CallbackType.RESOURCE_EXAMPLE_ERROR;
    }

    @Override
    public String process(PodErrorNotifyParam context) {
        ResourceExampleErrorInfo data = context.getData();
        String resourceId = String.valueOf(data.getResourceId());
        String podId = String.valueOf(data.getResourceExampleId());
        if (redisClientUtils.lockNoExpire(PressureStartCache.getPodErrorFirstKey(resourceId), podId)) {
            callStopEventIfNecessary(resourceId, data.getErrorMessage());
        }
        return resourceId;
    }
}
