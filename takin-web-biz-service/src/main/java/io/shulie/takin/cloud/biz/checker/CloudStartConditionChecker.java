package io.shulie.takin.cloud.biz.checker;

import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import org.springframework.core.Ordered;

public interface CloudStartConditionChecker extends Ordered {

    String type();

    default CheckResult preCheck(Long sceneId, String resourceId) throws TakinCloudException {
        return CheckResult.success(type());
    }

    default void runningCheck(SceneManageWrapperOutput sceneData, SceneTaskStartInput input) {
    }
}
