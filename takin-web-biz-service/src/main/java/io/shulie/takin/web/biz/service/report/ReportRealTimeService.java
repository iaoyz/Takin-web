package io.shulie.takin.web.biz.service.report;

import java.util.List;

import com.github.pagehelper.PageInfo;
import com.pamirs.takin.entity.domain.dto.report.ReportTraceDTO;
import com.pamirs.takin.entity.domain.dto.report.ReportTraceQueryDTO;
import io.shulie.takin.web.amdb.bean.query.trace.EntranceRuleDTO;
import io.shulie.takin.web.biz.pojo.response.report.ReportLinkDetailResponse;

/**
 * @author qianshui
 * @date 2020/8/17 下午8:22
 */
public interface ReportRealTimeService {

    PageInfo<ReportTraceDTO> getReportLinkList(ReportTraceQueryDTO queryDTO);

    PageInfo<ReportTraceDTO> getReportLinkListByReportId(ReportTraceQueryDTO queryDTO);

    /**
     * 报告链路详情
     *
     * @param traceId           traceId
     * @param amdbReportTraceId amdb 报告踪迹详情的主键id
     * @return 报告链路详情
     */
    ReportLinkDetailResponse getLinkDetail(String traceId, Integer amdbReportTraceId);

    List<EntranceRuleDTO> getEntryListByBusinessActivityIds(List<Long> businessActivityIds);

    /**
     * 查询场景对应的业务活动Id
     *
     * @param xpathMd5 xpathMd5
     * @param sceneId  场景Id
     * @param reportId 报告Id
     * @return 业务活动Id集合
     */
    List<Long> querySceneActivities(String xpathMd5, Long sceneId, Long reportId);
}
