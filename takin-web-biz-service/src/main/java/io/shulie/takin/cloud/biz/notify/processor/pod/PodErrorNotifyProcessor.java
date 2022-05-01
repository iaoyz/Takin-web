package io.shulie.takin.cloud.biz.notify.processor.pod;

import java.util.Objects;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.cloud.model.callback.ResourceExampleError.ResourceExampleErrorInfo;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.eventcenter.EventCenterTemplate;
import io.shulie.takin.web.biz.checker.StartConditionChecker.CheckStatus;
import org.springframework.stereotype.Component;

@Component
public class PodErrorNotifyProcessor extends AbstractIndicators implements CloudNotifyProcessor<PodErrorNotifyParam> {

    @Resource
    private RedisClientUtils redisClientUtils;

    @Resource
    private EventCenterTemplate eventCenterTemplate;

    @Override
    public CallbackType type() {
        return CallbackType.RESOURCE_EXAMPLE_ERROR;
    }

    @Override
    public void process(PodErrorNotifyParam context) {
        ResourceExampleErrorInfo data = context.getData();
        ResourceContext resourceContext = getResourceContext(String.valueOf(data.getResourceId()));
        if (resourceContext != null) {
            String curStatus = resourceContext.getStatus();
            if (Objects.nonNull(curStatus) && curStatus.equals(String.valueOf(CheckStatus.PENDING.ordinal()))) {
                processError(data, resourceContext);
            }
        }
    }

    private void processError(ResourceExampleErrorInfo data, ResourceContext resourceContext) {
        redisClientUtils.setString(PressureStartCache.getResourceJmeterFailKey(String.valueOf(data.getResourceId())),
            String.valueOf(data.getResourceExampleId()));
        resourceContext.setMessage(data.getErrorMessage());
        Event event = new Event();
        event.setEventName(PressureStartCache.LACK_POD_RESOURCE_EVENT);
        event.setExt(resourceContext);
        eventCenterTemplate.doEvents(event);
    }
}
