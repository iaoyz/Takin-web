package io.shulie.takin.web.data.dao;

import java.util.List;

import io.shulie.takin.web.data.param.application.CreateApplicationNodeProbeParam;
import io.shulie.takin.web.data.param.probe.UpdateOperateResultParam;
import io.shulie.takin.web.data.result.application.ApplicationNodeProbeResult;

/**
 * 应用节点探针操作表(ApplicationNodeProbe)表数据库 dao
 *
 * @author liuchuan
 * @since 2021-06-03 13:43:18
 */
public interface ApplicationNodeProbeDAO {

    /**
     * 通过 应用名称, agentId 获得对应的探针操作记录
     *
     * @param applicationName 应用名称
     * @param agentId         agentId
     * @return 探针操作记录
     */
    ApplicationNodeProbeResult getByApplicationNameAndAgentId(String applicationName, String agentId);

    /**
     * 根据 应用名称,  agentId 更新操作结果
     *
     * @param updateOperateResultParam 更新所需参数
     * @return 是否更新成功
     */
    boolean updateById(UpdateOperateResultParam updateOperateResultParam);

    /**
     * 创建节点探针操作记录
     *
     * @param createApplicationNodeProbeParam 创建所需参数
     * @return 是否成功
     */
    boolean create(CreateApplicationNodeProbeParam createApplicationNodeProbeParam);

    /**
     * 根据应用名称, agentIds 获得节点探针操作记录
     *
     * @param applicationName 应用名称
     * @param agentIds        agentIds
     * @return 节点探针操作记录列表
     */
    List<ApplicationNodeProbeResult> listByApplicationNameAndAgentIds(String applicationName, List<String> agentIds);

    /**
     * 根据appnames和操作id删除
     *
     * @param operate
     * @param appNames
     */
    void delByAppNamesAndOperate(Integer operate, List<String> appNames);

    /**
     * 通过拿到最大的 tenantId 的操作
     * 因为这个接口没有做拦截, 所以先这么操作
     * admin 的是 1, 比他大的就是那个租户的操作
     *
     * @param applicationName 应用名称
     * @param agentId         agentId
     * @param tenantId        租户 id
     * @return 探针操作记录
     */
    ApplicationNodeProbeResult getByApplicationNameAndAgentIdAndMaxTenantId(String applicationName, String agentId,
        Long tenantId);

}

