package io.shulie.takin.web.amdb.api;

import java.util.List;

import com.pamirs.pradar.log.parser.trace.RpcBased;
import com.pamirs.pradar.log.parser.trace.RpcStack;
import io.shulie.takin.common.beans.page.PagingList;
import io.shulie.takin.web.amdb.bean.query.script.QueryLinkDetailDTO;
import io.shulie.takin.web.amdb.bean.query.trace.TraceInfoQueryDTO;
import io.shulie.takin.web.amdb.bean.result.trace.EntryTraceInfoDTO;

/**
 * @author shiyajian
 * create: 2020-10-12
 */
public interface TraceClient {

    /**
     * 获得请求流量明细
     *
     * @param dto 请求参数
     * @return 分页数据
     */
    PagingList<EntryTraceInfoDTO> listEntryTraceByTaskId(QueryLinkDetailDTO dto);

    /**
     * 查询入口的trace请求流量信息
     * 用于压测实况，还有压测报告中的请求流量明细功能
     */
    PagingList<EntryTraceInfoDTO> listEntryTraceInfo(TraceInfoQueryDTO query);

    /**
     * 根据 traceId 查询Trace的调用栈
     */
    RpcStack getTraceDetailById(String traceId);

    /**
     * 根据traceID 查询base
     * @param traceId
     * @return
     */
    List<RpcBased> getTraceBaseById(String traceId);

}
