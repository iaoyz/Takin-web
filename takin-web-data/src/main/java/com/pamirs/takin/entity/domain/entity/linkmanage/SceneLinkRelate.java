package com.pamirs.takin.entity.domain.entity.linkmanage;

import java.io.Serializable;
import java.util.Date;

import io.shulie.takin.web.data.model.mysql.base.TenantBaseEntity;
import lombok.Data;

/**
 * t_scene_link_relate
 *
 * @author
 */
@Data
public class SceneLinkRelate extends TenantBaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键id
     */
    private Long id;
    /**
     * 场景id
     */
    private String sceneId;
    /**
     * 链路入口
     */
    private String entrance;
    /**
     * 是否有效 0:有效;1:无效
     */
    private Integer isDeleted;
    /**
     * 插入时间
     */
    private Date createTime;
    /**
     * 变更时间
     */
    private Date updateTime;
    /**
     * 上级业务链路ID
     */
    private String parentBusinessLinkId;
    /**
     * 业务链路ID
     */
    private String businessLinkId;
    /**
     * 技术链路ID
     */
    private String techLinkId;
    /**
     * 前端树结构的key--前端产生
     */
    private String frontUUIDKey;

}
