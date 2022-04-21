package io.shulie.takin.cloud.data.model.mysql;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * -
 *
 * @author -
 */
@Data
@TableName(value = "t_machine_task_log")
public class MachineTaskLogEntity {
    /**
     * id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务id
     */
    @TableField(value = "task_id")
    private Long taskId;

    /**
     * 序列号
     */
    @TableField(value = "serial_no")
    private String serialNo;

    /**
     * 机器IP
     */
    @TableField(value = "ip")
    private String ip;

    /**
     * 机器名称
     */
    @TableField(value = "hostname")
    private String hostname;

    /**
     * 状态 1、开通中 2、开通成功 3、开通失败 4：启动中 5、启动成功 6、启动失败 7、初始化中 8、初始化失败 9、运行中 10、销毁中 11、已过期 12、已锁定 13、销毁失败 14、已销毁
     */
    @TableField(value = "status")
    private Integer status;

    /**
     * 操作日志
     */
    @TableField(value = "log")
    private String log;

    /**
     * 状态 0: 正常 1： 删除
     */
    @TableField(value = "is_delete")
    private Boolean isDelete;

    /**
     * 创建时间
     */
    @TableField(value = "gmt_create")
    private LocalDateTime gmtCreate;

    /**
     * 修改时间
     */
    @TableField(value = "gmt_update")
    private LocalDateTime gmtUpdate;
}
