package io.shulie.takin.web.data.model.mysql;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.*;
import io.shulie.takin.web.data.annocation.EnableSign;
import io.shulie.takin.web.data.annocation.SignField;
import io.shulie.takin.web.data.model.mysql.base.UserBaseEntity;
import lombok.Data;

@Data
@TableName(value = "t_script_manage")
@EnableSign
public class ScriptManageEntity extends UserBaseEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 名称
     */
    @TableField(value = "name")
    @SignField(order = 1)
    private String name;

    /**
     * 脚本管理 - 版本
     */
    @TableField(value = "m_version")
    private Integer mVersion;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create")
    private Date gmtCreate;

    /**
     * 更新时间
     */
    @TableField(value = "gmt_update")
    private Date gmtUpdate;

    @TableField(value = "script_version")
    private Integer scriptVersion;

    /**
     * 添加逻辑删除注解
     */
    @TableField(value = "is_deleted")
    private Integer isDeleted;

    /**
     * 拓展字段
     */
    @TableField(value = "feature")
    private String feature;

    @TableField(value = "sign",fill = FieldFill.INSERT)
    private String sign;

}
