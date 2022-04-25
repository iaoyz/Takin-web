package io.shulie.takin.web.biz.checker;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.google.common.collect.Lists;
import com.pamirs.takin.common.constant.ConfigConstants;
import com.pamirs.takin.entity.domain.dto.scenemanage.SceneBusinessActivityRefDTO;
import com.pamirs.takin.entity.domain.dto.scenemanage.SceneManageWrapperDTO;
import com.pamirs.takin.entity.domain.entity.TBaseConfig;
import io.shulie.takin.adapter.api.model.request.scenemanage.SceneManageIdReq;
import io.shulie.takin.adapter.api.model.response.scenemanage.SceneManageWrapperResp;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.utils.json.JsonHelper;
import io.shulie.takin.web.biz.service.BaseConfigService;
import io.shulie.takin.web.biz.service.scenemanage.SceneTaskService;
import io.shulie.takin.web.common.enums.config.ConfigServerKeyEnum;
import io.shulie.takin.web.common.exception.TakinWebException;
import io.shulie.takin.web.common.exception.TakinWebExceptionEnum;
import io.shulie.takin.web.common.vo.scene.BaffleAppVO;
import io.shulie.takin.web.data.dao.SceneExcludedApplicationDAO;
import io.shulie.takin.web.data.dao.application.ApplicationDAO;
import io.shulie.takin.web.data.result.application.ApplicationDetailResult;
import io.shulie.takin.web.data.util.ConfigServerHelper;
import io.shulie.takin.web.diff.api.scenemanage.SceneManageApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationChecker implements WebStartConditionChecker {

    @Resource
    private ApplicationDAO applicationDAO;

    @Resource
    private BaseConfigService baseConfigService;

    @Resource
    private SceneExcludedApplicationDAO sceneExcludedApplicationDAO;

    @Resource
    private SceneTaskService sceneTaskService;

    @Resource
    private SceneManageApi sceneManageApi;

    @Override
    public CheckResult check(WebConditionCheckerContext context) {
        try {
            fileContext(context);
            doCheck(context);
            return CheckResult.success(type());
        } catch (Exception e) {
            return CheckResult.fail(type(), e.getMessage());
        }
    }

    private void fileContext(WebConditionCheckerContext context) {
        SceneManageIdReq req = new SceneManageIdReq();
        Long sceneId = context.getSceneId();
        req.setId(sceneId);
        ResponseResult<SceneManageWrapperResp> resp = sceneManageApi.getSceneDetail(req);
        if (!resp.getSuccess()) {
            ResponseResult.ErrorInfo errorInfo = resp.getError();
            String errorMsg = Objects.isNull(errorInfo) ? "" : errorInfo.getMsg();
            log.error("takin-cloud查询场景信息返回错误，id={},错误信息：{}", sceneId, errorMsg);
            throw new TakinWebException(TakinWebExceptionEnum.SCENE_THIRD_PARTY_ERROR,
                getCloudMessage(errorInfo.getCode(), errorInfo.getMsg()));
        }
        String jsonString = JsonHelper.bean2Json(resp.getData());
        SceneManageWrapperDTO sceneData = JsonHelper.json2Bean(jsonString, SceneManageWrapperDTO.class);
        if (null == sceneData) {
            log.error("takin-cloud查询场景信息返回错误，id={},错误信息：{}", sceneId,
                "sceneData is null! jsonString=" + jsonString);
            throw new TakinWebException(TakinWebExceptionEnum.SCENE_THIRD_PARTY_ERROR,
                "场景，id=" + sceneId + " 信息为空");
        }
        context.setSceneData(sceneData);
    }

    private void doCheck(WebConditionCheckerContext context) {
        this.checkBusinessActivity(context);
    }

    /**
     * 检查业务活动相关
     *
     * @param context 校验上下文
     */
    private void checkBusinessActivity(WebConditionCheckerContext context) {
        SceneManageWrapperDTO sceneData = context.getSceneData();
        //检查场景是否存可以开启启压测
        List<SceneBusinessActivityRefDTO> activityConfig = sceneData.getBusinessActivityConfig();
        if (CollectionUtils.isEmpty(activityConfig)) {
            log.error("[{}]场景没有配置业务活动", sceneData.getId());
            throw new TakinWebException(TakinWebExceptionEnum.SCENE_START_VALIDATE_ERROR,
                "启动压测失败，没有配置业务活动，场景ID为" + sceneData.getId());
        }
        //需求要求，业务验证异常需要详细输出
        StringBuilder errorMsg = new StringBuilder();

        // 获得场景关联的排除应用ids
        List<Long> applicationIds = this.listApplicationIdsFromScene(sceneData.getId(), activityConfig);

        // 应用相关检查
        boolean checkApplication = ConfigServerHelper.getBooleanValueByKey(
            ConfigServerKeyEnum.TAKIN_START_TASK_CHECK_APPLICATION);
        if (!CollectionUtils.isEmpty(applicationIds) && checkApplication) {
            List<ApplicationDetailResult> applicationMntList = applicationDAO.getApplicationByIds(applicationIds);
            // todo 临时方案，过滤挡板应用
            TBaseConfig config = baseConfigService.queryByConfigCode(ConfigConstants.SCENE_BAFFLE_APP_CONFIG);
            if (config != null && StringUtils.isNotBlank(config.getConfigValue())) {
                try {
                    List<BaffleAppVO> baffleAppVos = JsonHelper.json2List(config.getConfigValue(), BaffleAppVO.class);
                    List<String> appNames = Optional.of(baffleAppVos.stream()
                            .filter(appVO -> sceneData.getId() != null && sceneData.getId().equals(appVO.getSceneId()))
                            .collect(Collectors.toList()))
                        .map(t -> t.get(0)).map(BaffleAppVO::getAppName).orElse(Lists.newArrayList());
                    List<Long> appIds = Lists.newArrayList();

                    List<ApplicationDetailResult> tempApps = applicationMntList.stream().filter(app -> {
                        if (appNames.contains(app.getApplicationName())) {
                            // 用于过滤应用id
                            appIds.add(app.getApplicationId());
                            return false;
                        }
                        return true;
                    }).collect(Collectors.toList());
                    List<Long> tempAppIds = applicationIds.stream().filter(id -> !appIds.contains(id))
                        .collect(Collectors.toList());
                    applicationMntList = tempApps;
                    applicationIds = tempAppIds;
                } catch (Exception e) {
                    log.error("场景挡板配置转化异常：配置项：{},配置项内容:{}", ConfigConstants.SCENE_BAFFLE_APP_CONFIG,
                        config.getConfigValue());
                }
            }
            if (CollectionUtils.isEmpty(applicationMntList) || applicationMntList.size() != applicationIds.size()) {
                log.error("启动压测失败, 没有找到关联的应用信息，场景ID：{}", sceneData.getId());
                throw new TakinWebException(TakinWebExceptionEnum.SCENE_START_VALIDATE_ERROR,
                    "启动压测失败, 没有找到关联的应用信息，场景ID：" + sceneData.getId());
            }

            // 检查应用相关
            errorMsg.append(sceneTaskService.checkApplicationCorrelation(applicationMntList));
        }

        if (errorMsg.length() > 0) {
            throw new TakinWebException(TakinWebExceptionEnum.SCENE_START_VALIDATE_ERROR, errorMsg.toString());
        }
    }

    /**
     * 获得场景
     *
     * @param sceneId 场景id
     * @param refList 场景关联业务活动列表
     * @return 应用ids
     */
    private List<Long> listApplicationIdsFromScene(Long sceneId, List<SceneBusinessActivityRefDTO> refList) {
        // 从活动中提取应用ID，去除重复ID
        List<Long> applicationIds = refList.stream()
            .map(SceneBusinessActivityRefDTO::getApplicationIds).filter(StringUtils::isNotEmpty)
            .flatMap(appIds -> Arrays.stream(appIds.split(","))
                .map(Long::valueOf)).filter(data -> data > 0L).distinct().collect(Collectors.toList());
        if (applicationIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 场景对应的排除的id
        List<Long> excludedApplicationIds = sceneExcludedApplicationDAO.listApplicationIdsBySceneId(sceneId);
        if (excludedApplicationIds.isEmpty()) {
            return applicationIds;
        }

        // 排除掉排除的id
        applicationIds.removeAll(excludedApplicationIds);
        return applicationIds;
    }

    /**
     * 返回cloud 数据
     *
     * @param code     错误编码
     * @param errorMsg 错误信息
     * @return 拼接后的错误信息
     */
    private String getCloudMessage(String code, String errorMsg) {
        return String.format("takin-cloud启动场景失败，异常代码【%s】,异常原因【%s】", code, errorMsg);
    }

    @Override
    public String type() {
        return "application";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
