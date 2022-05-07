package io.shulie.takin.cloud.ext.api;

import java.util.List;

import io.shulie.takin.cloud.ext.content.enginecall.ScheduleRunRequest;
import io.shulie.takin.cloud.ext.content.enginecall.ScheduleStopRequestExt;
import io.shulie.takin.cloud.ext.content.enginecall.StrategyConfigExt;
import io.shulie.takin.cloud.ext.content.enginecall.StrategyOutputExt;
import io.shulie.takin.plugin.framework.core.extension.ExtensionPoint;

/**
 * @author zhaoyong
 * 引擎调用拓展点
 */
public interface EngineCallExtApi extends ExtensionPoint, Typed {

    /**
     * 构建压测任务
     *
     * @param scheduleRunRequest -
     * @return 返回失败原因，成功返回null
     */
    String buildJob(ScheduleRunRequest scheduleRunRequest);

    /**
     * 删除压测任务
     *
     * @param scheduleStopRequest -
     */
    void deleteJob(ScheduleStopRequestExt scheduleStopRequest);

    /**
     * 获取所有正在运行的任务名称
     *
     * @return -
     */
    List<String> getAllRunningJobName();

    /**
     * 获取当前任务状态
     *
     * @param jobName -
     * @return -
     */
    String getJobStatus(String jobName);

    /**
     * 获取建议的机器节点数量范围
     *
     * @param strategyConfigExt -
     * @return -
     */
    StrategyOutputExt getPressureNodeNumRange(StrategyConfigExt strategyConfigExt);

    /**
     * 获取默认的调度策略
     *
     * @return -
     */
    StrategyConfigExt getDefaultStrategyConfig();

}
