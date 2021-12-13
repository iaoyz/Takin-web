package io.shulie.takin.web.biz.service.scenemanage;

import java.util.List;

import com.pamirs.takin.entity.domain.dto.scenemanage.SceneManageWrapperDTO;
import com.pamirs.takin.entity.domain.dto.scenemanage.ScriptCheckDTO;
import io.shulie.takin.cloud.sdk.model.request.scenemanage.SceneManageDeleteReq;
import io.shulie.takin.cloud.sdk.model.request.scenemanage.SceneScriptRefOpen;
import com.pamirs.takin.entity.domain.vo.scenemanage.SceneManageQueryVO;
import com.pamirs.takin.entity.domain.vo.scenemanage.SceneManageWrapperVO;
import io.shulie.takin.cloud.sdk.model.request.scenemanage.SceneManageQueryByIdsReq;
import io.shulie.takin.cloud.sdk.model.request.scenemanage.SceneManageWrapperReq;
import io.shulie.takin.cloud.sdk.model.response.scenemanage.SceneManageWrapperResp;
import io.shulie.takin.cloud.sdk.model.response.strategy.StrategyResp;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.web.biz.pojo.input.scenemanage.SceneManageListOutput;
import io.shulie.takin.web.biz.pojo.response.scenemanage.ScenePositionPointResponse;
import io.shulie.takin.web.common.domain.WebResponse;

/**
 * @author qianshui
 * @date 2020/4/17 下午3:31
 */
public interface SceneManageService {

    /**
     * 添加场景
     *
     * @param vo 请求参数
     * @return 添加结果
     */
    WebResponse<List<SceneScriptRefOpen>> addScene(SceneManageWrapperVO vo);

    /**
     * 更新压测场景
     *
     * @param vo 更新所需的参数
     * @return 更新结果
     */
    WebResponse<String> updateScene(SceneManageWrapperVO vo);

    String deleteScene(SceneManageDeleteReq vo);

    ResponseResult<SceneManageWrapperResp> detailScene(Long id);

    ScriptCheckDTO checkBusinessActivityAndScript(SceneManageWrapperDTO sceneData);

    ResponseResult<List<SceneManageListOutput>> getPageList(SceneManageQueryVO vo);

    ResponseResult<StrategyResp> getIpNum(Integer concurrenceNum, Integer tpsNum);

    ResponseResult<List<SceneManageWrapperResp>> getByIds(SceneManageQueryByIdsReq req);

    void checkParam(SceneManageWrapperVO sceneVO);

    WebResponse<List<SceneScriptRefOpen>> buildSceneForFlowVerify(SceneManageWrapperVO vo, SceneManageWrapperReq req, Long userId);

    ResponseResult<List<ScenePositionPointResponse>> getPositionPoint(Long sceneId);
}
