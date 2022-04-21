package io.shulie.takin.cloud.biz.checker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import io.shulie.takin.adapter.api.model.request.check.ScriptCheckRequest.FileInfo;
import io.shulie.takin.cloud.biz.input.scenemanage.SceneTaskStartInput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput;
import io.shulie.takin.cloud.biz.output.scene.manage.SceneManageWrapperOutput.SceneScriptRefOutput;
import io.shulie.takin.cloud.biz.service.engine.EnginePluginFilesService;
import io.shulie.takin.cloud.biz.utils.FileTypeBusinessUtil;
import io.shulie.takin.cloud.common.exception.TakinCloudException;
import io.shulie.takin.cloud.common.exception.TakinCloudExceptionEnum;
import io.shulie.takin.utils.security.MD5Utils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class ScriptChecker implements PressureStartConditionChecker {

    private static final String SCRIPT_NAME_SUFFIX = "jmx";

    @Resource
    private EnginePluginFilesService enginePluginFilesService;

    @Override
    public void check(SceneManageWrapperOutput sceneData, SceneTaskStartInput input) throws TakinCloudException {
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
            .map(file -> new FileInfo(deduceFileType(file), file.getUploadPath())).collect(Collectors.toList());
        List<String> pluginsPath = enginePluginFilesService
            .findPluginFilesPathByPluginIdAndVersion(sceneData.getEnginePlugins());
        List<FileInfo> plugins = pluginsPath.stream().filter(Objects::nonNull)
            .map(path -> new FileInfo(FileTypeEnum.JAR.ordinal(), path))
            .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(plugins)) {
            fileInfos.addAll(plugins);
        }
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
        if (CollectionUtils.isEmpty(errorMessage)) {
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

        boolean jmxCheckResult = checkOutJmx(scriptRefOutput, sceneData.getId());
        if (!jmxCheckResult) {
            throw new TakinCloudException(TakinCloudExceptionEnum.SCENE_JMX_FILE_CHECK_ERROR,
                "启动压测场景--场景ID:" + sceneData.getId() + ",脚本文件校验失败！");
        }
    }

    private boolean checkOutJmx(SceneScriptRefOutput uploadFile, Long sceneId) {
        if (Objects.nonNull(uploadFile) && StringUtils.isNotBlank(uploadFile.getUploadPath())) {
            String fileMd5 = MD5Utils.getInstance().getMD5(new File(uploadFile.getUploadPath()));
            if (StringUtils.isNotBlank(uploadFile.getFileMd5())) {
                return uploadFile.getFileMd5().equals(fileMd5);
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 2;
    }

    public enum FileTypeEnum {
        ATTACHMENT,
        JMX,
        DATA,
        JAR
    }
}
