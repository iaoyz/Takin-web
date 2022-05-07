package io.shulie.takin.cloud.data.dao.scene.task.impl;

import java.util.Date;

import javax.annotation.Resource;

import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskCallbackDAO;
import io.shulie.takin.cloud.data.mapper.mysql.PressureTaskCallbackMapper;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskCallbackEntity;
import org.springframework.stereotype.Repository;

@Repository
public class PressureTaskCallbackDAOImpl implements PressureTaskCallbackDAO {

    @Resource
    private PressureTaskCallbackMapper mapper;

    @Override
    public void save(PressureTaskCallbackEntity entity) {
        entity.setGmtCreate(new Date());
        mapper.insert(entity);
    }
}
