package io.shulie.takin.cloud.common.utils;

import java.util.List;

import javax.annotation.Resource;

import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.cloud.ext.api.EngineCallExtApi;
import io.shulie.takin.cloud.ext.api.EngineExtApi;
import io.shulie.takin.plugin.framework.core.PluginManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author fanxx
 * @date 2021/8/2 3:02 下午
 */
@Component
@Slf4j
public class EnginePluginUtils {
    @Value("${plugin.engine.call.ext.type:local_engine}")
    private String engineCallExtType;

    @Value("${plugin.engine.ext.type:jmeter_engine}")
    private String engineExtType;

    @Resource
    private PluginManager pluginManager;

    public EngineExtApi getEngineExtApi() {
        List<EngineExtApi> extensions = pluginManager.getExtensions(EngineExtApi.class);
        for (EngineExtApi engineExtApi : extensions) {
            if (engineExtType.equals(engineExtApi.getType())) {
                return engineExtApi;
            }
        }
        throw new TakinCloudException(TakinCloudExceptionEnum.PLUGIN_NOT_FIND, "引擎拓展插件未找到!");
    }

    public EngineCallExtApi getEngineCallExtApi() {
        List<EngineCallExtApi> extensions = pluginManager.getExtensions(EngineCallExtApi.class);
        for (EngineCallExtApi engineCallExtApi : extensions) {
            if (engineCallExtType.equals(engineCallExtApi.getType())) {
                return engineCallExtApi;
            }
        }
        throw new TakinCloudException(TakinCloudExceptionEnum.PLUGIN_NOT_FIND, "引擎调用拓展插件未找到!");
    }
}
