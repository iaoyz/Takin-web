package com.pamirs.takin.entity.domain.entity;

import java.util.Date;

public class DsModel {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.ID
     *
     * @mbggenerated
     */
    private Long id;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.APPLICATION_ID
     *
     * @mbggenerated
     */
    private Long applicationId;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.APPLICATION_NAME
     *
     * @mbggenerated
     */
    private String applicationName;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.DS_TYPE
     *
     * @mbggenerated
     */

    private Byte dbType;

    private Byte dsType;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.URL
     *
     * @mbggenerated
     */
    private String url;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.STATUS
     *
     * @mbggenerated
     */
    private Byte status;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.CREATE_TIME
     *
     * @mbggenerated
     */
    private Date createTime;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.UPDATE_TIME
     *
     * @mbggenerated
     */
    private Date updateTime;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_application_ds_manage.IS_DELETED
     *
     * @mbggenerated
     */
    private Byte isDeleted;

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_application_ds_manage
     *
     * @mbggenerated
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
     * @mbggenerated
     */
    public DsModel() {
        super();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.ID
     *
     * @return the value of t_application_ds_manage.ID
     * @mbggenerated
     */
    public Long getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.ID
     *
     * @param id the value for t_application_ds_manage.ID
     * @mbggenerated
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.APPLICATION_ID
     *
     * @return the value of t_application_ds_manage.APPLICATION_ID
     * @mbggenerated
     */
    public Long getApplicationId() {
        return applicationId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.APPLICATION_ID
     *
     * @param applicationId the value for t_application_ds_manage.APPLICATION_ID
     * @mbggenerated
     */
    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.APPLICATION_NAME
     *
     * @return the value of t_application_ds_manage.APPLICATION_NAME
     * @mbggenerated
     */
    public String getApplicationName() {
        return applicationName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.APPLICATION_NAME
     *
     * @param applicationName the value for t_application_ds_manage.APPLICATION_NAME
     * @mbggenerated
     */
    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName == null ? null : applicationName.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.DS_TYPE
     *
     * @return the value of t_application_ds_manage.DS_TYPE
     * @mbggenerated
     */
    public Byte getDsType() {
        return dsType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.DS_TYPE
     *
     * @param dsType the value for t_application_ds_manage.DS_TYPE
     * @mbggenerated
     */
    public void setDsType(Byte dsType) {
        this.dsType = dsType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.URL
     *
     * @return the value of t_application_ds_manage.URL
     * @mbggenerated
     */
    public String getUrl() {
        return url;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.URL
     *
     * @param url the value for t_application_ds_manage.URL
     * @mbggenerated
     */
    public void setUrl(String url) {
        this.url = url == null ? null : url.trim();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.STATUS
     *
     * @return the value of t_application_ds_manage.STATUS
     * @mbggenerated
     */
    public Byte getStatus() {
        return status;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.STATUS
     *
     * @param status the value for t_application_ds_manage.STATUS
     * @mbggenerated
     */
    public void setStatus(Byte status) {
        this.status = status;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.CREATE_TIME
     *
     * @return the value of t_application_ds_manage.CREATE_TIME
     * @mbggenerated
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.CREATE_TIME
     *
     * @param createTime the value for t_application_ds_manage.CREATE_TIME
     * @mbggenerated
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.UPDATE_TIME
     *
     * @return the value of t_application_ds_manage.UPDATE_TIME
     * @mbggenerated
     */
    public Date getUpdateTime() {
        return updateTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.UPDATE_TIME
     *
     * @param updateTime the value for t_application_ds_manage.UPDATE_TIME
     * @mbggenerated
     */
    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_application_ds_manage.IS_DELETED
     *
     * @return the value of t_application_ds_manage.IS_DELETED
     * @mbggenerated
     */
    public Byte getIsDeleted() {
        return isDeleted;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_application_ds_manage.IS_DELETED
     *
     * @param isDeleted the value for t_application_ds_manage.IS_DELETED
     * @mbggenerated
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
