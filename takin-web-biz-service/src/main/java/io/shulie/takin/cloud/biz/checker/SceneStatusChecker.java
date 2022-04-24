package io.shulie.takin.cloud.biz.checker;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import org.springframework.stereotype.Component;

@Component
public class SceneStatusChecker implements CloudStartConditionChecker {

    @Resource
    private CloudSceneManageService cloudSceneManageService;

    @Override
    public CheckResult preCheck(Long sceneId, String resourceId) throws TakinCloudException {
        try {
            SceneManageWrapperOutput sceneData = cloudSceneManageService.getSceneManage(sceneId, null);
            runningCheck(sceneData, null);
            return CheckResult.success(type());
        } catch (Exception e) {
            return CheckResult.fail(type(), e.getMessage());
        }
    }

    @Override
    public void runningCheck(SceneManageWrapperOutput sceneData, SceneTaskStartInput input) {
        if (!SceneManageStatusEnum.ifFree(sceneData.getStatus())) {
            throw new TakinCloudException(TakinCloudExceptionEnum.TASK_START_VERIFY_ERROR, "当前场景不为待启动状态！");
        }
    }

    @Override
    public String type() {
        return "status";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
