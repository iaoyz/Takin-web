package io.shulie.takin.adapter.api.model.request.resource;

import java.math.BigDecimal;

import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceCheckRequest extends ContextExt {

    public static final Long WATCH_MAN_ID = 1L;

    /**
     * 压力机所需要的cpu
     */
    private String cpu;
    /**
     * 压力机所需要的内存
     */
    private String memory;
    /**
     * 需要的压力机数
     */
    private Integer number;
    /**
     * 限制的CPU
     */
    private String limitCpu;
    /**
     * 限制的内存
     */
    private String limitMemory;
    /**
     * 调度主键
     */
    private Long watchmanId = WATCH_MAN_ID;

}
