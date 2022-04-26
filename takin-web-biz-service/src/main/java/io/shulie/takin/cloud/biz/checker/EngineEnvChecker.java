package io.shulie.takin.cloud.biz.checker;

import javax.annotation.Resource;

import io.shulie.takin.adapter.api.entrypoint.check.CloudCheckApi;
import io.shulie.takin.adapter.api.model.request.check.EnvCheckRequest;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import org.springframework.stereotype.Component;

@Component
public class EngineEnvChecker implements CloudStartConditionChecker {

    @Resource
    private CloudCheckApi cloudCheckApi;

    @Override
    public CheckResult check(CloudConditionCheckerContext context) throws TakinCloudException {
        try {
            // cloudCheckApi.checkEnv(new EnvCheckRequest());
            return CheckResult.success(type());
        } catch (Exception e) {
            return CheckResult.fail(type(), e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public String type() {
        return "env";
    }
}
