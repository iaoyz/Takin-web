package io.shulie.takin.web.common.util.application;

import com.pamirs.takin.common.util.MD5Util;
import io.shulie.amdb.common.enums.RpcType;
import io.shulie.takin.web.common.enums.application.AppRemoteCallConfigEnum;
import io.shulie.takin.web.common.enums.application.AppRemoteCallTypeEnum;
import io.shulie.takin.web.ext.util.WebPluginUtils;

/**
 * @author 无涯
 * @date 2021/6/2 3:25 下午
 */
public class RemoteCallUtils {


    /**
     * 去重 求md5
     * @param appName
     * @param interfaceType
     * @param interfaceName
     * @return
     */
    public static String buildRemoteCallName(String appName,String interfaceName,Object interfaceType) {
        String data = appName + "@@"+  interfaceName + "@@" + (interfaceType == null ? "" :interfaceType) + "@@" +
            WebPluginUtils.traceTenantId() + "@@" + WebPluginUtils.traceEnvCode();
        return MD5Util.getMD5(data);
    }


    /**
     * 导入导出用
     * @param interfaceName
     * @param type
     * @return
     */
    public static String buildImportRemoteCallName(String interfaceName,Object type) {
        return  interfaceName + "@@" + (type == null ? "" :type);
    }

    /**
     * 获取interfaceName
     * @param rpcType
     * @param serviceName
     * @param methodName
     * @return
     */
    public static String getInterfaceName(String rpcType,String serviceName,String methodName) {
        String interfaceName = "";
        switch (Integer.parseInt(rpcType)) {
            case RpcType.TYPE_WEB_SERVER:
                interfaceName = serviceName;
                break;
            case RpcType.TYPE_RPC:
                // 去掉参数列表  setUser(com.example.clientdemo.userModel)   setUser~(com.example.clientdemo
                // .userModel)
                interfaceName = serviceName.split(":")[0] + "#" + methodName.split("~")[0].split("\\(")[0];
                break;
            default:
                interfaceName = serviceName + "#" + methodName;
        }
        return interfaceName;
    }

    /**
     * 获取interfaceName
     * @param rpcName
     * @param serviceName
     * @param methodName
     * @return
     */
    public static String getInterfaceNameByRpcName(String rpcName,String serviceName,String methodName) {
        String interfaceName = "";
        AppRemoteCallTypeEnum typeEnum = AppRemoteCallTypeEnum.getEnumByDesc(rpcName.toUpperCase());
        if(typeEnum == null) {
            return serviceName;
        }
        switch (typeEnum) {
            case HTTP:
                interfaceName = serviceName;
                break;
            case FEIGN:
                interfaceName = serviceName.split(":")[0] + "#" + methodName.split("~")[0].split("\\(")[0];
                break;
            case DUBBO:
                // 去掉参数列表  setUser(com.example.clientdemo.userModel)   setUser~(com.example.clientdemo
                // .userModel)
                interfaceName = serviceName.split(":")[0] + "#" + methodName.split("~")[0].split("\\(")[0];
                break;
            default:
                interfaceName = serviceName + "#" + methodName;
        }
        return interfaceName;
    }

    /**
     * 是否校验白名单异常
     * @param type
     * @return
     */
    public static boolean checkWhite(Integer type) {
        return AppRemoteCallConfigEnum.OPEN_WHITELIST.getType().equals(type);
    }


}
