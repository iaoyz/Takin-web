package io.shulie.takin.cloud.biz.notify.processor.pod;

import java.util.Objects;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.cloud.model.callback.basic.ResourceExample;
import io.shulie.takin.web.biz.checker.StartConditionChecker.CheckStatus;
import org.springframework.stereotype.Component;

@Component
public class PodStartNotifyProcessor extends AbstractIndicators implements CloudNotifyProcessor<PodStartNotifyParam> {

    @Resource
    private RedisClientUtils redisClientUtils;

    @Override
    public CallbackType type() {
        return CallbackType.RESOURCE_EXAMPLE_START;
    }

    @Override
    public void process(PodStartNotifyParam context) {
        ResourceExample data = context.getData();
        ResourceContext resourceContext = getResourceContext(String.valueOf(data.getResourceId()));
        if (resourceContext != null) {
            String curStatus = resourceContext.getStatus();
            if (Objects.nonNull(curStatus) && curStatus.equals(String.valueOf(CheckStatus.PENDING.ordinal()))) {
                processStartSuccess(data);
            }
        }
    }

    // 增加pod实例数
    private void processStartSuccess(ResourceExample context) {
        String resourceId = String.valueOf(context.getResourceId());
        if (!redisClientUtils.hasKey(PressureStartCache.getResourceJmeterFailKey(resourceId))) {
            redisClientUtils.setSetValue(PressureStartCache.getResourcePodSuccessKey(resourceId),
                String.valueOf(context.getResourceExampleId()));
        }
    }
}
