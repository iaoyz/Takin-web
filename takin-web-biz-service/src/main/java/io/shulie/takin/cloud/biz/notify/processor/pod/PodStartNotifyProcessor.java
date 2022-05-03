package io.shulie.takin.cloud.biz.notify.processor.pod;

import java.util.Objects;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.biz.service.async.CloudAsyncService;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.cloud.model.callback.basic.ResourceExample;
import io.shulie.takin.web.biz.checker.StartConditionChecker.CheckStatus;
import org.springframework.stereotype.Component;

@Component
public class PodStartNotifyProcessor extends AbstractIndicators implements CloudNotifyProcessor<PodStartNotifyParam> {

    @Resource
    private RedisClientUtils redisClientUtils;

    @Resource
    private CloudAsyncService cloudAsyncService;

    @Override
    public CallbackType type() {
        return CallbackType.RESOURCE_EXAMPLE_START;
    }

    @Override
    public void process(PodStartNotifyParam context) {
        ResourceExample data = context.getData();
        String resourceId = String.valueOf(data.getResourceId());
        ResourceContext resourceContext = getResourceContext(resourceId);
        if (resourceContext != null) {
            if (!redisClientUtils.hasKey(
                RedisClientUtils.getLockPrefix(PressureStartCache.getStopFlag(resourceContext.getSceneId(), resourceId)))) {
                String curStatus = resourceContext.getCheckStatus();
                if (Objects.nonNull(curStatus) && curStatus.equals(String.valueOf(CheckStatus.PENDING.ordinal()))) {
                    processStartSuccess(data, resourceContext);
                }
            }
        }
    }

    // 增加pod实例数
    private void processStartSuccess(ResourceExample context, ResourceContext resourceContext) {
        String resourceId = String.valueOf(context.getResourceId());
        String podId = String.valueOf(context.getResourceExampleId());
        redisClientUtils.setSetValue(PressureStartCache.getResourcePodSuccessKey(resourceId), podId);
        if (Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(PressureStartCache.getPodStartFirstKey(resourceId), podId))) {
            cloudAsyncService.checkPodHeartbeatTask(resourceContext);
        }
    }
}
