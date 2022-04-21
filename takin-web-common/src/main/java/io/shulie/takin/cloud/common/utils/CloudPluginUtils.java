package io.shulie.takin.cloud.common.utils;

import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import io.shulie.takin.web.ext.util.WebPluginUtils;

/**
 * @author hezhongqi
 * @author 张天赐
 * @date 2021/8/4 14:42
 */
public class CloudPluginUtils {

    /**
     * 返回用户id
     *
     * @return -
     */
    public static ContextExt getContext() {
        ContextExt ext = new ContextExt();
        ext.setUserId(WebPluginUtils.traceUserId());
        ext.setTenantId(WebPluginUtils.traceTenantId());
        ext.setFilterSql("");
        ext.setEnvCode(WebPluginUtils.traceEnvCode());
        ext.setTenantCode(WebPluginUtils.traceTenantCode());
        return ext;
    }

    /**
     * 用户主键
     *
     * @return -
     */
    public static Long getUserId() {
        return getContext().getUserId();
    }

    /**
     * 租户主键
     *
     * @return -
     */
    public static Long getTenantId() {
        return getContext().getTenantId();
    }

    /**
     * 环境编码
     *
     * @return -
     */
    public static String getEnvCode() {
        return getContext().getEnvCode();
    }

    /**
     * 返回过滤sql
     *
     * @return -
     */
    public static String getFilterSql() {
        return getContext().getFilterSql();
    }

    /**
     * 公共补充 查询 用户数据
     *
     * @param ext -
     */
    public static void fillUserData(ContextExt ext) {
        ext.setUserId(getContext().getUserId());
        ext.setTenantId(getContext().getTenantId());
        ext.setEnvCode(getContext().getEnvCode());
        ext.setFilterSql(getContext().getFilterSql());
    }
}
