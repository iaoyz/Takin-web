package io.shulie.takin.web.data.model.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.shulie.takin.web.data.model.mysql.base.TenantBaseEntity;
import lombok.Data;

/**
 * 场景添加暂存表
 */
@Data
@TableName(value = "t_scene_add_temp_table")
public class SceneAddTempTableEntity extends TenantBaseEntity {
    /**
     * 主键
     */
    @TableId(value = "ID", type = IdType.AUTO)
    private Long id;

    /**
     * 业务链路id
     */
    @TableField(value = "BUSENESS_ID")
    private String busenessId;

    /**
     * 技术链路id
     */
    @TableField(value = "TECH_ID")
    private String techId;

    /**
     * 业务链路的上级业务链路
     */
    @TableField(value = "PARENT_BUSINESS_ID")
    private String parentBusinessId;
}
