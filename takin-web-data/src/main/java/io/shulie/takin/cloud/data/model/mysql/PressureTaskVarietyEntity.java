package io.shulie.takin.cloud.data.model.mysql;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName(value = "t_pressure_task_variety")
public class PressureTaskVarietyEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "task_id")
    private Long taskId;

    @TableField(value = "status")
    private Integer status;

    @TableField(value = "message")
    private String message;

    @TableField(value = "gmt_create")
    private Date gmtCreate;

    public static PressureTaskVarietyEntity of(Long taskId, Integer status) {
        PressureTaskVarietyEntity entity = new PressureTaskVarietyEntity();
        entity.setTaskId(taskId);
        entity.setStatus(status);
        return entity;
    }

    public static PressureTaskVarietyEntity of(Long taskId, Integer status, String message) {
        PressureTaskVarietyEntity entity = PressureTaskVarietyEntity.of(taskId, status);
        entity.setMessage(message);
        return entity;
    }
}
