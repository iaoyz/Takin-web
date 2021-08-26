package com.pamirs.takin.entity.domain.dto;

import java.io.Serializable;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 故障检测报告详情
 *
 * @author 710524
 * @date 2019年 05月16日 09:26
 */
public class ChaosSceneHostRecordDTO implements Serializable {

    private Long id;

    private String senceName;
    private Long senceRecordId;
    private String senceRecordName;
    private Long senceRuleId;
    private Long hostId;
    private String hostIp;
    private String chaosUid;

    private String command;
    /**
     * 0-createjvm命令、 1-create系统命令  2、销毁命令
     **/
    private Integer commandType;
    /**
     * 0-执行成功 1-执行失败 2-恢复成功 3-恢复失败
     */
    private Integer status;

    private String errMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date sendTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSenceName() {
        return senceName;
    }

    public void setSenceName(String senceName) {
        this.senceName = senceName;
    }

    public Long getSenceRecordId() {
        return senceRecordId;
    }

    public void setSenceRecordId(Long senceRecordId) {
        this.senceRecordId = senceRecordId;
    }

    public String getSenceRecordName() {
        return senceRecordName;
    }

    public void setSenceRecordName(String senceRecordName) {
        this.senceRecordName = senceRecordName;
    }

    public Long getSenceRuleId() {
        return senceRuleId;
    }

    public void setSenceRuleId(Long senceRuleId) {
        this.senceRuleId = senceRuleId;
    }

    public Long getHostId() {
        return hostId;
    }

    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public String getChaosUid() {
        return chaosUid;
    }

    public void setChaosUid(String chaosUid) {
        this.chaosUid = chaosUid;
    }

    public Integer getCommandType() {
        return commandType;
    }

    public void setCommandType(Integer commandType) {
        this.commandType = commandType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }
}
