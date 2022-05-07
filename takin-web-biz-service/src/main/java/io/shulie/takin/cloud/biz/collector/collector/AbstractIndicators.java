package io.shulie.takin.cloud.biz.collector.collector;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.google.common.collect.Lists;
import io.shulie.takin.cloud.biz.cache.SceneTaskStatusCache;
import io.shulie.takin.cloud.biz.notify.StopEventSource;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.takin.cloud.common.bean.task.TaskResult;
import io.shulie.takin.cloud.common.constants.SceneTaskRedisConstants;
import io.shulie.takin.cloud.common.constants.ScheduleConstants;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneRunTaskStatusEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.dao.report.ReportDao;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.eventcenter.EventCenterTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.util.CollectionUtils;

/**
 * @author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @date 2020-04-20 21:08
 */
@Slf4j
public abstract class AbstractIndicators {

    /**
     * 1、判断key是否存在，不存在插入value
     * 2、key存在，比较值大小
     */
    private static final String MAX_SCRIPT =
        "if (redis.call('exists', KEYS[1]) == 0 or redis.call('get', KEYS[1]) < ARGV[1]) then\n" +
            "    redis.call('set', KEYS[1], ARGV[1]);\n" +
            //            "    return 1;\n" +
            "else\n" +
            //            "    return 0;\n" +
            "end";
    private static final String MIN_SCRIPT =
        "if (redis.call('exists', KEYS[1]) == 0 or redis.call('get', KEYS[1]) > ARGV[1]) then\n" +
            "    redis.call('set', KEYS[1], ARGV[1]);\n" +
            //            "    return 1;\n" +
            "else\n" +
            //            "    return 0;\n" +
            "end";
    private static final String UNLOCK_SCRIPT = "if redis.call('exists',KEYS[1]) == 1 then\n" +
        "   redis.call('del',KEYS[1])\n" +
        "else\n" +
        //                    "   return 0\n" +
        "end";
    @Autowired
    protected CloudSceneManageService cloudSceneManageService;
    @Autowired
    protected EventCenterTemplate eventCenterTemplate;
    @Resource
    protected RedisTemplate<String, Object> redisTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisClientUtils redisClientUtils;
    @Resource
    private SceneTaskStatusCache taskStatusCache;
    @Resource
    private ReportDao reportDao;
    private DefaultRedisScript<Void> minRedisScript;
    private DefaultRedisScript<Void> maxRedisScript;
    private DefaultRedisScript<Void> unlockRedisScript;

    private static final int REDIS_KEY_TIMEOUT = 60;

    private final Expiration expiration = Expiration.seconds(REDIS_KEY_TIMEOUT);

    /**
     * 获取Metrics key
     * 示例：COLLECTOR:TASK:102121:213124512312
     *
     * @param sceneId  场景主键
     * @param reportId 报告主键
     * @return -
     */
    protected String getPressureTaskKey(Long sceneId, Long reportId, Long tenantId) {
        // 兼容原始redis key
        if (tenantId == null) {
            return String.format("COLLECTOR:TASK:%s:%s", sceneId, reportId);
        }
        return String.format("COLLECTOR:TASK:%s:%s:%S", sceneId, reportId, tenantId);
    }

    public Boolean lock(String key, String value) {
        return redisTemplate.execute((RedisCallback<Boolean>)connection -> {
            Boolean bl = connection.set(getLockPrefix(key).getBytes(), value.getBytes(), expiration,
                RedisStringCommands.SetOption.SET_IF_ABSENT);
            return null != bl && bl;
        });
    }

    public void unlock(String key, String value) {
        redisTemplate.execute(unlockRedisScript, Lists.newArrayList(getLockPrefix(key)), value);
    }

    private String getLockPrefix(String key) {
        return String.format("COLLECTOR LOCK:%s", key);
    }

    /**
     * 获取Metrics 指标key
     * 示例：COLLECTOR:TASK:102121:213124512312:1587375600000:rt
     *
     * @param indicatorsName 指标名称
     * @return -
     */
    protected String getIndicatorsKey(String windowKey, String indicatorsName) {
        return String.format("%s:%s", windowKey, indicatorsName);
    }

    /**
     * time 不进行转换
     *
     * @param taskKey 任务key
     * @return -
     */
    protected String last(String taskKey) {
        return getIndicatorsKey(String.format("%s:%s", taskKey, "last"), "last");
    }

    protected void setLast(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    protected void setError(String key, String timestampPodNum, String value) {
        redisTemplate.opsForHash().put(key, timestampPodNum, value);
        setTtl(key);
    }

    protected void setMax(String key, Long value) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            long temp = getEventTimeStrap(key);
            if (value > temp) {
                redisTemplate.opsForValue().set(key, value);
            }
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    protected void setMin(String key, Long value) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            long temp = getEventTimeStrap(key);
            if (value < temp) {
                redisTemplate.opsForValue().set(key, value);
            }
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    private void setTtl(String key) {
        redisTemplate.expire(key, REDIS_KEY_TIMEOUT, TimeUnit.SECONDS);
    }

    /**
     * 获取时间搓，取time 求min max
     *
     * @param key key
     * @return -
     */
    protected Long getEventTimeStrap(String key) {
        Object object = redisTemplate.opsForValue().get(key);
        if (null != object) {
            return (long)object;
        }
        return null;
    }

    public String getTaskKey(Long sceneId, Long reportId, Long tenantId) {
        // 兼容原始redis key
        if (tenantId == null) {
            return String.format("%s_%s", sceneId, reportId);
        }
        return String.format("%s_%s_%s", sceneId, reportId, tenantId);
    }

    /**
     * 强行自动标识
     *
     * @param taskKey 任务key
     * @return -
     */
    protected String forceCloseTime(String taskKey) {
        return getIndicatorsKey(String.format("%s:%s", taskKey, "forceClose"), "force");
    }

    @PostConstruct
    public void init() {
        minRedisScript = new DefaultRedisScript<>();
        minRedisScript.setResultType(Void.class);
        minRedisScript.setScriptText(MIN_SCRIPT);

        maxRedisScript = new DefaultRedisScript<>();
        maxRedisScript.setResultType(Void.class);
        maxRedisScript.setScriptText(MAX_SCRIPT);

        unlockRedisScript = new DefaultRedisScript<>();
        unlockRedisScript.setResultType(Void.class);
        unlockRedisScript.setScriptText(UNLOCK_SCRIPT);

    }

    protected ResourceContext getResourceContext(String resourceId) {
        String resourceKey = PressureStartCache.getResourceKey(resourceId);
        Map<Object, Object> resource = redisClientUtils.hmget(resourceKey);
        if (CollectionUtils.isEmpty(resource)) {
            return null;
        }
        ResourceContext context = new ResourceContext();
        context.setResourceId(resourceId);
        context.setSceneId(Long.valueOf(String.valueOf(resource.get(PressureStartCache.SCENE_ID))));
        context.setReportId(Long.valueOf(String.valueOf(resource.get(PressureStartCache.REPORT_ID))));
        context.setTenantId(Long.valueOf(String.valueOf(resource.get(PressureStartCache.TENANT_ID))));
        context.setCheckStatus(String.valueOf(resource.get(PressureStartCache.CHECK_STATUS)));
        context.setTaskId(Long.valueOf(String.valueOf(resource.get(PressureStartCache.TASK_ID))));
        context.setUniqueKey(String.valueOf(resource.get(PressureStartCache.UNIQUE_KEY)));
        Object jobId = resource.get(PressureStartCache.JOB_ID);
        if (Objects.nonNull(jobId)) {
            context.setJobId(Long.valueOf(String.valueOf(jobId)));
        }
        return context;
    }

    protected void setTryRunTaskInfo(Long sceneId, Long reportId, Long tenantId, String errorMsg) {
        log.info("压测启动失败--sceneId:【{}】,reportId:【{}】,tenantId:【{}】,errorMsg:【{}】", sceneId, reportId, tenantId, errorMsg);
        String tryRunTaskKey = String.format(SceneTaskRedisConstants.SCENE_TASK_RUN_KEY + "%s_%s", sceneId, reportId);
        stringRedisTemplate.opsForHash().put(tryRunTaskKey,
            SceneTaskRedisConstants.SCENE_RUN_TASK_STATUS_KEY, SceneRunTaskStatusEnum.FAILED.getText());
        if (StringUtils.isNotBlank(errorMsg)) {
            stringRedisTemplate.opsForHash().put(tryRunTaskKey, SceneTaskRedisConstants.SCENE_RUN_TASK_ERROR, errorMsg);
        }
    }

    protected void callStopEventIfNecessary(String resourceId, String message) {
        ResourceContext context = getResourceContext(resourceId);
        if (context != null
            && redisClientUtils.lockNoExpire(PressureStartCache.getStopFlag(context.getSceneId(), resourceId), message)) {
            Event event = new Event();
            event.setEventName(PressureStartCache.START_FAILED);
            StopEventSource source = new StopEventSource();
            source.setMessage(message);
            source.setContext(context);
            source.setPressureRunning(Objects.nonNull(context.getJobId()));
            event.setExt(source);
            eventCenterTemplate.doEvents(event);
        }
    }

    protected void removeSuccessKey(String resourceId, String podId, String jmeterId) {
        redisClientUtils.removeSetValue(
            PressureStartCache.getResourcePodSuccessKey(resourceId), podId);
        if (Objects.nonNull(jmeterId)) {
            redisClientUtils.removeSetValue(
                PressureStartCache.getResourceJmeterSuccessKey(resourceId), jmeterId);
        }
    }

    protected void detectEnd(String resourceId, Date time) {
        if (redisClientUtils.hasKey(PressureStartCache.getJmeterStartFirstKey(resourceId)) &&
            (redisClientUtils.getSetSize(PressureStartCache.getResourcePodSuccessKey(resourceId)) == 0 ||
                redisClientUtils.getSetSize(PressureStartCache.getResourceJmeterSuccessKey(resourceId)) == 0)) {
            ResourceContext context = getResourceContext(resourceId);
            Long sceneId = context.getSceneId();
            Long reportId = context.getReportId();
            Long tenantId = context.getTenantId();
            String engineName = ScheduleConstants.getEngineName(sceneId, reportId, tenantId);
            setLast(last(getPressureTaskKey(sceneId, reportId, tenantId)), ScheduleConstants.LAST_SIGN);
            setMax(engineName + ScheduleConstants.LAST_SIGN, time.getTime());
            // 删除临时标识
            redisClientUtils.del(ScheduleConstants.TEMP_LAST_SIGN + engineName);
            // 压测停止
            notifyEnd(context, time);
        }
    }

    private void notifyEnd(ResourceContext context, Date time) {
        Long sceneId = context.getSceneId();
        Long reportId = context.getReportId();
        Long tenantId = context.getTenantId();
        log.info("场景[{}-{}]压测任务已完成,更新结束时间{}", sceneId, reportId, System.currentTimeMillis());
        // 刷新任务状态的Redis缓存
        taskStatusCache.cacheStatus(sceneId, reportId, SceneRunTaskStatusEnum.ENDED);
        // 更新压测场景状态  压测引擎运行中,压测引擎停止压测 ---->压测引擎停止压测
        cloudSceneManageService.updateSceneLifeCycle(UpdateStatusBean.build(sceneId, reportId, tenantId)
            .checkEnum(SceneManageStatusEnum.ENGINE_RUNNING, SceneManageStatusEnum.STOP)
            .updateEnum(SceneManageStatusEnum.STOP)
            .build());
        reportDao.updateReportEndTime(reportId, time);

        // 清除 SLA配置  生成报告拦截 状态拦截
        Event event = new Event();
        event.setEventName("finished");
        TaskResult result = new TaskResult(sceneId, reportId, tenantId);
        result.setResourceId(context.getResourceId());
        event.setExt(result);
        eventCenterTemplate.doEvents(event);
    }

    @Data
    public static class ResourceContext {
        private Long sceneId;
        private Long reportId;
        private Long taskId;
        private Long jobId;
        private String resourceId;
        private Long tenantId;
        private Long podNumber;
        private String checkStatus;
        private String uniqueKey;

        private String message;
    }
}
