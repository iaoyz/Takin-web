package io.shulie.takin.web.biz.service.agentcommand.impl;

import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;

import io.shulie.takin.web.biz.pojo.bo.agentupgradeonline.AgentHeartbeatBO;
import io.shulie.takin.web.biz.service.DistributedLock;
import io.shulie.takin.web.biz.service.agentcommand.AgentCommandSupport;
import io.shulie.takin.web.biz.service.agentupgradeonline.AgentReportService;
import io.shulie.takin.web.biz.service.agentupgradeonline.ApplicationPluginUpgradeService;
import io.shulie.takin.web.common.enums.agentupgradeonline.AgentCommandEnum;
import io.shulie.takin.web.common.enums.agentupgradeonline.AgentUpgradeEnum;
import io.shulie.takin.web.common.enums.application.ApplicationAgentPathValidStatusEnum;
import io.shulie.takin.web.common.enums.fastagentaccess.AgentReportStatusEnum;
import io.shulie.takin.web.data.result.application.AgentReportDetailResult;
import io.shulie.takin.web.data.result.application.ApplicationPluginDownloadPathDetailResult;
import io.shulie.takin.web.data.result.application.ApplicationPluginUpgradeDetailResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @Description 处理应用升级状态
 * @Author ocean_wll
 * @Date 2021/11/18 3:25 下午
 */
@Component
public class AgentUpgradeProcessor extends AgentCommandSupport {

    private static final String STRING_FINISH = "finish";

    private static final String STRING_UPGRADE_BATCH = "upgradeBatch";

    private static final String STRING_ERROR_INFO = "errorInfo";

    private static final String UPGRADE_LOCK_TEMPLATE = "AGENT_UPGRADE:%s-%s";

    @Resource
    private ApplicationPluginUpgradeService applicationPluginUpgradeService;

    @Resource
    private AgentReportService agentReportService;

    @Resource
    private DistributedLock distributedLock;

    @Override
    public void process0(AgentHeartbeatBO agentHeartbeatBO, JSONObject commandParam) {
        Boolean finish = commandParam.getBoolean(STRING_FINISH);
        String upgradeBatch = commandParam.getString(STRING_UPGRADE_BATCH);
        if (finish == null || StringUtils.isEmpty(upgradeBatch)) {
            return;
        }
        AgentUpgradeEnum upgradeEnum = finish ? AgentUpgradeEnum.UPGRADE_SUCCESS : AgentUpgradeEnum.UPGRADE_FILE;
        applicationPluginUpgradeService.changeUpgradeStatus(agentHeartbeatBO.getApplicationId(), upgradeBatch,
            upgradeEnum.getVal(), commandParam.getString(STRING_ERROR_INFO));
    }

    @Override
    public boolean needDealHeartbeat(AgentHeartbeatBO agentHeartbeatBO) {
        // 如果上报上来的当前批次号是-1则不处理
        if (DEFAULT_UPGRADE_BATH.equals(agentHeartbeatBO.getCurUpgradeBatch())) {
            return false;
        }
        // 查询当前应用当前未升级成功的最新批次号
        ApplicationPluginUpgradeDetailResult upgradeDetailResult
            = applicationPluginUpgradeService.queryLatestUpgradeByAppIdAndStatus(agentHeartbeatBO.getApplicationId(),
            AgentUpgradeEnum.NOT_UPGRADE.getVal());

        if (upgradeDetailResult == null) {
            return false;
        }

        String dealUpgradeAgentId = null;
        // 查询处理应用升级的agent节点是否存活
        if (!StringUtils.isEmpty(upgradeDetailResult.getUpgradeAgentId())) {
            AgentReportDetailResult detailResult = agentReportService.queryAgentReportDetail(
                agentHeartbeatBO.getApplicationId(), upgradeDetailResult.getUpgradeAgentId());
            if (detailResult != null) {
                dealUpgradeAgentId = upgradeDetailResult.getUpgradeAgentId();
            }
        }

        // 如果当前agentId与升级单中需要升级的agentId一致则处理
        if (agentHeartbeatBO.getAgentId().equals(dealUpgradeAgentId)) {
            return true;
        }

        // 所有agent对当前升级单进行抢锁
        String lockKey = String.format(UPGRADE_LOCK_TEMPLATE, agentHeartbeatBO.getApplicationId(),
            agentHeartbeatBO.getAgentId());
        if (distributedLock.tryLock(lockKey, 0L, 60L, TimeUnit.SECONDS)) {
            try {
                // 抢锁成功更新升级单数据、并且返回true，然后释放锁
                agentReportService.updateAgentIdById(upgradeDetailResult.getId(), agentHeartbeatBO.getAgentId());
            } finally {
                distributedLock.unLockSafely(lockKey);
            }
            return true;
        }
        return false;
    }

    @Override
    public Object dealHeartbeat0(AgentHeartbeatBO agentHeartbeatBO) {
        // 查询当前应用当前未升级成功的最新批次号
        ApplicationPluginUpgradeDetailResult upgradeDetailResult
            = applicationPluginUpgradeService.queryLatestUpgradeByAppIdAndStatus(agentHeartbeatBO.getApplicationId(),
            AgentUpgradeEnum.NOT_UPGRADE.getVal());

        ApplicationPluginDownloadPathDetailResult downloadResult = getPluginDownloadPath(
            ApplicationAgentPathValidStatusEnum.CHECK_PASSED);

        UpgradeResult upgradeResult = new UpgradeResult(downloadResult);
        upgradeResult.setUpgradeBath(upgradeDetailResult.getUpgradeBatch());
        upgradeResult.setDownloadPath(upgradeDetailResult.getDownloadPath());
        return upgradeResult;
    }

    @Override
    public AgentReportStatusEnum workStatus() {
        return AgentReportStatusEnum.RUNNING;
    }

    @Override
    public AgentCommandEnum getCommand() {
        return AgentCommandEnum.REPORT_UPGRADE_RESULT;
    }

    static class UpgradeResult extends PluginDownloadPathResult {

        /**
         * 升级批次号
         */
        private String upgradeBath;

        /**
         * 下载地址
         */
        private String downloadPath;

        public UpgradeResult() {
            super(null);
        }

        public UpgradeResult(ApplicationPluginDownloadPathDetailResult detailResult) {
            super(detailResult);
        }

        public String getUpgradeBath() {
            return upgradeBath;
        }

        public void setUpgradeBath(String upgradeBath) {
            this.upgradeBath = upgradeBath;
        }

        public String getDownloadPath() {
            return downloadPath;
        }

        public void setDownloadPath(String downloadPath) {
            this.downloadPath = downloadPath;
        }
    }
}
