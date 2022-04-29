package io.shulie.takin.cloud.biz.collector.collector;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.pamirs.takin.cloud.entity.domain.vo.scenemanage.SceneManageStartRecordVO;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.cloud.biz.service.report.CloudReportService;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneTaskService;
import io.shulie.takin.cloud.common.constants.ReportConstants;
import io.shulie.takin.cloud.common.constants.SceneTaskRedisConstants;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneRunTaskStatusEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.eventcenter.EventCenterTemplate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
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
    private CloudSceneTaskService sceneTaskService;
    @Resource
    private CloudReportService cloudReportService;
    @Resource
    private RedisClientUtils redisClientUtils;
    @Value("${pressure.heartbeat.timeout:30}")
    private Long heartbeatTimeout;
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
        context.setResourceKey(resourceKey);
        context.setSceneId(Long.valueOf(String.valueOf(resource.get(PressureStartCache.SCENE_ID))));
        context.setReportId(Long.valueOf(String.valueOf(resource.get(PressureStartCache.REPORT_ID))));
        context.setTenantId(Long.valueOf(String.valueOf(resource.get(PressureStartCache.TENANT_ID))));
        context.setEndTime(Long.valueOf(String.valueOf(resource.get(PressureStartCache.RESOURCE_END_TIME))));
        context.setStatus(String.valueOf(resource.get(PressureStartCache.RESOURCE_STATUS)));
        context.setHeartbeatTime(Long.valueOf(String.valueOf(resource.get(PressureStartCache.HEARTBEAT_TIME))));
        context.setPressureTaskId(Long.valueOf(String.valueOf(resource.get(PressureStartCache.PRESSURE_TASK_ID))));
        return context;
    }

    protected void callStop(Long sceneId, Long taskId, String resourceId, String message, Long tenantId) {
        // 汇报失败
        cloudReportService.updateReportFeatures(taskId, ReportConstants.FINISH_STATUS, ReportConstants.PRESSURE_MSG, message);
        cloudSceneManageService.reportRecord(
            SceneManageStartRecordVO.build(sceneId, taskId, tenantId).success(false).errorMsg(message).build());
    }

    protected void setTryRunTaskInfo(Long sceneId, Long reportId, Long tenantId, String errorMsg) {
        log.info("压测启动失败--sceneId:【{}】,reportId:【{}】,tenantId:【{}】,errorMsg:【{}】", sceneId, reportId, tenantId, errorMsg);
        String tryRunTaskKey = String.format(SceneTaskRedisConstants.SCENE_TASK_RUN_KEY + "%s_%s", sceneId, reportId);
        redisClientUtils.hmset(tryRunTaskKey,
            SceneTaskRedisConstants.SCENE_RUN_TASK_STATUS_KEY, SceneRunTaskStatusEnum.FAILED.getText());
        redisClientUtils.hmset(tryRunTaskKey, SceneTaskRedisConstants.SCENE_RUN_TASK_ERROR, errorMsg);
        //试跑失败，停掉pod
        sceneTaskService.stop(sceneId);
    }

    protected boolean heartbeatTimeout(String resourceId) {
        ResourceContext resourceContext = getResourceContext(resourceId);
        if (resourceContext == null) {
            return false;
        }
        Long heartbeatTime = resourceContext.getHeartbeatTime();
        long now = System.currentTimeMillis();
        if (Objects.isNull(heartbeatTime) || heartbeatTime == 0 || heartbeatTime + heartbeatTimeout <= now) {
            String resourceKey = PressureStartCache.getResourceKey(resourceId);
            if (redisClientUtils.hasKey(resourceKey)) {
                redisClientUtils.hmset(resourceKey, PressureStartCache.HEARTBEAT_TIME, now);
            }
            return false;
        }
        return true;
    }

    @Data
    public static class ResourceContext {
        private Long sceneId;
        private Long reportId;
        private Long pressureTaskId;
        private String resourceId;
        private Long tenantId;
        private Long podNumber;
        private Long endTime;
        private String resourceKey;
        private String status;
        private Long heartbeatTime;
        private String uniqueKey;

        private String message;
    }
}
