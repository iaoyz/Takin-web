package io.shulie.takin.cloud.biz.notify;

import java.util.Date;

import lombok.Data;

@Data
public class NotifyContext {

    private String resourceId;

    private String podId;

    /**
     * 0-失败，1-成功
     */
    private Integer status;
    private String message;

    // 公共参数
    private Date time;
    private Date callbackTime;
    private String sign;

}