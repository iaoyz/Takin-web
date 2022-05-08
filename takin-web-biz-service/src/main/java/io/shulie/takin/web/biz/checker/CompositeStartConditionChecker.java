package io.shulie.takin.web.biz.checker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import cn.hutool.core.util.StrUtil;
import com.pamirs.takin.entity.domain.dto.scenemanage.SceneManageWrapperDTO;
import io.shulie.takin.adapter.api.entrypoint.scene.manage.SceneManageApi;
import io.shulie.takin.adapter.api.model.request.scenemanage.SceneManageIdReq;
import io.shulie.takin.cloud.biz.collector.collector.AbstractIndicators.ResourceContext;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneTaskService;
import io.shulie.takin.cloud.common.bean.scenemanage.SceneManageQueryOptions;
import io.shulie.takin.cloud.common.bean.task.TaskResult;
import io.shulie.takin.cloud.common.enums.PressureTaskStateEnum;
import io.shulie.takin.cloud.common.enums.scenemanage.TaskStatusEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.dao.scene.manage.SceneManageDAO;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskDAO;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskVarietyDAO;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskEntity;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskVarietyEntity;
import io.shulie.takin.cloud.data.model.mysql.ReportEntity;
import io.shulie.takin.cloud.data.util.PressureStartCache;
import io.shulie.takin.cloud.ext.content.enums.AssetTypeEnum;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.eventcenter.EventCenterTemplate;
import io.shulie.takin.eventcenter.annotation.IntrestFor;
import io.shulie.takin.utils.json.JsonHelper;
import io.shulie.takin.web.biz.checker.StartConditionChecker.CheckResult;
import io.shulie.takin.web.biz.checker.StartConditionChecker.CheckStatus;
import io.shulie.takin.web.ext.util.WebPluginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Slf4j
@Component
public class CompositeStartConditionChecker implements InitializingBean {

    /**
     * 压力节点 启动时间超时
     */
    @Value("${pressure.pod.start.expireTime: 30}")
    private Integer pressurePodStartExpireTime;

    @Resource
    private RedisClientUtils redisClientUtils;

    @Resource
    private SceneManageApi sceneManageApi;

    @Resource
    private PressureTaskDAO pressureTaskDAO;

    @Resource
    private CloudSceneManageService cloudSceneManageService;

    @Resource
    private CloudSceneTaskService cloudSceneTaskService;

    @Resource
    private List<StartConditionChecker> checkerList;

    @Resource
    private EventCenterTemplate eventCenterTemplate;

    @Resource
    private SceneManageDAO sceneManageDAO;

    @Resource
    private PressureTaskVarietyDAO pressureTaskVarietyDAO;

    // 此处特殊使用
    public List<CheckResult> doCheck(StartConditionCheckerContext context) {
        initContext(context);
        initTaskAndReportIfNecessary(context);
        List<CheckResult> resultList = new ArrayList<>(checkerList.size());
        for (StartConditionChecker checker : checkerList) {
            CheckResult checkResult = doCheck(context, checker);
            resultList.add(checkResult);
            if (checkResult.getStatus().equals(CheckStatus.FAIL.ordinal())) {
                context.setMessage(checkResult.getMessage());
                callStartFailEvent(context);
                break;
            }
        }
        return resultList;
    }

    private CheckResult doCheck(StartConditionCheckerContext context, StartConditionChecker checker) {
        String preStopKey = PressureStartCache.getScenePreStopKey(context.getSceneId(),
            StrUtil.nullToEmpty(context.getResourceId()));
        String preStopTime = redisClientUtils.getString(preStopKey);
        if (StringUtils.isNotBlank(preStopTime)) {
            if (Long.parseLong(preStopTime) > context.getTime()) {
                redisClientUtils.delete(preStopKey);
                return new CheckResult(checker.type(), CheckStatus.FAIL.ordinal(), "取消压测");
            }
        }
        return checker.check(context);
    }

    private void initContext(StartConditionCheckerContext context) {
        Long sceneId = context.getSceneId();
        if (context.getSceneData() == null) {
            SceneManageQueryOptions options = new SceneManageQueryOptions();
            options.setIncludeBusinessActivity(true);
            options.setIncludeScript(true);
            context.setSceneData(cloudSceneManageService.getSceneManage(sceneId, options));
        }

        SceneTaskStartInput input = new SceneTaskStartInput();
        input.setOperateId(WebPluginUtils.traceUserId());
        input.setAssetType(AssetTypeEnum.PRESS_REPORT.getCode());
        context.setInput(input);

        SceneManageIdReq req = new SceneManageIdReq();
        req.setId(sceneId);
        context.setSceneDataDTO(JsonHelper.json2Bean(JsonHelper.bean2Json(sceneManageApi.getSceneDetail(req)),
            SceneManageWrapperDTO.class));
    }

    private void initTaskAndReportIfNecessary(StartConditionCheckerContext context) {
        String resourceId = context.getResourceId();
        if (StringUtils.isBlank(resourceId)) {
            SceneManageWrapperOutput sceneData = context.getSceneData();
            SceneTaskStartInput input = context.getInput();
            PressureTaskEntity pressureTask = cloudSceneTaskService.initPressureTask(sceneData, input);
            ReportEntity report = cloudSceneTaskService.initReport(sceneData, input, pressureTask);
            context.setTaskId(pressureTask.getId());
            context.setReportId(report.getId());
        } else {
            completeByCache(context);
        }
        context.setInitTaskAndReport(true);
    }

    private void completeByCache(StartConditionCheckerContext context) {
        String resourceId = context.getResourceId();
        Object taskId = redisClientUtils.hmget(PressureStartCache.getResourceKey(resourceId), PressureStartCache.TASK_ID);
        if (Objects.nonNull(taskId)) {
            context.setTaskId(Long.valueOf(String.valueOf(taskId)));
        }
        Object reportId = redisClientUtils.hmget(PressureStartCache.getResourceKey(resourceId), PressureStartCache.REPORT_ID);
        if (Objects.nonNull(reportId)) {
            context.setReportId(Long.valueOf(String.valueOf(reportId)));
        }
        Object uniqueKey = redisClientUtils.hmget(PressureStartCache.getResourceKey(resourceId), PressureStartCache.UNIQUE_KEY);
        if (Objects.nonNull(reportId)) {
            context.setUniqueKey(String.valueOf(uniqueKey));
        }
    }

    private void callStartFailEvent(StartConditionCheckerContext context) {
        Event event = new Event();
        event.setEventName(PressureStartCache.CHECK_FAIL_EVENT);
        ResourceContext resourceContext = new ResourceContext();
        resourceContext.setSceneId(context.getSceneId());
        resourceContext.setTaskId(context.getTaskId());
        resourceContext.setReportId(context.getReportId());
        resourceContext.setResourceId(context.getResourceId());
        resourceContext.setMessage(context.getMessage());
        resourceContext.setUniqueKey(context.getUniqueKey());
        event.setExt(resourceContext);
        eventCenterTemplate.doEvents(event);
    }

    @IntrestFor(event = PressureStartCache.CHECK_FAIL_EVENT, order = 1)
    public void callStartFailClear(Event event) {
        ResourceContext ext = (ResourceContext)event.getExt();
        StartConditionCheckerContext context = new StartConditionCheckerContext();
        context.setSceneId(ext.getSceneId());
        context.setTaskId(ext.getTaskId());
        context.setReportId(ext.getReportId());
        context.setMessage(ext.getMessage());
        context.setUniqueKey(ext.getUniqueKey());
        callStartFailClear(context);
    }

    @IntrestFor(event = PressureStartCache.PRE_STOP_EVENT, order = 0)
    public void callPreStop(Event event) {
        ResourceContext resourceContext = (ResourceContext)event.getExt();
        Long sceneId = resourceContext.getSceneId();
        cachePreStopEvent(resourceContext);
        StartConditionCheckerContext context = new StartConditionCheckerContext();
        context.setSceneId(sceneId);
        context.setResourceId(resourceContext.getResourceId());
        context.setMessage(resourceContext.getMessage());
        Map<Object, Object> sceneResource = redisClientUtils.hmget(PressureStartCache.getSceneResourceKey(sceneId));
        if (!CollectionUtils.isEmpty(sceneResource)) {
            Object reportId = sceneResource.get(PressureStartCache.REPORT_ID);
            if (Objects.nonNull(reportId)) {
                context.setReportId(Long.valueOf(String.valueOf(reportId)));
            }
            Object taskId = sceneResource.get(PressureStartCache.TASK_ID);
            if (Objects.nonNull(taskId)) {
                context.setTaskId(Long.valueOf(String.valueOf(taskId)));
            }
            Object uniqueKey = sceneResource.get(PressureStartCache.UNIQUE_KEY);
            if (Objects.nonNull(uniqueKey)) {
                context.setUniqueKey(String.valueOf(uniqueKey));
            }
            String resourceId = context.getResourceId();
            if (StringUtils.isBlank(resourceId)) {
                Object resource = sceneResource.get(PressureStartCache.RESOURCE_ID);
                if (Objects.nonNull(uniqueKey)) {
                    context.setResourceId(String.valueOf(resource));
                }
            }
        }
        callStartFailClear(context);
    }

    private void cachePreStopEvent(ResourceContext resourceContext) {
        Long sceneId = resourceContext.getSceneId();
        String curTime = String.valueOf(System.currentTimeMillis());
        redisClientUtils.setString(PressureStartCache.getScenePreStopKey(sceneId, ""),
            curTime, pressurePodStartExpireTime, TimeUnit.SECONDS);
        String resourceId = resourceContext.getResourceId();
        if (StringUtils.isNotBlank(resourceId)) {
            redisClientUtils.setString(PressureStartCache.getScenePreStopKey(sceneId, resourceId),
                curTime, pressurePodStartExpireTime, TimeUnit.SECONDS);
        }
    }

    private void callStartFailClear(StartConditionCheckerContext context) {
        boolean flag = redisClientUtils.unlock(PressureStartCache.getSceneResourceLockingKey(context.getSceneId()), context.getUniqueKey());
        Long taskId = context.getTaskId();
        if (Objects.nonNull(taskId)) {
            PressureTaskEntity entity = new PressureTaskEntity();
            entity.setId(taskId);
            entity.setStatus(PressureTaskStateEnum.RESOURCE_LOCK_FAILED.ordinal());
            entity.setExceptionMsg(context.getMessage());
            entity.setIsDeleted(1);
            entity.setGmtUpdate(new Date());
            pressureTaskDAO.updateById(entity);
            pressureTaskVarietyDAO.save(PressureTaskVarietyEntity.of(taskId,
                PressureTaskStateEnum.RESOURCE_LOCK_FAILED, context.getMessage()));
        }
        Long reportId = context.getReportId();
        if (Objects.nonNull(reportId)) {
            TaskResult result = new TaskResult();
            result.setSceneId(context.getSceneId());
            result.setTaskId(reportId);
            result.setMsg(context.getMessage());
            result.setStatus(TaskStatusEnum.FAILED);
            cloudSceneTaskService.handleSceneTaskEvent(result);
        }
        if (flag) {
            redisClientUtils.delete(PressureStartCache.getSceneResourceKey(context.getSceneId()));
        }
    }

    @Override
    public void afterPropertiesSet() {
        if (checkerList == null) {
            checkerList = new ArrayList<>(0);
        }
        checkerList.sort((it1, it2) -> {
            int i1 = it1.getOrder();
            int i2 = it2.getOrder();
            return Integer.compare(i1, i2);
        });
    }
}
