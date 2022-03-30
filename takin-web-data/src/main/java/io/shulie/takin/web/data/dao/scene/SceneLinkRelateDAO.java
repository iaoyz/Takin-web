package io.shulie.takin.web.data.dao.scene;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.pamirs.takin.entity.domain.vo.linkmanage.BusinessFlowTree;
import io.shulie.takin.web.data.model.mysql.SceneLinkRelateEntity;
import io.shulie.takin.web.data.param.scene.SceneLinkRelateParam;
import io.shulie.takin.web.data.param.scene.SceneLinkRelateQuery;
import io.shulie.takin.web.data.param.scene.SceneLinkRelateSaveParam;
import io.shulie.takin.web.data.result.scene.SceneLinkRelateResult;

/**
 * @author 无涯
 * @date 2021/5/28 5:38 下午
 */
public interface SceneLinkRelateDAO extends IService<SceneLinkRelateEntity> {

    /**
     * 批量增加
     *
     * @param ofSceneLinkRelateResults -
     */
    void batchInsert(List<SceneLinkRelateSaveParam> ofSceneLinkRelateResults);

    /**
     * 根据流程删除删除
     *
     * @param sceneId
     */
    void deleteBySceneId(String sceneId);

    /**
     * 根据sceneId 查询链路
     *
     * @param sceneId
     * @return
     */
    List<SceneLinkRelateResult> selectBySceneId(Long sceneId);

    /**
     * 返回业务活动个数
     *
     * @param sceneId
     * @return
     */
    Long countBySceneId(Long sceneId);

    /**
     * 返回链路个数
     *
     * @param techLinkIds
     * @return
     */
    int countByTechLinkIds(List<String> techLinkIds);

    /**
     * 根据业务活动获取个数
     *
     * @param businessLinkId
     * @return
     */
    long countByBusinessLinkId(Long businessLinkId);

    /**
     * 获取关联
     *
     * @param param
     * @return
     */
    List<SceneLinkRelateResult> getList(SceneLinkRelateParam param);

    /**
     * 根据业务流程ids, 获得关联业务活动ids
     *
     * @param businessFlowIds 业务流程ids
     * @return 业务活动ids
     */
    List<Long> listBusinessLinkIdsByBusinessFlowIds(List<Long> businessFlowIds);

    /**
     * 业务id, frontUuidKey, flowId
     *
     * @param flowId   业务流程id
     * @param tenantId 租户id
     * @param envCode  环境表示
     * @return tree列表
     */
    List<BusinessFlowTree> listRecursion(Long flowId, Long tenantId, String envCode);

    /**
     * 查询接口
     */
    List<SceneLinkRelateResult> query(SceneLinkRelateQuery query);

    /**
     * 通过scriptIdentification查找匹配的记录
     */
    List<SceneLinkRelateResult> getByIdentification(String scriptIdentification);

    void deleteByIds(List<Long> oldIds);

}
