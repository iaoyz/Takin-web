package io.shulie.takin.adapter.api.model.common;

import java.io.Serializable;

import lombok.Data;

@Data
public class ResponseResult<T> implements Serializable {

    private T data;
    private String msg;
    private Integer total;
    private Boolean success;

}
