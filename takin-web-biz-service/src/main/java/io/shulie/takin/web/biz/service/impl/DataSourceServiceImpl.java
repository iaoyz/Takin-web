package io.shulie.takin.web.biz.service.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.pamirs.takin.common.constant.DataSourceVerifyTypeEnum;
import com.pamirs.takin.common.util.AESUtil;
import com.pamirs.takin.common.util.JdbcConnection;
import io.shulie.takin.common.beans.page.PagingList;
import io.shulie.takin.web.biz.pojo.request.datasource.DataSourceCreateRequest;
import io.shulie.takin.web.biz.pojo.request.datasource.DataSourceQueryRequest;
import io.shulie.takin.web.biz.pojo.request.datasource.DataSourceTestRequest;
import io.shulie.takin.web.biz.pojo.request.datasource.DataSourceUpdateRequest;
import io.shulie.takin.web.biz.pojo.request.datasource.DataSourceUpdateTagsRequest;
import io.shulie.takin.web.biz.pojo.response.datasource.DataSourceTypeResponse;
import io.shulie.takin.web.biz.pojo.response.datasource.DatasourceDetailResponse;
import io.shulie.takin.web.biz.pojo.response.datasource.DatasourceDictionaryResponse;
import io.shulie.takin.web.biz.pojo.response.datasource.DatasourceListResponse;
import io.shulie.takin.web.biz.pojo.response.tagmanage.TagManageResponse;
import io.shulie.takin.web.biz.service.DataSourceService;
import io.shulie.takin.web.common.exception.ExceptionCode;
import io.shulie.takin.web.common.exception.TakinWebException;
import io.shulie.takin.web.ext.util.WebPluginUtils;
import io.shulie.takin.web.data.dao.datasource.DataSourceDAO;
import io.shulie.takin.web.data.dao.datasource.DataSourceTagRefDAO;
import io.shulie.takin.web.data.dao.leakcheck.LeakCheckConfigDAO;
import io.shulie.takin.web.data.dao.leakcheck.LeakCheckConfigDetailDAO;
import io.shulie.takin.web.data.dao.linkmanage.BusinessLinkManageDAO;
import io.shulie.takin.web.data.dao.tagmanage.TagManageDAO;
import io.shulie.takin.web.data.param.datasource.DataSourceCreateParam;
import io.shulie.takin.web.data.param.datasource.DataSourceDeleteParam;
import io.shulie.takin.web.data.param.datasource.DataSourceQueryParam;
import io.shulie.takin.web.data.param.datasource.DataSourceSingleQueryParam;
import io.shulie.takin.web.data.param.datasource.DataSourceUpdateParam;
import io.shulie.takin.web.data.param.leakcheck.LeakCheckConfigDeleteParam;
import io.shulie.takin.web.data.param.leakcheck.LeakCheckConfigDetailDeleteParam;
import io.shulie.takin.web.data.param.leakcheck.LeakCheckConfigDetailQueryParam;
import io.shulie.takin.web.data.param.leakcheck.LeakCheckConfigQueryParam;
import io.shulie.takin.web.data.param.tagmanage.TagManageParam;
import io.shulie.takin.web.data.result.datasource.DataSourceResult;
import io.shulie.takin.web.data.result.datasource.DataSourceTagRefResult;
import io.shulie.takin.web.data.result.leakcheck.LeakCheckConfigBatchDetailResult;
import io.shulie.takin.web.data.result.leakcheck.LeakCheckConfigResult;
import io.shulie.takin.web.data.result.linkmange.BusinessLinkResult;
import io.shulie.takin.web.data.result.tagmanage.TagManageResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author shiyajian
 * create: 2020-12-28
 */
@Slf4j
@Component
public class DataSourceServiceImpl implements DataSourceService {

    @Autowired
    private TagManageDAO tagManageDAO;

    @Autowired
    private DataSourceTagRefDAO dataSourceTagRefDAO;

    @Autowired
    private DataSourceDAO dataSourceDAO;

    @Autowired
    private LeakCheckConfigDAO checkConfigDAO;

    @Autowired
    private LeakCheckConfigDetailDAO checkDetailDAO;

    @Autowired
    private BusinessLinkManageDAO businessLinkManageDAO;

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void createDatasource(DataSourceCreateRequest createRequest) {
        Integer type = createRequest.getType();
        String name = createRequest.getDatasourceName();
        String jdbcUrl = createRequest.getJdbcUrl();
        String username = createRequest.getUsername();
        String password = createRequest.getPassword();
        //1、检查数据源是否能正常连接
        if (!checkConnection(jdbcUrl, username, password, type)) {
            throw new TakinWebException(ExceptionCode.DATASOURCE_TEST_CONNECTION_ERROR, "数据源连接失败");
        }
        // 2、检查数据源类型是否支持
        if (!checkIsSupport(type)) {
            throw new TakinWebException(ExceptionCode.DATASOURCE_ADD_ERROR, "不支持的数据源类型");
        }
        // 3、检查名称是否重复
        if (checkIsExistName(name)) {
            throw new TakinWebException(ExceptionCode.DATASOURCE_ADD_ERROR, "数据源名称已存在");
        }
        // 4、检查数据源是否重复
        if (checkIsExistJdbcUrl(jdbcUrl)) {
            throw new TakinWebException(ExceptionCode.DATASOURCE_ADD_ERROR, "数据源地址已存在");
        }
        DataSourceCreateParam createParam = new DataSourceCreateParam();
        createParam.setType(type);
        createParam.setName(name);
        createParam.setJdbcUrl(jdbcUrl);
        createParam.setUsername(username);
        // 4、密码加密（AES128）
        createParam.setPassword(AESUtil.encrypt(password));
        dataSourceDAO.insert(createParam);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void updateDatasource(DataSourceUpdateRequest updateRequest) {
        Long id = updateRequest.getDatasourceId();
        Integer type = updateRequest.getType().getCode();
        String name = updateRequest.getDatasourceName();
        String jdbcUrl = updateRequest.getJdbcUrl();
        String username = updateRequest.getUsername();
        String password = updateRequest.getPassword();
        DataSourceSingleQueryParam queryIdParam = new DataSourceSingleQueryParam();
        queryIdParam.setId(id);
        DataSourceResult idResult = dataSourceDAO.selectSingle(queryIdParam);
        // 1、检查待更新的数据源是否存在
        if (Objects.isNull(idResult)) {
            throw new TakinWebException(ExceptionCode.DATASOURCE_UPDATE_ERROR, "该数据源不存在");
        }
        // 2、检查数据源类型是否支持
        if (!idResult.getType().equals(type) && !checkIsSupport(type)) {
            throw new TakinWebException(ExceptionCode.DATASOURCE_UPDATE_ERROR, "不支持的数据源类型");
        }
        // 3、判断当前登录用户是否有更新权限
        if (!checkUpdatePermission(idResult.getUserId())) {
            throw new TakinWebException(ExceptionCode.DATASOURCE_UPDATE_ERROR, "更新权限不足");
        }
        // 4、除过本id外，检查名称是否重复
        if (!idResult.getName().equals(name) && checkIsExistName(name)) {
            throw new TakinWebException(ExceptionCode.DATASOURCE_UPDATE_ERROR, "数据源名称已存在");
        }
        // 5、除过本id外，检查数据源是否重复
        if (!idResult.getJdbcUrl().equals(jdbcUrl) && checkIsExistJdbcUrl(jdbcUrl)) {
            throw new TakinWebException(ExceptionCode.DATASOURCE_UPDATE_ERROR, "数据源地址已存在");
        }
        DataSourceUpdateParam updateParam = new DataSourceUpdateParam();
        updateParam.setId(id);
        updateParam.setType(type);
        updateParam.setName(name);
        updateParam.setJdbcUrl(jdbcUrl);
        updateParam.setUsername(username);
        String currentPassword = AESUtil.decrypt(idResult.getPassword());
        if (StringUtils.isNotBlank(password) && !currentPassword.equals(password)) {
            // 6、检查数据源是否能正常连接
            if (!checkConnection(jdbcUrl, username, AESUtil.decrypt(idResult.getPassword()), type)) {
                throw new TakinWebException(ExceptionCode.DATASOURCE_TEST_CONNECTION_ERROR, "数据源连接失败");
            }
            updateParam.setPassword(AESUtil.encrypt(password));
        }
        dataSourceDAO.update(updateParam);
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void deleteDatasource(List<Long> datasourceIds) {
        // 根据id查询出来数据源
        DataSourceQueryParam queryParam = new DataSourceQueryParam();
        queryParam.setDataSourceIdList(datasourceIds);
        List<DataSourceResult> dataSourceResultList = dataSourceDAO.selectList(queryParam);
        if (CollectionUtils.isNotEmpty(dataSourceResultList)) {
            List<Long> userIdList = dataSourceResultList.stream().map(DataSourceResult::getUserId).collect(
                Collectors.toList());
            // 判断数据源的权限、状态等情况；
            if (!checkDeletePermission(userIdList)) {
                throw new TakinWebException(ExceptionCode.DATASOURCE_DELETE_ERROR, "数据权限不足");
            }
            // 删除数据源
            DataSourceDeleteParam deleteParam = new DataSourceDeleteParam();
            deleteParam.setIdList(datasourceIds);
            dataSourceDAO.delete(deleteParam);
            // 删除数据源对应的命令
            LeakCheckConfigQueryParam configQueryParam = new LeakCheckConfigQueryParam();
            configQueryParam.setDatasourceIds(datasourceIds);
            List<LeakCheckConfigResult> configResultList = checkConfigDAO.selectList(configQueryParam);
            if (CollectionUtils.isNotEmpty(configResultList)) {
                List<Long> configIds = configResultList.stream().map(LeakCheckConfigResult::getId).collect(
                    Collectors.toList());
                LeakCheckConfigDeleteParam configDeleteParam = new LeakCheckConfigDeleteParam();
                configDeleteParam.setIds(configIds);
                checkConfigDAO.delete(configDeleteParam);

            }

            LeakCheckConfigDetailQueryParam detailQueryParam = new LeakCheckConfigDetailQueryParam();
            detailQueryParam.setDatasourceIds(datasourceIds);
            List<LeakCheckConfigBatchDetailResult> detailResultList = checkDetailDAO.selectList(detailQueryParam);
            if (CollectionUtils.isNotEmpty(detailResultList)) {
                List<Long> detailIds = detailResultList.stream().map(LeakCheckConfigBatchDetailResult::getId).collect(
                    Collectors.toList());
                LeakCheckConfigDetailDeleteParam detailDeleteParam = new LeakCheckConfigDetailDeleteParam();
                detailDeleteParam.setIds(detailIds);
                checkDetailDAO.delete(detailDeleteParam);
            }
        }
    }

    @Override
    public PagingList<DatasourceListResponse> listDatasource(DataSourceQueryRequest queryRequest) {
        // 分页查询数据源
        List<Long> filterDataSourceIdList = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(queryRequest.getTagsIdList())) {
            List<Long> queryTagIdList = queryRequest.getTagsIdList();
            List<DataSourceTagRefResult> tagRefResultListByTagId =
                dataSourceTagRefDAO.selectDataSourceTagRefByTagIds(queryTagIdList);
            if (CollectionUtils.isNotEmpty(tagRefResultListByTagId)) {
                filterDataSourceIdList =
                    tagRefResultListByTagId
                        .stream()
                        .map(DataSourceTagRefResult::getDataSourceId)
                        .collect(Collectors.toList());
            } else {
                return PagingList.empty();
            }
        }
        String name = queryRequest.getDatasourceName();
        name = name == null ? null : name.replace("%", "\\%").replace("_", "\\_").replace("-", "\\-");
        String jdbcUrl = queryRequest.getJdbcUrl();
        jdbcUrl = jdbcUrl == null ? null : jdbcUrl.replace("%", "\\%").replace("_", "\\_").replace("-", "\\-");
        DataSourceQueryParam queryParam = new DataSourceQueryParam();
        queryParam.setCurrent(queryRequest.getCurrent() + 1);
        queryParam.setPageSize(queryRequest.getPageSize());
        if (!Objects.isNull(queryRequest.getType())) {
            Integer type = queryRequest.getType().getCode();
            queryParam.setType(type);
        }
        queryParam.setName(name);
        queryParam.setJdbcUrl(jdbcUrl);
        if (CollectionUtils.isNotEmpty(filterDataSourceIdList)) {
            queryParam.setDataSourceIdList(filterDataSourceIdList);
        }
        PagingList<DataSourceResult> pagingList = dataSourceDAO.selectPage(queryParam);
        if (pagingList.isEmpty()) {
            return PagingList.of(Lists.newArrayList(), pagingList.getTotal());
        }
        List<Long> dataSourceIdList =
            pagingList.getList().stream().map(DataSourceResult::getId).collect(Collectors.toList());
        //查询数据源与标签的关系联系
        List<DataSourceTagRefResult> tagRefResultList = dataSourceTagRefDAO.selectTagRefByDataSourceIds(
            dataSourceIdList);
        if (CollectionUtils.isNotEmpty(tagRefResultList)) {
            List<Long> tagIdList = tagRefResultList.stream().map(DataSourceTagRefResult::getTagId).collect(
                Collectors.toList());
            //查询标签的基础信息
            List<TagManageResult> tagManageResultList = tagManageDAO.selectDataSourceTagsByIds(tagIdList);
            if (CollectionUtils.isNotEmpty(tagManageResultList)) {
                //组装标签信息
                for (DataSourceTagRefResult refResult : tagRefResultList) {
                    Optional<TagManageResult> optional = tagManageResultList
                        .stream()
                        .filter(tagManageResult -> tagManageResult.getId().equals(refResult.getTagId()))
                        .findFirst();
                    optional.ifPresent(tagManageResult -> refResult.setTagName(tagManageResult.getTagName()));
                }
            }
        }
        List<DatasourceListResponse> responseList = pagingList.getList().stream().map(result -> {
            DatasourceListResponse response = new DatasourceListResponse();
            response.setDatasourceId(result.getId());
            response.setDatasourceName(result.getName());
            DataSourceTypeResponse typeResponse = new DataSourceTypeResponse();
            typeResponse.setLabel(DataSourceVerifyTypeEnum.getTypeByCode(result.getType()).name());
            typeResponse.setValue(result.getType());
            response.setType(typeResponse);
            response.setJdbcUrl(result.getJdbcUrl());
            response.setUsername(result.getUsername());
            response.setGmtUpdate(result.getUpdateTime());
            response.setTags(Collections.emptyList());
            if (CollectionUtils.isNotEmpty(tagRefResultList)) {
                //组装数据源标签信息
                List<DataSourceTagRefResult> dataSourceTagRefResultList = tagRefResultList
                    .stream()
                    .filter(tagRefResult -> tagRefResult.getDataSourceId().equals(result.getId()))
                    .collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(dataSourceTagRefResultList)) {
                    List<TagManageResponse> tagManageResponseList = dataSourceTagRefResultList.stream().map(
                        tagRefResult -> {
                            TagManageResponse tagManageResponse = new TagManageResponse();
                            tagManageResponse.setId(tagRefResult.getTagId());
                            tagManageResponse.setTagName(tagRefResult.getTagName());
                            return tagManageResponse;
                        }).collect(Collectors.toList());
                    response.setTags(tagManageResponseList);
                }
            }
            // 补充权限
            WebPluginUtils.fillQueryResponse(response);
            return response;
        }).collect(Collectors.toList());
        return PagingList.of(responseList, pagingList.getTotal());
    }

    @Override
    public List<DatasourceDictionaryResponse> listDatasourceNoPage() {
        List<DatasourceDictionaryResponse> datasourceDictionaryResponseList = Lists.newArrayList();
        DataSourceQueryParam queryParam = new DataSourceQueryParam();
        List<DataSourceResult> dataSourceResultList = dataSourceDAO.selectList(queryParam);
        if (CollectionUtils.isNotEmpty(dataSourceResultList)) {
            datasourceDictionaryResponseList = dataSourceResultList.stream().map(dataSourceResult -> {
                DatasourceDictionaryResponse dictionaryResponse = new DatasourceDictionaryResponse();
                dictionaryResponse.setDatasourceId(dataSourceResult.getId());
                dictionaryResponse.setDatasourceName(dataSourceResult.getName());
                dictionaryResponse.setJdbcUrl(dataSourceResult.getJdbcUrl());
                return dictionaryResponse;
            }).collect(Collectors.toList());
        }
        return datasourceDictionaryResponseList;
    }

    @Override
    public DatasourceDetailResponse getDatasource(Long datasourceId) {
        DataSourceSingleQueryParam queryParam = new DataSourceSingleQueryParam();
        queryParam.setId(datasourceId);
        DataSourceResult dataSourceResult = dataSourceDAO.selectSingle(queryParam);
        if (!Objects.isNull(dataSourceResult)) {
            DatasourceDetailResponse datasourceResponse = new DatasourceDetailResponse();
            datasourceResponse.setDatasourceId(dataSourceResult.getId());
            datasourceResponse.setDatasourceName(dataSourceResult.getName());
            datasourceResponse.setType(dataSourceResult.getType());
            datasourceResponse.setJdbcUrl(dataSourceResult.getJdbcUrl());
            datasourceResponse.setUsername(dataSourceResult.getUsername());
            return datasourceResponse;
        }
        return null;
    }

    @Override
    public List<TagManageResponse> getDatasourceTags() {
        List<TagManageResult> tagManageResults = tagManageDAO.selectDataSourceTags();
        if (CollectionUtils.isNotEmpty(tagManageResults)) {
            return tagManageResults.stream().map(tagManageResult -> {
                TagManageResponse tagManageResponse = new TagManageResponse();
                tagManageResponse.setId(tagManageResult.getId());
                tagManageResponse.setTagName(tagManageResult.getTagName());
                return tagManageResponse;
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public void updateDatasourceTags(DataSourceUpdateTagsRequest tagsRequest) {
        List<String> collect = tagsRequest.getTagNames().stream().filter(o -> o.length() > 64).collect(
            Collectors.toList());
        if (CollectionUtils.isNotEmpty(collect)) {
            throw new TakinWebException(ExceptionCode.DATASOURCE_MANAGE_TAG_ADD_VALID_ERROR, "标签长度超过64");
        }
        Long dataSourceId = tagsRequest.getDatasourceId();
        DataSourceSingleQueryParam queryParam = new DataSourceSingleQueryParam();
        queryParam.setId(dataSourceId);
        DataSourceResult dataSourceResult = dataSourceDAO.selectSingle(queryParam);
        if (dataSourceResult != null) {
            dataSourceTagRefDAO.deleteByDataSourceId(dataSourceId);
            if (CollectionUtils.isNotEmpty(tagsRequest.getTagNames())) {
                List<TagManageParam> tagManageParams = tagsRequest.getTagNames().stream().distinct().map(tagName -> {
                    TagManageParam tagManageParam = new TagManageParam();
                    tagManageParam.setTagName(tagName);
                    tagManageParam.setTagType(1);
                    return tagManageParam;
                }).collect(Collectors.toList());
                List<Long> tagIds = tagManageDAO.addDatasourceTags(tagManageParams);
                dataSourceTagRefDAO.addDataSourceTagRef(tagIds, dataSourceId);
            }
        }
    }

    @Override
    public String testConnection(DataSourceTestRequest testRequest) {
        try {
            Long currentTime = JdbcConnection.fetchCurrentTime(
                testRequest.getJdbcUrl(),
                testRequest.getUsername(),
                testRequest.getPassword(),
                DataSourceVerifyTypeEnum.getTypeByCode(testRequest.getType()));
            if (Objects.isNull(currentTime)) {
                return "数据源连接失败!";
            }
        } catch (RuntimeException exception) {
            String message = exception.getMessage();
            if (message.contains("，")) {
                message = message.split("，")[0];
            }
            log.warn("数据源连接失败:{}", message);
            return "数据源连接失败：" + message;
        }
        return "";
    }

    @Override
    public List<String> getBizActivitiesName(Long datasourceId) {
        LeakCheckConfigQueryParam checkConfigQueryParam = new LeakCheckConfigQueryParam();
        checkConfigQueryParam.setDatasourceIds(Arrays.asList(datasourceId));
        List<LeakCheckConfigResult> checkConfigResults = checkConfigDAO.selectList(checkConfigQueryParam);
        if (CollectionUtils.isNotEmpty(checkConfigResults)) {
            List<Long> businessActivityIdList =
                checkConfigResults.stream().map(LeakCheckConfigResult::getBusinessActivityId).collect(
                    Collectors.toList());
            List<BusinessLinkResult> businessLinkResults = businessLinkManageDAO.selectBussinessLinkByIdList(
                businessActivityIdList);
            if (CollectionUtils.isNotEmpty(businessLinkResults)) {
                return businessLinkResults.stream().map(BusinessLinkResult::getLinkName).collect(Collectors.toList());
            }
        }
        return Collections.emptyList();
    }

    private Boolean checkConnection(String jdbcUrl, String username, String password, Integer type) {
        try {
            Long currentTime = JdbcConnection.fetchCurrentTime(jdbcUrl, username, password,
                DataSourceVerifyTypeEnum.getTypeByCode(type));
            if (Objects.isNull(currentTime)) {
                return Boolean.FALSE;
            } else {
                return Boolean.TRUE;
            }
        } catch (RuntimeException exception) {
            log.warn("数据源连接失败:{}", exception.getMessage());
        }
        return Boolean.FALSE;
    }

    private Boolean checkIsExistName(String name) {
        DataSourceSingleQueryParam queryNameParam = new DataSourceSingleQueryParam();
        queryNameParam.setName(name);
        DataSourceResult nameResult = dataSourceDAO.selectSingle(queryNameParam);
        if (!Objects.isNull(nameResult)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean checkIsExistJdbcUrl(String jdbcUrl) {
        DataSourceSingleQueryParam queryJdbcParam = new DataSourceSingleQueryParam();
        queryJdbcParam.setJdbcUrl(jdbcUrl);
        DataSourceResult jdbcResult = dataSourceDAO.selectSingle(queryJdbcParam);
        if (!Objects.isNull(jdbcResult)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean checkIsExistDataSource(Long id) {
        DataSourceSingleQueryParam queryJdbcParam = new DataSourceSingleQueryParam();
        queryJdbcParam.setId(id);
        DataSourceResult jdbcResult = dataSourceDAO.selectSingle(queryJdbcParam);
        if (!Objects.isNull(jdbcResult)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    private Boolean checkUpdatePermission(Long userId) {
        List<Long> allowUpdateUserIdList = WebPluginUtils.getUpdateAllowUserIdList();
        if (CollectionUtils.isNotEmpty(allowUpdateUserIdList)
            && !allowUpdateUserIdList.contains(userId)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private Boolean checkDeletePermission(List<Long> userIdList) {
        List<Long> allowDeleteUserIdList = WebPluginUtils.getDeleteAllowUserIdList();
        if (CollectionUtils.isNotEmpty(allowDeleteUserIdList)
            && !allowDeleteUserIdList.containsAll(userIdList)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private Boolean checkIsSupport(Integer code) {
        DataSourceVerifyTypeEnum typeEnum = DataSourceVerifyTypeEnum.getTypeByCode(code);
        if (Objects.isNull(typeEnum)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }
}
