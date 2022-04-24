package io.shulie.takin.web.biz.checker;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import com.pamirs.takin.entity.domain.dto.scenemanage.SceneManageWrapperDTO;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class CompositeWebStartConditionChecker implements InitializingBean {

    @Resource
    private List<WebStartConditionChecker> checkerList;

    // 此处特殊使用
    public List<CheckResult> doCheck(WebConditionCheckerContext context) {
        List<CheckResult> resultList = new ArrayList<>(checkerList.size());
        checkerList.forEach(checker -> {
            resultList.add(checker.check(context));
        });
        return resultList;
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
