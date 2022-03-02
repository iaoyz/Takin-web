package io.shulie.takin.web.biz.service.scenemanage;

import java.util.List;

import com.pamirs.takin.entity.domain.dto.scenemanage.SceneManageWrapperDTO;
import com.pamirs.takin.entity.domain.dto.scenemanage.ScriptCheckDTO;
import com.pamirs.takin.entity.domain.vo.scenemanage.SceneManageQueryVO;
import com.pamirs.takin.entity.domain.vo.scenemanage.SceneManageWrapperVO;
import io.shulie.takin.cloud.sdk.model.request.scenemanage.SceneManageDeleteReq;
import io.shulie.takin.cloud.sdk.model.request.scenemanage.SceneManageQueryByIdsReq;
import io.shulie.takin.cloud.sdk.model.request.scenemanage.SceneManageWrapperReq;
import io.shulie.takin.cloud.sdk.model.request.scenemanage.SceneScriptRefOpen;
import io.shulie.takin.cloud.sdk.model.response.scenemanage.SceneManageWrapperResp;
import io.shulie.takin.cloud.sdk.model.response.strategy.StrategyResp;
import io.shulie.takin.common.beans.response.ResponseResult;
import io.shulie.takin.web.biz.pojo.input.scenemanage.SceneManageListOutput;
import io.shulie.takin.web.biz.pojo.output.scene.SceneListForSelectOutput;
import io.shulie.takin.web.biz.pojo.output.scene.SceneReportListOutput;
import io.shulie.takin.web.biz.pojo.request.scene.ListSceneForSelectRequest;
import io.shulie.takin.web.biz.pojo.request.scene.ListSceneReportRequest;
import io.shulie.takin.web.biz.pojo.response.scenemanage.SceneDetailResponse;
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

    List<String> getAppIdsByBusinessActivityId(Long businessActivityId);

    ResponseResult<List<SceneManageListOutput>> getPageList(SceneManageQueryVO vo);

    ResponseResult<StrategyResp> getIpNum(Integer concurrenceNum, Integer tpsNum);

    ResponseResult<List<SceneManageWrapperResp>> getByIds(SceneManageQueryByIdsReq req);

    void checkParam(SceneManageWrapperVO sceneVO);

    WebResponse<List<SceneScriptRefOpen>> buildSceneForFlowVerify(SceneManageWrapperVO vo, SceneManageWrapperReq req, Long userId);

    ResponseResult<List<ScenePositionPointResponse>> getPositionPoint(Long sceneId);

    /**
     * 根据场景id获得详情
     *
     * @param sceneId 场景id
     * @return 场景详情
     */
    SceneDetailResponse getById(Long sceneId);

    /**
     * 创建排除应用
     *
     * @param sceneId                场景id
     * @param excludedApplicationIds 排除应用ids
     */
    void createSceneExcludedApplication(Long sceneId, List<Long> excludedApplicationIds);

    /**
     * 下拉框的压测场景列表, 暂时只查询压测中状态的
     *
     * @param request 请求入参
     * @return 压测场景列表
     */
    List<SceneListForSelectOutput> listForSelect(ListSceneForSelectRequest request);

    /**
     * 通过场景id, 查询对应的正在运行的报告
     *
     * @param request 请求入参
     * @return 报告列表
     */
    List<SceneReportListOutput> listReportBySceneIds(ListSceneReportRequest request);

    /**
     * 报告排名
     *
     * @param request 请求入参
     * @return 报告排名
     */
    List<SceneReportListOutput> rankReport(ListSceneReportRequest request);

}
