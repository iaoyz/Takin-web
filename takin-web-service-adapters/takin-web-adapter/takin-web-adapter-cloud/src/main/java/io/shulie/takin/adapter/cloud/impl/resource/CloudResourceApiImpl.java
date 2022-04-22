package io.shulie.takin.adapter.cloud.impl.resource;

import com.alibaba.fastjson.TypeReference;
import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.adapter.api.entrypoint.resource.CloudResourceApi;
import io.shulie.takin.adapter.api.model.request.cloud.resources.CloudResourcesRequest;
import io.shulie.takin.adapter.api.model.request.resource.PhysicalResourceRequest;
import io.shulie.takin.adapter.api.model.request.resource.ResourceLockRequest;
import io.shulie.takin.adapter.api.model.response.cloud.resources.CloudResource;
import io.shulie.takin.adapter.api.model.response.cloud.resources.Status;
import io.shulie.takin.adapter.api.model.response.resource.PhysicalResourceResponse;
import io.shulie.takin.adapter.api.model.response.resource.ResourceLockResponse;
import io.shulie.takin.adapter.api.service.CloudApiSenderService;
import io.shulie.takin.common.beans.response.ResponseResult;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CloudResourceApiImpl implements CloudResourceApi {

    @Resource
    private CloudApiSenderService cloudApiSenderService;

    @Override
    public io.shulie.takin.adapter.api.model.response.cloud.resources.Resource getDetails(int taskId, String resourceId) {
        io.shulie.takin.adapter.api.model.response.cloud.resources.Resource resource = new io.shulie.takin.adapter.api.model.response.cloud.resources.Resource();
        //1.查询压力机明细
        CloudResourcesRequest crr = new CloudResourcesRequest();
        crr.setTaskId(taskId);
        crr.setResourceId(resourceId);
//        List<CloudResource> resources = cloudApiSenderService.get(EntrypointUrl.join(EntrypointUrl.METHOD_RESOURCE_MACHINE), crr, new TypeReference<ResponseResult<List<CloudResource>>>() {
//        }).getData();
        List<CloudResource> resources = getResources();
        //2.更新状态
        if (CollectionUtils.isNotEmpty(resources)) {
            resource.setResources(resources);
            int size = resources.size();
            resource.setResourcesAmount(size);
            Map<String, Integer> collect = resources.stream().collect(Collectors.toMap(CloudResource::getStatus, n -> 1, (n1, n2) -> n1 + 1));
            collect.entrySet().forEach(entry -> {
                switch (entry.getKey()) {
                    case "initialized":
//                        resource.setInitializedAmount(entry.getValue());
                        //默认全部压测中完成
                        break;
                    case "Running":
                        resource.setAliveAmount(entry.getValue());
                        break;
                    case "Failed":
                        resource.setUnusualAmount(entry.getValue());
                        break;
                    case "Succeeded":
                        resource.setInactiveAmount(entry.getValue());
                }
            });

            if (resource.getUnusualAmount() != 0) {//存在异常情况
                //调用停止接口
                //TODO stop
                if (resource.getUnusualAmount() + resource.getInactiveAmount() == size) {//异常➕停止等于总数代表结束
                    resource.setStatus(Status.INACTIVE);
                } else {//存在非异常非停止的情况，停止中
                    resource.setStatus(Status.STOPPING);
                }
            } else {//不存在异常情况
                if (resource.getInactiveAmount() != 0) {//存在停止情况
                    if (resource.getInactiveAmount() == size) {//停止数等于总数代表结束
                        resource.setStatus(Status.INACTIVE);
                    } else {//部分停止代表停止中
                        resource.setStatus(Status.STOPPING);
                    }
                } else {//不存在停止及异常情况
                    if (resource.getAliveAmount() == size) {//压测运行中数量等于总数代表进入压测中
                        resource.setStatus(Status.PRESSURING);
                    } else {//部分运行中(理论不会出现这种情况默认都是压测中)
                        resource.setStatus(Status.STARTING);
                    }
                }
            }
        }
        return resource;
    }

    private List<CloudResource> getResources() {
        ArrayList<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource1 = new CloudResource("1", "Running", 1, "1111", "1111", "1111");
        CloudResource cloudResource2 = new CloudResource("2", "Failed", 2, "2222", "2222", "2222");
        CloudResource cloudResource3 = new CloudResource("3", "Running", 3, "3333", "3333", "3333");
        CloudResource cloudResource4 = new CloudResource("4", "Running", 4, "4444", "4444", "4444");
        resources.add(cloudResource1);
        resources.add(cloudResource2);
        resources.add(cloudResource3);
        resources.add(cloudResource4);
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
            request, new TypeReference<ResponseResult<ResourceLockResponse>>() {}).getData();
    }

}
