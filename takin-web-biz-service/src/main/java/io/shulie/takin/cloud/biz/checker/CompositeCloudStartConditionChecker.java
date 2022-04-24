package io.shulie.takin.cloud.biz.checker;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class CompositeCloudStartConditionChecker implements CloudStartConditionChecker, InitializingBean {

    @Resource
    private List<CloudStartConditionChecker> checkerList;

    @Override
    public void preCheck(Long sceneId) throws TakinCloudException {
        checkerList.forEach(checker -> {
            try {
                checker.preCheck(sceneId);
            } catch (TakinCloudException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
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

    @Override
    public String type() {
        return "composite-cloud";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
