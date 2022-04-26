package io.shulie.takin.web.biz.checker;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.utils.json.JsonHelper;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class CompositeWebStartConditionChecker implements InitializingBean {

    @Resource
    private RedisClientUtils redisClientUtils;

    @Resource
    private List<WebStartConditionChecker> checkerList;

    // 此处特殊使用
    public List<CheckResult> doCheck(WebConditionCheckerContext context) {
        List<CheckResult> resultList = new ArrayList<>(checkerList.size());
        for (WebStartConditionChecker checker : checkerList) {
            CheckResult checkResult = doCheck(context, checker);
            resultList.add(checkResult);
            if (checkResult.getStatus().equals(CheckStatus.FAIL.ordinal())) {
                break;
            }
        }
        return resultList;
    }

    private CheckResult doCheck(WebConditionCheckerContext context, WebStartConditionChecker checker) {
        String checkResultKey = CheckResult.getCheckResultKey(context.getSceneId());
        String result = (String)redisClientUtils.hmget(checkResultKey, checker.type());
        if (StringUtils.isNotBlank(result)) {
            return JsonHelper.json2Bean(result, CheckResult.class);
        }
        CheckResult checkResult = checker.check(context);
        if (checkResult.getStatus().equals(CheckStatus.SUCCESS.ordinal())) {
            redisClientUtils.hmset(checkResultKey, checker.type(), JsonHelper.bean2Json(result));
        } else if(checkResult.getStatus().equals(CheckStatus.FAIL.ordinal())) {
            redisClientUtils.delete(checkResultKey);
        }
        return checkResult;
    }

    @Override
    public void afterPropertiesSet() {
        if (checkerList == null) {
            checkerList = new ArrayList<>(0);
        }
        checkerList.sort((it1, it2) -> {
            int i1 = it1.getOrder();
            int i2 = it2.getOrder();
            return Integer.compare(i1, i2);
        });
    }
}
