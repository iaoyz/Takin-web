package io.shulie.takin.cloud.biz.service.async.impl;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.service.async.CloudAsyncService;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.common.constants.SceneManageConstant;
import io.shulie.takin.cloud.common.constants.SceneTaskRedisConstants;
import io.shulie.takin.cloud.common.constants.ScheduleConstants;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneRunTaskStatusEnum;
import io.shulie.takin.cloud.common.utils.EnginePluginUtils;
import io.shulie.takin.cloud.data.dao.scene.manage.SceneManageDAO;
import io.shulie.takin.cloud.data.model.mysql.SceneManageEntity;
import io.shulie.takin.cloud.ext.api.EngineCallExtApi;
import io.shulie.takin.eventcenter.EventCenterTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * @author qianshui
 * @date 2020/10/30 下午7:13
 */
@Service
@Slf4j
public class CloudAsyncServiceImpl implements CloudAsyncService {

    @Resource
    private SceneManageDAO sceneManageDAO;
    @Resource
    private CloudSceneManageService cloudSceneManageService;
    @Resource
    private EventCenterTemplate eventCenterTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private EnginePluginUtils enginePluginUtils;

    /**
     * 线程定时检查休眠时间
     */
    private final static Integer CHECK_INTERVAL_TIME = 3;

    @Async("updateStatusPool")
    @Override
    public void updateSceneRunningStatus(Long sceneId, Long reportId, Long customerId) {
        while (true) {
            boolean isSceneFinished = isSceneFinished(reportId);
            boolean jobFinished = isJobFinished(sceneId, reportId, customerId);
            if (jobFinished || isSceneFinished) {
                String statusKey = String.format(SceneTaskRedisConstants.SCENE_TASK_RUN_KEY + "%s_%s", sceneId,
                    reportId);
                stringRedisTemplate.opsForHash().put(
                    statusKey, SceneTaskRedisConstants.SCENE_RUN_TASK_STATUS_KEY,
                    SceneRunTaskStatusEnum.ENDED.getText());
                break;
            }
            try {
                TimeUnit.SECONDS.sleep(CHECK_INTERVAL_TIME);
            } catch (InterruptedException e) {
                log.error("更新场景运行状态缓存失败！异常信息:{}", e.getMessage());
            }
        }
    }

    private boolean isSceneFinished(Long sceneId) {
        SceneManageEntity sceneManage = sceneManageDAO.getSceneById(sceneId);
        if (Objects.isNull(sceneManage) || Objects.isNull(sceneManage.getStatus())) {
            return true;
        }
        return SceneManageStatusEnum.ifFinished(sceneManage.getStatus());
    }

    private boolean isJobFinished(Long sceneId, Long reportId, Long customerId) {
        String jobName = ScheduleConstants.getScheduleName(sceneId, reportId, customerId);
        // TODO：此处使用心跳接口数据
        EngineCallExtApi engineCallExtApi = enginePluginUtils.getEngineCallExtApi();
        return !SceneManageConstant.SCENE_TASK_JOB_STATUS_RUNNING.equals(engineCallExtApi.getJobStatus(jobName));
    }
}
