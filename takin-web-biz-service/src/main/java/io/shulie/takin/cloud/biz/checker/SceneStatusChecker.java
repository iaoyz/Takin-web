package io.shulie.takin.cloud.biz.checker;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.common.bean.scenemanage.SceneManageQueryOptions;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class SceneStatusChecker implements CloudStartConditionChecker {

    @Resource
    private CloudSceneManageService cloudSceneManageService;

    @Resource
    private RedisClientUtils redisClientUtils;

    @Override
    public CheckResult check(CloudConditionCheckerContext context) throws TakinCloudException {
        try {
            fillContext(context);
            doCheck(context);
            return CheckResult.success(type());
        } catch (Exception e) {
            return CheckResult.fail(type(), e.getMessage());
        }
    }

    private void fillContext(CloudConditionCheckerContext context) {
        if (context.getSceneData() == null) {
            SceneManageQueryOptions options = new SceneManageQueryOptions();
            options.setIncludeBusinessActivity(true);
            options.setIncludeScript(true);
            context.setSceneData(cloudSceneManageService.getSceneManage(context.getSceneId(), options));
        }
    }

    private void doCheck(CloudConditionCheckerContext context) {
        SceneManageWrapperOutput sceneData = context.getSceneData();
        if (!SceneManageStatusEnum.ifFree(sceneData.getStatus()) || pressureRunning(context)) {
            throw new TakinCloudException(TakinCloudExceptionEnum.TASK_START_VERIFY_ERROR, "当前场景不为待启动状态！");
        }
    }

    private boolean pressureRunning(CloudConditionCheckerContext context) {
        String resourceId = context.getResourceId();
        return StringUtils.isBlank(resourceId) &&
            redisClientUtils.hmget(EngineResourceChecker.getResourceKey(resourceId)) != null;
    }

    @Override
    public String type() {
        return "status";
    }
}
