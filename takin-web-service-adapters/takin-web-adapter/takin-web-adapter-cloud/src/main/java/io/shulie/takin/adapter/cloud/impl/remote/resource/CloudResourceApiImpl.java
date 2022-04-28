package io.shulie.takin.adapter.cloud.impl.remote.resource;

import java.util.List;

import javax.annotation.Resource;

import com.alibaba.fastjson.TypeReference;

import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.adapter.api.entrypoint.resource.CloudResourceApi;
import io.shulie.takin.adapter.api.model.common.ResponseResult;
import io.shulie.takin.adapter.api.model.request.resource.ResourceCheckRequest;
import io.shulie.takin.adapter.api.model.request.cloud.resources.CloudResourcesRequest;
import io.shulie.takin.adapter.api.model.request.resource.PhysicalResourceRequest;
import io.shulie.takin.adapter.api.model.request.resource.ResourceLockRequest;
import io.shulie.takin.adapter.api.model.request.resource.ResourceUnLockRequest;
import io.shulie.takin.adapter.api.model.response.cloud.resources.CloudResource;
import io.shulie.takin.adapter.api.model.response.resource.PhysicalResourceResponse;
import io.shulie.takin.adapter.api.model.response.resource.ResourceUnLockResponse;
import io.shulie.takin.adapter.api.service.CloudApiSenderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
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
    public Boolean check(ResourceCheckRequest request) {
        return cloudApiSenderService.post(
            EntrypointUrl.join(EntrypointUrl.MODULE_RESOURCE, EntrypointUrl.MODULE_CHECK),
            request, new TypeReference<ResponseResult<Boolean>>() {
            }).getData();
    }

    @Override
    public String lock(ResourceLockRequest request) {
        return cloudApiSenderService.post(
                EntrypointUrl.join(EntrypointUrl.MODULE_RESOURCE, String.format(EntrypointUrl.METHOD_RESOURCE_LOCK, request.getCallbackUrl())),
                request, new TypeReference<ResponseResult<String>>() {
                }).getData();
    }

    @Override
    public void unLock(ResourceUnLockRequest request) {
        try {
            cloudApiSenderService.get(
                EntrypointUrl.join(EntrypointUrl.MODULE_RESOURCE, EntrypointUrl.METHOD_RESOURCE_UNLOCK),
                request, new TypeReference<ResponseResult<ResourceUnLockResponse>>() {
                }).getData();
        } catch (Exception e) {
            log.error("释放资源异常", e);
        }
    }
}
