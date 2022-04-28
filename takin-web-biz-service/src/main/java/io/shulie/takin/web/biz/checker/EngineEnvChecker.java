package io.shulie.takin.web.biz.checker;

import javax.annotation.Resource;

import io.shulie.takin.adapter.api.entrypoint.check.CloudCheckApi;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import org.springframework.stereotype.Component;

@Component
public class EngineEnvChecker implements StartConditionChecker {

    @Resource
    private CloudCheckApi cloudCheckApi;

    @Override
    public CheckResult check(StartConditionCheckerContext context) throws TakinCloudException {
        try {
            //cloudCheckApi.checkEnv(new EnvCheckRequest());
            return CheckResult.success(type());
        } catch (Exception e) {
            return CheckResult.fail(type(), e.getMessage());
        }
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
