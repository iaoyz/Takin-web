package io.shulie.takin.web.biz.pojo.request.pradar;

import io.shulie.takin.web.common.pojo.dto.PageBaseDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Data
@ApiModel(value = "PradarZKConfigQueryRequest", description = "zk配置查询入参")
public class PradarZKConfigQueryRequest extends PageBaseDTO {

    @ApiModelProperty("配置ID")
    private Long id;

    @ApiModelProperty(value = "路径")
    private String zkPath;

    @ApiModelProperty(value = "类型")
    private String type;

    @ApiModelProperty(value = "数值")
    private String value;

    @ApiModelProperty(value = "说明")
    private String remark;

    @ApiModelProperty(value = "开始时间")
    private String startTime;

    @ApiModelProperty(value = "结束时间")
    private String endTime;
}
