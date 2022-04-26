package io.shulie.takin.adapter.api.model.response.statistics;

import lombok.Data;

/**
 * @author 无涯
 * @date 2020/11/30 9:23 下午
 */
@Data
public class PressureListTotalResp {
    private Long id;
    private String name;
    private String label;
    private String gmtCreate;
    private String createName;
    private Integer count;
    private Integer success;
    private Integer fail;
}
