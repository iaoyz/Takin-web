package io.shulie.takin.web.biz.checker;

import com.pamirs.takin.entity.domain.dto.scenemanage.SceneManageWrapperDTO;
import lombok.Data;

@Data
public class WebConditionCheckerContext {

    private Long sceneId;
    private String resourceId;
    private SceneManageWrapperDTO sceneData;
}
