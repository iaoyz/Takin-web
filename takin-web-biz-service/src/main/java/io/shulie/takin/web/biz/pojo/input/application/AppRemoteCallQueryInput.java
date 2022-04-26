package io.shulie.takin.web.biz.pojo.input.application;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

import io.shulie.takin.web.ext.entity.UserCommonExt;

/**
 * @author 无涯
 * @date 2021/5/29 12:18 上午
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AppRemoteCallQueryInput extends UserCommonExt {
    private Integer type;
    private String interfaceName;
    private Integer status;
    private Long applicationId;
    private List<String> appNames;
}
