package io.shulie.takin.cloud.biz.notify.processor.jmeter;

import javax.annotation.Resource;

import cn.hutool.core.util.StrUtil;
import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.notify.CallbackType;
import io.shulie.takin.cloud.biz.notify.CloudNotifyProcessor;
import io.shulie.takin.cloud.common.enums.PressureTaskStateEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskDAO;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskVarietyDAO;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskEntity;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskVarietyEntity;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import org.springframework.stereotype.Component;

@Component
public class PressureErrorNotifyProcessor extends AbstractIndicators
    implements CloudNotifyProcessor<PressureErrorNotifyParam> {

    @Resource
    private RedisClientUtils redisClientUtils;
    @Resource
    private PressureTaskDAO pressureTaskDAO;
    @Resource
    private PressureTaskVarietyDAO pressureTaskVarietyDAO;

    @Override
    public void process(PressureErrorNotifyParam param) {
        processError(param);
    }

    private void processError(PressureErrorNotifyParam context) {
        JobExampleErrorInfo data = context.getData();
        String resourceId = String.valueOf(data.getResourceId());
        ResourceContext resourceContext = getResourceContext(resourceId);
        if (resourceContext == null) {
            return;
        }
        if (redisClientUtils.lockNoExpire(PressureStartCache.getJmeterErrorFirstKey(resourceId), "1")) {
            notifyError(resourceContext);
        }
        callStopEventIfNecessary(String.valueOf(data.getResourceId()), data.getErrorMessage());
    }

    private void notifyError(ResourceContext context) {
        Long taskId = context.getTaskId();
        PressureTaskEntity entity = pressureTaskDAO.selectById(taskId);
        if (entity.getStatus() == PressureTaskStateEnum.STARTING.ordinal()) { // 启动中
            pressureTaskVarietyDAO.updateMessage(PressureTaskVarietyEntity.of(taskId,
                PressureTaskStateEnum.STARTING, StrUtil.nullToEmpty(context.getMessage())));
        }
        pressureTaskDAO.updateStatus(taskId, PressureTaskStateEnum.STOPPING);
    }

    @Override
    public CallbackType type() {
        return CallbackType.JMETER_ERROR;
    }
}
