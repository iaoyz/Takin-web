package io.shulie.takin.web.data.result.application;

import lombok.ToString;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.shulie.takin.web.data.model.mysql.MqConfigTemplateEntity;

/**
 * MQ配置模版表(MqConfigTemplate)详情出参类
 *
 * @author 南风
 * @date 2021-08-31 15:34:04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MqConfigTemplateDetailResult extends MqConfigTemplateEntity {

}
