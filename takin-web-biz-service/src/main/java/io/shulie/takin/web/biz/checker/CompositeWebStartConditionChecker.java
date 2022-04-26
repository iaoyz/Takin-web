package io.shulie.takin.web.biz.checker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class CompositeWebStartConditionChecker implements InitializingBean {

    @Resource
    private List<WebStartConditionChecker> checkerList;

    private final Map<String, WebStartConditionChecker> checkerMap = new HashMap<>();

    // 此处特殊使用
    public CheckResult doCheck(WebConditionCheckerContext context) {
        String type = context.getType();
        WebStartConditionChecker checker = checkerMap.get(type);
        if (checker == null) {
            return CheckResult.success(type);
        }
        return checker.check(context);
    }

    @Override
    public void afterPropertiesSet() {
        if (!CollectionUtils.isEmpty(checkerList)) {
            for (WebStartConditionChecker checker : checkerList) {
                checkerMap.putIfAbsent(checker.type(), checker);
            }
        }
    }
}
