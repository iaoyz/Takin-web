package io.shulie.takin.cloud.data.dao.scene.task;

import io.shulie.takin.cloud.data.model.mysql.PressureTaskEntity;

public interface PressureTaskDAO {

    void save(PressureTaskEntity entity);

    void updateResourceAssociation(String resourceId, Long pressureTaskId);

    void updateById(PressureTaskEntity entity);

    void updateByReportId(PressureTaskEntity entity);
}
