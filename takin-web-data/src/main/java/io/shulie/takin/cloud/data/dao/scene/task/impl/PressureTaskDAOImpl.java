package io.shulie.takin.cloud.data.dao.scene.task.impl;

import java.util.Date;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.shulie.takin.cloud.common.enums.PressureTaskStateEnum;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskDAO;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskVarietyDAO;
import io.shulie.takin.cloud.data.mapper.mysql.PressureTaskMapper;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskEntity;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskVarietyEntity;
import org.springframework.stereotype.Repository;

@Repository
public class PressureTaskDAOImpl implements PressureTaskDAO {

    @Resource
    private PressureTaskMapper pressureTaskMapper;
    @Resource
    private PressureTaskVarietyDAO pressureTaskVarietyDAO;

    @Override
    public PressureTaskEntity selectById(Long id) {
        return pressureTaskMapper.selectById(id);
    }

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
            Wrappers.lambdaQuery(PressureTaskEntity.class).eq(PressureTaskEntity::getReportId, entity.getReportId()));
    }

    @Override
    public PressureTaskEntity queryByResourceId(String resourceId) {
        return pressureTaskMapper.selectOne(Wrappers.lambdaQuery(PressureTaskEntity.class)
            .eq(PressureTaskEntity::getResourceId, resourceId));
    }

    @Override
    public void updateStatus(Long taskId, PressureTaskStateEnum state) {
        PressureTaskEntity entity = new PressureTaskEntity();
        entity.setId(taskId);
        entity.setStatus(state.ordinal());
        entity.setGmtUpdate(new Date());
        pressureTaskMapper.updateById(entity);
        pressureTaskVarietyDAO.save(PressureTaskVarietyEntity.of(taskId, state));
    }
}
