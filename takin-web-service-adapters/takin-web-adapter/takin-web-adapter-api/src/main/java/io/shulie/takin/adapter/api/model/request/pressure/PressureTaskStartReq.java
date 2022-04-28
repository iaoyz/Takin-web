package io.shulie.takin.adapter.api.model.request.pressure;

import java.util.List;

import io.shulie.takin.cloud.constant.enums.JobType;
import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import io.shulie.takin.cloud.model.request.StartRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
public class PressureTaskStartReq extends ContextExt {

    /**
     * 状态接口回调地址
     */
    private String callbackUrl;
    /**
     * 资源Id
     */
    private Long resourceId;
    /**
     * jvm参数
     */
    private String jvmOptions;
    /**
     * 采样率
     */
    private Integer sampling;
    /**
     * 任务类型
     * 0-常规模式
     * 3-流量模式
     * 4-巡检模式
     * 5-试跑模式
     */
    private JobType type;
    /**
     * 任务名称
     */
    private String name;
    /**
     * 脚本文件(jmx)
     */
    private StartRequest.FileInfo scriptFile;
    /**
     * 运行时依赖文件(插件)
     */
    private List<StartRequest.FileInfo> dependency;
    /**
     * 数据文件(csv)
     */
    private List<StartRequest.FileInfo> data;
    /**
     * 线程配置
     */
    private List<StartRequest.ThreadConfigInfo> threadConfig;
    /**
     * 调试模式下脚本调试配置
     */
    private DebugConfig debugInfo;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DebugConfig {
        /**
         * 调试请求条数
         */
        private Integer number;
        /**
         * 并发数
         */
        private Integer thread;
    }
}
