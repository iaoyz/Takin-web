package io.shulie.takin.adapter.api.model.request.resource;

import java.math.BigDecimal;

import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static io.shulie.takin.adapter.api.model.request.check.ResourceCheckRequest.WATCH_MAN_ID;

@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceLockRequest extends ContextExt {

    private BigDecimal cpu;
    private BigDecimal memory;
    private Integer pod;
    private String watchmanId = WATCH_MAN_ID;
    private String callBackUrl;
}
