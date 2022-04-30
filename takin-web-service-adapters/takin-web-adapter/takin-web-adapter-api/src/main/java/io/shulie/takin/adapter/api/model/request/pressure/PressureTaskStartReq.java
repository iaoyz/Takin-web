package io.shulie.takin.adapter.api.model.request.pressure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.shulie.takin.cloud.constant.enums.FormulaSymbol;
import io.shulie.takin.cloud.constant.enums.FormulaTarget;
import io.shulie.takin.cloud.constant.enums.JobType;
import io.shulie.takin.cloud.constant.enums.ThreadGroupType;
import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import io.shulie.takin.cloud.model.request.StartRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PressureTaskStartReq extends ContextExt {

    /**
     * 状态接口回调地址
     */
    private String callbackUrl;
    /**
     * 资源Id
     */
    private Long resourceId;
    /**
     * jvm参数
     */
    private String jvmOptions;
    /**
     * 采样率
     */
    private Integer sampling;
    /**
     * 任务类型
     * 0-常规模式
     * 3-流量模式
     * 4-巡检模式
     * 5-试跑模式
     */
    private JobType type;
    /**
     * 任务名称
     */
    private String name;
    /**
     * 脚本文件(jmx)
     */
    private StartRequest.FileInfo scriptFile;
    /**
     * 运行时依赖文件(插件)
     */
    private List<StartRequest.FileInfo> dependency;
    /**
     * 数据文件(csv)
     */
    private List<StartRequest.FileInfo> data;
    /**
     * 线程配置
     */
    private List<StartRequest.ThreadConfigInfo> threadConfig;
    /**
     * sla配置
     */
    private List<StartRequest.SlaInfo> slaConfig;
    private List<StartRequest.MetricsInfo> metricsConfig;
    /**
     * 调试模式下脚本调试配置
     */
    private Map<String, String> ext;

    private static final Map<Integer, JobType> JOB_TYPE_MAP = new HashMap<>(8);
    private static final Map<String, ThreadGroupType> GROUP_TYPE_MAP = new HashMap<>(8);
    private static final Map<Integer, FormulaSymbol> SYMBOL_MAP = new HashMap<>(8);
    private static final Map<Integer, FormulaTarget> FORMULA_MAP = new HashMap<>(8);
    static {
        for (JobType value : JobType.values()) {
            JOB_TYPE_MAP.put(value.getCode(), value);
        }
        for (ThreadGroupType groupType : ThreadGroupType.values()) {
            GROUP_TYPE_MAP.put(groupType.getType() + "_" + groupType.getModel(), groupType);
        }
        for (FormulaSymbol symbol : FormulaSymbol.values()) {
            SYMBOL_MAP.put(symbol.ordinal(), symbol);
        }
        for (FormulaTarget target : FormulaTarget.values()) {
            FORMULA_MAP.put(target.getCode(), target);
        }
    }

    public static JobType ofJobType(Integer code) {
        return JOB_TYPE_MAP.get(code);
    }

    public static ThreadGroupType ofGroupType(Integer type, Integer model) {
        return GROUP_TYPE_MAP.get(type + "_" + model);
    }

    public static FormulaSymbol ofSymbol(Integer symbol) {
        return SYMBOL_MAP.get(symbol);
    }

    public static FormulaTarget ofTarget(Integer code) {
        return FORMULA_MAP.get(code);
    }
}
