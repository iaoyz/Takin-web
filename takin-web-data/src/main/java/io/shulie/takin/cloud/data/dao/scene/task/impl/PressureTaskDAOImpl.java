package io.shulie.takin.cloud.data.dao.scene.task.impl;

import java.util.Date;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskDAO;
import io.shulie.takin.cloud.data.mapper.mysql.PressureTaskMapper;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskEntity;
import org.springframework.stereotype.Repository;

@Repository
public class PressureTaskDAOImpl implements PressureTaskDAO {

    @Resource
    private PressureTaskMapper pressureTaskMapper;

    @Override
    public void deleteById(Long taskId) {
        PressureTaskEntity entity = new PressureTaskEntity();
        entity.setId(taskId);
        entity.setGmtUpdate(new Date());
        entity.setIsDeleted(1);
        pressureTaskMapper.updateById(entity);
    }

    @Override
    public void save(PressureTaskEntity entity) {
        pressureTaskMapper.insert(entity);
    }

    @Override
    public void updateResourceAssociation(String resourceId, Long pressureTaskId) {
        pressureTaskMapper.update(new PressureTaskEntity() {{setPressureTaskId(pressureTaskId);}},
            Wrappers.lambdaQuery(PressureTaskEntity.class).eq(PressureTaskEntity::getResourceId, resourceId));
    }

    @Override
    public void updateById(PressureTaskEntity entity) {
        pressureTaskMapper.updateById(entity);
    }

    @Override
    public void updateByReportId(PressureTaskEntity entity) {
        pressureTaskMapper.update(entity,
            Wrappers.lambdaQuery(PressureTaskEntity.class).eq(PressureTaskEntity::getResourceId, entity.getResourceId()));
    }

    @Override
    public PressureTaskEntity queryByResourceId(String resourceId) {
        return pressureTaskMapper.selectOne(Wrappers.lambdaQuery(PressureTaskEntity.class)
            .eq(PressureTaskEntity::getResourceId, resourceId));
    }
}
