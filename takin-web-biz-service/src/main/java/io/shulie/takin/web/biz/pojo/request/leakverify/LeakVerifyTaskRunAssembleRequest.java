package io.shulie.takin.web.biz.pojo.request.leakverify;

import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * @author fanxx
 * @date 2021/1/28 9:05 下午
 */
@Data
public class LeakVerifyTaskRunAssembleRequest {
    /**
     * 引用类型：压测场景
     */
    @NotNull
    private Integer refType;

    /**
     * 压测场景id
     */
    @NotNull
    private Long refId;

    /**
     * 压测报告id
     */
    private Long reportId;

    /**
     * 业务活动id
     */
    @NotEmpty
    private List<Long> businessActivityIds;
}
