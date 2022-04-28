package io.shulie.takin.cloud.common.bean.collector;

import lombok.Data;

@Data
public class SlaInfo {
    /**
     * 关键词
     */
    private String ref;
    /**
     * 任务主键
     */
    private long jobId;
    /**
     * 任务实例主键
     */
    private long jobExampleId;
    /**
     * 资源主键
     */
    private long resourceId;
    /**
     * 资源实例主键
     */
    private long resourceExampleId;
    /**
     * 算式目标
     * <p>(RT、TPS、SA、成功率)</p>
     */
    private Integer formulaTarget;
    /**
     * 算式符号
     * <p>(>=、>、=、<=、<)</p>
     */
    private Integer formulaSymbol;
    /**
     * 算式数值
     * <p>(用户输入)</p>
     */
    private Double formulaNumber;
    /**
     * 比较的值
     * <p>(实际变化的值)</p>
     */
    private Double number;
}
