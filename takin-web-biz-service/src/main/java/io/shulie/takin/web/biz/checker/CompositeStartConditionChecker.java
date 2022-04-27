package io.shulie.takin.web.biz.checker;

import java.util.ArrayList;
import java.util.List;

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

    public static final String CHECK_FAIL_EVENT = "check_failed";
    public static final String CHECK_SUCCESS_EVENT = "check_success";
    public static final String LACK_POD_RESOURCE = "lack_pod_resource";

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
            CheckResult checkResult = doCheck(context, checker);
            resultList.add(checkResult);
            if (checkResult.getStatus().equals(CheckStatus.FAIL.ordinal())) {
                break;
            }
        }
        return resultList;
    }

    private CheckResult doCheck(StartConditionCheckerContext context, StartConditionChecker checker) {
        String checkResultKey = CheckResult.getCheckResultKey(context.getSceneId());
        String result = (String)redisClientUtils.hmget(checkResultKey, checker.type());
        if (StringUtils.isNotBlank(result)) {
            return JsonHelper.json2Bean(result, CheckResult.class);
        }
        CheckResult checkResult = checker.check(context);
        if (checkResult.getStatus().equals(CheckStatus.SUCCESS.ordinal())) {
            redisClientUtils.hmset(checkResultKey, checker.type(), JsonHelper.bean2Json(result));
        } else if (checkResult.getStatus().equals(CheckStatus.FAIL.ordinal())) {
            callStartFailClear(context);
        }
        return checkResult;
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
        }
        context.setInitTaskAndReport(true);
    }

    @IntrestFor(event = CHECK_FAIL_EVENT)
    public void callStartFailClear(Event event) {
        ResourceContext ext = (ResourceContext)event.getExt();
        StartConditionCheckerContext context = new StartConditionCheckerContext();
        context.setSceneId(ext.getSceneId());
        context.setTaskId(ext.getPressureTaskId());
        context.setReportId(ext.getReportId());
        callStartFailClear(context);
    }

    private void callStartFailClear(StartConditionCheckerContext context) {
        redisClientUtils.delete(CheckResult.getCheckResultKey(context.getSceneId()));
        pressureTaskDAO.deleteById(context.getTaskId());
        TaskResult result = new TaskResult();
        result.setSceneId(context.getSceneId());
        result.setTaskId(context.getReportId());
        result.setStatus(TaskStatusEnum.FAILED);
        cloudSceneTaskService.handleSceneTaskEvent(result);
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
