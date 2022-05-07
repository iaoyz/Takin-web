package io.shulie.takin.cloud.biz.notify.processor.jmeter;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import org.springframework.stereotype.Component;

@Component
public class PressureErrorNotifyProcessor extends AbstractIndicators
    implements CloudNotifyProcessor<PressureErrorNotifyParam> {

    @Resource
    private RedisClientUtils redisClientUtils;

    @Override
    public String process(PressureErrorNotifyParam param) {
        processError(param);
        return String.valueOf(param.getData().getResourceId());
    }

    private void processError(PressureErrorNotifyParam param) {
        JobExampleErrorInfo data = param.getData();
        String resourceId = String.valueOf(data.getResourceId());
        String jmeterId = String.valueOf(data.getJobExampleId());
        if (redisClientUtils.lockNoExpire(PressureStartCache.getJmeterErrorFirstKey(resourceId), jmeterId)) {
            callStopEventIfNecessary(String.valueOf(data.getResourceId()), data.getErrorMessage());
        }
        removeSuccessKey(resourceId, String.valueOf(data.getResourceExampleId()), jmeterId);
        detectEnd(resourceId, param.getTime());
    }

    @Override
    public CallbackType type() {
        return CallbackType.JMETER_ERROR;
    }
}
