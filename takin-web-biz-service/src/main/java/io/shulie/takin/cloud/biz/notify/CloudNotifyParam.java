package io.shulie.takin.cloud.biz.notify;

import lombok.Data;

@Data
public class CloudNotifyParam {

    private String type;
    private NotifyContext context;
}
