package io.shulie.takin.web.biz.checker;

import com.pamirs.takin.entity.domain.dto.scenemanage.SceneManageWrapperDTO;
import org.springframework.core.Ordered;

public interface WebStartConditionChecker extends Ordered {

    default void preCheck(Long sceneId) {}

    default void runningCheck(SceneManageWrapperDTO sceneData) {}

    String type();
}
