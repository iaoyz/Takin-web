package io.shulie.takin.adapter.cloud.impl.pressure;

import javax.annotation.Resource;

import com.alibaba.fastjson.TypeReference;

import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.adapter.api.entrypoint.pressure.PressureTaskApi;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStartReq;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStopReq;
import io.shulie.takin.adapter.api.model.response.pressure.PressureActionResp;
import io.shulie.takin.adapter.api.service.CloudApiSenderService;
import org.springframework.stereotype.Service;

@Service
public class PressureTaskApiImpl implements PressureTaskApi {

    @Resource
    CloudApiSenderService cloudApiSenderService;

    @Override
    public PressureActionResp start(PressureTaskStartReq req) {
        return cloudApiSenderService.post(
            EntrypointUrl.join(EntrypointUrl.MODULE_RRESSURE, EntrypointUrl.METHOD_RRESSURE_START),
            req, new TypeReference<PressureActionResp>() {});
    }

    @Override
    public PressureActionResp stop(PressureTaskStopReq req) {
        return cloudApiSenderService.post(
            EntrypointUrl.join(EntrypointUrl.MODULE_RRESSURE, EntrypointUrl.METHOD_RRESSURE_STOP),
            req, new TypeReference<PressureActionResp>() {});
    }
}
