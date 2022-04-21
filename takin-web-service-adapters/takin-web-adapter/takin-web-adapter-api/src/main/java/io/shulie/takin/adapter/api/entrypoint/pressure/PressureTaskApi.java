package io.shulie.takin.adapter.api.entrypoint.pressure;

import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStartReq;
import io.shulie.takin.adapter.api.model.response.pressure.PressureActionResp;

public interface PressureTaskApi {

    PressureActionResp start(PressureTaskStartReq req);

}
