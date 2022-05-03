package io.shulie.takin.cloud.biz.notify;

import java.util.Date;

import lombok.Data;

@Data
public class CloudNotifyParam {

    private Date time;
    private Date callbackTime;
}
