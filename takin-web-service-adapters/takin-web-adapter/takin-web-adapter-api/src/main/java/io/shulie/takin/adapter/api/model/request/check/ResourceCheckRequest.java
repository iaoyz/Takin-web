package io.shulie.takin.adapter.api.model.request.check;

import java.math.BigDecimal;

import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceCheckRequest extends ContextExt {

    public static final String WATCH_MAN_ID = "cloud";

    /**
     * 压力机所需要的cpu
     */
    private BigDecimal cpu;
    /**
     * 压力机所需要的内存
     */
    private BigDecimal memory;
    /**
     * 需要的压力机数
     */
    private Integer pod;
    /**
     * 调度主键
     */
    private String watchmanId = WATCH_MAN_ID;

}
