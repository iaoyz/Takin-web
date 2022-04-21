package io.shulie.takin.adapter.api.entrypoint.resource;

import io.shulie.takin.adapter.api.model.response.cloud.resources.CloudResource;
import io.shulie.takin.adapter.api.model.response.cloud.resources.Resource;

import java.util.List;

public interface CloudResourceApi {
    Resource getDetails(int taskId, String resourceId);
}
