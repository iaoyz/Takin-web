package io.shulie.takin.cloud.biz.checker;

import javax.annotation.Resource;

import io.shulie.takin.adapter.api.entrypoint.check.CloudCheckApi;
import io.shulie.takin.adapter.api.model.request.check.ResourceCheckRequest;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.biz.service.strategy.StrategyConfigService;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.ext.content.enginecall.StrategyConfigExt;
import org.springframework.stereotype.Component;

@Component
public class EngineResourceChecker implements CloudStartConditionChecker {

    @Resource
    private CloudCheckApi cloudCheckApi;

    @Resource
    private StrategyConfigService strategyConfigService;

    @Resource
    private CloudSceneManageService cloudSceneManageService;

    @Override
    public void preCheck(Long sceneId) throws TakinCloudException {
        SceneManageWrapperOutput sceneData = cloudSceneManageService.getSceneManage(sceneId, null);
        runningCheck(sceneData, null);
    }

    @Override
    public void runningCheck(SceneManageWrapperOutput sceneData, SceneTaskStartInput input) {
        StrategyConfigExt config = getStrategy();
        sceneData.setStrategy(config);
        ResourceCheckRequest request = new ResourceCheckRequest();
        request.setCpu(config.getCpuNum());
        request.setMemory(config.getMemorySize());
        request.setPod(sceneData.getIpNum());
        cloudCheckApi.checkResources(request);
    }

    private StrategyConfigExt getStrategy() {
        StrategyConfigExt config = strategyConfigService.getCurrentStrategyConfig();
        if (config == null) {
            throw new RuntimeException("未配置策略");
        }
        return config;
    }

    @Override
    public String type() {
        return "resource";
    }

    @Override
    public int getOrder() {
        return 4;
    }
}
