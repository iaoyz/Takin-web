package io.shulie.takin.cloud.biz.collector.collector;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pamirs.takin.cloud.entity.dao.report.TReportMapper;
import com.pamirs.takin.cloud.entity.domain.entity.report.Report;
import io.shulie.takin.cloud.biz.cache.SceneTaskStatusCache;
import io.shulie.takin.cloud.biz.config.AppConfig;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.service.async.CloudAsyncService;
import io.shulie.takin.cloud.biz.utils.DataUtils;
import io.shulie.takin.cloud.common.bean.collector.EventMetrics;
import io.shulie.takin.cloud.common.bean.collector.ResponseMetrics;
import io.shulie.takin.cloud.common.bean.scenemanage.SceneManageQueryOptions;
import io.shulie.takin.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.takin.cloud.common.constants.CollectorConstants;
import io.shulie.takin.cloud.common.constants.NoLengthBlockingQueue;
import io.shulie.takin.cloud.common.constants.PressureEngineConstants;
import io.shulie.takin.cloud.common.constants.ScheduleConstants;
import io.shulie.takin.cloud.common.enums.PressureSceneEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneRunTaskStatusEnum;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.cloud.common.influxdb.InfluxUtil;
import io.shulie.takin.cloud.common.influxdb.InfluxWriter;
import io.shulie.takin.cloud.common.utils.CollectorUtil;
import io.shulie.takin.cloud.common.utils.GsonUtil;
import io.shulie.takin.cloud.data.dao.report.ReportDao;
import io.shulie.takin.utils.json.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author <a href="tangyuhan@shulie.io">yuhan.tang</a>
 * @date 2020-04-20 14:38
 */
@Slf4j
@Service
public class CollectorService extends AbstractIndicators {

    public static final String METRICS_EVENTS_STARTED = "started";
    public static final String METRICS_EVENTS_ENDED = "ended";

    @Resource
    private TReportMapper tReportMapper;
    @Resource
    private CloudAsyncService cloudAsyncService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ReportDao reportDao;
    @Resource
    private SceneTaskStatusCache taskStatusCache;
    @Resource
    private InfluxWriter influxWriter;
    @Resource
    private AppConfig appConfig;

    private final static ExecutorService THREAD_POOL = new ThreadPoolExecutor(100, 200,
        300L, TimeUnit.SECONDS,
        new NoLengthBlockingQueue<>(), new ThreadFactoryBuilder()
        .setNameFormat("ptl-log-push-%d").build(), new ThreadPoolExecutor.AbortPolicy());

    public void collectorToInfluxdb(Long sceneId, Long reportId, Long customerId, List<ResponseMetrics> metricsList) {
        if (CollectionUtils.isEmpty(metricsList)) {
            return;
        }
        String measurement = InfluxUtil.getMetricsMeasurement(sceneId, reportId, customerId);
        metricsList.stream().filter(Objects::nonNull)
            .peek(metrics -> {
                //判断有没有MD5值
                int strPosition = metrics.getTransaction().lastIndexOf(PressureEngineConstants.TRANSACTION_SPLIT_STR);
                if (strPosition > 0) {
                    String transaction = metrics.getTransaction();
                    metrics.setTransaction(
                        transaction.substring(strPosition + PressureEngineConstants.TRANSACTION_SPLIT_STR.length()));
                    metrics.setTestName((transaction.substring(0, strPosition)));
                } else {
                    metrics.setTransaction(metrics.getTransaction());
                    metrics.setTestName(metrics.getTransaction());
                }
            })
            .peek(metrics -> {
                //处理时间戳-纳秒转成毫秒，防止插入influxdb报错
                if (Objects.nonNull(metrics.getTime()) && metrics.getTime() > InfluxUtil.MAX_ACCEPT_TIMESTAMP) {
                    metrics.setTime(metrics.getTime() / 1000000);
                }
                if (metrics.getTimestamp() > InfluxUtil.MAX_ACCEPT_TIMESTAMP) {
                    metrics.setTimestamp(metrics.getTimestamp() / 1000000);
                }
            })
            .map(metrics -> InfluxUtil.toPoint(measurement, metrics.getTimestamp(), metrics))
            .forEach(influxWriter::insert);
    }

    /**
     * 记录时间
     */
    public void collector(Long sceneId, Long reportId, Long tenantId, List<ResponseMetrics> metrics) {
        if (StringUtils.isNotBlank(appConfig.getCollector()) && "influxdb".equalsIgnoreCase(appConfig.getCollector())) {
            collectorToInfluxdb(sceneId, reportId, tenantId, metrics);
            return;
        }
        String taskKey = getPressureTaskKey(sceneId, reportId, tenantId);
        for (ResponseMetrics metric : metrics) {
            try {
                long timeWindow = CollectorUtil.getTimeWindowTime(metric.getTimestamp());
                if (validate(timeWindow, sceneId, reportId, tenantId, metrics)) {
                    // 写入redis
                    log.info("{}-{}-{} write redis , timestamp-{},timeWindow-{}", sceneId, reportId, tenantId,
                        metric.getTimestamp(), timeWindow);
                    String source = metric.getTransaction();
                    String transaction = source;
                    String testName = source;
                    int strPosition = metric.getTransaction().lastIndexOf(
                        PressureEngineConstants.TRANSACTION_SPLIT_STR);
                    if (strPosition > 0) {
                        transaction = source.substring(
                            strPosition + PressureEngineConstants.TRANSACTION_SPLIT_STR.length());
                        testName = source.substring(0, strPosition);
                    }
                    String timePod = CollectorUtil.getTimestampPodNum(metric.getTimestamp(), metric.getPodNum());
                    intSaveRedisMap(countKey(taskKey, transaction, timeWindow), timePod, metric.getCount());
                    intSaveRedisMap(failCountKey(taskKey, transaction, timeWindow), timePod, metric.getFailCount());
                    intSaveRedisMap(saCountKey(taskKey, transaction, timeWindow), timePod, metric.getSaCount());
                    intSaveRedisMap(activeThreadsKey(taskKey, transaction, timeWindow), timePod,
                        metric.getActiveThreads());

                    // 错误信息
                    setError(errorKey(taskKey, transaction, timeWindow), timePod,
                        GsonUtil.gsonToString(metric.getErrorInfos()));
                    //1-100%每个百分点位sa数据
                    saveRedisMap(percentDataKey(taskKey, transaction, timeWindow), timePod, metric.getPercentData());
                    //testName
                    saveRedisMap(testNameKey(taskKey, transaction, timeWindow), timePod, testName);
                    /*
                     * all指标额外计算，累加所有业务活动的saCount all 为空
                     */
                    intSaveRedisMap(saCountKey(taskKey, "all", timeWindow),
                        // 计算所有业务活动的saCount 用特殊标识 _transaction
                        timePod + "_" + transaction,
                        metric.getSaCount());

                    longSaveRedisMap(rtKey(taskKey, transaction, timeWindow), timePod, metric.getSumRt());
                    Double maxRt = DataUtils.getMaxRt(metric);
                    mostValue(maxRtKey(taskKey, transaction, timeWindow), maxRt, 0);
                    mostValue(minRtKey(taskKey, transaction, timeWindow), metric.getMinRt(), 1);
                }
            } catch (Exception e) {
                log.error("write redis error :{}", e.getMessage());
            }
        }
    }

    public synchronized void verifyEvent(Long sceneId, Long reportId, Long tenantId, List<EventMetrics> metrics) {
        String engineName = ScheduleConstants.getEngineName(sceneId, reportId, tenantId);
        String taskKey = getPressureTaskKey(sceneId, reportId, tenantId);

        for (EventMetrics metric : metrics) {
            try {
                // 解决多pod
                boolean isFirst = METRICS_EVENTS_STARTED.equals(metric.getEventName());
                boolean isLast = METRICS_EVENTS_ENDED.equals(metric.getEventName());
                //每个pod只会启动或者一次，处理数据重复发送问题
                String enginePodNoStartKey = ScheduleConstants.getEnginePodNoStartKey(sceneId, reportId, tenantId,
                    metric.getPodNo(), metric.getEventName());
                Long startPod = stringRedisTemplate.opsForValue().increment(enginePodNoStartKey, 1);
                if (startPod != null && startPod > 1) {
                    continue;
                }
                if (isFirst) {
                    // 超时自动检修，强行触发关闭
                    if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(forceCloseTime(taskKey)))) {
                        // 获取压测时长
                        log.info("本次压测{}-{}-{}:记录超时自动检修时间-{}", sceneId, reportId, tenantId, metric.getTimestamp());
                        SceneManageWrapperOutput wrapperDTO = cloudSceneManageService.getSceneManage(sceneId,
                            new SceneManageQueryOptions());
                        setForceCloseTime(forceCloseTime(taskKey), metric.getTimestamp(),
                            wrapperDTO.getPressureTestSecond());
                    }
                    // 取min
                    setMin(engineName + ScheduleConstants.FIRST_SIGN, metric.getTimestamp());
                    //多个压力节点 解决方案 只要一个节点 过来，状态就是压测引擎已启动，但是会通过redis计数 数据将归属于报告
                    // 压力节点 running -- > 压测引擎已启动
                    // 计数 压测引擎实际运行个数
                    Long count = stringRedisTemplate.opsForValue().increment(engineName, 1);

                    if (count != null && count == 1) {
                        cloudSceneManageService.updateSceneLifeCycle(UpdateStatusBean.build(sceneId, reportId, tenantId)
                            .checkEnum(SceneManageStatusEnum.PRESSURE_NODE_RUNNING)
                            .updateEnum(SceneManageStatusEnum.ENGINE_RUNNING)
                            .build());
                        notifyStart(sceneId, reportId, metric.getTimestamp());
                        cacheTryRunTaskStatus(sceneId, reportId, tenantId, SceneRunTaskStatusEnum.RUNNING);
                    }
                    ////如果从cloud上传请求流量明细，则需要启动异步线程去读取ptl文件上传
                    //if (PressureLogUploadConstants.UPLOAD_BY_CLOUD.equals(appConfig.getEngineLogUploadModel())) {
                    //    log.info("开始异步上传ptl日志，场景ID：{},报告ID:{},PodNum:{}", sceneId, reportId, metric.getPodNo());
                    //    EngineCallExtApi engineCallExtApi = enginePluginUtils.getEngineCallExtApi();
                    //    String fileName = metric.getTags().get(SceneTaskRedisConstants
                    //    .CURRENT_PTL_FILE_NAME_SYSTEM_PROP_KEY);
                    //    THREAD_POOL.submit(new PressureTestLogUploadTask(sceneId, reportId, tenantId, logUploadDAO,
                    //    stringRedisTemplate,
                    //        pushLogService, sceneManageDAO, ptlDir, fileName, engineCallExtApi) {});
                    //}
                }
                if (isLast) {
                    // 取max flag 是否更新过
                    Long engineNameNum = Optional.ofNullable(redisTemplate.opsForValue().get(engineName)).map(
                        String::valueOf).map(Long::valueOf).orElse(0L);
                    if (engineNameNum.equals(1L)) {
                        // 压测引擎只有一个运行 压测停止
                        log.info("本次压测{}-{}-{}:打入结束标识", sceneId, reportId, tenantId);
                        setLast(last(taskKey), ScheduleConstants.LAST_SIGN);
                        setMax(engineName + ScheduleConstants.LAST_SIGN, metric.getTimestamp());
                        notifyEnd(sceneId, reportId, metric.getTimestamp(), tenantId);
                        return;
                    }
                    // 计数 回传标识数量
                    Long tempLastSignCount = stringRedisTemplate.opsForValue().increment(
                        ScheduleConstants.TEMP_LAST_SIGN + engineName, 1);
                    // 是否是最后一个结束标识 回传个数 == 压测实际运行个数
                    if (isLastSign(tempLastSignCount, engineName)) {
                        // 标识结束标识
                        log.info("本次压测{}-{}-{}:打入结束标识", sceneId, reportId, tenantId);
                        setLast(last(taskKey), ScheduleConstants.LAST_SIGN);
                        setMax(engineName + ScheduleConstants.LAST_SIGN, metric.getTimestamp());
                        // 删除临时标识
                        stringRedisTemplate.delete(ScheduleConstants.TEMP_LAST_SIGN + engineName);
                        // 压测停止
                        notifyEnd(sceneId, reportId, metric.getTimestamp(), tenantId);
                    }
                }
            } catch (Exception e) {
                log.error("异常代码【{}】,异常内容：接收压测引擎回传事件数据异常 --> 【Collector-metrics-Error】接收处理事件数据，异常信息: {}",
                    TakinCloudExceptionEnum.TASK_RUNNING_RECEIVE_PT_DATA_ERROR, e);
            }
        }

    }

    private void cacheTryRunTaskStatus(Long sceneId, Long reportId, Long customerId, SceneRunTaskStatusEnum status) {
        taskStatusCache.cacheStatus(sceneId, reportId, status);
        Report report = tReportMapper.selectByPrimaryKey(reportId);
        if (Objects.nonNull(report) && !report.getPressureType().equals(PressureSceneEnum.FLOW_DEBUG.getCode())
            && !report.getPressureType().equals(PressureSceneEnum.INSPECTION_MODE.getCode())
            && status.getCode() == SceneRunTaskStatusEnum.RUNNING.getCode()) {
            cloudAsyncService.updateSceneRunningStatus(sceneId, reportId, customerId);
        }
    }

    private void notifyStart(Long sceneId, Long reportId, long startTime) {
        log.info("场景[{}]压测任务开始，更新报告[{}]开始时间[{}]", sceneId, reportId, startTime);
        reportDao.updateReportStartTime(reportId, new Date(startTime));
    }

    private void notifyEnd(Long sceneId, Long reportId, long endTime, Long tenantId) {
        log.info("场景[{}]压测任务已完成,更新结束时间{}", sceneId, reportId);
        // 刷新任务状态的Redis缓存
        taskStatusCache.cacheStatus(sceneId, reportId, SceneRunTaskStatusEnum.ENDED);
        // 更新压测场景状态  压测引擎运行中,压测引擎停止压测 ---->压测引擎停止压测
        cloudSceneManageService.updateSceneLifeCycle(UpdateStatusBean.build(sceneId, reportId, tenantId)
            .checkEnum(SceneManageStatusEnum.ENGINE_RUNNING, SceneManageStatusEnum.STOP)
            .updateEnum(SceneManageStatusEnum.STOP)
            .build());
        reportDao.updateReportEndTime(reportId, new Date(endTime));
    }

    private boolean isLastSign(Long lastSignCount, String engineName) {
        String redisResult = stringRedisTemplate.opsForValue().get(engineName);
        // redis中有信息 且信息匹配
        return StringUtils.isNotEmpty(redisResult) && lastSignCount.equals(Long.valueOf(redisResult));
    }

    /**
     * 统计每个时间窗口pod调用数量
     */
    public void statisticalIp(Long sceneId, Long reportId, Long tenantId, long time, String ip) {

        String windowsTimeKey = String.format("%s:%s", getPressureTaskKey(sceneId, reportId, tenantId),
            "windowsTime");
        String timeInMillis = String.valueOf(CollectorUtil.getTimeWindowTime(time));
        List<String> ips;
        Long windowsTimeValue = redisTemplate.getExpire(windowsTimeKey);
        if (Long.valueOf(-2L).equals(windowsTimeValue)) {
            ips = new ArrayList<>();
            ips.add(ip);
            redisTemplate.opsForHash().put(windowsTimeKey, timeInMillis, ips);
            redisTemplate.expire(windowsTimeKey, 60 * 60 * 2, TimeUnit.SECONDS);
        } else {
            Object cacheData = redisTemplate.opsForHash().get(windowsTimeKey, timeInMillis);
            if (cacheData instanceof List) {
                ips = ((List<?>)cacheData).stream()
                    .filter(t -> t instanceof String)
                    .map(Object::toString)
                    .collect(Collectors.toList());
            } else {
                ips = new ArrayList<>(0);
            }
            redisTemplate.opsForHash().put(windowsTimeKey, timeInMillis, ips);
        }

    }

    /**
     * 校验数据是否丢弃
     *
     * @return -
     */
    private boolean validate(long time, Long sceneId, Long reportId, Long tenantId, List<ResponseMetrics> metrics) {
        if ((System.currentTimeMillis() - time) > CollectorConstants.OVERDUE_TIME) {
            log.info("{}-{}-{}数据丢失,超时时间{}，数据原文：{}", sceneId, reportId, tenantId,
                System.currentTimeMillis() - time, JsonHelper.bean2Json(metrics));
            return false;
        }
        return true;
    }

}
