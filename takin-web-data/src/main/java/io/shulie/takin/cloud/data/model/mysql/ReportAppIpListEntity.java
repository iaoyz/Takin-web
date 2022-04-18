package io.shulie.takin.cloud.data.model.mysql;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 报告应用ip列表 entity
 *
 * @author -
 */
@Data
@TableName(value = "t_report_app_ip_list")
public class ReportAppIpListEntity {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    @TableField(value = "report_id")
    private String reportId;

    @TableField(value = "link_id")
    private String linkId;

    @TableField(value = "application_name")
    private String applicationName;

    @TableField(value = "type")
    private String type;

    @TableField(value = "system_name")
    private String systemName;

    @TableField(value = "ip")
    private String ip;

    @TableField(value = "cpu")
    private String cpu;

    @TableField(value = "memory")
    private String memory;

    @TableField(value = "io_read")
    private String ioRead;

    @TableField(value = "io_write")
    private String ioWrite;

    @TableField(value = "io_all")
    private String ioAll;
}