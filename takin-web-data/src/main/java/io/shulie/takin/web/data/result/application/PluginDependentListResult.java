package io.shulie.takin.web.data.result.application;

import lombok.ToString;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.shulie.takin.web.data.model.mysql.PluginDependentEntity;

/**
 * 插件依赖库(PluginDependent)列表出参类
 *
 * @author ocean_wll
 * @date 2021-11-09 20:46:16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PluginDependentListResult extends PluginDependentEntity {

}
