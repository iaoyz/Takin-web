package io.shulie.takin.web.data.model.mysql.base;

import java.io.Serializable;
import java.util.Date;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

/**
 * 数据库映射共有类
 *
 * @author liuchuan
 * @date 2021/4/7 5:27 下午
 */
@Data
public class CommonEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 删除
     * 1 删除, 0 未删除
     */
    @TableLogic
    private Integer isDeleted;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 更新时间
     */
    private Date gmtUpdate;

    /**
     * 租户id
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 用户id
     */
    @TableField(fill = FieldFill.INSERT)
    private String envCode;

}
