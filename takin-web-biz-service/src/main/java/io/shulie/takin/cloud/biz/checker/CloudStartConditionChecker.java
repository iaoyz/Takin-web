package io.shulie.takin.cloud.biz.checker;

import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import org.springframework.core.Ordered;

public interface CloudStartConditionChecker extends Ordered {

    String type();

    default CheckResult check(CloudConditionCheckerContext context) throws TakinCloudException {
        return CheckResult.success(type());
    }
}
