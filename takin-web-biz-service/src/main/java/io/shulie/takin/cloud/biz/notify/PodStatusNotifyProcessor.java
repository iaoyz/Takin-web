package io.shulie.takin.cloud.biz.notify;

import java.util.Objects;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.eventcenter.EventCenterTemplate;
import io.shulie.takin.web.biz.checker.StartConditionChecker.CheckStatus;
import org.springframework.stereotype.Component;

@Component
public class PodStatusNotifyProcessor extends AbstractIndicators implements CloudNotifyProcessor {

    @Resource
    private RedisClientUtils redisClientUtils;

    @Resource
    private EventCenterTemplate eventCenterTemplate;

    @Override
    public String type() {
        return "pod";
    }

    @Override
    public ResponseResult<?> process(NotifyContext context) {
        PodStatus status = PodStatus.of(context.getStatus());
        if (status == null) {
            return ResponseResult.success();
        }
        ResourceContext resourceContext = getResourceContext(context.getResourceId());
        if (resourceContext == null) {
            return ResponseResult.success();
        }
        String curStatus = resourceContext.getStatus();
        if (Objects.nonNull(curStatus) && curStatus.equals(String.valueOf(CheckStatus.PENDING.ordinal()))) {
            switch (status) {
                case START_FAIL:
                    processStartFail(context, resourceContext);
                    break;
                case START_SUCCESS:
                    processStartSuccess(context);
                    break;
                default:
                    break;
            }
        }
        return ResponseResult.success();
    }

    // 增加pod实例数
    private void processStartSuccess(NotifyContext context) {
        String resourceId = context.getResourceId();
        if (!redisClientUtils.hasKey(PressureStartCache.getResourceJmeterFailKey(resourceId))) {
            redisClientUtils.setSetValue(PressureStartCache.getResourcePodSuccessKey(resourceId), context.getPodId());
        }
    }

    private void processStartFail(NotifyContext context, ResourceContext resourceContext) {
        redisClientUtils.setString(PressureStartCache.getResourceJmeterFailKey(context.getResourceId()), context.getPodId());
        Event event = new Event();
        event.setEventName(PressureStartCache.LACK_POD_RESOURCE_EVENT);
        event.setExt(resourceContext);
        eventCenterTemplate.doEvents(event);
    }

    public enum PodStatus {
        START_FAIL,
        START_SUCCESS,
        STOP_SUCCESS;

        public static PodStatus of(Integer status) {
            if (Objects.isNull(status)) {
                return null;
            }
            for (PodStatus value : values()) {
                if (value.ordinal() == status) {
                    return value;
                }
            }
            return null;
        }
    }
}
