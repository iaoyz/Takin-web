package io.shulie.takin.cloud.data.model.mysql;

import java.util.Date;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName(value = "t_pressure_task_callback")
public class PressureTaskCallbackEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField(value = "task_id")
    private Long taskId;

    @TableField(value = "resource_id")
    private Long resourceId;

    @TableField(value = "pod_id")
    private String podId;

    @TableField(value = "time")
    private Long time;

    @TableField(value = "type")
    private String type;

    @TableField(value = "source")
    private String source;

    @TableField(value = "gmt_create")
    private Date gmtCreate;
}
