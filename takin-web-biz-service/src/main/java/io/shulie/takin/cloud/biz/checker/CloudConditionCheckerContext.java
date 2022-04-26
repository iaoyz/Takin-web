package io.shulie.takin.cloud.biz.checker;

import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CloudConditionCheckerContext {
    private Long sceneId;
    private String resourceId;
    private String type;
    private SceneManageWrapperOutput sceneData;
    private SceneTaskStartInput input;
    // 运行中补充
    private Long reportId;
    private Long taskId;

    public static CloudConditionCheckerContext of(Long sceneId) {
        CloudConditionCheckerContext context = new CloudConditionCheckerContext();
        context.setSceneId(sceneId);
        return context;
    }
}
