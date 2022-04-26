package io.shulie.takin.cloud.biz.notify;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;

import com.pamirs.takin.cloud.entity.domain.vo.scenemanage.SceneManageStartRecordVO;
import io.shulie.takin.cloud.biz.checker.EngineResourceChecker;
import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.common.bean.task.TaskResult;
import io.shulie.takin.cloud.common.enums.scenemanage.TaskStatusEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckStatus;
import org.springframework.stereotype.Component;

@Component
public class PodStatusNotifyProcessor extends AbstractIndicators implements CloudNotifyProcessor {

    @Resource
    private RedisClientUtils redisClientUtils;

    @Resource
    private CloudSceneManageService cloudSceneManageService;

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
                    processFail(context, resourceContext);
                    break;
                case START_SUCCESS:
                    processSuccess(context, resourceContext);
                    break;
                default:
                    break;
            }
        }
        return ResponseResult.success();
    }

    private void processSuccess(NotifyContext context, ResourceContext resourceContext) {
        String resourceId = context.getResourceId();
        String statusKey = EngineResourceChecker.getResourceKey(resourceId);
        Long podNumber = resourceContext.getPodNumber();
        Long endTime = resourceContext.getEndTime();
        long endDate = Long.parseLong(String.valueOf(endTime));
        if (endDate < System.currentTimeMillis()) {
            Long successNumber = redisClientUtils.hIncrBy(statusKey, EngineResourceChecker.RESOURCE_SUCCESS_NUM, 1);
            if (!Objects.equals(successNumber, podNumber)) {
                NotifyContext notifyContext = new NotifyContext();
                notifyContext.setResourceId(resourceId);
                notifyContext.setMessage("压力机资源不足");
                processFail(notifyContext, resourceContext);
            }
            return;
        }
        notifyEvent(resourceContext, context, true);
        Long successNumber = redisClientUtils.hIncrBy(statusKey, EngineResourceChecker.RESOURCE_SUCCESS_NUM, 1);
        if (Objects.equals(successNumber, podNumber)) {
            Map<String, Object> param = new HashMap<>(4);
            param.put(EngineResourceChecker.RESOURCE_STATUS, CheckStatus.SUCCESS.ordinal());
            redisClientUtils.hmset(EngineResourceChecker.getResourceKey(context.getResourceId()), param);
            redisClientUtils.delete(EngineResourceChecker.getSceneResourceLockingKey(resourceContext.getSceneId()));
        }
    }

    private void processFail(NotifyContext context, ResourceContext resourceContext) {
        Map<String, Object> param = new HashMap<>(4);
        param.put(EngineResourceChecker.RESOURCE_STATUS, CheckStatus.FAIL.ordinal());
        param.put(EngineResourceChecker.RESOURCE_MESSAGE, context.getMessage());
        redisClientUtils.hmset(EngineResourceChecker.getResourceKey(context.getResourceId()), param);
        redisClientUtils.delete(EngineResourceChecker.getSceneResourceLockingKey(resourceContext.getSceneId()));
        notifyEvent(resourceContext, context, false);
        // 流量验证检查的是report的状态
        cloudSceneManageService.reportRecord(
            SceneManageStartRecordVO
                .build(resourceContext.getSceneId(), resourceContext.getReportId(), resourceContext.getTenantId())
                .success(false).errorMsg(context.getMessage()).build());
    }

    private void notifyEvent(ResourceContext resourceContext, NotifyContext context, boolean started) {
        TaskResult result = new TaskResult();
        result.setSceneId(resourceContext.getSceneId());
        result.setTaskId(resourceContext.getReportId());
        result.setTenantId(resourceContext.getTenantId());
        result.setMsg(context.getMessage());
        result.setStatus(started ? TaskStatusEnum.STARTED : TaskStatusEnum.FAILED);
        Event event = new Event();
        event.setEventName(started ? "started" : "failed");
        event.setExt(result);
        eventCenterTemplate.doEvents(event);
    }

    public enum PodStatus {
        START_FAIL,
        START_SUCCESS;

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
