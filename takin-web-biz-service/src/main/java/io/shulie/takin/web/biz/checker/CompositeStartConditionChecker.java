package io.shulie.takin.web.biz.checker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

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
import io.shulie.takin.cloud.common.enums.scenemanage.TaskStatusEnum;
import io.shulie.takin.cloud.common.redis.RedisClientUtils;
import io.shulie.takin.cloud.data.dao.scene.task.PressureTaskDAO;
import io.shulie.takin.cloud.data.model.mysql.PressureTaskEntity;
import io.shulie.takin.cloud.data.model.mysql.ReportEntity;
import io.shulie.takin.eventcenter.Event;
import io.shulie.takin.eventcenter.annotation.IntrestFor;
import io.shulie.takin.utils.json.JsonHelper;
import io.shulie.takin.web.biz.cache.PressureStartCache;
import io.shulie.takin.web.biz.checker.StartConditionChecker.CheckResult;
import io.shulie.takin.web.biz.checker.StartConditionChecker.CheckStatus;
import io.shulie.takin.web.ext.util.WebPluginUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class CompositeStartConditionChecker implements InitializingBean {


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

    // 此处特殊使用
    public List<CheckResult> doCheck(StartConditionCheckerContext context) {
        initContext(context);
        initTaskAndReportIfNecessary(context);
        List<CheckResult> resultList = new ArrayList<>(checkerList.size());
        for (StartConditionChecker checker : checkerList) {
            CheckResult checkResult = checker.check(context);
            resultList.add(checkResult);
            if (checkResult.getStatus().equals(CheckStatus.FAIL.ordinal())) {
                context.setMessage(checkResult.getMessage());
                callStartFailClear(context);
                break;
            }
        }
        return resultList;
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
        Object taskId = redisClientUtils.hmget(PressureStartCache.getResourceKey(resourceId), PressureStartCache.PRESSURE_TASK_ID);
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

    @IntrestFor(event = PressureStartCache.CHECK_FAIL_EVENT, order = 0)
    public void callStartFailClear(Event event) {
        ResourceContext ext = (ResourceContext)event.getExt();
        StartConditionCheckerContext context = new StartConditionCheckerContext();
        context.setSceneId(ext.getSceneId());
        context.setTaskId(ext.getPressureTaskId());
        context.setReportId(ext.getReportId());
        context.setMessage("压力机资源不足");
        context.setUniqueKey(ext.getUniqueKey());
        callStartFailClear(context);
    }

    @IntrestFor(event = PressureStartCache.PRE_STOP_EVENT, order = 0)
    public void callPreStop(Event event) {
        Long sceneId = (Long)event.getExt();
        StartConditionCheckerContext context = new StartConditionCheckerContext();
        context.setSceneId(sceneId);
        context.setMessage("压测取消");
        Object reportId = redisClientUtils.hmget(PressureStartCache.getSceneResourceKey(sceneId), PressureStartCache.REPORT_ID);
        if (Objects.nonNull(reportId)) {
            context.setReportId(Long.valueOf(String.valueOf(reportId)));
        }
        Object taskId = redisClientUtils.hmget(PressureStartCache.getSceneResourceKey(sceneId), PressureStartCache.PRESSURE_TASK_ID);
        if (Objects.nonNull(taskId)) {
            context.setTaskId(Long.valueOf(String.valueOf(taskId)));
        }
        Object uniqueKey = redisClientUtils.hmget(PressureStartCache.getSceneResourceKey(sceneId), PressureStartCache.UNIQUE_KEY);
        if (Objects.nonNull(uniqueKey)) {
            context.setUniqueKey(String.valueOf(uniqueKey));
        }
        callStartFailClear(context);
    }

    private void callStartFailClear(StartConditionCheckerContext context) {
        redisClientUtils.unlock(PressureStartCache.getSceneResourceLockingKey(context.getSceneId()), context.getUniqueKey());
        Long taskId = context.getTaskId();
        if (Objects.nonNull(taskId)) {
            PressureTaskEntity entity = new PressureTaskEntity();
            entity.setId(context.getTaskId());
            entity.setExceptionMsg(context.getMessage());
            entity.setIsDeleted(1);
            entity.setGmtUpdate(new Date());
            pressureTaskDAO.updateById(entity);
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
