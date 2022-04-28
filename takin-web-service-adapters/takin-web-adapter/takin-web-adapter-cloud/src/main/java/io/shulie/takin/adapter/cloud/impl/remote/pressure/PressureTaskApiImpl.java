package io.shulie.takin.adapter.cloud.impl.remote.pressure;

import javax.annotation.Resource;

import com.alibaba.fastjson.TypeReference;

import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.adapter.api.entrypoint.pressure.PressureTaskApi;
import io.shulie.takin.adapter.api.model.common.ResponseResult;
import io.shulie.takin.adapter.api.model.request.pressure.PressureParamModifyReq;
import io.shulie.takin.adapter.api.model.request.pressure.PressureParamsReq;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStartReq;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStopReq;
import io.shulie.takin.adapter.api.model.response.pressure.PressureActionResp;
import io.shulie.takin.adapter.api.model.response.pressure.PressureParamsResponse;
import io.shulie.takin.adapter.api.service.CloudApiSenderService;
import org.springframework.stereotype.Service;

@Service
public class PressureTaskApiImpl implements PressureTaskApi {

    @Resource
    private CloudApiSenderService cloudApiSenderService;

    @Override
    public PressureActionResp start(PressureTaskStartReq req) {
        return cloudApiSenderService.post(
            EntrypointUrl.join(EntrypointUrl.MODULE_RRESSURE, EntrypointUrl.METHOD_RRESSURE_START),
            req, new TypeReference<ResponseResult<PressureActionResp>>() {}).getData();
    }

    @Override
    public PressureActionResp stop(PressureTaskStopReq req) {
        return cloudApiSenderService.post(
            EntrypointUrl.join(EntrypointUrl.MODULE_RRESSURE, EntrypointUrl.METHOD_RRESSURE_STOP),
            req, new TypeReference<ResponseResult<PressureActionResp>>() {}).getData();
    }

    @Override
    public PressureActionResp modifyParam(PressureParamModifyReq req) {
        return cloudApiSenderService.post(
            EntrypointUrl.join(EntrypointUrl.MODULE_RRESSURE, EntrypointUrl.METHOD_RRESSURE_MODIFY),
            req, new TypeReference<ResponseResult<PressureActionResp>>() {}).getData();
    }

    @Override
    public PressureParamsResponse params(PressureParamsReq req) {
        return cloudApiSenderService.post(
            EntrypointUrl.join(EntrypointUrl.MODULE_RRESSURE, EntrypointUrl.METHOD_RRESSURE_PARAMS),
            req, new TypeReference<ResponseResult<PressureParamsResponse>>() {}).getData();
    }
}
