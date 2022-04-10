package io.shulie.takin.web.biz.service.report.impl;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pamirs.takin.entity.domain.dto.report.ReportPerformanceInterfaceDTO;
import com.pamirs.takin.entity.domain.entity.report.ReportActivityInterfaceEntity;
import io.shulie.takin.web.amdb.api.ReportClient;
import io.shulie.takin.web.amdb.bean.query.report.ReportQueryDTO;
import io.shulie.takin.web.amdb.bean.query.trace.EntranceRuleDTO;
import io.shulie.takin.web.amdb.bean.result.report.ReportActivityInterfaceDTO;
import io.shulie.takin.web.biz.pojo.request.report.ReportPerformanceInterfaceRequest;
import io.shulie.takin.web.biz.service.report.ReportActivityInterfaceService;
import io.shulie.takin.web.biz.service.report.ReportRealTimeService;
import io.shulie.takin.web.data.dao.report.ReportActivityInterfaceDAO;
import io.shulie.takin.web.data.param.report.ReportActivityInterfaceQueryParam;
import io.shulie.takin.web.data.param.report.ReportActivityInterfaceQueryParam.EntranceParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ReportActivityInterfaceServiceImpl implements ReportActivityInterfaceService {

    @Resource
    private ReportClient reportClient;

    @Resource
    private ReportActivityInterfaceDAO interfaceDAO;

    @Resource
    private ReportRealTimeService reportRealTimeService;

    @Override
    public void syncActivityInterface(String reportId) {
        if (log.isDebugEnabled()) {
            log.debug("开始同步压测报告[{}]业务活动接口数据", reportId);
        }
        List<ReportActivityInterfaceDTO> interfaces = reportClient.listReportActivityInterface(
            new ReportQueryDTO(reportId));
        if (CollectionUtils.isNotEmpty(interfaces)) {
            Date now = new Date();
            List<ReportActivityInterfaceEntity> entityList = interfaces.stream().map(activity -> {
                ReportActivityInterfaceEntity entity = new ReportActivityInterfaceEntity();
                BeanUtils.copyProperties(activity, entity);
                entity.setSyncTime(now);
                return entity;
            }).collect(Collectors.toList());
            interfaceDAO.batchInsert(entityList);
            if (log.isDebugEnabled()) {
                log.debug("同步压测报告[{}]业务活动接口数据完成", reportId);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("同步压测报告[{}]业务活动接口数据结束，没有需要同步的数据", reportId);
            }
        }
    }

    @Override
    public Pair<List<ReportPerformanceInterfaceDTO>, Long> queryInterfaceByRequest(ReportPerformanceInterfaceRequest request) {
        ReportActivityInterfaceQueryParam queryParam = buildMetricsParam(request);
        Page<ReportActivityInterfaceEntity> page = PageHelper.startPage(queryParam.getCurrentPage() + 1, queryParam.getPageSize());
        List<ReportActivityInterfaceEntity> entities = interfaceDAO.queryByParam(queryParam);
        List<ReportPerformanceInterfaceDTO> interfaces = entities.stream().map(interfaceEntity -> {
            ReportPerformanceInterfaceDTO dto = new ReportPerformanceInterfaceDTO();
            BeanUtils.copyProperties(interfaceEntity, dto);
            return dto;
        }).collect(Collectors.toList());
        return Pair.of(interfaces, page.getTotal());
    }

    @Override
    public List<ReportPerformanceInterfaceDTO> queryTop5ByRequest(ReportPerformanceInterfaceRequest request) {
        request.setCurrent(0);
        request.setCurrentPage(5);
        request.setSortField("avg_cost");
        request.setSortType("desc");
        return this.queryInterfaceByRequest(request).getKey();
    }

    @Override
    public List<ReportActivityInterfaceEntity> selectByReportId(String reportId) {
        return interfaceDAO.selectByReportId(reportId);
    }

    private ReportActivityInterfaceQueryParam buildMetricsParam(ReportPerformanceInterfaceRequest request) {
        ReportActivityInterfaceQueryParam metricsQueryParam = new ReportActivityInterfaceQueryParam();
        BeanUtils.copyProperties(request, metricsQueryParam);
        List<Long> activityIds = reportRealTimeService.querySceneActivities(request.getXpathMd5(),
            request.getSceneId(), request.getReportId());
        List<EntranceRuleDTO> entrances = reportRealTimeService.getEntryListByBusinessActivityIds(activityIds);
        List<EntranceParam> entranceParamList = entrances.stream()
            .filter(entrance -> StringUtils.countMatches(entrance.getEntrance(), "|") == 2)
            .map(entrance -> {
                EntranceParam param = new EntranceParam();
                param.setAppName(entrance.getAppName());
                String[] entranceArr = entrance.getEntrance().split("\\|");
                param.setServiceName(entranceArr[1]);
                param.setMethodName(entranceArr[0]);
                param.setRpcType(entranceArr[2]);
                return param;
            }).collect(Collectors.toList());
        metricsQueryParam.setEntrances(entranceParamList);
        return metricsQueryParam;
    }
}
