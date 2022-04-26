package io.shulie.takin.cloud.biz.checker;

import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;

public interface CloudStartConditionChecker {

    String type();

    default CheckResult check(CloudConditionCheckerContext context) throws TakinCloudException {
        return CheckResult.success(type());
    }
}
