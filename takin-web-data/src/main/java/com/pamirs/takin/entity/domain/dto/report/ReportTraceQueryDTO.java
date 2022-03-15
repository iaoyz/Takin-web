package com.pamirs.takin.entity.domain.dto.report;

import io.shulie.takin.web.common.pojo.dto.PageBaseDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 压测报告；统计返回
 *
 * @author qianshui
 * @date 2020/7/22 下午2:19
 */
@EqualsAndHashCode(callSuper = true)
@ApiModel("入参类 -- 报告查询入参类")
@Data
public class ReportTraceQueryDTO extends PageBaseDTO {

    private static final long serialVersionUID = 8928035842416997931L;

    @ApiModelProperty("场景id")
    private Long sceneId;

    @ApiModelProperty("报告id，如果是压测报告中有此参数")
    private Long reportId;

    @ApiModelProperty("开始压测的时间戳")
    private Long startTime;

    @ApiModelProperty("压测结束的时间戳")
    private Long endTime;

    @ApiModelProperty("查询条件，null 为全部，1为成功，0为失败, 2 断言失败")
    private Integer type;

    @ApiModelProperty(value = "耗时ms，比较规则 大于")
    private Long minCost;

    @ApiModelProperty(value = "耗时ms，比较规则 小于等于")
    private Long maxCost;

    @ApiModelProperty("调用类型")
    private String middlewareName;

    @ApiModelProperty("调用接口")
    private String interfaceName;

    @ApiModelProperty("调用参数")
    private String request;

    @ApiModelProperty("排序字段：startDate、cost")
    private String sortField;

    @ApiModelProperty("排序方式：asc、desc")
    private String sortType;
}
