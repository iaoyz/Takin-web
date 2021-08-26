package io.shulie.takin.web.data.result.leakverify;

import java.util.Date;

import lombok.Data;

/**
 * @author zhaoyong
 */
@Data
@Deprecated
public class LeakVerifyDeployResult {

    private Long id;

    /**
     * 漏数记录id
     */
    private Long leakVerifyId;

    /**
     * 应用名
     */
    private String applicationName;

    /**
     * 链路入口名称
     */
    private String entryName;

    /**
     * 压测请求数量
     */
    private Long pressureRequestCount;

    /**
     * 压测漏数数量
     */
    private Long pressureLeakCount;

    /**
     * 业务请求数量
     */
    private Long bizRequestCount;

    /**
     * 业务漏数数量
     */
    private Long bizLeakCount;

    private Date gmtCreate;

    private Date gmtUpdate;

    private Boolean isDeleted;
}
