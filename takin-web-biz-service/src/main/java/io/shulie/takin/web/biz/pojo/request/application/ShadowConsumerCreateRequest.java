package io.shulie.takin.web.biz.pojo.request.application;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @author shiyajian
 * create: 2021-02-05
 */
@Data
public class ShadowConsumerCreateRequest {

    @NotBlank
    private String topicGroup;

    @NotNull
    private String type;

    @NotNull
    private Long applicationId;

    /**
     * 是否可用
     */
    private Integer status;

}
