package io.shulie.takin.web.biz.checker;

import javax.annotation.Resource;

import com.pamirs.takin.entity.domain.dto.scenemanage.SceneManageWrapperDTO;
import io.shulie.takin.web.biz.service.scenemanage.SceneTaskService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class WebScriptChecker implements WebStartConditionChecker {

    @Resource
    private SceneTaskService sceneTaskService;

    @Override
    public CheckResult check(WebConditionCheckerContext context) {
        SceneManageWrapperDTO sceneData = context.getSceneData();
        // 压测脚本文件检查
        String scriptCorrelation = sceneTaskService.checkScriptCorrelation(sceneData);
        if (StringUtils.isNotBlank(scriptCorrelation)) {
            return CheckResult.fail(type(), scriptCorrelation);
        }
        return CheckResult.success(type());
    }

    @Override
    public String type() {
        return "file";
    }
}
