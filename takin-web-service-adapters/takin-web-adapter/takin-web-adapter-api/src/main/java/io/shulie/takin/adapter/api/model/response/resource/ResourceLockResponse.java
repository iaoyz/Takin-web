package io.shulie.takin.adapter.api.model.response.resource;

import lombok.Data;

@Data
public class ResourceLockResponse {

    private ResourceData data;

    @Data
    public static class ResourceData {
        private String resourceId;
    }
}
