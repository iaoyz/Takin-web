package io.shulie.takin.web.biz.checker;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import com.pamirs.takin.entity.domain.dto.scenemanage.SceneManageWrapperDTO;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class CompositeWebStartConditionChecker implements WebStartConditionChecker, InitializingBean {

    @Resource
    private List<WebStartConditionChecker> checkerList;

    @Override
    public void preCheck(Long sceneId) {
        checkerList.forEach(checker -> {
            try {
                checker.preCheck(sceneId);
            } catch (TakinCloudException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void runningCheck(SceneManageWrapperDTO sceneData) {
        checkerList.forEach(checker -> checker.runningCheck(sceneData));
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
        return "composite-web";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
