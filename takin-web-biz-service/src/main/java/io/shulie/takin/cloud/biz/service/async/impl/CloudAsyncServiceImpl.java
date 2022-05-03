package io.shulie.takin.cloud.biz.service.async.impl;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators;
import io.shulie.takin.cloud.biz.service.async.CloudAsyncService;
import io.shulie.takin.cloud.common.constants.SceneTaskRedisConstants;
import io.shulie.takin.cloud.common.enums.PressureTaskStateEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneRunTaskStatusEnum;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.dao.scene.manage.SceneManageDAO;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskDAO;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskVarietyDAO;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskEntity;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskVarietyEntity;
import io.shulie.takin.cloud.data.model.mysql.SceneManageEntity;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.eventcenter.EventCenterTemplate;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.web.biz.checker.StartConditionCheckerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

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
    private EventCenterTemplate eventCenterTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisClientUtils redisClientUtils;
    @Resource
    private PressureTaskDAO pressureTaskDAO;
    @Resource
    private PressureTaskVarietyDAO pressureTaskVarietyDAO;

    /**
     * 压力节点 启动时间超时
     */
    @Value("${pressure.pod.start.expireTime: 300}")
    private Integer pressurePodStartExpireTime;

    /**
     * 压力引擎 启动时间超时
     */
    @Value("${pressure.node.start.expireTime: 300}")
    private Integer pressureNodeStartExpireTime;

    /**
     * 压力引擎 心跳时间超时
     */
    @Value("${pressure.node.heartbeat.expireTime: 300}")
    private Integer pressureNodeHeartbeatExpireTime;

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
        Long sceneId = context.getSceneId();
        String resourceId = context.getResourceId();
        Object totalPodNumber = redisClientUtils.hmget(PressureStartCache.getResourceKey(resourceId),
            PressureStartCache.RESOURCE_POD_NUM);
        if (Objects.isNull(totalPodNumber)) {
            return;
        }
        String podNumber = String.valueOf(totalPodNumber);
        while (currentTime <= pressurePodStartExpireTime
            && !redisClientUtils.hasKey(RedisClientUtils.getLockPrefix(PressureStartCache.getStopFlag(sceneId, resourceId)))) {
            Long startedPod = redisClientUtils.getSetSize(PressureStartCache.getResourcePodSuccessKey(resourceId));
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

    @Async("checkStartedPodPool")
    @Override
    public void checkJmeterStartedTask(ResourceContext context) {
        log.info("启动后台检查jmeter启动状态线程.....");
        int currentTime = 0;
        boolean checkPass = false;
        String resourceId = context.getResourceId();
        Long sceneId = context.getSceneId();
        String podNumber = String.valueOf(redisClientUtils.hmget(PressureStartCache.getResourceKey(resourceId),
            PressureStartCache.RESOURCE_POD_NUM));
        while (currentTime <= pressureNodeStartExpireTime
            && !redisClientUtils.hasKey(RedisClientUtils.getLockPrefix(PressureStartCache.getStopFlag(sceneId, resourceId)))) {
            Long startedPod = redisClientUtils.getSetSize(PressureStartCache.getResourceJmeterSuccessKey(resourceId));
            try {
                if (Long.parseLong(podNumber) == startedPod) {
                    checkPass = true;
                    log.info("后台检查到jmeter全部启动成功.....");
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
        //压力jmeter没有在设定时间内启动完毕，停止检测
        if (!checkPass) {
            String message = String.format("节点没有在设定时间【%s】s内启动，计划启动节点个数【%s】,实际启动节点个数【%s】,"
                    + "导致压测停止", pressureNodeStartExpireTime, podNumber,
                redisClientUtils.getSetSize(PressureStartCache.getResourceJmeterSuccessKey(context.getResourceId())));
            callStopEventIfNecessary(context.getResourceId(), message);
        } else {
            // 启动完成
            markJmeterStarted(context);
        }
    }

    @Async("checkStartedPodPool")
    @Override
    public void checkJmeterHeartbeatTask(ResourceContext context) {
        log.info("启动后台检查jmeter心跳状态线程.....");
        Long sceneId = context.getSceneId();
        String resourceId = context.getResourceId();
        int checkTime = pressureNodeHeartbeatExpireTime * 1000;
        while (!redisClientUtils.hasKey(RedisClientUtils.getLockPrefix(PressureStartCache.getStopFlag(sceneId, resourceId)))) {
            long now = System.currentTimeMillis();
            Map<Object, Object> heartbeatMap = redisClientUtils.hmget(PressureStartCache.getJmeterHeartbeatKey(sceneId));
            if (CollectionUtils.isEmpty(heartbeatMap)) {
                break;
            }
            for (Entry<Object, Object> entry : heartbeatMap.entrySet()) {
                if (Long.parseLong(String.valueOf(entry.getValue())) + checkTime > now) {
                    callStopEventIfNecessary(resourceId, String.format("jmeter节点[%s]心跳超时", entry.getKey()));
                    return;
                }
            }
            try {
                TimeUnit.SECONDS.sleep(CHECK_INTERVAL_TIME);
            } catch (InterruptedException ignore) {
            }
        }
    }

    @Async("checkStartedPodPool")
    @Override
    public void checkPodHeartbeatTask(ResourceContext context) {
        log.info("启动后台检查pod心跳状态线程.....");
        Long sceneId = context.getSceneId();
        String resourceId = context.getResourceId();
        int checkTime = pressureNodeHeartbeatExpireTime * 1000;
        while (!redisClientUtils.hasKey(RedisClientUtils.getLockPrefix(PressureStartCache.getStopFlag(sceneId, resourceId)))) {
            long now = System.currentTimeMillis();
            Map<Object, Object> heartbeatMap = redisClientUtils.hmget(PressureStartCache.getPodHeartbeatKey(sceneId));
            if (CollectionUtils.isEmpty(heartbeatMap)) {
                break;
            }
            for (Entry<Object, Object> entry : heartbeatMap.entrySet()) {
                if (Long.parseLong(String.valueOf(entry.getValue())) + checkTime > now) {
                    callStopEventIfNecessary(resourceId, String.format("pod节点[%s]心跳超时", entry.getKey()));
                    return;
                }
            }
            try {
                TimeUnit.SECONDS.sleep(CHECK_INTERVAL_TIME);
            } catch (InterruptedException ignore) {
            }
        }
    }

    @Async("updateStatusPool")
    @Override
    public void updateSceneRunningStatus(Long sceneId, Long reportId, Long customerId) {
        while (true) {
            boolean isSceneFinished = isSceneFinished(reportId);
            boolean jobFinished = isJobFinished(sceneId);
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
        resourceContext.setPressureTaskId(context.getTaskId());
        resourceContext.setUniqueKey(context.getUniqueKey());
        if (success) {
            Event event = new Event();
            event.setEventName(PressureStartCache.CHECK_SUCCESS_EVENT);
            event.setExt(resourceContext);
            eventCenterTemplate.doEvents(event);
        } else {
            log.info("调度任务{}-{}-{},压力节点 没有在设定时间{}s内启动，停止压测,", sceneId, reportId, tenantId, pressurePodStartExpireTime);
            callStopEventIfNecessary(resourceContext.getResourceId(), "压力机资源不足");
        }
    }

    private void markJmeterStarted(ResourceContext context) {
        redisClientUtils.hmset(PressureStartCache.getResourceKey(context.getResourceId()),
            PressureStartCache.TASK_STATUS, PressureTaskStateEnum.PRESSURING.ordinal());

        PressureTaskEntity entity = new PressureTaskEntity();
        Long taskId = context.getTaskId();
        entity.setId(taskId);
        entity.setStatus(PressureTaskStateEnum.PRESSURING.ordinal());
        entity.setGmtUpdate(new Date());
        pressureTaskDAO.updateById(entity);
        pressureTaskVarietyDAO.save(PressureTaskVarietyEntity.of(taskId, PressureTaskStateEnum.ALIVE));
        pressureTaskVarietyDAO.save(PressureTaskVarietyEntity.of(taskId, PressureTaskStateEnum.PRESSURING));
    }

    private boolean isSceneFinished(Long sceneId) {
        SceneManageEntity sceneManage = sceneManageDAO.getSceneById(sceneId);
        if (Objects.isNull(sceneManage) || Objects.isNull(sceneManage.getStatus())) {
            return true;
        }
        return SceneManageStatusEnum.ifFinished(sceneManage.getStatus());
    }

    private boolean isJobFinished(Long sceneId) {
        return "1".equals(redisClientUtils.getString(PressureStartCache.getSceneFinishKey(sceneId)));
    }
}
