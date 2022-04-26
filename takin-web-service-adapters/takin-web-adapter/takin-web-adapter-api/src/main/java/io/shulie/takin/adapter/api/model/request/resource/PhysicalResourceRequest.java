package io.shulie.takin.adapter.api.model.request.resource;

import lombok.Data;

import static io.shulie.takin.adapter.api.model.request.check.ResourceCheckRequest.WATCH_MAN_ID;

@Data
public class PhysicalResourceRequest {

    private String watchmanId = WATCH_MAN_ID;
}
