package io.shulie.takin.cloud.data.dao.scene.task.impl;

import java.util.Date;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.shulie.takin.cloud.common.enums.PressureTaskStateEnum;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskDAO;
import io.shulie.takin.cloud.data.mapper.mysql.PressureTaskMapper;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskEntity;
import org.springframework.stereotype.Repository;

@Repository
public class PressureTaskDAOImpl implements PressureTaskDAO {

    @Resource
    private PressureTaskMapper pressureTaskMapper;

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
    public void updateLockStatusAndResourceId(Long id, String resourceId) {
        PressureTaskEntity entity = new PressureTaskEntity();
        entity.setId(id);
        entity.setResourceId(resourceId);
        entity.setStatus(PressureTaskStateEnum.RESOURCES_LOCKING.ordinal());
        entity.setGmtUpdate(new Date());
        pressureTaskMapper.updateById(entity);
    }
}
