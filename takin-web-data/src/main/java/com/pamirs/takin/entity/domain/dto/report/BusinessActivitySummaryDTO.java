package com.pamirs.takin.entity.domain.dto.report;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * @author 莫问
 * @date 2020-04-17
 */
@Data
public class BusinessActivitySummaryDTO implements Serializable {

    private static final long serialVersionUID = 2170610535603454800L;

    @ApiModelProperty(value = "活动ID")
    private Long businessActivityId;

    @ApiModelProperty(value = "活动名称")
    private String businessActivityName;

    @ApiModelProperty(value = "应用ID")
    private String applicationIds;

    @ApiModelProperty(value = "绑定关系")
    private String bindRef;

    @ApiModelProperty(value = "总请求")
    private Long totalRequest;

    @ApiModelProperty(value = "平均线程数")
    private BigDecimal avgConcurrenceNum;

    @ApiModelProperty(value = "TPS")
    private Data tps;

    @ApiModelProperty(value = "平均RT")
    private Data avgRT;

    @ApiModelProperty(value = "请求成功率")
    private Data sucessRate;

    @ApiModelProperty(value = "SA")
    private Data sa;

    @ApiModelProperty(value = "最大tps")
    private BigDecimal maxTps;

    @ApiModelProperty(value = "最大rt")
    private BigDecimal maxRt;

    @ApiModelProperty(value = "最小rt")
    private BigDecimal minRt;

    @ApiModelProperty(value = "分布")
    private List<Distribute> distribute;

    @ApiModelProperty(value = "通过标识")
    private Integer passFlag;


    @lombok.Data
    public static class Data implements Serializable {

        @ApiModelProperty(value = "实际")
        private Object result;
        @ApiModelProperty(value = "目标")
        private Object value;

        public Data() { }

        public Data(Object result, Object value) {
            this.result = result;
            this.value = value;
        }

    }

    @lombok.Data
    public static class Distribute implements Serializable {

        private static final long serialVersionUID = -1276142153135835055L;

        @ApiModelProperty(value = "分位")
        private String lable;

        @ApiModelProperty(value = "分位值")
        private String value;
    }
}

