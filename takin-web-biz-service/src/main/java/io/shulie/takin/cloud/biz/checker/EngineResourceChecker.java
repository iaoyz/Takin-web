package io.shulie.takin.cloud.biz.checker;

import javax.annotation.Resource;

import io.shulie.takin.adapter.api.entrypoint.check.CloudCheckApi;
import io.shulie.takin.adapter.api.model.request.check.ResourceCheckRequest;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.service.strategy.StrategyConfigService;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.ext.content.enginecall.StrategyConfigExt;
import org.springframework.stereotype.Component;

@Component
public class EngineResourceChecker implements PressureStartConditionChecker {

    @Resource
    private CloudCheckApi cloudCheckApi;

    @Resource
    private StrategyConfigService strategyConfigService;

    @Override
    public void check(SceneManageWrapperOutput sceneData, SceneTaskStartInput input) throws TakinCloudException {
        StrategyConfigExt config = strategyConfigService.getCurrentStrategyConfig();
        if (config != null) {
            sceneData.setStrategy(config);
            ResourceCheckRequest request = new ResourceCheckRequest();
            request.setCpu(config.getCpuNum());
            request.setMemory(config.getMemorySize());
            request.setPod(sceneData.getIpNum());
            cloudCheckApi.checkResources(request);
        } else {
            throw new RuntimeException("未配置策略");
        }
    }

    @Override
    public int getOrder() {
        return 4;
    }
}
