package com.pamirs.takin.entity.domain.entity.linkmanage;

import java.io.Serializable;
import java.util.Date;

/**
 * t_middleware_link_relate
 *
 * @author
 */
public class MiddlewareLinkRelate implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    private Long id;
    /**
     * 中间件id
     */
    private String middlewareId;
    /**
     * 技术链路id
     */
    private String techLinkId;
    /**
     * 业务链路id
     */
    private String businessLinkId;
    /**
     * 场景id
     */
    private String sceneId;
    /**
     * 是否有效 0:有效;1:无效
     */
    private Integer isDeleted;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMiddlewareId() {
        return middlewareId;
    }

    public void setMiddlewareId(String middlewareId) {
        this.middlewareId = middlewareId;
    }

    public String getTechLinkId() {
        return techLinkId;
    }

    public void setTechLinkId(String techLinkId) {
        this.techLinkId = techLinkId;
    }

    public String getBusinessLinkId() {
        return businessLinkId;
    }

    public void setBusinessLinkId(String businessLinkId) {
        this.businessLinkId = businessLinkId;
    }

    public String getSceneId() {
        return sceneId;
    }

    public void setSceneId(String sceneId) {
        this.sceneId = sceneId;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
