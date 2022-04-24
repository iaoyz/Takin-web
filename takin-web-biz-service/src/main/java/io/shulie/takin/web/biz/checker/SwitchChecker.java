package io.shulie.takin.web.biz.checker;

import javax.annotation.Resource;

import com.pamirs.takin.common.constant.AppSwitchEnum;
import com.pamirs.takin.entity.domain.dto.scenemanage.SceneManageWrapperDTO;
import io.shulie.takin.web.biz.service.ApplicationService;
import io.shulie.takin.web.common.exception.TakinWebException;
import io.shulie.takin.web.common.exception.TakinWebExceptionEnum;
import org.springframework.stereotype.Component;

@Component
public class SwitchChecker implements WebStartConditionChecker {

    @Resource
    private ApplicationService applicationService;

    @Override
    public CheckResult preCheck(Long sceneId) {
        try {
            runningCheck(null);
            return CheckResult.success(type());
        } catch (Exception e) {
            return CheckResult.fail(type(), e.getMessage());
        }
    }

    @Override
    public void runningCheck(SceneManageWrapperDTO sceneData) {
        //检查压测开关，压测开关处于关闭状态时禁止压测
        String switchStatus = applicationService.getUserSwitchStatusForVo();
        if (!AppSwitchEnum.OPENED.getCode().equals(switchStatus)) {
            throw new TakinWebException(TakinWebExceptionEnum.SCENE_START_STATUS_ERROR, "压测开关处于关闭状态，禁止压测");
        }
    }

    @Override
    public String type() {
        return "switch";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
