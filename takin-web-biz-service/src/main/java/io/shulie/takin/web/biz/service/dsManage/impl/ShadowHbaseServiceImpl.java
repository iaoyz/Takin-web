package io.shulie.takin.web.biz.service.dsManage.impl;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;

import com.pamirs.takin.common.constant.AppAccessTypeEnum;
import io.shulie.takin.web.biz.cache.AgentConfigCacheManager;
import io.shulie.takin.web.biz.pojo.input.application.ApplicationDsCreateInput;
import io.shulie.takin.web.biz.pojo.input.application.ApplicationDsDeleteInput;
import io.shulie.takin.web.biz.pojo.input.application.ApplicationDsEnableInput;
import io.shulie.takin.web.biz.pojo.input.application.ApplicationDsUpdateInput;
import io.shulie.takin.web.biz.pojo.output.application.ApplicationDsDetailOutput;
import io.shulie.takin.web.biz.service.ApplicationService;
import io.shulie.takin.web.biz.service.dsManage.AbstractDsService;
import io.shulie.takin.web.common.common.Response;
import io.shulie.takin.web.common.exception.TakinWebException;
import io.shulie.takin.web.common.exception.TakinWebExceptionEnum;
import io.shulie.takin.web.ext.util.WebPluginUtils;
import io.shulie.takin.web.data.dao.application.ApplicationDAO;
import io.shulie.takin.web.data.dao.application.ApplicationDsDAO;
import io.shulie.takin.web.data.param.application.ApplicationDsCreateParam;
import io.shulie.takin.web.data.param.application.ApplicationDsDeleteParam;
import io.shulie.takin.web.data.param.application.ApplicationDsEnableParam;
import io.shulie.takin.web.data.param.application.ApplicationDsUpdateParam;
import io.shulie.takin.web.data.result.application.ApplicationDetailResult;
import io.shulie.takin.web.data.result.application.ApplicationDsResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Hbase数据源存储服务
 *
 * @author HengYu
 * @date 2021/4/12 9:25 下午
 */
@Slf4j
@Component
public class ShadowHbaseServiceImpl extends AbstractDsService {

    @Resource
    private ApplicationDAO applicationDAO;
    @Resource
    private ApplicationDsDAO applicationDsDAO;
    @Resource
    private ApplicationService applicationService;
    @Resource
    private AgentConfigCacheManager agentConfigCacheManager;

    @Override
    public Response dsAdd(ApplicationDsCreateInput createRequest) {

        ApplicationDetailResult applicationDetailResult = applicationDAO.getApplicationById(
            createRequest.getApplicationId());

        log.warn("应用不存在! id:{}", createRequest.getApplicationId());
        if (applicationDetailResult == null) {
            return Response.fail("应用不存在!");
        }

        ApplicationDsCreateParam createParam = new ApplicationDsCreateParam();

        String config = createRequest.getConfig();

        addParserConfig(createRequest, createParam, config);

        createParam.setApplicationId(createRequest.getApplicationId());
        createParam.setApplicationName(applicationDetailResult.getApplicationName());
        createParam.setDbType(createRequest.getDbType());
        createParam.setDsType(Integer.parseInt(String.valueOf(createRequest.getDsType())));
        createParam.setTenantId(applicationDetailResult.getTenantId());
        WebPluginUtils.fillUserData(createParam);
        syncInfo(createRequest.getApplicationId(), createParam.getApplicationName());
        // 新增配置
        applicationDsDAO.insert(createParam);
        return Response.success();
    }

    private void addParserConfig(ApplicationDsCreateInput createRequest, ApplicationDsCreateParam createParam,
        String config) {
        Map<String, Object> map = parseConfig(config);
        String url = formatUrl(map);
        createParam.setUrl(url);
        createParam.setConfig(config);
        createParam.setParseConfig(createRequest.getConfig());
    }

    private void syncInfo(Long applicationId, String applicationName) {
        syncShadowHbase(applicationId, null);
        //修改应用状态
        applicationService.modifyAccessStatus(String.valueOf(applicationId),
            AppAccessTypeEnum.UNUPLOAD.getValue(), null);
        //todo agent改造
        agentConfigCacheManager.evictShadowHbase(applicationName);
    }

    private void syncShadowHbase(Long applicationId, String o) {
        //todo 核对同步配置
        //configSyncService.syncShadowDB(RestContext.getUser().getKey(), applicationId, o);
    }

    private Map<String, Object> parseConfig(String config) {
        try {
            return JSON.parseObject(config, Map.class);
        } catch (Throwable e) {
            throw new TakinWebException(TakinWebExceptionEnum.APPLICATION_CONFIG_FILE_VALIDATE_ERROR, "JSON 格式解析出错！请检查格式是否正确！");
        }
    }

    private Response validator(Map<String, Object> map) {
        if (map == null) {
            String msg = "数据源格式配置不正确!";
            log.warn(msg);
            return Response.fail(msg);
        }
        Object dataSourceBusiness = map.get("dataSourceBusiness");
        Object dataSourcePerformanceTest = map.get("dataSourcePerformanceTest");
        if (dataSourceBusiness == null || dataSourcePerformanceTest == null) {
            String msg = "数据源格式配置不正确!";
            log.warn(msg);
            return Response.fail(msg);
        }
        return null;
    }

    @Override
    public Response dsUpdate(ApplicationDsUpdateInput updateRequest) {
        ApplicationDsResult dsResult = getApplicationDsResult(updateRequest.getId());

        if (Objects.isNull(dsResult)) {
            return Response.fail("0", "该配置不存在");
        }

        ApplicationDsUpdateParam updateParam = new ApplicationDsUpdateParam();
        updateParserConfig(updateRequest, updateParam);

        updateParam.setId(updateRequest.getId());
        updateParam.setStatus(updateRequest.getStatus());
        syncInfo(dsResult.getApplicationId(), dsResult.getApplicationName());
        applicationDsDAO.update(updateParam);
        return Response.success();
    }

    private void updateParserConfig(ApplicationDsUpdateInput updateRequest, ApplicationDsUpdateParam updateParam) {
        String config = updateRequest.getConfig();
        Map<String, Object> map = parseConfig(config);
        String url = formatUrl(map);
        updateParam.setUrl(url);
        updateParam.setConfig(config);
        updateParam.setParseConfig(config);
    }

    private String formatUrl(Map<String, Object> map) {
        Map<String, String> dataSourceBusinessObj = (Map<String, String>)map.get("dataSourceBusiness");
        return dataSourceBusinessObj.get("quorum");
    }

    @Override
    public Response<ApplicationDsDetailOutput> dsQueryDetail(Long dsId, boolean isOldVersion) {
        ApplicationDsResult dsResult = getApplicationDsResult(dsId);

        if (Objects.isNull(dsResult)) {
            return Response.fail("该影子配置不存在");
        }
        ApplicationDsDetailOutput dsDetailResponse = new ApplicationDsDetailOutput();
        dsDetailResponse.setId(dsResult.getId());
        dsDetailResponse.setApplicationId(dsResult.getApplicationId());
        dsDetailResponse.setApplicationName(dsResult.getApplicationName());
        dsDetailResponse.setDbType(dsResult.getDbType());
        dsDetailResponse.setDsType(dsResult.getDsType());

        queryParserConfig(dsResult, dsDetailResponse);
        return Response.success(dsDetailResponse);
    }

    private void queryParserConfig(ApplicationDsResult dsResult, ApplicationDsDetailOutput dsDetailResponse) {
        dsDetailResponse.setUrl(dsResult.getUrl());
        String config = dsResult.getConfig();
        dsDetailResponse.setConfig(config);
    }

    @Override
    public Response enableConfig(ApplicationDsEnableInput enableRequest) {
        ApplicationDsResult dsResult = getApplicationDsResult(enableRequest.getId());

        if (Objects.isNull(dsResult)) {
            return Response.fail("0", "该配置不存在");
        }

        ApplicationDsEnableParam enableParam = new ApplicationDsEnableParam();
        enableParam.setId(enableRequest.getId());
        enableParam.setStatus(enableRequest.getStatus());
        applicationDsDAO.enable(enableParam);

        syncInfo(dsResult.getApplicationId(), dsResult.getApplicationName());
        return Response.success();
    }

    @Override
    public Response dsDelete(ApplicationDsDeleteInput dsDeleteRequest) {
        ApplicationDsResult dsResult = getApplicationDsResult(dsDeleteRequest.getId());
        if (Objects.isNull(dsResult)) {
            return Response.fail("0", "该配置不存在");
        }
        ApplicationDsDeleteParam deleteParam = new ApplicationDsDeleteParam();
        deleteParam.setIdList(Collections.singletonList(dsDeleteRequest.getId()));
        applicationDsDAO.delete(deleteParam);
        syncInfo(dsResult.getApplicationId(), dsResult.getApplicationName());
        return Response.success();
    }

    private ApplicationDsResult getApplicationDsResult(Long id) {
        return applicationDsDAO.queryByPrimaryKey(id);
    }

}