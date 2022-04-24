package io.shulie.takin.cloud.biz.checker;

import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import org.springframework.stereotype.Component;

@Component
public class SceneStatusChecker implements CloudStartConditionChecker {

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
