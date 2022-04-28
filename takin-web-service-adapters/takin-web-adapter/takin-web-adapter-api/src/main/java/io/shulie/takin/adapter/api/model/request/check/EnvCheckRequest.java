package io.shulie.takin.adapter.api.model.request.check;

import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import lombok.Data;
import lombok.EqualsAndHashCode;

import static io.shulie.takin.adapter.api.model.request.resource.ResourceCheckRequest.WATCH_MAN_ID;

@Data
@EqualsAndHashCode(callSuper = true)
public class EnvCheckRequest extends ContextExt {

    private Integer watchmanId = WATCH_MAN_ID;
}
