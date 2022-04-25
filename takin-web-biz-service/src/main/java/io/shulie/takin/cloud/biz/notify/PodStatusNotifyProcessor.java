package io.shulie.takin.cloud.biz.notify;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.checker.EngineResourceChecker;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class PodStatusNotifyProcessor implements CloudNotifyProcessor {

    @Resource
    private RedisClientUtils redisClientUtils;

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
        String curStatus = String.valueOf(
            redisClientUtils.hmget(EngineResourceChecker.getResourceKey(context.getResourceId())
                , EngineResourceChecker.RESOURCE_STATUS));
        if (StringUtils.isNotBlank(curStatus) && curStatus.equals(String.valueOf(CheckStatus.PENDING.ordinal()))) {
            switch (status) {
                case FAIL:
                    processFail(context);
                    break;
                case SUCCESS:
                    processSuccess(context);
                    break;
                default:
                    break;
            }
        }
        return ResponseResult.success();
    }

    private void processSuccess(NotifyContext context) {
        String resourceId = context.getResourceId();
        String statusKey = EngineResourceChecker.getResourceKey(resourceId);
        long podNumber = Long.parseLong(
            String.valueOf(redisClientUtils.hmget(statusKey, EngineResourceChecker.RESOURCE_POD_NUM)));
        Object endTime = redisClientUtils.hmget(statusKey, EngineResourceChecker.RESOURCE_END_TIME);
        if (Objects.isNull(endTime)) {
            return;
        }
        long endDate = Long.parseLong(String.valueOf(endTime));
        if (endDate < System.currentTimeMillis()) {
            Long successNumber = redisClientUtils.hIncrBy(statusKey, EngineResourceChecker.RESOURCE_SUCCESS_NUM, 1);
            if (successNumber != podNumber) {
                NotifyContext notifyContext = new NotifyContext();
                notifyContext.setResourceId(resourceId);
                notifyContext.setMessage("压力机资源不足");
                processFail(notifyContext);
            }
            return;
        }
        Long successNumber = redisClientUtils.hIncrBy(statusKey, EngineResourceChecker.RESOURCE_SUCCESS_NUM, 1);
        if (successNumber == podNumber) {
            Map<String, Object> param = new HashMap<>(4);
            param.put(EngineResourceChecker.RESOURCE_STATUS, CheckStatus.SUCCESS.ordinal());
            redisClientUtils.hmset(EngineResourceChecker.getResourceKey(context.getResourceId()), param);
        }
    }

    private void processFail(NotifyContext context) {
        Map<String, Object> param = new HashMap<>(4);
        param.put(EngineResourceChecker.RESOURCE_STATUS, CheckStatus.FAIL.ordinal());
        param.put(EngineResourceChecker.RESOURCE_MESSAGE, context.getMessage());
        redisClientUtils.hmset(EngineResourceChecker.getResourceKey(context.getResourceId()), param);
    }

    public enum PodStatus {
        FAIL,
        SUCCESS;

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
