package io.shulie.takin.cloud.common.influxdb;

import java.lang.reflect.Field;

import org.influxdb.BuilderException;
import org.influxdb.annotation.Column;
import org.influxdb.dto.Point;

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

    private static void addFieldByAttribute(final Point.Builder builder, final Object pojo, final Field field, final Column column, final String fieldName) {
        try {
            Object fieldValue = field.get(pojo);
            if (column.tag()) {
                builder.tag(fieldName, (String)fieldValue);
            } else {
                builder.field(fieldName, fieldValue);
            }
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // Can not happen since we use metadata got from the object
            throw new BuilderException("Field " + fieldName + " could not found on class " + pojo.getClass().getSimpleName());
        }
    }
}
