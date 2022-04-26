package io.shulie.takin.adapter.api.entrypoint.check;

import io.shulie.takin.adapter.api.model.request.check.EnvCheckRequest;
import io.shulie.takin.adapter.api.model.request.check.ResourceCheckRequest;
import io.shulie.takin.adapter.api.model.response.check.EnvCheckResponse;
import io.shulie.takin.adapter.api.model.response.check.ResourceCheckResponse;

public interface CloudCheckApi {

    ResourceCheckResponse checkResources(ResourceCheckRequest request);

    EnvCheckResponse checkEnv(EnvCheckRequest request);

}
