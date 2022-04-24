package io.shulie.takin.cloud.biz.notify;

import lombok.Data;

@Data
public class NotifyContext {

    private String resourceId;
    /**
     * 0-失败，1-成功
     */
    private Integer status;
    private String message;

}