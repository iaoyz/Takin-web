package io.shulie.takin.web.data.dao.cloud.resouces.impl;

import io.shulie.takin.adapter.api.model.response.cloud.resources.Resource;
import io.shulie.takin.web.data.dao.cloud.resouces.CloudResourcesDao;
import io.shulie.takin.web.data.mapper.mysql.CloudResourcesMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CloudResourcesDaoImpl implements CloudResourcesDao {

    @javax.annotation.Resource
    private CloudResourcesMapper cloudResourcesMapper;

    @Override
    public void getResourceStatus(Resource resource) {
        Resource statusAndErrorMessage = cloudResourcesMapper.getResourceStatus(resource);
        //TODO convert status and error message
        if (null != statusAndErrorMessage) {
            resource.setStatus(statusAndErrorMessage.getStatus());
            resource.setErrorMessage(statusAndErrorMessage.getErrorMessage());
        }
    }
}
