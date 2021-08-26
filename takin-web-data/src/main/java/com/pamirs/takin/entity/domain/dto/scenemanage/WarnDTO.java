package com.pamirs.takin.entity.domain.dto.scenemanage;

import java.io.Serializable;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author 莫问
 * @date 2020-04-17
 */
@Data
public class WarnDTO implements Serializable {

    @ApiModelProperty(value = "报告 ID")
    private Long reportId;

    @ApiModelProperty(value = "SLA ID")
    private Long slaId;

    @ApiModelProperty(value = "SLA名称")
    private String slaName;

    @ApiModelProperty(value = "活动ID")
    private String businessActivityId;

    @ApiModelProperty(value = "活动名称")
    private String businessActivityName;

    @ApiModelProperty(value = "警告次数")
    private Long total;

    @ApiModelProperty(value = "规则明细")
    private String content;

    @ApiModelProperty(value = "最新警告时间")
    private String lastWarnTime;

}
