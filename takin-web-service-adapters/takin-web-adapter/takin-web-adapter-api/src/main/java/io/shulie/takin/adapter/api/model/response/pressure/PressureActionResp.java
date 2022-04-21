package io.shulie.takin.adapter.api.model.response.pressure;

import lombok.Data;

@Data
public class PressureActionResp {
    private Boolean success;
    private String msg;
    private DataResp data;

    @Data
    public static class DataResp {
        private Long taskId;
    }
}
