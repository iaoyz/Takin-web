package io.shulie.takin.cloud.biz.checker;

import javax.annotation.Resource;

import io.shulie.takin.adapter.api.entrypoint.check.CloudCheckApi;
import io.shulie.takin.adapter.api.model.request.check.EnvCheckRequest;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import org.springframework.stereotype.Component;

@Component
public class EngineEnvChecker implements CloudStartConditionChecker {

    @Resource
    private CloudCheckApi cloudCheckApi;

    @Override
    public CheckResult preCheck(Long sceneId, String resourceId) throws TakinCloudException {
        try {
            cloudCheckApi.checkEnv(new EnvCheckRequest());
            return CheckResult.success(type());
        } catch (Exception e) {
            return CheckResult.fail(type(), e.getMessage());
        }
    }

    @Override
    public void runningCheck(SceneManageWrapperOutput sceneData, SceneTaskStartInput input) {
        cloudCheckApi.checkEnv(new EnvCheckRequest());
    }

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public String type() {
        return "env";
    }
}
