package io.shulie.takin.cloud.biz.checker;

import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import org.springframework.core.Ordered;

public interface CloudStartConditionChecker extends Ordered {

    String type();

    default void preCheck(Long sceneId) throws TakinCloudException {
    }

    default void runningCheck(SceneManageWrapperOutput sceneData, SceneTaskStartInput input) {
    }
}
