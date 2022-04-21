package io.shulie.takin.adapter.api.model.request.check;

import java.util.List;

import io.shulie.takin.cloud.ext.content.trace.ContextExt;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScriptCheckRequest extends ContextExt {

    private List<FileInfo> files;

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
