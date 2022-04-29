package io.shulie.takin.web.biz.checker;

import javax.annotation.Resource;

import io.shulie.takin.adapter.api.entrypoint.watchman.CloudWatchmanApi;
import io.shulie.takin.adapter.api.model.request.watchman.WatchmanStatusRequest;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import org.springframework.stereotype.Component;

@Component
public class EngineEnvChecker implements StartConditionChecker {

    @Resource
    private CloudWatchmanApi cloudWatchmanApi;

    @Override
    public CheckResult check(StartConditionCheckerContext context) throws TakinCloudException {
        try {
            cloudWatchmanApi.status(new WatchmanStatusRequest());
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
