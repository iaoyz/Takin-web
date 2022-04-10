package io.shulie.takin.web.data.dao.report;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pamirs.takin.entity.domain.entity.report.ReportActivityEntity;
import io.shulie.takin.web.data.mapper.mysql.ReportActivityMapper;
import io.shulie.takin.web.data.util.MPUtil;
import org.springframework.stereotype.Repository;

@Repository
public class ReportActivityDAOImpl extends ServiceImpl<ReportActivityMapper, ReportActivityEntity>
    implements ReportActivityDAO, MPUtil<ReportActivityEntity> {

    @Override
    public boolean batchInsert(List<ReportActivityEntity> entityList) {
        return this.saveBatch(entityList);
    }
}
