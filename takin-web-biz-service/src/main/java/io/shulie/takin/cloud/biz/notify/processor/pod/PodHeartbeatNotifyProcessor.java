package io.shulie.takin.cloud.biz.notify.processor.pod;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.cloud.model.callback.basic.JobExample;
import org.springframework.stereotype.Component;

@Component
public class PodHeartbeatNotifyProcessor extends AbstractIndicators
    implements CloudNotifyProcessor<PodHeartbeatNotifyParam> {

    @Resource
    private RedisClientUtils redisClientUtils;

    @Override
    public String process(PodHeartbeatNotifyParam param) {
        JobExample data = param.getData();
        String resourceId = String.valueOf(data.getResourceId());
        ResourceContext resourceContext = getResourceContext(resourceId);
        if (resourceContext != null) {
            String podId = String.valueOf(data.getJobExampleId());
            redisClientUtils.hmset(PressureStartCache.getPodHeartbeatKey(resourceContext.getSceneId()), podId,
                System.currentTimeMillis());
        }
        return resourceId;
    }

    @Override
    public CallbackType type() {
        return CallbackType.RESOURCE_EXAMPLE_HEARTBEAT;
    }
}
