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
    @Deprecated
    @Value("${k8s.jvm.settings:-XX:MaxRAMPercentage=90.0 -XX:InitialRAMPercentage=90.0 -XX:MinRAMPercentage=90.0}")
    private String k8sJvmSettings;

    public DeploymentMethodEnum getDeploymentMethod() {
        return CommonUtil.getValue(DeploymentMethodEnum.PRIVATE, this.deploymentMethod, DeploymentMethodEnum::valueBy);
    }
}
