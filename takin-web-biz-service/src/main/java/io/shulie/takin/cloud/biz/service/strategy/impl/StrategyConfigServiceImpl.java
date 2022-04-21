package io.shulie.takin.cloud.biz.service.strategy.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import cn.hutool.core.date.DateUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.pamirs.takin.cloud.entity.dao.strategy.TStrategyConfigMapper;
import com.pamirs.takin.cloud.entity.domain.dto.strategy.StrategyConfigDetailDTO;
import com.pamirs.takin.cloud.entity.domain.entity.strategy.StrategyConfig;
import com.pamirs.takin.cloud.entity.domain.vo.strategy.StrategyConfigAddVO;
import com.pamirs.takin.cloud.entity.domain.vo.strategy.StrategyConfigQueryVO;
import com.pamirs.takin.cloud.entity.domain.vo.strategy.StrategyConfigUpdateVO;
import io.shulie.takin.cloud.biz.config.AppConfig;
import io.shulie.takin.cloud.biz.service.strategy.StrategyConfigService;
import io.shulie.takin.cloud.common.enums.deployment.DeploymentMethodEnum;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.cloud.common.utils.EnginePluginUtils;
import io.shulie.takin.cloud.ext.api.EngineCallExtApi;
import io.shulie.takin.cloud.ext.content.enginecall.StrategyConfigExt;
import io.shulie.takin.cloud.ext.content.enginecall.StrategyOutputExt;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author qianshui
 * @date 2020/5/9 下午3:17
 */
@Slf4j
@Service
public class StrategyConfigServiceImpl implements StrategyConfigService {

    @Resource
    private TStrategyConfigMapper tStrategyConfigMapper;

    @Autowired
    private AppConfig appConfig;

    @Override
    public Boolean add(StrategyConfigAddVO addVO) {
        StrategyConfig config = new StrategyConfig();
        config.setStrategyName(addVO.getStrategyName());
        config.setStrategyConfig(addVO.getStrategyConfig());
        tStrategyConfigMapper.insert(config);
        return true;
    }

    @Override
    public Boolean update(StrategyConfigUpdateVO updateVO) {
        StrategyConfig config = new StrategyConfig();
        config.setId(updateVO.getId());
        config.setStrategyName(updateVO.getStrategyName());
        config.setStrategyConfig(updateVO.getStrategyConfig());
        tStrategyConfigMapper.updateByPrimaryKeySelective(config);
        return true;
    }

    @Override
    public Boolean delete(Long id) {
        tStrategyConfigMapper.deleteByPrimaryKey(id);
        return true;
    }

    @Override
    public StrategyConfigDetailDTO getDetail(Long id) {
        StrategyConfig strategyConfig = tStrategyConfigMapper.selectByPrimaryKey(id);
        if (strategyConfig == null) {
            return null;
        }
        StrategyConfigDetailDTO dto = new StrategyConfigDetailDTO();
        dto.setStrategyName(strategyConfig.getStrategyName());
        dto.setStrategyConfig(strategyConfig.getStrategyConfig());
        return dto;
    }

    @Override
    public StrategyOutputExt getStrategy(Integer expectThroughput, Integer tpsNum) {
        StrategyConfigExt strategyConfigExt = new StrategyConfigExt();
        strategyConfigExt.setThreadNum(expectThroughput);
        strategyConfigExt.setTpsNum(tpsNum);
        return getPressureNodeNumRange(strategyConfigExt);

    }

    @Override
    public StrategyConfigExt getDefaultStrategyConfig() {
        PageInfo<StrategyConfigExt> strategyConfig = queryPageList(new StrategyConfigQueryVO());
        if (strategyConfig != null && strategyConfig.getSize() > 0) {
            return strategyConfig.getList().get(0);
        }
        return null;
    }

    @Override
    public StrategyConfigExt getCurrentStrategyConfig() {
        StrategyConfigExt strategyConfig = null;
        PageInfo<StrategyConfigExt> pageInfo = queryPageList(new StrategyConfigQueryVO());
        if (null != pageInfo && CollectionUtils.isNotEmpty(pageInfo.getList())) {
            strategyConfig = pageInfo.getList().stream().filter(Objects::nonNull)
                .filter(config -> appConfig.getDeploymentMethod().getDesc().equals(config.getDeploymentMethod()))
                .findFirst()
                .orElse(pageInfo.getList().get(0));
        }
        return strategyConfig;
    }

    @Override
    public PageInfo<StrategyConfigExt> queryPageList(StrategyConfigQueryVO queryVO) {
        Page<?> pageInfo = PageHelper.startPage(queryVO.getCurrentPage() + 1, queryVO.getPageSize());
        List<StrategyConfig> queryList = tStrategyConfigMapper.getPageList(queryVO);
        if (CollectionUtils.isEmpty(queryList)) {
            return new PageInfo<>(Lists.newArrayList());
        }
        List<StrategyConfigExt> resultList = Lists.newArrayList();
        queryList.forEach(data -> {
            StrategyConfigExt dto = new StrategyConfigExt();
            dto.setId(data.getId());
            dto.setStrategyName(data.getStrategyName());
            parseConfig(dto, data.getStrategyConfig());
            dto.setUpdateTime(DateUtil.formatDateTime(data.getUpdateTime()));
            resultList.add(dto);
        });

        return new PageInfo<StrategyConfigExt>(resultList) {{
            setTotal(pageInfo.getTotal());
        }};
    }

    private void parseConfig(StrategyConfigExt dto, String config) {
        try {
            JSONObject object = JSON.parseObject(config);
            dto.setThreadNum(object.getInteger("threadNum"));
            //默认2cpu
            BigDecimal cpuNum = object.getBigDecimal("cpuNum");
            dto.setCpuNum(cpuNum == null ? new BigDecimal(2) : cpuNum);
            //默认3G内存
            BigDecimal memorySize = object.getBigDecimal("memorySize");
            dto.setMemorySize(memorySize == null ? new BigDecimal(3072) : memorySize);
            //限制cpu 不填则默认为cpuNum
            BigDecimal limitCpuNum = object.getBigDecimal("limitCpuNum");
            dto.setLimitCpuNum(limitCpuNum == null ? cpuNum : limitCpuNum);
            //限制内存 不填默认为memorySize
            BigDecimal limitMemorySize = object.getBigDecimal("limitMemorySize");
            dto.setLimitMemorySize(limitMemorySize == null ? memorySize : limitMemorySize);
            dto.setTpsNum(object.getInteger("tpsNum"));
            dto.setDeploymentMethod(DeploymentMethodEnum.getByType(object.getInteger("deploymentMethod")));

            dto.setTpsThreadMode(object.getInteger("tpsThreadMode"));
            Double tpsTargetLevelFactor = object.getDouble("tpsTargetLevelFactor");
            if (null != tpsTargetLevelFactor) {
                dto.setTpsTargetLevelFactor(tpsTargetLevelFactor);
            }
            dto.setTpsRealThreadNum(object.getInteger("tpsRealThreadNum"));
            dto.setPressureEngineImage(object.getString("pressureEngineImage"));
            dto.setPressureEngineName(object.getString("pressureEngineName"));
            dto.setK8sJvmSettings(object.getString("k8sJvmSettings"));
        } catch (Exception e) {
            log.error("异常代码【{}】,异常内容：解析配置失败 --> Parse Config Failure = {}，异常信息: {}",
                TakinCloudExceptionEnum.SCHEDULE_START_ERROR, config, e);
        }
    }

    // TODO: 要修改
    private StrategyOutputExt getPressureNodeNumRange(StrategyConfigExt strategyConfigExt) {
        StrategyOutputExt result = new StrategyOutputExt();
        BigDecimal min = BigDecimal.ONE;
        BigDecimal max = BigDecimal.ONE;
        Integer tpsNum = strategyConfigExt.getTpsNum();
        Integer threadNum = strategyConfigExt.getThreadNum();
        // 获取k8s的机器
        //List<Node> nodes = microService.getNodeList();
        // TODO: 等待cloud添加新接口
        //List<Node> nodes = null;
        if (DeploymentMethodEnum.PRIVATE == appConfig.getDeploymentMethod()) {
            // nodes
            //if (CollectionUtils.isEmpty(nodes)) {
                throw new TakinCloudException(TakinCloudExceptionEnum.K8S_NODE_EMPTY, "未找到k8s节点");
            //}
        }
        // 获取分配策略
        StrategyConfigExt config = getCurrentStrategyConfig();
        if (config != null) {
            if (tpsNum != null) {
                min = new BigDecimal(tpsNum).divide(new BigDecimal(config.getTpsNum()), 0, RoundingMode.CEILING);
                if (appConfig.getDeploymentMethod() == DeploymentMethodEnum.PRIVATE) {
                    // 私有化部署
                    //max = getMaxByNode(nodes, config.getMemorySize());
                } else if (tpsNum > config.getTpsNum()) {
                    // 公有化计算规则，根据pod的maxTps计算
                    max = min.add(min.multiply(new BigDecimal("0.8")).setScale(0, RoundingMode.CEILING));
                }
            }
            if (threadNum != null) {
                min = new BigDecimal(threadNum).divide(new BigDecimal(config.getThreadNum()), 0,
                    RoundingMode.CEILING);
                if (appConfig.getDeploymentMethod() == DeploymentMethodEnum.PRIVATE) {
                    //max = getMaxByNode(nodes, config.getMemorySize());
                } else if (threadNum > config.getThreadNum()) {
                    // 公有化计算规则，根据pod并发数计算
                    BigDecimal rate;
                    //增加一定的浮动比例
                    if (min.intValue() < 5) {
                        rate = new BigDecimal("0.5");
                    } else if (min.intValue() < 10) {
                        rate = new BigDecimal("0.7");
                    } else if (min.intValue() < 20) {
                        rate = new BigDecimal("0.8");
                    } else {
                        rate = new BigDecimal("0.9");
                    }
                    max = min.add(min.multiply(rate).setScale(0, RoundingMode.CEILING));
                }
            }
        }
        // 做一个逻辑判断
        result.setMax(max.intValue());
        result.setMin(min.intValue());
        // 保证最大值不为0
        if (result.getMax() <= 0) {result.setMax(1);}
        if (appConfig.getDeploymentMethod() == DeploymentMethodEnum.PRIVATE && min.intValue() > max.intValue()) {
            result.setMin(max.intValue());
        }
        // 保证最小值不为0
        if (result.getMin() <= 0) {result.setMin(1);}
        return result;
    }

    //private BigDecimal getMaxByNode(List<Node> nodes, BigDecimal memorySize) {
    //    BigDecimal tempMax = BigDecimal.ZERO;
    //    for (Node node : nodes) {
    //        Map<String, Quantity> map = node.getStatus().getAllocatable();
    //        // 通过内存计算pod
    //        Quantity quantity = map.get("memory");
    //        tempMax = tempMax.add(BigDecimal.valueOf(getMemory(quantity))
    //            .divide(memorySize.multiply(BigDecimal.valueOf(1024 * 1024L)), 0, RoundingMode.DOWN));
    //    }
    //    // 初始化
    //    return tempMax;
    //}
    //
    //private long getMemory(Quantity quantity) {
    //    if ("Ki".equalsIgnoreCase(quantity.getFormat())) {
    //        return Long.parseLong(quantity.getAmount()) * 1024L;
    //    } else if ("Mi".equalsIgnoreCase(quantity.getFormat())) {
    //        return Long.parseLong(quantity.getAmount()) * 1024L * 1024L;
    //    } else if ("Gi".equalsIgnoreCase(quantity.getFormat())) {
    //        return Long.parseLong(quantity.getAmount()) * 1024L * 1024L * 1024L;
    //    } else {
    //        return Long.parseLong(quantity.getAmount());
    //    }
    //}
}
