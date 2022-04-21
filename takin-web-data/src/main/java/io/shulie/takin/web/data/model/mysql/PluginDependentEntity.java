package io.shulie.takin.web.data.model.mysql;

import java.io.Serializable;

import com.baomidou.mybatisplus.annotation.TableName;
import io.shulie.takin.web.data.model.mysql.base.NewBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 插件依赖库(PluginDependent)实体类
 *
 * @author ocean_wll
 * @date 2021-11-09 20:46:15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_plugin_dependent")
@ToString(callSuper = true)
public class PluginDependentEntity extends NewBaseEntity implements Serializable {
    private static final long serialVersionUID = 887180013380812378L;

    /**
     * 插件名称
     */
    private String pluginName;

    /**
     * 插件版本
     */
    private String pluginVersion;

    /**
     * 插件版本对应数值
     */
    private Long pluginVersionNum;

    /**
     * 依赖插件名称
     */
    private String dependentPluginName;

    /**
     * 依赖插件版本
     */
    private String dependentPluginVersion;

    /**
     * 依赖插件版本对应的数值
     */
    private Long dependentPluginVersionNum;

    /**
     * 是否必须 0:必须 1:非必须
     */
    private Integer requisite;

    /**
     * 备注
     */
    private String remark;

}
