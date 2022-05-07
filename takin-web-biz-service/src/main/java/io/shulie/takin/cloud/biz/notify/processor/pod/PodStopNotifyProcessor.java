package io.shulie.takin.cloud.biz.notify.processor.pod;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.cloud.model.callback.basic.ResourceExample;
import org.springframework.stereotype.Component;

@Component
public class PodStopNotifyProcessor extends AbstractIndicators implements CloudNotifyProcessor<PodStopNotifyParam> {

    @Resource
    private RedisClientUtils redisClientUtils;

    @Override
    public CallbackType type() {
        return CallbackType.RESOURCE_EXAMPLE_STOP;
    }

    @Override
    public String process(PodStopNotifyParam param) {
        processStop(param);
        return String.valueOf(param.getData().getResourceExampleId());
    }

    private void processStop(PodStopNotifyParam param) {
        ResourceExample data = param.getData();
        String resourceId = String.valueOf(data.getResourceId());
        String podId = String.valueOf(data.getResourceExampleId());
        if (redisClientUtils.lockNoExpire(PressureStartCache.getPodStopFirstKey(resourceId), podId)) {
            callStopEventIfNecessary(String.valueOf(data.getResourceId()), "pod停止");
        }
        removeSuccessKey(resourceId, podId, String.valueOf(data.getJobExampleId()));
        detectEnd(resourceId, param.getTime());
    }
}
