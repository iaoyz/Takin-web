package io.shulie.takin.cloud.biz.notify;

import io.shulie.takin.common.beans.response.ResponseResult;
import org.springframework.stereotype.Component;

@Component
public class PressureStartNotifyProcessor implements CloudNotifyProcessor {

    @Override
    public String type() {
        return "pressure";
    }

    @Override
    public ResponseResult<?> process(NotifyContext context) {
        return null;
    }
}
