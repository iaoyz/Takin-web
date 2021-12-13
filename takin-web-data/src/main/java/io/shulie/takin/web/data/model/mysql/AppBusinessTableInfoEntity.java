package io.shulie.takin.web.data.model.mysql;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.shulie.takin.web.data.model.mysql.base.UserBaseEntity;
import lombok.Data;

/**
 * 应用业务表
 */
@Data
@TableName(value = "t_app_business_table_info")
public class AppBusinessTableInfoEntity extends UserBaseEntity {
    /**
     * 主键
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 应用ID
     */
    @TableField(value = "APPLICATION_ID")
    private Long applicationId;

    /**
     * jar名称
     */
    @TableField(value = "table_name")
    private String tableName;

    /**
     * Pradar插件名称
     */
    @TableField(value = "url")
    private String url;


    /**
     * 是否有效 0:有效;1:无效
     */
    @TableField(value = "IS_DELETED")
    private Boolean isDeleted;

    /**
     * 创建时间
     */
    @TableField(value = "CREATE_TIME")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "UPDATE_TIME")
    private LocalDateTime updateTime;
}
