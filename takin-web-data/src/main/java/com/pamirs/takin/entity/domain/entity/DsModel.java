package com.pamirs.takin.entity.domain.entity;

import java.util.Date;

import io.shulie.takin.web.data.model.mysql.base.UserBaseEntity;

public class DsModel extends UserBaseEntity {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.ID
     *
     * @mbg generated
     */
    private Long id;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.APPLICATION_ID
     *
     * @mbg generated
     */
    private Long applicationId;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.APPLICATION_NAME
     *
     * @mbg generated
     */
    private String applicationName;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.DS_TYPE
     *
     * @mbg generated
     */

    private Byte dbType;

    private Byte dsType;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.URL
     *
     * @mbg generated
     */
    private String url;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.STATUS
     *
     * @mbg generated
     */
    private Byte status;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.CREATE_TIME
     *
     * @mbg generated
     */
    private Date createTime;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.UPDATE_TIME
     *
     * @mbg generated
     */
    private Date updateTime;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.IS_DELETED
     *
     * @mbg generated
     */
    private Byte isDeleted;

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_application_ds_manage
     *
     * @mbg generated
     */
    public DsModel(Long id, Long applicationId, String applicationName, Byte dbType, Byte dsType, String url, Byte status,
                   Date createTime, Date updateTime, Byte isDeleted) {
        this.id = id;
        this.applicationId = applicationId;
        this.applicationName = applicationName;
        this.dbType = dbType;
        this.dsType = dsType;
        this.url = url;
        this.status = status;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.isDeleted = isDeleted;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_application_ds_manage
     *
     * @mbg generated
     */
    public DsModel() {
        super();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.ID
     *
     * @return the value of t_application_ds_manage.ID
     * @mbg generated
     */
    public Long getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.ID
     *
     * @param id the value for t_application_ds_manage.ID
     * @mbg generated
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.APPLICATION_ID
     *
     * @return the value of t_application_ds_manage.APPLICATION_ID
     * @mbg generated
     */
    public Long getApplicationId() {
        return applicationId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.APPLICATION_ID
     *
     * @param applicationId the value for t_application_ds_manage.APPLICATION_ID
     * @mbg generated
     */
    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.APPLICATION_NAME
     *
     * @return the value of t_application_ds_manage.APPLICATION_NAME
     * @mbg generated
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.APPLICATION_NAME
     *
     * @param applicationName the value for t_application_ds_manage.APPLICATION_NAME
     * @mbg generated
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName == null ? null : applicationName.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.DS_TYPE
     *
     * @return the value of t_application_ds_manage.DS_TYPE
     * @mbg generated
     */
    public Byte getDsType() {
        return dsType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.DS_TYPE
     *
     * @param dsType the value for t_application_ds_manage.DS_TYPE
     * @mbg generated
     */
    public void setDsType(Byte dsType) {
        this.dsType = dsType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.URL
     *
     * @return the value of t_application_ds_manage.URL
     * @mbg generated
     */
    public String getUrl() {
        return url;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.URL
     *
     * @param url the value for t_application_ds_manage.URL
     * @mbg generated
     */
    public void setUrl(String url) {
        this.url = url == null ? null : url.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.STATUS
     *
     * @return the value of t_application_ds_manage.STATUS
     * @mbg generated
     */
    public Byte getStatus() {
        return status;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.STATUS
     *
     * @param status the value for t_application_ds_manage.STATUS
     * @mbg generated
     */
    public void setStatus(Byte status) {
        this.status = status;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.CREATE_TIME
     *
     * @return the value of t_application_ds_manage.CREATE_TIME
     * @mbg generated
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.CREATE_TIME
     *
     * @param createTime the value for t_application_ds_manage.CREATE_TIME
     * @mbg generated
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.UPDATE_TIME
     *
     * @return the value of t_application_ds_manage.UPDATE_TIME
     * @mbg generated
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.UPDATE_TIME
     *
     * @param updateTime the value for t_application_ds_manage.UPDATE_TIME
     * @mbg generated
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.IS_DELETED
     *
     * @return the value of t_application_ds_manage.IS_DELETED
     * @mbg generated
     */
    public Byte getIsDeleted() {
        return isDeleted;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.IS_DELETED
     *
     * @param isDeleted the value for t_application_ds_manage.IS_DELETED
     * @mbg generated
     */
    public void setIsDeleted(Byte isDeleted) {
        this.isDeleted = isDeleted;
    }

    public Byte getDbType() {
        return dbType;
    }

    public void setDbType(Byte dbType) {
        this.dbType = dbType;
    }
}
