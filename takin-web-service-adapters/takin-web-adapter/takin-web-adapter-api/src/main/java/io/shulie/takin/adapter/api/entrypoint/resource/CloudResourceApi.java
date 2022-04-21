package io.shulie.takin.adapter.api.entrypoint.resource;

import io.shulie.takin.adapter.api.model.request.resource.PhysicalResourceRequest;
import io.shulie.takin.adapter.api.model.request.resource.ResourceLockRequest;
import io.shulie.takin.adapter.api.model.response.cloud.resources.CloudResource;
import io.shulie.takin.adapter.api.model.response.cloud.resources.Resource;
import io.shulie.takin.adapter.api.model.response.resource.PhysicalResourceResponse;
import io.shulie.takin.adapter.api.model.response.resource.ResourceLockResponse;

import java.util.List;

public interface CloudResourceApi {

    Resource getDetails(int taskId, String resourceId);

    PhysicalResourceResponse physicalResource(PhysicalResourceRequest request);

    ResourceLockResponse lockResource(ResourceLockRequest request);
}
