package io.shulie.takin.cloud.biz.checker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class CompositeCloudStartConditionChecker implements InitializingBean {

    @Resource
    private List<CloudStartConditionChecker> checkerList;

    private final Map<String, CloudStartConditionChecker> checkerMap = new HashMap<>();

    public CheckResult doCheck(CloudConditionCheckerContext context) throws TakinCloudException {
        String type = context.getType();
        CloudStartConditionChecker checker = checkerMap.get(type);
        if (checker == null) {
            return CheckResult.success(type);
        }
        return checker.check(context);
    }

    @Override
    public void afterPropertiesSet() {
        if (!CollectionUtils.isEmpty(checkerList)) {
            for (CloudStartConditionChecker checker : checkerList) {
                checkerMap.putIfAbsent(checker.type(), checker);
            }
        }
    }
}
