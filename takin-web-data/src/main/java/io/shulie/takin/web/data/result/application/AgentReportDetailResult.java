package io.shulie.takin.web.data.result.application;

import lombok.ToString;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.shulie.takin.web.data.model.mysql.AgentReportEntity;

/**
 * 探针心跳数据(AgentReport)详情出参类
 *
 * @author ocean_wll
 * @date 2021-11-09 20:35:33
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class AgentReportDetailResult extends AgentReportEntity {

}
