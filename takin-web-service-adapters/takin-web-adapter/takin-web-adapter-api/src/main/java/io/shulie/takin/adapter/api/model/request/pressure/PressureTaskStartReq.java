package io.shulie.takin.adapter.api.model.request.pressure;

import java.util.List;
import java.util.Map;

import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PressureTaskStartReq extends ContextExt {
    /**
     * 资源Id
     */
    private String resourceId;

    /**
     * jvm参数
     */
    private String jvmParams;

    /**
     * 压测类型
     * 0-常规模式
     * 3-流量模式
     * 4-巡检模式
     * 5-试跑模式
     */
    private Integer pressureType;

    private Map<String, ThreadGroupConfig> test;

    private List<PressureDataFile> files;

    @Data
    public static class ThreadGroupConfig {
        /**
         * 压力模式
         * 0-并发模式
         * 1-TPS模式
         * 2-自定义模式
         */
        private Integer type;
        /**
         * 施压模式
         * 1-固定压力值
         * 2-线下递增
         * 3-阶梯递增
         */
        private Integer model;
        /**
         * 并发线程数(并发模式下)
         */
        private Integer threadNum;
        /**
         * tps值(tps模式下)
         */
        private Integer tps;
        /**
         * 线程递增时长(并发模式下[线性/递增])
         */
        private Integer rampUp;
        /**
         * 线程递增时长单位(并发模式下[线性/递增])
         */
        private String rampUnit;
        /**
         * 阶梯层数(并发模式下[线性/递增])
         * 0-最大线程
         */
        private Integer steps;
    }

    @Data
    public static class PressureDataFile {

        /**
         * 文件路径
         * nfs相对路径
         */
        private String path;

        /**
         * 文件类型
         * 1-脚本
         * 2-csv
         * 3-jar
         */
        private Integer type;

        /**
         * 是否分割文件
         */
        private boolean split;

        /**
         * 是否有序
         */
        private boolean ordered;

        /**
         * 是否大文件
         */
        private boolean isBigFile;

        /**
         * MD5值
         */
        private String md5;

        private Map<Integer, List<PressureDataFilePosition>> splitPositions;
    }

    @Data
    public static class PressureDataFilePosition {
        /**
         * 分区
         */
        private String partition;

        /**
         * pod读取文件开始位置
         */
        private String start = "-1";

        /**
         * pod读取文件结束位置
         */
        private String end = "-1";
    }
}
