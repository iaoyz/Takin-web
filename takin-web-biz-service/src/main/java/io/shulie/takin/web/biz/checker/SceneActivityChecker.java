package io.shulie.takin.web.biz.checker;

import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import com.pamirs.takin.entity.domain.dto.scenemanage.SceneBusinessActivityRefDTO;
import com.pamirs.takin.entity.domain.dto.scenemanage.SceneManageWrapperDTO;
import io.shulie.takin.adapter.api.model.request.scenemanage.SceneManageIdReq;
import io.shulie.takin.adapter.api.model.response.scenemanage.SceneManageWrapperResp;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.utils.json.JsonHelper;
import io.shulie.takin.web.common.exception.TakinWebException;
import io.shulie.takin.web.common.exception.TakinWebExceptionEnum;
import io.shulie.takin.web.diff.api.scenemanage.SceneManageApi;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SceneActivityChecker implements WebStartConditionChecker {

    @Resource
    private SceneManageApi sceneManageApi;

    @Override
    public CheckResult check(WebConditionCheckerContext context) {
        SceneManageIdReq req = new SceneManageIdReq();
        Long sceneId = context.getSceneId();
        req.setId(sceneId);
        try {
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
            doCheck(context);
            return CheckResult.success(type());
        } catch (Exception e) {
            return CheckResult.fail(type(), e.getMessage());
        }
    }

    private void doCheck(WebConditionCheckerContext context) {
        SceneManageWrapperDTO sceneData = context.getSceneData();
        //检查场景是否存可以开启启压测
        List<SceneBusinessActivityRefDTO> sceneBusinessActivityList = sceneData.getBusinessActivityConfig();
        if (CollectionUtils.isEmpty(sceneBusinessActivityList)) {
            log.error("[{}]场景没有配置业务活动", sceneData.getId());
            throw new TakinWebException(TakinWebExceptionEnum.SCENE_START_VALIDATE_ERROR,
                "启动压测失败，没有配置业务活动，场景ID为" + sceneData.getId());
        }
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
        return "activity";
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
