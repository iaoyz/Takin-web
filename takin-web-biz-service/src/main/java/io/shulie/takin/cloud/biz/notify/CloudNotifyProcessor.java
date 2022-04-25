package io.shulie.takin.cloud.biz.notify;

import io.shulie.takin.common.beans.response.ResponseResult;

public interface CloudNotifyProcessor {

    String type();

    ResponseResult<?> process(NotifyContext context);
}
