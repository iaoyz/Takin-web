package io.shulie.takin.adapter.cloud.impl.check;

import javax.annotation.Resource;

import com.alibaba.fastjson.TypeReference;

import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.adapter.api.entrypoint.check.CloudCheckApi;
import io.shulie.takin.adapter.api.model.request.check.EnvCheckRequest;
import io.shulie.takin.adapter.api.model.request.check.ResourceCheckRequest;
import io.shulie.takin.adapter.api.model.response.check.EnvCheckResponse;
import io.shulie.takin.adapter.api.model.response.check.ResourceCheckResponse;
import io.shulie.takin.adapter.api.service.CloudApiSenderService;
import io.shulie.takin.common.beans.response.ResponseResult;
import org.springframework.stereotype.Service;

@Service
public class CloudCheckApiImpl implements CloudCheckApi {

    @Resource
    private CloudApiSenderService cloudApiSenderService;

    @Override
    public ResourceCheckResponse checkResources(ResourceCheckRequest request) {
        return cloudApiSenderService.get(
            EntrypointUrl.join(EntrypointUrl.MODULE_CHECK, EntrypointUrl.METHOD_CHECK_RESOURCES),
            request, new TypeReference<ResponseResult<ResourceCheckResponse>>() {}).getData();
    }

    @Override
    public EnvCheckResponse checkEnv(EnvCheckRequest request) {
        return cloudApiSenderService.get(EntrypointUrl.join(EntrypointUrl.MODULE_CHECK, EntrypointUrl.METHOD_CHECK_ENV),
            request, new TypeReference<ResponseResult<EnvCheckResponse>>() {}).getData();
    }
}
