package io.shulie.takin.cloud.biz.checker;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;

import com.google.common.collect.Lists;
import com.pamirs.takin.cloud.entity.domain.entity.report.Report;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.service.scene.CloudSceneManageService;
import io.shulie.takin.cloud.biz.utils.DataUtils;
import io.shulie.takin.cloud.common.enums.PressureModeEnum;
import io.shulie.takin.cloud.common.enums.ThreadGroupTypeEnum;
import io.shulie.takin.cloud.common.enums.TimeUnitEnum;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.cloud.common.utils.CloudPluginUtils;
import io.shulie.takin.cloud.common.utils.JsonUtil;
import io.shulie.takin.cloud.data.dao.report.ReportDao;
import io.shulie.takin.cloud.data.param.report.ReportUpdateParam;
import io.shulie.takin.cloud.data.result.report.ReportResult;
import io.shulie.takin.cloud.ext.api.AssetExtApi;
import io.shulie.takin.cloud.ext.content.asset.AccountInfoExt;
import io.shulie.takin.cloud.ext.content.asset.AssetBillExt;
import io.shulie.takin.cloud.ext.content.asset.AssetInvoiceExt;
import io.shulie.takin.cloud.ext.content.enums.AssetTypeEnum;
import io.shulie.takin.cloud.ext.content.response.Response;
import io.shulie.takin.plugin.framework.core.PluginManager;
import io.shulie.takin.web.biz.checker.WebStartConditionChecker.CheckResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FlowConditionChecker implements CloudStartConditionChecker {

    @Resource
    private CloudSceneManageService cloudSceneManageService;

    @Resource
    private PluginManager pluginManager;

    @Resource
    private ReportDao reportDao;

    @Resource
    @Qualifier("redisTemplate")
    private RedisTemplate redisTemplate;

    @Override
    public CheckResult check(CloudConditionCheckerContext context) throws TakinCloudException {
        try {
            fillContext(context);
            flowCheck(context);
            return CheckResult.success(type());
        } catch (Exception e) {
            return CheckResult.fail(type(), e.getMessage());
        }
    }

    private void flowCheck(CloudConditionCheckerContext context) {
        SceneManageWrapperOutput sceneData = context.getSceneData();
        if (null == sceneData.getTenantId()) {
            throw new TakinCloudException(TakinCloudExceptionEnum.TASK_START_VERIFY_ERROR, "场景没有绑定客户信息");
        }
        SceneTaskStartInput input = context.getInput();
        AssetExtApi assetExtApi = pluginManager.getExtension(AssetExtApi.class);
        if (assetExtApi != null) {
            AccountInfoExt account = assetExtApi.queryAccount(sceneData.getTenantId(), input.getOperateId());
            if (null == account || account.getBalance().compareTo(sceneData.getEstimateFlow()) < 0) {
                throw new TakinCloudException(TakinCloudExceptionEnum.TASK_START_VERIFY_ERROR, "压测流量不足！");
            }
            ReportResult report = reportDao.selectById(context.getReportId());
            //流量冻结
            frozenAccountFlow(input, report, sceneData);
        }
    }

    /**
     * 冻结流量
     *
     * @param input     {@link SceneTaskStartInput}
     * @param report    {@link Report}
     * @param sceneData {@link SceneManageWrapperOutput}
     */
    private void frozenAccountFlow(SceneTaskStartInput input, ReportResult report, SceneManageWrapperOutput sceneData) {
        AssetExtApi assetExtApi = pluginManager.getExtension(AssetExtApi.class);
        if (assetExtApi != null) {
            //得到数据来源ID
            Long resourceId = input.getResourceId();
            if (AssetTypeEnum.PRESS_REPORT.getCode().equals(input.getAssetType())) {
                resourceId = report.getId();
            }
            AssetInvoiceExt<List<AssetBillExt>> invoice = new AssetInvoiceExt<>();
            invoice.setSceneId(sceneData.getId());
            invoice.setTaskId(report.getId());
            invoice.setResourceId(resourceId);
            invoice.setResourceType(input.getAssetType());
            invoice.setResourceName(input.getResourceName());
            invoice.setOperateId(input.getOperateId());
            invoice.setOperateName(input.getOperateName());
            invoice.setCustomerId(report.getTenantId());
            AssetBillExt.TimeBean pressureTestTime = new AssetBillExt.TimeBean(sceneData.getTotalTestTime(),
                TimeUnitEnum.SECOND.getValue());
            String testTimeCost = DataUtils.formatTime(sceneData.getTotalTestTime());
            if (MapUtils.isNotEmpty(sceneData.getThreadGroupConfigMap())) {
                List<AssetBillExt> bills = sceneData.getThreadGroupConfigMap().values().stream()
                    .filter(Objects::nonNull)
                    .map(config -> {
                        AssetBillExt bill = new AssetBillExt();
                        bill.setIpNum(sceneData.getIpNum());
                        bill.setConcurrenceNum(config.getThreadNum());
                        bill.setPressureTestTime(pressureTestTime);
                        bill.setPressureMode(config.getMode());
                        bill.setPressureScene(sceneData.getPressureType());
                        bill.setPressureType(config.getType());
                        if (null != config.getRampUp()) {
                            AssetBillExt.TimeBean rampUp = new AssetBillExt.TimeBean(config.getRampUp().longValue(),
                                config.getRampUpUnit());
                            bill.setIncreasingTime(rampUp);
                        }
                        bill.setStep(config.getSteps());
                        bill.setPressureTestTimeCost(testTimeCost);
                        return bill;
                    })
                    .collect(Collectors.toList());
                invoice.setData(bills);
            } else if (null != sceneData.getConcurrenceNum()) {
                AssetBillExt bill = new AssetBillExt();
                bill.setIpNum(sceneData.getIpNum());
                bill.setConcurrenceNum(sceneData.getConcurrenceNum());
                bill.setPressureTestTime(pressureTestTime);
                bill.setPressureMode(PressureModeEnum.FIXED.getCode());
                bill.setPressureScene(sceneData.getPressureType());
                bill.setPressureType(ThreadGroupTypeEnum.CONCURRENCY.getCode());
                bill.setPressureTestTimeCost(DataUtils.formatTime(sceneData.getTotalTestTime()));
                invoice.setData(Lists.newArrayList(bill));
            }
            try {
                Response<String> res = assetExtApi.lock(invoice);
                if (null != res && res.isSuccess() && StringUtils.isNotBlank(res.getData())) {
                    ReportUpdateParam rp = new ReportUpdateParam();
                    rp.setId(report.getId());
                    JSONObject features = JsonUtil.parse(report.getFeatures());
                    if (null == features) {
                        features = new JSONObject();
                    }
                    features.put("lockId", res.getData());
                    reportDao.updateReport(rp);
                } else {
                    log.error("流量冻结失败");
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw e;
            }
        }
    }
    private void fillContext(CloudConditionCheckerContext context) {
        if (context.getSceneData() == null) {
            context.setSceneData(cloudSceneManageService.getSceneManage(context.getSceneId(), null));
            SceneTaskStartInput input = new SceneTaskStartInput();
            input.setOperateId(CloudPluginUtils.getUserId());
            context.setInput(input);
        }
    }

    @Override
    public String type() {
        return "flow";
    }

    @Override
    public int getOrder() {
        return 4;
    }
}
