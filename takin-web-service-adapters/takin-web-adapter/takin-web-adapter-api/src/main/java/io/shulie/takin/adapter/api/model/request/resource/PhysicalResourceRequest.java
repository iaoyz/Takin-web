package io.shulie.takin.adapter.api.model.request.resource;

import lombok.Data;

import static io.shulie.takin.adapter.api.model.request.resource.ResourceCheckRequest.WATCH_MAN_ID;

@Data
public class PhysicalResourceRequest {

    private Integer watchmanId = WATCH_MAN_ID;
}
