package io.shulie.takin.cloud.common.influxdb;

import java.util.Objects;

/**
 * @author qianshui
 * @date 2020/7/20 下午4:34
 */
public class InfluxUtil {

    /**
     * 实时统计数据表
     */
    public static String getMeasurement(Long sceneId, Long reportId, Long customerId) {
        return getMeasurement("pressure", sceneId, reportId, customerId);
    }

    /**
     * 拼装influxdb表名
     */
    public static String getMeasurement(String measurementName, Long sceneId, Long reportId, Long customerId) {
        if (customerId == null) {
            return String.format("%s_%s_%s", measurementName, sceneId, reportId);
        }
        String cId = customerId.toString();
        if (customerId < 0) {
            cId = "f" + Math.abs(customerId);
        }
        return String.format("%s_%s_%s_%s", measurementName, sceneId, reportId, cId);
    }

    // 此处考虑旧数据兼容性
    public static String getMeasurement(Long jobId, Long sceneId, Long reportId, Long customerId) {
        if (Objects.isNull(jobId)) {
            return getMeasurement(sceneId, reportId, customerId);
        }
        return getMeasurement(jobId);
    }

    public static String getMeasurement(Long jobId) {
        return String.format("pressure_%s", jobId);
    }
}
