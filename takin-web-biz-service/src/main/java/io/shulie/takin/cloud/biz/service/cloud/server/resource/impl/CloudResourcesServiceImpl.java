package io.shulie.takin.cloud.biz.service.cloud.server.resource.impl;

import io.shulie.takin.adapter.api.model.response.cloud.resources.CloudResource;
import io.shulie.takin.adapter.api.model.response.cloud.resources.Resource;
import io.shulie.takin.cloud.biz.service.cloud.server.resource.CloudResourcesService;
import io.shulie.takin.web.data.dao.cloud.resouces.CloudResourcesDao;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CloudResourcesServiceImpl implements CloudResourcesService {
    @javax.annotation.Resource
    private CloudResourcesDao cloudResourcesDao;

    @Override
    public Resource getDetail(List<CloudResource> resources, int taskId, String resourceId, String sortField, String sortType, Integer currentPage, Integer pageSize) {
        Resource resource = new Resource();
        resource.setTaskId(taskId);
        resource.setResourceId(resourceId);
        resource.setCurrentPage(currentPage);
        resource.setPageSize(pageSize);
        //1.查询压力机明细
        if (CollectionUtils.isNotEmpty(resources)) {
            resource.setResources(doTurnPage(resources, sortField, sortType, currentPage, pageSize));
            int size = resources.size();
            resource.setResourcesAmount(size);
            Map<Integer, Integer> collect = resources.stream().collect(Collectors.toMap(CloudResource::getStatus, n -> 1, (n1, n2) -> n1 + 1));
            collect.entrySet().forEach(entry -> {
                switch (entry.getKey()) {
                    case 0:
                        resource.setInitializedAmount(entry.getValue());
                        //默认全部压测中完成
                        break;
                    case 1:
                        resource.setAliveAmount(entry.getValue());
                        break;
                    case 3:
                        resource.setUnusualAmount(entry.getValue());
                        break;
                    case 2:
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
}
