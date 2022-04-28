package io.shulie.takin.adapter.cloud.impl.remote.check;

import javax.annotation.Resource;

import com.alibaba.fastjson.TypeReference;

import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.adapter.api.entrypoint.check.CloudCheckApi;
import io.shulie.takin.adapter.api.model.common.ResponseResult;
import io.shulie.takin.adapter.api.model.request.check.EnvCheckRequest;
import io.shulie.takin.adapter.api.service.CloudApiSenderService;
import org.springframework.stereotype.Service;

@Service
public class CloudCheckApiImpl implements CloudCheckApi {

    @Resource
    private CloudApiSenderService cloudApiSenderService;

    @Override
    public Boolean checkEnv(EnvCheckRequest request) {
        return cloudApiSenderService.get(EntrypointUrl.join(EntrypointUrl.MODULE_CHECK, EntrypointUrl.METHOD_CHECK_ENV),
            request, new TypeReference<ResponseResult<Boolean>>() {}).getData();
    }
}
