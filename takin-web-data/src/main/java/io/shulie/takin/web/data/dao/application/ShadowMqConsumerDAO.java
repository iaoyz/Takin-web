package io.shulie.takin.web.data.dao.application;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import io.shulie.takin.web.data.model.mysql.ShadowMqConsumerEntity;

/**
 * 白名单配置 dao 层
 *
 * @author liuchuan
 * @date 2021/4/8 10:50 上午
 */
public interface ShadowMqConsumerDAO extends IService<ShadowMqConsumerEntity> {

    /**
     * 通过应用id, 获得影子消费者配置
     *
     * @param applicationId 应用id
     * @return 影子消费者配置列表
     */
    List<ShadowMqConsumerEntity> listByApplicationId(Long applicationId);

    /**
     * 更新应用名
     * @param applicationId
     * @param appName
     */
    void updateAppName(Long applicationId,String appName);

    /**
     * 插入更新
     * @param updateEntity
     */
    void importUpdateData(ShadowMqConsumerEntity updateEntity);
}
