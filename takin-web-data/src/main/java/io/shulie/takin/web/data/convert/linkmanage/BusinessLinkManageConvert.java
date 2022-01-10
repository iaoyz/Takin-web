package io.shulie.takin.web.data.convert.linkmanage;

import java.util.List;

import io.shulie.takin.web.data.model.mysql.BusinessLinkManageTableEntity;
import io.shulie.takin.web.data.model.mysql.SceneEntity;
import io.shulie.takin.web.data.model.mysql.SceneLinkRelateEntity;
import io.shulie.takin.web.data.param.scene.SceneLinkRelateSaveParam;
import io.shulie.takin.web.data.result.linkmange.BusinessLinkResult;
import io.shulie.takin.web.data.result.linkmange.SceneResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author 无涯
 * @date 2020/12/30 10:52 上午
 */
@Mapper
public interface BusinessLinkManageConvert {
    BusinessLinkManageConvert INSTANCE = Mappers.getMapper(BusinessLinkManageConvert.class);

    /**
     * 转换
     * @param entities
     * @return
     */
    List<BusinessLinkResult> ofList(List<BusinessLinkManageTableEntity> entities);

    List<SceneLinkRelateEntity> ofSceneLinkRelateSaveParams(List<SceneLinkRelateSaveParam> saveParams);

    List<SceneResult> ofSceneEntityList(List<SceneEntity> records);
}
