package io.shulie.takin.cloud.biz.checker;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.cloud.common.utils.CloudPluginUtils;
import io.shulie.takin.cloud.ext.api.AssetExtApi;
import io.shulie.takin.cloud.ext.content.asset.AccountInfoExt;
import io.shulie.takin.plugin.framework.core.PluginManager;
import org.springframework.stereotype.Component;

@Component
public class FlowConditionChecker implements CloudStartConditionChecker {

    @Resource
    private PluginManager pluginManager;

    @Resource
    private CloudSceneManageService cloudSceneManageService;

    @Override
    public void preCheck(Long sceneId) throws TakinCloudException {
        SceneManageWrapperOutput sceneData = cloudSceneManageService.getSceneManage(sceneId, null);
        SceneTaskStartInput input = new SceneTaskStartInput();
        input.setOperateId(CloudPluginUtils.getUserId());
        runningCheck(sceneData, input);
    }

    @Override
    public void runningCheck(SceneManageWrapperOutput sceneData, SceneTaskStartInput input) {
        if (null == sceneData.getTenantId()) {
            throw new TakinCloudException(TakinCloudExceptionEnum.TASK_START_VERIFY_ERROR, "场景没有绑定客户信息");
        }
        AssetExtApi assetExtApi = pluginManager.getExtension(AssetExtApi.class);
        if (assetExtApi != null) {
            AccountInfoExt account = assetExtApi.queryAccount(sceneData.getTenantId(), input.getOperateId());
            if (null == account || account.getBalance().compareTo(sceneData.getEstimateFlow()) < 0) {
                throw new TakinCloudException(TakinCloudExceptionEnum.TASK_START_VERIFY_ERROR, "压测流量不足！");
            }
        }
    }

    @Override
    public String type() {
        return "flow";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
