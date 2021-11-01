package io.shulie.takin.web.biz.init.sync;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import io.shulie.takin.web.ext.util.WebPluginUtils;
import org.apache.commons.collections4.CollectionUtils;
import io.shulie.takin.web.biz.service.ApplicationService;
import com.pamirs.takin.entity.domain.entity.TApplicationMnt;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author shiyajian
 * create: 2020-09-17
 */
@Component
@Slf4j
public class ConfigSynchronizer {

    @Autowired
    private ConfigSyncService configSyncService;

    @Autowired
    private ApplicationService applicationService;

    /**
     * 项目启动后，去更新下配置
     */
    public void initSyncAgentConfig() {
        // 企业版不执行
        if (WebPluginUtils.checkUserPlugin()) {
            return;
        }
        log.info("项目启动，重新同步信息去配置中心");
        List<TApplicationMnt> applications = applicationService.getAllApplications();
        if (CollectionUtils.isEmpty(applications)) {
            return;
        } else {
            for (TApplicationMnt application : applications) {
                configSyncService.syncGuard(WebPluginUtils.traceTenantCommonExt(), application.getApplicationId(),
                    application.getApplicationName());
                sleep();
                configSyncService.syncShadowDB(WebPluginUtils.traceTenantCommonExt(), application.getApplicationId(),
                    application.getApplicationName());
                sleep();
                configSyncService.syncAllowList(WebPluginUtils.traceTenantCommonExt(), application.getApplicationId(),
                    application.getApplicationName());
                sleep();
                configSyncService.syncShadowJob(WebPluginUtils.traceTenantCommonExt(), application.getApplicationId(),
                    application.getApplicationName());
                sleep();
                configSyncService.syncShadowConsumer(WebPluginUtils.traceTenantCommonExt(), application.getApplicationId(),
                    application.getApplicationName());
            }
        }
        configSyncService.syncClusterTestSwitch(WebPluginUtils.traceTenantCommonExt());
        sleep();
        configSyncService.syncAllowListSwitch(WebPluginUtils.traceTenantCommonExt());
        sleep();
        configSyncService.syncBlockList(WebPluginUtils.traceTenantCommonExt());
        log.info("所有配置同步到配置中心成功");
    }

    /**
     * 给zk写的压力不要太大，慢慢写
     */
    private void sleep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            //ignore
        }
    }
}
