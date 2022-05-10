package io.shulie.takin.cloud.biz.notify.processor.jmeter;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.common.enums.PressureTaskStateEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskDAO;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.cloud.model.callback.basic.JobExample;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PressureStopNotifyProcessor extends AbstractIndicators
    implements CloudNotifyProcessor<PressureStopNotifyParam> {

    @Resource
    private RedisClientUtils redisClientUtils;

    @Override
    public String process(PressureStopNotifyParam param) {
        processStopped(param);
        return String.valueOf(param.getData().getResourceId());
    }

    private void processStopped(PressureStopNotifyParam param) {
        JobExample data = param.getData();
        String resourceId = String.valueOf(data.getResourceId());
        if (!redisClientUtils.hasKey(PressureStartCache.getResourceKey(resourceId))) {
            return;
        }
        String podId = String.valueOf(data.getResourceExampleId());
        String jmeterId = String.valueOf(data.getJobExampleId());
        ResourceContext context = getResourceContext(resourceId);
        if (redisClientUtils.lockNoExpire(
            PressureStartCache.getStopFlag(context.getSceneId(), resourceId), "jmeter停止")) {
            notifyStop(context);
        }
        removeSuccessKey(resourceId, podId, jmeterId);
        detectEnd(resourceId, param.getTime());
    }

    @Override
    public CallbackType type() {
        return CallbackType.JMETER_STOP;
    }
}
