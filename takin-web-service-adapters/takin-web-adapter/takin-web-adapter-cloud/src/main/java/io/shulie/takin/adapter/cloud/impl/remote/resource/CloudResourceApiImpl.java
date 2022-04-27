package io.shulie.takin.adapter.cloud.impl.remote.resource;

import com.alibaba.fastjson.TypeReference;
import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.adapter.api.entrypoint.resource.CloudResourceApi;
import io.shulie.takin.adapter.api.model.request.cloud.resources.CloudResourcesRequest;
import io.shulie.takin.adapter.api.model.request.resource.PhysicalResourceRequest;
import io.shulie.takin.adapter.api.model.request.resource.ResourceLockRequest;
import io.shulie.takin.adapter.api.model.response.cloud.resources.CloudResource;
import io.shulie.takin.adapter.api.model.response.resource.PhysicalResourceResponse;
import io.shulie.takin.adapter.api.model.response.resource.ResourceLockResponse;
import io.shulie.takin.adapter.api.service.CloudApiSenderService;
import io.shulie.takin.common.beans.response.ResponseResult;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class CloudResourceApiImpl implements CloudResourceApi {

    @Resource
    private CloudApiSenderService cloudApiSenderService;

    @Override
    public List<CloudResource> getDetails(int taskId, String resourceId) {
        CloudResourcesRequest crr = new CloudResourcesRequest();
        crr.setTaskId(taskId);
        crr.setResourceId(resourceId);
        List<CloudResource> resources = cloudApiSenderService.get(EntrypointUrl.join(EntrypointUrl.METHOD_RESOURCE_MACHINE), crr, new TypeReference<ResponseResult<List<CloudResource>>>() {
        }).getData();
        return resources;
    }

    @Override
    public PhysicalResourceResponse physicalResource(PhysicalResourceRequest request) {
        return null;
    }

    @Override
    public ResourceLockResponse lockResource(ResourceLockRequest request) {
        return cloudApiSenderService.get(
                EntrypointUrl.join(EntrypointUrl.MODULE_RESOURCE, EntrypointUrl.METHOD_RESOURCE_LOCK),
                request, new TypeReference<ResponseResult<ResourceLockResponse>>() {
                }).getData();
    }

}
