package io.shulie.takin.cloud.data.dao.scene.task.impl;

import java.util.Date;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskVarietyDAO;
import io.shulie.takin.cloud.data.mapper.mysql.PressureTaskVarietyMapper;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskVarietyEntity;
import org.springframework.stereotype.Repository;

@Repository
public class PressureTaskVarietyDAOImpl implements PressureTaskVarietyDAO {

    @Resource
    private PressureTaskVarietyMapper mapper;

    @Override
    public void save(PressureTaskVarietyEntity entity) {
        entity.setGmtCreate(new Date());
        mapper.insert(entity);
    }

    @Override
    public void updateMessage(PressureTaskVarietyEntity entity) {
        mapper.update(new PressureTaskVarietyEntity() {{setMessage(entity.getMessage());}},
            Wrappers.lambdaQuery(PressureTaskVarietyEntity.class)
            .eq(PressureTaskVarietyEntity::getTaskId, entity.getTaskId())
            .eq(PressureTaskVarietyEntity::getStatus, entity.getStatus()));
    }
}
