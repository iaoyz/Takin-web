package io.shulie.takin.cloud.biz.checker;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class CompositeCloudStartConditionChecker implements InitializingBean {

    @Resource
    private List<CloudStartConditionChecker> checkerList;

    public List<CheckResult> preCheck(Long sceneId, String resourceId) throws TakinCloudException {
        List<CheckResult> resultList = new ArrayList<>(checkerList.size());
        checkerList.forEach(checker -> {
            resultList.add(checker.preCheck(sceneId, resourceId));
        });
        return resultList;
    }

    public void runningCheck(SceneManageWrapperOutput sceneData, SceneTaskStartInput input) {
        checkerList.forEach(checker -> checker.runningCheck(sceneData, input));
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
