package io.shulie.takin.adapter.api.entrypoint.watchman;

import io.shulie.takin.adapter.api.model.request.watchman.WatchmanStatusRequest;
import io.shulie.takin.cloud.model.response.WatchmanStatusResponse;

public interface CloudWatchmanApi {

    WatchmanStatusResponse status(WatchmanStatusRequest request);
}
