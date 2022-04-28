package io.shulie.takin.adapter.api.entrypoint.check;

import io.shulie.takin.adapter.api.model.request.check.EnvCheckRequest;

public interface CloudCheckApi {

    Boolean checkEnv(EnvCheckRequest request);

}
