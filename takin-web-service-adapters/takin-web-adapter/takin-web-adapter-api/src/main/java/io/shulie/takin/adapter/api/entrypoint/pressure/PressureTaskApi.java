package io.shulie.takin.adapter.api.entrypoint.pressure;

import io.shulie.takin.adapter.api.model.request.pressure.PressureParamModifyReq;
import io.shulie.takin.adapter.api.model.request.pressure.PressureParamsReq;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStartReq;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStopReq;
import io.shulie.takin.adapter.api.model.response.pressure.PressureActionResp;
import io.shulie.takin.adapter.api.model.response.pressure.PressureParamsResponse;

public interface PressureTaskApi {

    PressureActionResp start(PressureTaskStartReq req);

    PressureActionResp stop(PressureTaskStopReq req);

    PressureActionResp modifyParam(PressureParamModifyReq req);

    PressureParamsResponse params(PressureParamsReq req);
}
