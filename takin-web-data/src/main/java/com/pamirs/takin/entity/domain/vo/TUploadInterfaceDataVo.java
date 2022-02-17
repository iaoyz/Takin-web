package com.pamirs.takin.entity.domain.vo;

import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.pamirs.takin.common.util.DateToStringFormatSerialize;
import com.pamirs.takin.common.util.LongToStringFormatSerialize;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 说明:
 *
 * @author shulie
 * @version v1.0
 * @date Create in 2019/3/12 22:03
 */
public class TUploadInterfaceDataVo {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_upload_interface_data.ID
     *
     * @mbg generated
     */
    @JsonSerialize(using = LongToStringFormatSerialize.class)
    private Long id;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_upload_interface_data.APP_NAME
     *
     * @mbg generated
     */
    private String appName;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_upload_interface_data.INTERFACE_VALUE
     *
     * @mbg generated
     */
    private String interfaceName;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_upload_interface_data.INTERFACE_TYPE
     *
     * @mbg generated
     */
    private Integer interfaceType;

    /**
     * 接口类型中文名称
     */
    private String interfaceValueName;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column t_upload_interface_data.CREATE_TIME
     *
     * @mbg generated
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = DateToStringFormatSerialize.class)
    private Date createTime;

    /**
     * This method was generated by MyBatis Generator.
     * This method corresponds to the database table t_upload_interface_data
     *
     * @mbg generated
     */
    public TUploadInterfaceDataVo() {
        super();
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_upload_interface_data.ID
     *
     * @return the value of t_upload_interface_data.ID
     * @mbg generated
     */
    public Long getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_upload_interface_data.ID
     *
     * @param id the value for t_upload_interface_data.ID
     * @mbg generated
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_upload_interface_data.APP_NAME
     *
     * @return the value of t_upload_interface_data.APP_NAME
     * @mbg generated
     */
    public String getAppName() {
        return appName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_upload_interface_data.APP_NAME
     *
     * @param appName the value for t_upload_interface_data.APP_NAME
     * @mbg generated
     */
    public void setAppName(String appName) {
        this.appName = appName == null ? null : appName.trim();
    }

    public String getInterfaceValueName() {
        return interfaceValueName;
    }

    public void setInterfaceValueName(String interfaceValueName) {
        this.interfaceValueName = interfaceValueName;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_upload_interface_data.INTERFACE_TYPE
     *
     * @return the value of t_upload_interface_data.INTERFACE_TYPE
     * @mbg generated
     */
    public Integer getInterfaceType() {
        return interfaceType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_upload_interface_data.INTERFACE_TYPE
     *
     * @param interfaceType the value for t_upload_interface_data.INTERFACE_TYPE
     * @mbg generated
     */
    public void setInterfaceType(Integer interfaceType) {
        this.interfaceType = interfaceType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column t_upload_interface_data.CREATE_TIME
     *
     * @return the value of t_upload_interface_data.CREATE_TIME
     * @mbg generated
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column t_upload_interface_data.CREATE_TIME
     *
     * @param createTime the value for t_upload_interface_data.CREATE_TIME
     * @mbg generated
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "TUploadInterfaceDataVo{" +
            "id=" + id +
            ", appName='" + appName + '\'' +
            ", interfaceName='" + interfaceName + '\'' +
            ", interfaceType=" + interfaceType +
            ", interfaceValueName='" + interfaceValueName + '\'' +
            ", createTime=" + createTime +
            '}';
    }
}
