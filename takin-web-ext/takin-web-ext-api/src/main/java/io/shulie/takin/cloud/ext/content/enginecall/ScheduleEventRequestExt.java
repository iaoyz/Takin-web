package io.shulie.takin.cloud.ext.content.enginecall;

import java.util.Map;

import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author 莫问
 * @date 2020-05-14
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleEventRequestExt extends ContextExt {

    /**
     * 场景ID
     */
    private Long sceneId;

    /**
     * 场景任务ID
     */
    private Long taskId;

    /**
     * 扩展参数
     */
    private Map<String, Object> extend;
}
