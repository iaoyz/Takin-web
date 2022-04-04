package io.shulie.takin.web.data.dao.report;

import javax.annotation.Resource;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pamirs.takin.entity.domain.entity.report.ReportTaskEntity;
import io.shulie.takin.web.data.mapper.mysql.ReportTaskMapper;
import io.shulie.takin.web.data.util.MPUtil;
import org.springframework.stereotype.Repository;

@Repository
public class ReportTaskDAOImpl extends ServiceImpl<ReportTaskMapper, ReportTaskEntity>
    implements ReportTaskDAO, MPUtil<ReportTaskEntity> {

    @Resource
    private ReportTaskMapper reportTaskMapper;

    public void syncSuccess(String reportId) {
        reportTaskMapper.syncSuccess(reportId);
    }
}
