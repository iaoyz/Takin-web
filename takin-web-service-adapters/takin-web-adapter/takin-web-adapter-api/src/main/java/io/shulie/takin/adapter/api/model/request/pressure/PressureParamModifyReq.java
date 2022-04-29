package io.shulie.takin.adapter.api.model.request.pressure;

import io.shulie.takin.cloud.constant.enums.ThreadGroupType;
import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import io.shulie.takin.cloud.model.request.StartRequest.ThreadConfigInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PressureParamModifyReq extends ContextExt {

    private Long jobId;
    /**
     * 修改的关联节点
     */
    private String ref;
    private ThreadGroupType type;
    private ThreadConfigInfo context;
}
