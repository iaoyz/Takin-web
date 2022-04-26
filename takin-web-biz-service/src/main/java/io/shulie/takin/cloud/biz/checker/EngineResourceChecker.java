package io.shulie.takin.cloud.biz.checker;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;

import com.pamirs.takin.cloud.entity.domain.entity.report.ReportBusinessActivityDetail;
import io.shulie.takin.adapter.api.constant.EntrypointUrl;
import io.shulie.takin.adapter.api.entrypoint.check.CloudCheckApi;
import io.shulie.takin.adapter.api.entrypoint.resource.CloudResourceApi;
import io.shulie.takin.adapter.api.model.request.check.ResourceCheckRequest;
import io.shulie.takin.adapter.api.model.request.resource.ResourceLockRequest;
import io.shulie.takin.adapter.api.model.response.resource.ResourceLockResponse;
import io.shulie.takin.cloud.biz.config.AppConfig;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.notify.PodStatusNotifyProcessor.PodStatus;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput.SceneBusinessActivityRefOutput;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.biz.service.strategy.StrategyConfigService;
import io.shulie.takin.cloud.biz.utils.DataUtils;
import io.shulie.takin.cloud.common.bean.scenemanage.SceneManageQueryOptions;
import io.shulie.takin.cloud.common.bean.scenemanage.UpdateStatusBean;
import io.shulie.takin.cloud.common.bean.task.TaskResult;
import io.shulie.takin.cloud.common.constants.ReportConstants;
import io.shulie.takin.cloud.common.constants.SceneManageConstant;
import io.shulie.takin.cloud.common.enums.PressureTaskStateEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.SceneManageStatusEnum;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.common.utils.CloudPluginUtils;
import io.shulie.takin.cloud.common.utils.CommonUtil;
import io.shulie.takin.cloud.common.utils.JsonPathUtil;
import io.shulie.takin.cloud.common.utils.JsonUtil;
import io.shulie.takin.cloud.data.dao.report.ReportBusinessActivityDetailDao;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskDAO;
import io.shulie.takin.cloud.data.mapper.mysql.ReportMapper;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskEntity;
import io.shulie.takin.cloud.data.model.mysql.ReportEntity;
import io.shulie.takin.cloud.ext.content.enginecall.StrategyConfigExt;
import io.shulie.takin.cloud.ext.content.enums.NodeTypeEnum;
import io.shulie.takin.cloud.ext.content.script.ScriptNode;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.eventcenter.annotation.IntrestFor;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckStatus;
import io.shulie.takin.web.ext.util.WebPluginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EngineResourceChecker implements CloudStartConditionChecker {

    public static final String RESOURCE_STATUS = "status";
    public static final String RESOURCE_POD_NUM = "podNum";
    public static final String RESOURCE_SUCCESS_NUM = "successNum";
    public static final String RESOURCE_MESSAGE = "message";
    public static final String RESOURCE_END_TIME = "endTime";
    public static final String TENANT_ID = "tenant_id";
    public static final String ENV_CODE = "env_code";
    public static final String SCENE_ID = "scene_id";
    public static final String REPORT_ID = "report_id";
    public static final String PRESSURE_TASK_ID = "pressure_task_id";
    public static final String JMETER_STARTED = "jmeter_started";
    public static final String JMETER_STOP = "jmeter_stopped";
    public static final String HEARTBEAT_TIME = "heartbeat_time";

    @Resource
    private CloudCheckApi cloudCheckApi;

    @Resource
    private StrategyConfigService strategyConfigService;

    @Resource
    private CloudSceneManageService cloudSceneManageService;

    @Resource
    private CloudResourceApi cloudResourceApi;

    @Resource
    private RedisClientUtils redisClientUtils;

    @Value("${pressure.node.start.expireTime:30}")
    private Integer pressureNodeStartExpireTime;

    /**
     * 初始化报告开始时间偏移时间
     */
    @Value("${init.report.startTime.Offset:10}")
    private Long offsetStartTime;

    @Resource
    private PressureTaskDAO pressureTaskDAO;

    @Resource
    private ReportMapper reportMapper;

    @Resource
    private ReportBusinessActivityDetailDao reportBusinessActivityDetailDao;

    @Resource
    private AppConfig appConfig;

    @Override
    public CheckResult check(CloudConditionCheckerContext context) throws TakinCloudException {
        String resourceId = context.getResourceId();
        if (StringUtils.isBlank(resourceId)) {
            return firstCheck(context);
        }
        return getResourceStatus(resourceId);
    }

    private CheckResult firstCheck(CloudConditionCheckerContext context) {
        SceneManageWrapperOutput sceneData = context.getSceneData();
        if (sceneData == null) {
            SceneManageQueryOptions options = new SceneManageQueryOptions();
            sceneData = cloudSceneManageService.getSceneManage(context.getSceneId(), options);
            context.setSceneData(sceneData);
        }
        try {
            StrategyConfigExt config = getStrategy();
            sceneData.setStrategy(config);
            ResourceCheckRequest request = new ResourceCheckRequest();
            request.setCpu(config.getCpuNum());
            request.setMemory(config.getMemorySize());
            request.setPod(sceneData.getIpNum());
            cloudCheckApi.checkResources(request);

            // 锁定资源：异步接口，每个pod启动成功都会回调一次回调接口
            String resourceId = lockResource(sceneData);
            sceneData.setResourceId(resourceId);
            SceneTaskStartInput input = context.getInput();
            PressureTaskEntity entity = initPressureTask(sceneData, input);
            ReportEntity report = initReport(sceneData, input, entity);
            cache(report);

            context.setTaskId(entity.getId());
            context.setReportId(report.getId());
            return getResourceStatus(resourceId);
        } catch (Exception e) {
            return CheckResult.fail(type(), e.getMessage());
        }
    }

    private CheckResult getResourceStatus(String resourceId) {
        String statusKey = getResourceKey(resourceId);
        Object redisStatus = redisClientUtils.hmget(statusKey, RESOURCE_STATUS);
        if (redisStatus == null) {
            return new CheckResult(type(), CheckStatus.FAIL.ordinal(), resourceId, "未找到启动中的任务");
        }
        int status = Integer.parseInt(String.valueOf(redisStatus));
        String message = String.valueOf(redisClientUtils.hmget(statusKey, RESOURCE_MESSAGE));
        if (status == PodStatus.START_FAIL.ordinal()) {
            // 失败时，删除对应缓存
            clearCache(resourceId);
        }
        return new CheckResult(type(), status, resourceId, message);
    }

    private String lockResource(SceneManageWrapperOutput sceneData) {
        StrategyConfigExt strategy = sceneData.getStrategy();
        ResourceLockRequest request = new ResourceLockRequest();
        request.setCpu(strategy.getCpuNum());
        request.setMemory(strategy.getMemorySize());
        request.setPod(sceneData.getIpNum());
        request.setCallBackUrl(DataUtils.mergeUrl(appConfig.getConsole(),
            EntrypointUrl.join(EntrypointUrl.MODULE_ENGINE_CALLBACK,
                EntrypointUrl.METHOD_ENGINE_CALLBACK_TASK_RESULT_NOTIFY)));
        ResourceLockResponse lockResponse = cloudResourceApi.lockResource(request);
        String resourceId = lockResponse.getResourceId();
        cache(sceneData, resourceId);
        return resourceId;
    }

    private void cache(SceneManageWrapperOutput sceneData, String resourceId) {
        // 缓存资源锁定状态和pod数
        Map<String, Object> param = new HashMap<>(32);
        param.put(RESOURCE_STATUS, CheckStatus.PENDING.ordinal());
        param.put(RESOURCE_POD_NUM, sceneData.getIpNum());
        param.put(RESOURCE_SUCCESS_NUM, 0);
        param.put(RESOURCE_MESSAGE, "");
        param.put(RESOURCE_END_TIME, System.currentTimeMillis() + pressureNodeStartExpireTime * 1000);
        param.put(TENANT_ID, sceneData.getTenantId());
        param.put(ENV_CODE, sceneData.getEnvCode());
        param.put(SCENE_ID, String.valueOf(sceneData.getId()));
        param.put(JMETER_STARTED, 0);
        param.put(JMETER_STOP, 0);
        param.put(HEARTBEAT_TIME, 0);
        redisClientUtils.hmset(getResourceKey(resourceId), param);
    }

    private void cache(ReportEntity report) {
        String resourceId = report.getResourceId();
        Map<String, Object> param = new HashMap<>(4);
        param.put(REPORT_ID, report.getId());
        param.put(PRESSURE_TASK_ID, report.getPressureTaskId());
        redisClientUtils.hmset(getResourceKey(resourceId), param);
    }

    private StrategyConfigExt getStrategy() {
        StrategyConfigExt config = strategyConfigService.getCurrentStrategyConfig();
        if (config == null) {
            throw new RuntimeException("未配置策略");
        }
        return config;
    }

    public static List<String> clearCacheKey(String resourceId) {
        return Collections.singletonList(getResourceKey(resourceId));
    }

    public static String getResourceKey(String resourceId) {
        return String.format("pressure:resource:locking:%s", resourceId);
    }

    private PressureTaskEntity initPressureTask(SceneManageWrapperOutput scene, SceneTaskStartInput input) {
        PressureTaskEntity entity = new PressureTaskEntity();
        entity.setSceneId(scene.getId());
        entity.setResourceId(scene.getResourceId());
        entity.setSceneName(scene.getPressureTestSceneName());
        // 解决开始时间 偏移10s
        entity.setStartTime(new Date(System.currentTimeMillis() + offsetStartTime * 1000));
        entity.setStatus(PressureTaskStateEnum.RESOURCE_LOCKING.ordinal());
        entity.setGmtCreate(new Date());
        entity.setOperateId(input.getOperateId());
        entity.setUserId(WebPluginUtils.traceUserId());
        entity.setTenantId(scene.getTenantId());
        entity.setEnvCode(scene.getEnvCode());
        pressureTaskDAO.save(entity);
        return entity;
    }

    /**
     * 初始化报表
     *
     */
    private ReportEntity initReport(SceneManageWrapperOutput scene, SceneTaskStartInput input, PressureTaskEntity pressureTask) {
        ReportEntity report = new ReportEntity();
        report.setSceneId(scene.getId());
        report.setTaskId(pressureTask.getId());
        report.setResourceId(pressureTask.getResourceId());
        report.setConcurrent(scene.getConcurrenceNum());
        report.setStatus(ReportConstants.INIT_STATUS);
        // 初始化
        report.setEnvCode(scene.getEnvCode());
        report.setTenantId(scene.getTenantId());
        report.setOperateId(input.getOperateId());
        // 解决开始时间 偏移10s
        report.setStartTime(pressureTask.getStartTime());
        //负责人默认启动人
        report.setUserId(CloudPluginUtils.getUserId());
        report.setSceneName(scene.getPressureTestSceneName());

        if (StringUtils.isNotBlank(scene.getFeatures())) {
            JSONObject features = JsonUtil.parse(scene.getFeatures());
            if (null != features && features.containsKey(SceneManageConstant.FEATURES_SCRIPT_ID)) {
                report.setScriptId(features.getLong(SceneManageConstant.FEATURES_SCRIPT_ID));
            }
        }
        Integer sumTps = CommonUtil.sum(scene.getBusinessActivityConfig(), SceneBusinessActivityRefOutput::getTargetTPS);

        report.setTps(sumTps);
        report.setPressureType(scene.getPressureType());
        report.setType(scene.getType());
        if (StringUtils.isNotBlank(scene.getScriptAnalysisResult())) {
            report.setScriptNodeTree(JsonPathUtil.deleteNodes(scene.getScriptAnalysisResult()).jsonString());
        }
        reportMapper.insert(report);

        //标记场景
        // 待启动,压测失败，停止压测（压测工作已停止） 强制停止 ---> 启动中
        Boolean updateFlag = cloudSceneManageService.updateSceneLifeCycle(
            UpdateStatusBean.build(scene.getId(), report.getId(), scene.getTenantId())
                .checkEnum(SceneManageStatusEnum.WAIT, SceneManageStatusEnum.FAILED, SceneManageStatusEnum.STOP,
                    SceneManageStatusEnum.FORCE_STOP)
                .updateEnum(SceneManageStatusEnum.STARTING).build());
        if (!updateFlag) {
            //失败状态 获取最新的报告
            reportMapper.selectById(report.getId());
            return report;
        }
        Long reportId = report.getId();
        //初始化业务活动
        scene.getBusinessActivityConfig().forEach(activity -> {
            ReportBusinessActivityDetail reportBusinessActivityDetail = new ReportBusinessActivityDetail();
            reportBusinessActivityDetail.setReportId(reportId);
            reportBusinessActivityDetail.setSceneId(scene.getId());
            reportBusinessActivityDetail.setBusinessActivityId(activity.getBusinessActivityId());
            reportBusinessActivityDetail.setBusinessActivityName(activity.getBusinessActivityName());
            reportBusinessActivityDetail.setApplicationIds(activity.getApplicationIds());
            reportBusinessActivityDetail.setBindRef(activity.getBindRef());
            if (null != activity.getTargetTPS()) {
                reportBusinessActivityDetail.setTargetTps(new BigDecimal(activity.getTargetTPS()));
            }
            if (null != activity.getTargetRT()) {
                reportBusinessActivityDetail.setTargetRt(new BigDecimal(activity.getTargetRT()));
            }
            reportBusinessActivityDetail.setTargetSuccessRate(activity.getTargetSuccessRate());
            reportBusinessActivityDetail.setTargetSa(activity.getTargetSA());
            reportBusinessActivityDetailDao.insert(reportBusinessActivityDetail);
        });
        saveNonTargetNode(scene.getId(), reportId, report.getScriptNodeTree(), scene.getBusinessActivityConfig());
        log.info("启动[{}]场景测试，初始化报表数据,报表ID: {}", scene.getId(), report.getId());

        PressureTaskEntity tmp = new PressureTaskEntity();
        tmp.setId(pressureTask.getId());
        tmp.setReportId(report.getId());
        pressureTaskDAO.updateById(tmp);

        return report;

    }

    /**
     * 把节点树中的测试计划、线程组、控制器当作业务活动插入到报告关联的业务活动中
     *
     * @param sceneId                场景ID
     * @param reportId               报告ID
     * @param scriptNodeTree         节点树
     * @param businessActivityConfig 场景业务活动信息
     */
    private void saveNonTargetNode(Long sceneId, Long reportId, String scriptNodeTree, List<SceneBusinessActivityRefOutput> businessActivityConfig) {
        if (StringUtils.isBlank(scriptNodeTree) || CollectionUtils.isEmpty(businessActivityConfig)) {
            return;
        }
        List<String> bindRefList = businessActivityConfig.stream().filter(Objects::nonNull)
            .map(SceneBusinessActivityRefOutput::getBindRef)
            .collect(Collectors.toList());
        List<ReportBusinessActivityDetail> resultList = new ArrayList<>();
        List<ScriptNode> testPlanNodeList = JsonPathUtil.getCurrentNodeByType(scriptNodeTree,
            NodeTypeEnum.TEST_PLAN.name());
        if (CollectionUtils.isNotEmpty(testPlanNodeList) && testPlanNodeList.size() == 1) {
            ScriptNode scriptNode = testPlanNodeList.get(0);
            fillNonTargetActivityDetail(sceneId, reportId, scriptNode, resultList);
        }
        List<ScriptNode> threadGroupNodes = JsonPathUtil.getCurrentNodeByType(scriptNodeTree,
            NodeTypeEnum.THREAD_GROUP.name());
        if (CollectionUtils.isNotEmpty(threadGroupNodes)) {
            threadGroupNodes.stream().filter(Objects::nonNull)
                .forEach(node -> fillNonTargetActivityDetail(sceneId, reportId, node, resultList));
        }
        List<ScriptNode> controllerNodes = JsonPathUtil.getCurrentNodeByType(scriptNodeTree,
            NodeTypeEnum.CONTROLLER.name());
        if (CollectionUtils.isNotEmpty(controllerNodes)) {
            controllerNodes.stream().filter(Objects::nonNull)
                .filter(node -> !bindRefList.contains(node.getXpathMd5()))
                .forEach(node -> fillNonTargetActivityDetail(sceneId, reportId, node, resultList));
        }
        if (CollectionUtils.isNotEmpty(resultList)) {
            resultList.stream().filter(Objects::nonNull)
                .forEach(detail -> reportBusinessActivityDetailDao.insert(detail));
        }

    }

    /**
     * 计算子节点的目标值
     *
     * @param sceneId    场景ID
     * @param scriptNode 目标节点
     * @param detailList 结果
     */
    private void fillNonTargetActivityDetail(Long sceneId, Long reportId, ScriptNode scriptNode,
        List<ReportBusinessActivityDetail> detailList) {
        ReportBusinessActivityDetail detail = new ReportBusinessActivityDetail();
        detail.setTargetTps(new BigDecimal(-1));
        detail.setTargetRt(new BigDecimal(-1));
        detail.setTargetSa(new BigDecimal(-1));
        detail.setTargetSuccessRate(new BigDecimal(-1));
        detail.setSceneId(sceneId);
        detail.setReportId(reportId);
        detail.setBusinessActivityId(-1L);
        detail.setBusinessActivityName(scriptNode.getTestName());
        detail.setBindRef(scriptNode.getXpathMd5());
        detailList.add(detail);
    }

    @IntrestFor(event = "finished")
    public void clearResourceCache(Event event) {
        try {
            log.info("删除resource缓存");
            TaskResult taskResult = (TaskResult)event.getExt();
            clearCache(taskResult.getResourceId());
        } catch (Exception e) {
            log.error("删除resource缓存={}", e.getMessage(), e);
        }
    }

    private void clearCache(String resourceId) {
        redisClientUtils.del(clearCacheKey(resourceId).toArray(new String[0]));
    }

    @Override
    public String type() {
        return "resource";
    }
}
