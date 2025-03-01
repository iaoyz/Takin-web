package io.shulie.takin.web.data.param.application;

import java.util.List;

import io.shulie.takin.web.common.pojo.dto.PageBaseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author liuchuan
 * @date 2021/12/8 3:04 下午
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QueryApplicationParam extends PageBaseDTO {

    /**
     * 应用名称
     */
    private String applicationName;

    /**
     * 应用状态 @see AppAccessStatusEnum
     */
    private Integer accessStatus;

    private List<Long> userIds;

    private Long tenantId;
    private String envCode;

}
