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
import io.shulie.takin.web.data.dao.cloud.resouces.CloudResourcesDao;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CloudResourceApiImpl implements CloudResourceApi {

    @Resource
    private CloudApiSenderService cloudApiSenderService;

    @Resource
    private CloudResourcesDao cloudResourcesDao;

    @Override
    public io.shulie.takin.adapter.api.model.response.cloud.resources.Resource getDetails(int taskId, String resourceId, String sortField, String sortType, Integer currentPage, Integer pageSize) {
        io.shulie.takin.adapter.api.model.response.cloud.resources.Resource resource = new io.shulie.takin.adapter.api.model.response.cloud.resources.Resource();
        resource.setTaskId(taskId);
        resource.setResourceId(resourceId);
        resource.setCurrentPage(currentPage);
        resource.setPageSize(pageSize);
        //1.查询压力机明细
        CloudResourcesRequest crr = new CloudResourcesRequest();
        crr.setTaskId(taskId);
        crr.setResourceId(resourceId);
//        List<CloudResource> resources = cloudApiSenderService.get(EntrypointUrl.join(EntrypointUrl.METHOD_RESOURCE_MACHINE), crr, new TypeReference<ResponseResult<List<CloudResource>>>() {
//        }).getData();
        List<CloudResource> resources = getResources();
        if (CollectionUtils.isNotEmpty(resources)) {
            resource.setResources(doTurnPage(resources, sortField, sortType, currentPage, pageSize));
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
            //2.查询状态
            cloudResourcesDao.getResourceStatus(resource);
        }
        return resource;
    }

    private List doTurnPage(List<CloudResource> resources, String sortField, String sortType, Integer currentPage, Integer pageSize) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return resources.stream().sorted((r1, r2) -> {
            switch (sortField) {
                case "startTime":
                    try {
                        Date s1 = sdf.parse(r1.getStartTime());
                        Date s2 = sdf.parse(r2.getStartTime());
                        if (StringUtils.equals("asc", sortType)) {
                            return s1.before(s2) ? -1 : 1;
                        } else {
                            return s1.before(s2) ? 1 : -1;
                        }
                    } catch (ParseException e) {
                        //Ignore
                        return 0;
                    }
                case "stopTime":
                    try {
                        Date s1 = sdf.parse(r1.getStopTime());
                        Date s2 = sdf.parse(r2.getStopTime());
                        if (StringUtils.equals("asc", sortType)) {
                            return s1.before(s2) ? -1 : 1;
                        } else {
                            return s1.before(s2) ? 1 : -1;
                        }
                    } catch (ParseException e) {
                        //Ignore
                        return 0;
                    }
                case "hostIp":
                    String h1 = r1.getHostIp();
                    String h2 = r2.getHostIp();
                    if (StringUtils.equals("asc", sortType)) {
                        return h1.compareTo(h2);
                    } else {
                        return h1.compareTo(h2) == 1 ? -1 : 1;
                    }
                default:
                    return 0;
            }
        }).skip((currentPage - 1) * pageSize).limit(pageSize).collect(Collectors.toList());
    }

    private List<CloudResource> getResources() {
        ArrayList<CloudResource> resources = new ArrayList<>();
        CloudResource cloudResource1 = new CloudResource("1", "Running", 1, "2022-04-24 10:53:19", "2022-04-24 10:53:19", "1111", "a");
        CloudResource cloudResource2 = new CloudResource("2", "Failed", 2, "2021-04-25 10:53:19", "2022-04-25 10:53:19", "2222", "b");
        CloudResource cloudResource3 = new CloudResource("3", "Running", 3, "2022-04-26 10:53:19", "2022-04-26 10:53:19", "3333", "c");
        CloudResource cloudResource4 = new CloudResource("4", "Running", 4, "2023-04-27 10:53:19", "2022-04-27 10:53:19", "4444", "d");
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
                request, new TypeReference<ResponseResult<ResourceLockResponse>>() {
                }).getData();
    }

}
