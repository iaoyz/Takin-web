package io.shulie.takin.cloud.biz.notify;

public interface CloudNotifyProcessor<T extends CloudNotifyParam> {

    CallbackType type();

     void process(T param);
}
