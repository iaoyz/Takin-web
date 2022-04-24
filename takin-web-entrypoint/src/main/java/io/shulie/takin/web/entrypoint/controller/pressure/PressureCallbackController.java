package io.shulie.takin.web.entrypoint.controller.pressure;

import javax.annotation.Resource;

import com.pamirs.takin.cloud.entity.domain.vo.engine.EngineNotifyParam;
import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.cloud.biz.service.scene.EngineCallbackService;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(EntrypointUrl.BASIC + "/" + EntrypointUrl.MODULE_ENGINE_CALLBACK)
public class PressureCallbackController {

    @Resource
    private EngineCallbackService engineCallbackService;

    @PostMapping(EntrypointUrl.METHOD_ENGINE_CALLBACK_TASK_RESULT_NOTIFY)
    @ApiOperation(value = "cloud回调状态")
    public ResponseResult<?> taskResultNotify(@RequestBody EngineNotifyParam notify) {
        notify.setTenantId(notify.getTenantId() == null ? notify.getCustomerId() : notify.getTenantId());
        return engineCallbackService.notifyEngineState(notify);
    }
}
