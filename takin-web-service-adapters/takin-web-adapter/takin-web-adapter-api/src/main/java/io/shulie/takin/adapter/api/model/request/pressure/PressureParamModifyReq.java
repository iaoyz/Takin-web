package io.shulie.takin.adapter.api.model.request.pressure;

import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PressureParamModifyReq extends ContextExt {

    private Long taskId;
    /**
     * 修改的关联节点
     */
    private String bingRef;
    private PressureParam params;

    @Data
    public static class PressureParam {
        // 1-tps
        private Integer type;
        private String value;

        public PressureParam(Integer type, String value) {
            this.type = type;
            this.value = value;
        }

        public static PressureParam of(Integer type, String value) {
            return new PressureParam(type, value);
        }
    }
}
