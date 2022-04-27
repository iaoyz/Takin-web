package io.shulie.takin.cloud.biz.service.async;

import io.shulie.takin.web.biz.checker.StartConditionCheckerContext;

/**
 * 异步服务
 *
 * @author qianshui
 * @date 2020/10/30 下午7:13
 */
public interface CloudAsyncService {

    void checkPodStartedTask(StartConditionCheckerContext context);

    void checkPressureStartedTask(StartConditionCheckerContext context);

    /**
     * 更新场景运行状态
     *
     * @param sceneId  场景主键
     * @param reportId 报告主键
     */
    void updateSceneRunningStatus(Long sceneId, Long reportId, Long customerId);
}
