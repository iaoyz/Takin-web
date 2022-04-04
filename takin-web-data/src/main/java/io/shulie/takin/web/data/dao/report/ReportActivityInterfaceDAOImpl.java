package io.shulie.takin.web.data.dao.report;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pamirs.takin.entity.domain.entity.report.ReportActivityInterfaceEntity;
import io.shulie.takin.web.data.mapper.mysql.ReportActivityInterfaceMapper;
import io.shulie.takin.web.data.param.report.ReportActivityInterfaceQueryParam;
import io.shulie.takin.web.data.param.report.ReportActivityInterfaceQueryParam.EntranceParam;
import io.shulie.takin.web.data.util.MPUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

@Repository
public class ReportActivityInterfaceDAOImpl
    extends ServiceImpl<ReportActivityInterfaceMapper, ReportActivityInterfaceEntity>
    implements ReportActivityInterfaceDAO, MPUtil<ReportActivityInterfaceEntity> {

    @Override
    public boolean batchInsert(List<ReportActivityInterfaceEntity> entityList) {
        return this.saveBatch(entityList, 200);
    }

    @Override
    public List<ReportActivityInterfaceEntity> selectByReportId(String reportId) {
        LambdaQueryWrapper<ReportActivityInterfaceEntity> wrapper = this.getLambdaQueryWrapper()
            .eq(ReportActivityInterfaceEntity::getReportId, reportId);
        return this.list(wrapper);
    }

    @Override
    public List<ReportActivityInterfaceEntity> queryByParam(ReportActivityInterfaceQueryParam param) {
        return this.list(buildQueryWrapper(param));
    }

    private Wrapper<ReportActivityInterfaceEntity> buildQueryWrapper(ReportActivityInterfaceQueryParam param) {
        LambdaQueryWrapper<ReportActivityInterfaceEntity> wrapper = this.getLambdaQueryWrapper()
            .eq(ReportActivityInterfaceEntity::getReportId, param.getReportId());
        List<EntranceParam> entrances = param.getEntrances();
        if (CollectionUtils.isNotEmpty(entrances)) {
            wrapper.and(wp -> entrances.forEach(entrance -> wp.or()
                .and(
                    p -> p.eq(ReportActivityInterfaceEntity::getAppName, entrance.getAppName())
                        .eq(ReportActivityInterfaceEntity::getServiceName, entrance.getServiceName())
                        .eq(ReportActivityInterfaceEntity::getMethodName, entrance.getMethodName())
                        .eq(ReportActivityInterfaceEntity::getRpcType, entrance.getRpcType())
                )));
        }
        String sortField = param.getSortField();
        if (StringUtils.isNotBlank(sortField)) {
            wrapper.last(" order by " + sortField + " " + param.getSortType() + " ");
        }
        return wrapper;
    }
}
