package io.shulie.takin.adapter.api.entrypoint.pressure;

import java.util.List;

import io.shulie.takin.adapter.api.model.request.pressure.PressureParamModifyReq;
import io.shulie.takin.adapter.api.model.request.pressure.PressureParamsReq;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStartReq;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStopReq;
import io.shulie.takin.cloud.model.response.JobConfig;

public interface PressureTaskApi {

    Long start(PressureTaskStartReq req);

    String stop(PressureTaskStopReq req);

    Void modifyParam(PressureParamModifyReq req);

    List<JobConfig> params(PressureParamsReq req);
}
