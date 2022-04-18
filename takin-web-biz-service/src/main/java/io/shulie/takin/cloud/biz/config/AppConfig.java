package io.shulie.takin.cloud.biz.config;

import io.shulie.takin.cloud.common.enums.deployment.DeploymentMethodEnum;
import io.shulie.takin.cloud.common.utils.CommonUtil;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * @author liyuanba
 */
@Configuration
@Data
public class AppConfig {
    @Value("${console.url}")
    private String console;

    /**
     * 部署方式
     */
    @Value("${tro.cloud.deployment.method:private}")
    private String deploymentMethod;
    /**
     * 压测引擎版本
     */
    @Deprecated
    @Value("${pressure.engine.images}")
    private String pressureEngineImage;
    /**
     * 压测引擎名称
     */
    @Deprecated
    @Value("${pressure.engine.name}")
    private String pressureEngineImageName;
    /**
     * cloud版本
     */
    @Value("${info.app.version}")
    private String cloudVersion;

    @Deprecated
    @Value("${k8s.jvm.settings:-XX:MaxRAMPercentage=90.0 -XX:InitialRAMPercentage=90.0 -XX:MinRAMPercentage=90.0}")
    private String k8sJvmSettings;

    /**
     * 数据收集模式:redis，influxdb
     */
    @Value("${report.data.collector:influxdb}")
    private String collector;

    @Value("${report.data.holdRealThreadNum:true}")
    private Boolean holdRealThreadNum;

    @Value("${spring.redis.host}")
    private String engineRedisAddress;

    @Value("${spring.redis.port}")
    private String engineRedisPort;

    @Value("${spring.redis.sentinel.nodes:}")
    private String engineRedisSentinelNodes;

    @Value("${spring.redis.sentinel.master:}")
    private String engineRedisSentinelMaster;

    @Value("${spring.redis.password}")
    private String engineRedisPassword;

    @Value("${pradar.zk.servers}")
    private String zkServers;

    @Value("${engine.log.queue.size:25000}")
    private String logQueueSize;
    @Value("${pressure.engine.backendQueueCapacity:5000}")
    private String pressureEngineBackendQueueCapacity;

    @Value("${pressure.engine.logUpload:cloud}")
    private String engineLogUploadModel;

    @Value("${script.path}")
    private String nfsDir;

    public String getNfsDir() {
        if (!nfsDir.endsWith("/")) {
            nfsDir = nfsDir + "/";
        }
        return nfsDir;
    }

    public DeploymentMethodEnum getDeploymentMethod() {
        return CommonUtil.getValue(DeploymentMethodEnum.PRIVATE, this.deploymentMethod, DeploymentMethodEnum::valueBy);
    }
}
