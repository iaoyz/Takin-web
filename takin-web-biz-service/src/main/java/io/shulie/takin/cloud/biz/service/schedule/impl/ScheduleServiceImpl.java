package io.shulie.takin.cloud.biz.service.schedule.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;

import cn.hutool.core.bean.BeanUtil;
import com.pamirs.takin.cloud.entity.dao.schedule.TScheduleRecordMapper;
import com.pamirs.takin.cloud.entity.domain.entity.schedule.ScheduleRecord;
import com.pamirs.takin.cloud.entity.domain.vo.scenemanage.SceneManageStartRecordVO;
import io.shulie.takin.adapter.api.entrypoint.pressure.PressureTaskApi;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStartReq;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStartReq.DebugConfig;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStartReq.PressureDataFile;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStartReq.PressureDataFilePosition;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStartReq.ThreadGroupConfig;
import io.shulie.takin.adapter.api.model.request.pressure.PressureTaskStopReq;
import io.shulie.takin.adapter.api.model.response.pressure.PressureActionResp;
import io.shulie.takin.cloud.biz.config.AppConfig;
import io.shulie.takin.cloud.biz.service.record.ScheduleRecordEnginePluginService;
import io.shulie.takin.cloud.biz.service.report.CloudReportService;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.biz.service.schedule.ScheduleEventService;
import io.shulie.takin.cloud.biz.service.schedule.ScheduleService;
import io.shulie.takin.cloud.biz.service.strategy.StrategyConfigService;
import io.shulie.takin.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.takin.cloud.common.bean.task.TaskResult;
import io.shulie.takin.cloud.common.constants.PressureInstanceRedisKey;
import io.shulie.takin.cloud.common.constants.ScheduleConstants;
import io.shulie.takin.cloud.common.enums.PressureSceneEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.cloud.common.utils.CommonUtil;
import io.shulie.takin.cloud.common.utils.EnginePluginUtils;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskDAO;
import io.shulie.takin.cloud.ext.api.EngineCallExtApi;
import io.shulie.takin.cloud.ext.content.enginecall.ScheduleInitParamExt;
import io.shulie.takin.cloud.ext.content.enginecall.ScheduleRunRequest;
import io.shulie.takin.cloud.ext.content.enginecall.ScheduleStartRequestExt;
import io.shulie.takin.cloud.ext.content.enginecall.ScheduleStartRequestExt.StartEndPosition;
import io.shulie.takin.cloud.ext.content.enginecall.ScheduleStopRequestExt;
import io.shulie.takin.cloud.ext.content.enginecall.StrategyConfigExt;
import io.shulie.takin.cloud.ext.content.enginecall.ThreadGroupConfigExt;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.eventcenter.annotation.IntrestFor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * @author 莫问
 * @date 2020-05-12
 */
@Service
@Slf4j
public class ScheduleServiceImpl implements ScheduleService {
    @Resource
    private StrategyConfigService strategyConfigService;
    @Resource
    private TScheduleRecordMapper tScheduleRecordMapper;
    @Resource
    private ScheduleEventService scheduleEvent;
    @Resource
    private CloudSceneManageService cloudSceneManageService;
    @Resource
    private ScheduleRecordEnginePluginService scheduleRecordEnginePluginService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource
    private CloudReportService cloudReportService;
    @Resource
    private EnginePluginUtils pluginUtils;
    @Resource
    private AppConfig appConfig;
    @Resource
    private PressureTaskApi pressureTaskApi;
    @Resource
    private PressureTaskDAO pressureTaskDAO;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void startSchedule(ScheduleStartRequestExt request) {
        log.info("启动调度, 请求数据：{}", request);
        //任务只处理一次
        ScheduleRecord schedule = tScheduleRecordMapper.getScheduleByTaskId(request.getTaskId());
        if (schedule != null) {
            log.error("异常代码【{}】,异常内容：启动调度失败 --> 调度任务[{}]已经启动",
                TakinCloudExceptionEnum.SCHEDULE_START_ERROR, request.getTaskId());
            return;
        }
        //获取策略
        StrategyConfigExt config = strategyConfigService.getCurrentStrategyConfig();
        if (config == null) {
            log.error("异常代码【{}】,异常内容：启动调度失败 --> 调度策略未配置",
                TakinCloudExceptionEnum.SCHEDULE_START_ERROR);
            return;
        }

        String scheduleName = ScheduleConstants.getScheduleName(request.getSceneId(), request.getTaskId(), request.getTenantId());

        //保存调度记录
        ScheduleRecord scheduleRecord = new ScheduleRecord();
        scheduleRecord.setCpuCoreNum(config.getCpuNum());
        scheduleRecord.setPodNum(request.getTotalIp());
        scheduleRecord.setMemorySize(config.getMemorySize());
        scheduleRecord.setSceneId(request.getSceneId());
        scheduleRecord.setTaskId(request.getTaskId());
        scheduleRecord.setStatus(ScheduleConstants.SCHEDULE_STATUS_1);

        scheduleRecord.setTenantId(request.getTenantId());
        scheduleRecord.setPodClass(scheduleName);
        tScheduleRecordMapper.insertSelective(scheduleRecord);

        //add by 李鹏 保存调度对应压测引擎插件记录信息
        scheduleRecordEnginePluginService.saveScheduleRecordEnginePlugins(
            scheduleRecord.getId(), request.getEnginePluginsFilePath());
        //add end

        //发布事件
        ScheduleRunRequest eventRequest = new ScheduleRunRequest();
        eventRequest.setScheduleId(scheduleRecord.getId());
        eventRequest.setRequest(request);
        eventRequest.setStrategyConfig(config);
        String memSetting;
        if (PressureSceneEnum.INSPECTION_MODE.getCode().equals(request.getPressureScene())) {
            memSetting = "-XX:MaxRAMPercentage=90.0";
        } else {
            memSetting = CommonUtil.getValue(appConfig.getK8sJvmSettings(), config, StrategyConfigExt::getK8sJvmSettings);
        }
        eventRequest.setMemSetting(memSetting);
        // TODO：设置采样率
        //把数据放入缓存，初始化回调调度需要
        stringRedisTemplate.opsForValue().set(scheduleName, JSON.toJSONString(eventRequest));
        // 需要将 本次调度 pod数量存入redis,报告中用到
        // 总计 报告生成用到 调度期间出现错误，这份数据只存24小时
        stringRedisTemplate.opsForValue().set(
            ScheduleConstants.getPressureNodeTotalKey(request.getSceneId(), request.getTaskId(), request.getTenantId()),
            String.valueOf(request.getTotalIp()), 1, TimeUnit.DAYS);
        //调度初始化
        scheduleEvent.initSchedule(eventRequest);
    }

    @Override
    public void stopSchedule(ScheduleStopRequestExt request) {
        log.info("停止调度, 请求数据：{}", request);
        ScheduleRecord scheduleRecord = tScheduleRecordMapper.getScheduleByTaskId(request.getTaskId());
        if (scheduleRecord != null) {
            // 增加中断
            String scheduleName = ScheduleConstants.getScheduleName(request.getSceneId(), request.getTaskId(), request.getTenantId());
            stringRedisTemplate.opsForValue().set(
                ScheduleConstants.INTERRUPT_POD + scheduleName,
                Boolean.TRUE.toString(), 1, TimeUnit.DAYS);
            PressureTaskStopReq req = new PressureTaskStopReq();
            req.setTaskId(request.getPressureTaskId());
            pressureTaskApi.stop(req);
        }

    }

    @Override
    public void runSchedule(ScheduleRunRequest request) {
        ScheduleStartRequestExt startRequest = request.getRequest();
        // 压力机数目记录
        push(startRequest);

        Long sceneId = startRequest.getSceneId();
        Long taskId = startRequest.getTaskId();
        Long customerId = startRequest.getTenantId();

        // 场景生命周期更新 启动中(文件拆分完成) ---> 创建Job中
        cloudSceneManageService.updateSceneLifeCycle(
            UpdateStatusBean.build(sceneId, taskId, customerId)
                .checkEnum(SceneManageStatusEnum.STARTING, SceneManageStatusEnum.FILE_SPLIT_END)
                .updateEnum(SceneManageStatusEnum.JOB_CREATING)
                .build());
        PressureTaskStartReq req = buildStartReq(request);
        try {
            PressureActionResp actionResp = pressureTaskApi.start(req);
            // 是空的
            log.info("场景{},任务{},顾客{}开始启动压测， 压测启动成功", sceneId, taskId, customerId);
            updateReportAssociation(startRequest, actionResp);
        } catch (Exception e) {
            // 创建失败
            log.info("场景{},任务{},顾客{}开始启动压测，压测启动失败", sceneId, taskId, customerId);
            cloudSceneManageService.reportRecord(SceneManageStartRecordVO.build(sceneId, taskId, customerId).success(false)
                .errorMsg("压测启动创建失败，失败原因：" + e.getMessage()).build());
        }
    }

    @Override
    public void initScheduleCallback(ScheduleInitParamExt param) {

    }

    /**
     * 临时方案：
     * 拆分文件的索引都存入到redis队列, 避免控制台集群环境下索引获取不正确
     */
    private void push(ScheduleStartRequestExt request) {
        //把数据放入队列
        String key = ScheduleConstants.getFileSplitQueue(request.getSceneId(), request.getTaskId(), request.getTenantId());
        // 生成集合
        List<String> numList = IntStream.rangeClosed(1, request.getTotalIp())
            .boxed().map(String::valueOf)
            .collect(Collectors.toCollection(ArrayList::new));
        // 集合放入Redis
        stringRedisTemplate.opsForList().leftPushAll(key, numList);
    }

    /**
     * 压测结束，删除 压力节点 job configMap
     */
    @IntrestFor(event = "finished")
    public void doDeleteJob(Event event) {
        log.info("通知deleteJob模块， 监听到完成事件.....");
        try {
            Object object = event.getExt();
            TaskResult taskResult = (TaskResult)object;
            // 删除 压测任务
            String jobName = ScheduleConstants.getScheduleName(taskResult.getSceneId(), taskResult.getTaskId(),
                taskResult.getTenantId());
            String engineInstanceRedisKey = PressureInstanceRedisKey.getEngineInstanceRedisKey(taskResult.getSceneId(), taskResult.getTaskId(),
                taskResult.getTenantId());
            ScheduleStopRequestExt scheduleStopRequest = new ScheduleStopRequestExt();
            scheduleStopRequest.setJobName(jobName);
            scheduleStopRequest.setEngineInstanceRedisKey(engineInstanceRedisKey);

            EngineCallExtApi engineCallExtApi = pluginUtils.getEngineCallExtApi();
            engineCallExtApi.deleteJob(scheduleStopRequest);

            redisTemplate.expire(engineInstanceRedisKey, 10, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("异常代码【{}】,异常内容：任务停止失败失败 --> 【deleteJob】处理finished事件异常: {}",
                TakinCloudExceptionEnum.TASK_STOP_DELETE_TASK_ERROR, e);
        }

    }

    private PressureTaskStartReq buildStartReq(ScheduleRunRequest runRequest) {
        ScheduleStartRequestExt request = runRequest.getRequest();
        PressureTaskStartReq req = new PressureTaskStartReq();
        req.setJvmParams(runRequest.getMemSetting());
        req.setResourceId(request.getResourceId());
        req.setPressureType(request.getPressureScene());
        req.setSampling(runRequest.getTraceSampling());

        Map<String, ThreadGroupConfigExt> configMap = request.getThreadGroupConfigMap();
        Map<String, ThreadGroupConfig> testMap = new HashMap<>(configMap.size());
        configMap.forEach((key, value) -> {
            ThreadGroupConfig config = new ThreadGroupConfig();
            config.setType(value.getType());
            config.setModel(value.getMode());
            config.setThreadNum(value.getThreadNum());
            config.setRampUp(value.getRampUp());
            config.setRampUnit(value.getRampUpUnit());
            config.setSteps(value.getSteps());
            testMap.putIfAbsent(key, config);
        });
        req.setTest(testMap);
        List<PressureDataFile> dataFiles = request.getDataFile().stream().map(file -> {
            PressureDataFile dataFile = new PressureDataFile();
            dataFile.setPath(file.getPath());
            dataFile.setType(file.getFileType());
            dataFile.setSplit(file.isSplit());
            dataFile.setOrdered(file.isOrdered());
            dataFile.setBigFile(file.isBigFile());
            dataFile.setMd5(file.getFileMd5());

            Map<Integer, List<StartEndPosition>> positions = file.getStartEndPositions();
            if (!CollectionUtils.isEmpty(positions)) {
                Map<Integer, List<PressureDataFilePosition>> dataFilePositions = new HashMap<>(positions.size());
                positions.forEach((key, value) -> {
                    List<PressureDataFilePosition> filePositions = value.stream()
                        .map(val -> BeanUtil.copyProperties(val, PressureDataFilePosition.class))
                        .collect(Collectors.toList());
                    dataFilePositions.putIfAbsent(key, filePositions);
                });
                dataFile.setSplitPositions(dataFilePositions);
            }
            return dataFile;
        }).collect(Collectors.toList());
        req.setFiles(dataFiles);
        if (request.isTryRun()) {
            req.setDebugInfo(new DebugConfig(request.getLoopsNum(), request.getConcurrenceNum()));
        }
        return req;
    }

    private void updateReportAssociation(ScheduleStartRequestExt startRequest, PressureActionResp actionResp) {
        Long pressureTaskId = actionResp.getTaskId();
        String resourceId = startRequest.getResourceId();
        cloudReportService.updateResourceAssociation(resourceId, pressureTaskId);
        pressureTaskDAO.updateResourceAssociation(resourceId, pressureTaskId);
    }
}
