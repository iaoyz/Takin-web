package io.shulie.takin.cloud.biz.notify.processor.pod;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.eventcenter.EventCenterTemplate;
import org.springframework.stereotype.Component;

@Component
public class PodStopNotifyProcessor extends AbstractIndicators implements CloudNotifyProcessor<PodStopNotifyParam> {

    @Resource
    private RedisClientUtils redisClientUtils;

    @Resource
    private EventCenterTemplate eventCenterTemplate;

    @Override
    public CallbackType type() {
        return CallbackType.RESOURCE_EXAMPLE_STOP;
    }

    @Override
    public void process(PodStopNotifyParam context) {
    }
}
