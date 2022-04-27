package io.shulie.takin.cloud.biz.service.async.impl;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.service.async.CloudAsyncService;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.common.constants.SceneManageConstant;
import io.shulie.takin.cloud.common.constants.SceneTaskRedisConstants;
import io.shulie.takin.cloud.common.constants.ScheduleConstants;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneRunTaskStatusEnum;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.common.utils.EnginePluginUtils;
import io.shulie.takin.cloud.data.dao.scene.manage.SceneManageDAO;
import io.shulie.takin.cloud.data.model.mysql.SceneManageEntity;
import io.shulie.takin.cloud.ext.api.EngineCallExtApi;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.eventcenter.EventCenterTemplate;
import io.shulie.takin.web.biz.checker.EngineResourceChecker;
import io.shulie.takin.web.biz.checker.StartConditionCheckerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static io.shulie.takin.web.biz.checker.CompositeStartConditionChecker.CHECK_SUCCESS_EVENT;
import static io.shulie.takin.web.biz.checker.CompositeStartConditionChecker.LACK_POD_RESOURCE;

/**
 * @author qianshui
 * @date 2020/10/30 下午7:13
 */
@Service
@Slf4j
public class CloudAsyncServiceImpl extends AbstractIndicators implements CloudAsyncService {

    @Resource
    private SceneManageDAO sceneManageDAO;
    @Resource
    private CloudSceneManageService cloudSceneManageService;
    @Resource
    private EventCenterTemplate eventCenterTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private EnginePluginUtils enginePluginUtils;

    @Resource
    private RedisClientUtils redisClientUtils;

    /**
     * 压力节点 启动时间超时
     */
    @Value("${pressure.pod.start.expireTime: 30}")
    private Integer pressurePodStartExpireTime;

    /**
     * 压力引擎 启动时间超时
     */
    @Value("${pressure.node.start.expireTime: 30}")
    private Integer pressureNodeStartExpireTime;

    /**
     * 线程定时检查休眠时间
     */
    private final static Integer CHECK_INTERVAL_TIME = 3;

    @Async("checkStartedPodPool")
    @Override
    public void checkPodStartedTask(StartConditionCheckerContext context) {
        log.info("启动后台检查pod启动状态线程.....");
        int currentTime = 0;
        boolean checkPass = false;
        String resourceId = context.getResourceId();
        Object totalPodNumber = redisClientUtils.hmget(EngineResourceChecker.getResourceKey(resourceId),
            EngineResourceChecker.RESOURCE_POD_NUM);
        if (totalPodNumber == null) {
            return;
        }
        String podNumber = String.valueOf(totalPodNumber);
        while (currentTime <= pressurePodStartExpireTime) {
            Long startedPod = redisClientUtils.getSetSize(EngineResourceChecker.getResourcePodKey(resourceId));
            try {
                if (Long.parseLong(podNumber) == startedPod) {
                    checkPass = true;
                    log.info("后台检查到pod全部启动成功.....");
                    break;
                }
            } catch (Exception e) {
                log.error("异常代码【{}】,异常内容：任务启动异常 --> 从Redis里获取节点数量数据格式异常: {}",
                    TakinCloudExceptionEnum.TASK_START_ERROR_CHECK_POD, e);
            }
            try {
                TimeUnit.SECONDS.sleep(CHECK_INTERVAL_TIME);
            } catch (InterruptedException ignore) {
            }
            currentTime += CHECK_INTERVAL_TIME;
        }
        //压力pod没有在设定时间内启动完毕，停止检测
        markResourceStatus(checkPass, context);
    }

    private void markResourceStatus(boolean success, StartConditionCheckerContext context) {
        String resourceId = context.getResourceId();
        Long sceneId = context.getSceneId();
        Long reportId = context.getReportId();
        Long tenantId = context.getTenantId();
        ResourceContext resourceContext = new ResourceContext();
        resourceContext.setResourceId(resourceId);
        resourceContext.setSceneId(sceneId);
        resourceContext.setReportId(reportId);
        resourceContext.setTenantId(tenantId);
        if (success) {
            Event event = new Event();
            event.setEventName(CHECK_SUCCESS_EVENT);
            event.setExt(resourceContext);
            eventCenterTemplate.doEvents(event);
        } else {
            log.info("调度任务{}-{}-{},压力节点 没有在设定时间{}s内启动，停止压测,", sceneId, reportId, tenantId, pressurePodStartExpireTime);
            Event event = new Event();
            event.setEventName(LACK_POD_RESOURCE);
            event.setExt(resourceContext);
            eventCenterTemplate.doEvents(event);
        }
    }

    @Async("checkStartedPodPool")
    @Override
    public void checkPressureStartedTask(StartConditionCheckerContext context) {

    }

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
