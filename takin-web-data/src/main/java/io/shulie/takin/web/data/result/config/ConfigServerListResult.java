package io.shulie.takin.web.data.result.config;

import lombok.ToString;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.shulie.takin.web.data.model.mysql.ConfigServerEntity;

/**
 * 配置表-服务的配置(ConfigServer)列表出参类
 *
 * @author liuchuan
 * @date 2021-10-12 11:17:23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ConfigServerListResult extends ConfigServerEntity {

}
