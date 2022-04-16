package io.shulie.takin.web.biz.checker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import com.pamirs.takin.entity.domain.dto.scenemanage.SceneManageWrapperDTO;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput.EnginePluginRefOutput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput.SceneScriptRefOutput;
import io.shulie.takin.cloud.biz.service.engine.EnginePluginFilesService;
import io.shulie.takin.cloud.biz.utils.FileTypeBusinessUtil;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.utils.security.MD5Utils;
import io.shulie.takin.web.biz.pojo.response.scriptmanage.PluginConfigDetailResponse;
import io.shulie.takin.web.biz.pojo.response.scriptmanage.ScriptManageDeployDetailResponse;
import io.shulie.takin.web.biz.service.scenemanage.SceneTaskService;
import io.shulie.takin.web.biz.service.scriptmanage.ScriptManageService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScriptChecker implements StartConditionChecker {

    @Resource
    private SceneTaskService sceneTaskService;

    @Resource
    private ScriptManageService scriptManageService;

    @Resource
    private EnginePluginFilesService enginePluginFilesService;

    private static final String SCRIPT_NAME_SUFFIX = "jmx";

    @Value("${script.path}/")
    private String pathPrefix;

    @Override
    public CheckResult check(StartConditionCheckerContext context) {
        try {
            fillPlugins(context);
            SceneManageWrapperDTO sceneData = context.getSceneDataDTO();
            // 压测脚本文件检查
            String scriptCorrelation = sceneTaskService.checkScriptCorrelation(sceneData);
            if (StringUtils.isNotBlank(scriptCorrelation)) {
                return CheckResult.fail(type(), scriptCorrelation);
            }
            doCheck(context);
            return CheckResult.success(type());
        } catch (Exception e) {
            return CheckResult.fail(type(), e.getMessage());
        }
    }

    private void doCheck(StartConditionCheckerContext context) {
        SceneManageWrapperOutput sceneData = context.getSceneData();
        //检测脚本文件是否有变更
        checkModify(sceneData);
        // 校验是否与场景同步了
        checkSync(sceneData);
        // 压测文件完整性检测
        checkScriptComplete(sceneData);
    }

    private void checkScriptComplete(SceneManageWrapperOutput sceneData) {
        List<FileInfo> fileInfos = sceneData.getUploadFile().stream()
            .filter(file -> FileTypeBusinessUtil.isScriptOrData(file.getFileType()))
            .map(file -> new FileInfo(deduceFileType(file), pathPrefix + file.getUploadPath())).collect(Collectors.toList());
        checkExists(fileInfos);
    }

    private void checkExists(List<FileInfo> fileInfos) {
        List<String> errorMessage = new ArrayList<>();
        fileInfos.forEach(file -> {
            if (!new File(file.getPath()).exists()) {
                String message = buildNoExistsMessage(file);
                if (message != null) {
                    errorMessage.add(message);
                }
            }
        });
        if (!CollectionUtils.isEmpty(errorMessage)) {
            throw new TakinCloudException(TakinCloudExceptionEnum.SCENE_JMX_FILE_CHECK_ERROR,
                StringUtils.join(errorMessage, ","));
        }
    }

    private String buildNoExistsMessage(FileInfo fileInfo) {
        if (fileInfo.getType() == FileTypeEnum.JMX.ordinal()) {
            return "jmx脚本文件[" + fileInfo.getPath() + "]不存在";
        } else if (fileInfo.getType() == FileTypeEnum.DATA.ordinal()) {
            return "数据文件[" + fileInfo.getPath() + "]不存在";
        } else if (fileInfo.getType() == FileTypeEnum.JAR.ordinal()) {
            return "插件jar文件[" + fileInfo.getPath() + "]不存在";
        }
        return null;
    }

    private Integer deduceFileType(SceneScriptRefOutput file) {
        Integer fileType = file.getFileType();
        return FileTypeBusinessUtil.isScript(fileType) ? FileTypeEnum.JMX.ordinal() : FileTypeEnum.DATA.ordinal();
    }

    private void checkSync(SceneManageWrapperOutput sceneData) {
        String disabledKey = "DISABLED";
        String featureString = sceneData.getFeatures();
        Map<String, Object> feature = JSONObject.parseObject(featureString,
            new TypeReference<Map<String, Object>>() {});
        if (feature.containsKey(disabledKey)) {
            throw new TakinCloudException(TakinCloudExceptionEnum.TASK_START_VERIFY_ERROR,
                "场景【" + sceneData.getId() + "】对应的业务流程发生变更，未能自动匹配，请手动编辑后启动压测");
        }
    }

    private void checkModify(SceneManageWrapperOutput sceneData) {
        SceneScriptRefOutput scriptRefOutput = sceneData.getUploadFile().stream().filter(Objects::nonNull)
            .filter(fileRef -> fileRef.getFileType() == 0 && fileRef.getFileName().endsWith(SCRIPT_NAME_SUFFIX))
            .findFirst()
            .orElse(null);
        boolean jmxCheckResult = checkOutJmx(scriptRefOutput);
        if (!jmxCheckResult) {
            throw new TakinCloudException(TakinCloudExceptionEnum.SCENE_JMX_FILE_CHECK_ERROR,
                "启动压测场景--场景ID:" + sceneData.getId() + ",脚本文件校验失败！");
        }
    }

    private boolean checkOutJmx(SceneScriptRefOutput uploadFile) {
        if (Objects.nonNull(uploadFile) && StringUtils.isNotBlank(uploadFile.getUploadPath())) {
            String fileMd5 = MD5Utils.getInstance().getMD5(new File(pathPrefix + uploadFile.getUploadPath()));
            return StringUtils.isBlank(uploadFile.getFileMd5()) || uploadFile.getFileMd5().equals(fileMd5);
        }
        return false;
    }

    private void fillPlugins(StartConditionCheckerContext context) {
        SceneManageWrapperDTO dto = context.getSceneDataDTO();
        Long scriptId = dto.getScriptId();
        if (Objects.nonNull(scriptId)) {
            ScriptManageDeployDetailResponse deployDetail = scriptManageService.getScriptManageDeployDetail(scriptId);
            List<PluginConfigDetailResponse> pluginDetails = deployDetail.getPluginConfigDetailResponseList();
            if (CollectionUtils.isNotEmpty(pluginDetails)) {
                List<EnginePluginRefOutput> plugins = pluginDetails.stream()
                    .map(detail -> EnginePluginRefOutput.create(Long.parseLong(detail.getName()), detail.getVersion()))
                    .collect(Collectors.toList());
                context.getSceneData().setEnginePlugins(plugins);
            }
        }
    }

    @Override
    public String type() {
        return "file";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    public enum FileTypeEnum {
        ATTACHMENT,
        JMX,
        DATA,
        JAR
    }

    @Data
    @AllArgsConstructor
    public static class FileInfo {
        /**
         * 类型：
         * 1-脚本
         * 2-csv
         * 3-插件jar
         */
        private Integer type;
        private String path;
    }
}
