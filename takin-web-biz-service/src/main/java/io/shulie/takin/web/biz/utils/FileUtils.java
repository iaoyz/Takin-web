package io.shulie.takin.web.biz.utils;

import java.io.File;
import java.util.List;
import java.util.Arrays;
import java.nio.file.Path;
import java.io.FileWriter;
import java.util.ArrayList;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.nio.file.LinkOption;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import com.alibaba.fastjson.JSONObject;

import com.pamirs.takin.entity.domain.dto.scenemanage.SceneScriptRefDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;

/**
 * @author xuyh
 * @date 2020/4/18 16:00.
 */
public class FileUtils {
    private static final Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static boolean isAbsoluteUploadPath(List<SceneScriptRefDTO> files, final String uploadDir) {
        if (CollectionUtils.isEmpty(files)) {
            return false;
        }
        String scriptFile = files.stream().filter(Objects::nonNull)
                .filter(f -> Objects.nonNull(f.getFileType()))
                .filter(f -> f.getFileType() == 0)
                .map(SceneScriptRefDTO::getUploadPath)
                .findAny()
                .orElse("");
        return isAbsoluteUploadPath(scriptFile, uploadDir);
    }
    public static boolean isAbsoluteUploadPath(String filePath, final String uploadDir) {
        return filePath.startsWith(uploadDir);
    }

    public static File createFileDoNotExists(String filePathName) {
        File file = new File(filePathName);
        if (file.exists()) {
            if (!file.delete()) {
                return null;
            }
        }
        if (!makeDir(file.getParentFile())) {
            return null;
        }
        try {
            if (!file.createNewFile()) {
                return null;
            }
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
        return file;
    }

    public static boolean makeDir(File dir) {
        if (!dir.exists()) {
            File parent = dir.getParentFile();
            if (parent != null) {
                makeDir(parent);
            }
            return dir.mkdir();
        }
        return true;
    }

    public static List<File> getDirectoryFiles(String dir, String fileEndsWith) {
        if (dir == null) {
            return null;
        }
        File fileDir = new File(dir);
        if (!fileDir.isDirectory()) {
            logger.warn("Expected a dir, but not: '{}'", fileDir.getPath());
        }
        if (!fileDir.isAbsolute()) {
            logger.warn("Expected a absolute path, bu not: '{}'", fileDir.getPath());
        }
        File[] files = fileDir.listFiles(file -> {
            if (fileEndsWith == null) {
                return true;
            } else {
                return file.getName().endsWith(fileEndsWith);
            }
        });
        if (files == null || files.length == 0) {
            return null;
        }

        List<File> scriptFiles = new ArrayList<>(Arrays.asList(files));
        return scriptFiles;
    }

    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    if (!deleteDir(new File(child))) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }

    public static boolean writeTextFile(String content, String filePathName) {
        File file = createFileDoNotExists(filePathName);
        if (file == null) {
            return false;
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.flush();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        return true;
    }

    public static boolean appendTextFile(String content, String filePathName) {
        File file = new File(filePathName);
        if (!file.exists()) {
            makeDir(file.getParentFile());
            try {
                boolean createResult = file.createNewFile();
                if (!createResult) {
                    return false;
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.append(content);
            writer.flush();
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        return true;
    }

    public static String readTextFileContent(File file) {
        InputStreamReader reader = null;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            reader = new InputStreamReader(new FileInputStream(file));
            char[] buffer = new char[32];
            int length;
            while ((length = reader.read(buffer)) > 0) {
                stringBuilder.append(buffer, 0, length);
            }
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        }
        return stringBuilder.toString();
    }

    public static void appendContent2(String filePath, String content) {
        if (StringUtils.isBlank(content)) {
            return;
        }
        FileWriter fw = null;
        PrintWriter pw = null;
        try {
            //如果文件存在，则追加内容；如果文件不存在，则创建文件
            File newFile = new File(filePath);
            if (!newFile.exists()) {
                makeDir(newFile.getParentFile());
                try {
                    newFile.createNewFile();
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }

            fw = new FileWriter(newFile, true);
            pw = new PrintWriter(fw);
            pw.println(content);
            pw.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
        } finally {
            try {
                if (fw != null) {
                    fw.flush();
                }
                if (pw != null) {
                    pw.close();
                }
                if (fw != null) {
                    fw.close();
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }
    }

    /**
     * 使用LineNumberReader读取文件，1000w行比RandomAccessFile效率高，无法处理1亿条数据
     *
     * @param index 开始行位置
     * @param count 读取量
     *
     * @return pins文件内容
     */
    public static JSONObject readLine(String filePath, int index, int count) {
        JSONObject json = new JSONObject();
        List<String> pins = Lists.newArrayList();
        long lineNumber = 0;
        try {
            lineNumber = Files.lines(Paths.get(filePath)).count();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        File file = new File(filePath);
        LineNumberReader reader = null;
        int readLine = 0;
        InputStreamReader inputStreamReader = null;
        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(file);
            inputStreamReader = new InputStreamReader(fileInputStream, StandardCharsets.UTF_8);
            reader = new LineNumberReader(inputStreamReader);
            int lines = 0;
            while (true) {
                String pin = reader.readLine();
                readLine++;
                if (lines >= index) {
                    pins.add(pin);
                }
                if (count == pins.size() || readLine >= lineNumber) {
                    break;
                }
                lines++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (inputStreamReader != null) {
                    inputStreamReader.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        json.put("list", pins);
        json.put("lineNum", lineNumber);
        return json;
    }

    public static List<String> getFileNames(String path) {
        try {
            File file = new File(path);
            String[] list = file.list();
            if (list == null) {
                return Lists.newArrayList();
            }
            return Arrays.asList(list);
        } catch (Exception e) {
            return Lists.newArrayList();
        }
    }

    /**
     * 判断文件是否存在
     *
     * @param path -
     *
     * @return -
     */
    public static boolean existFile(String path) {
        File file = new File(path);
        return file.exists();
    }

    /**
     * 根据创建时间删除指定目录下的文件
     *
     * @param filePath   -
     * @param deleteTime -
     *
     * @throws IOException -
     */
    public static void deleteFileByCreateTime(String filePath, Long deleteTime) throws IOException {

        if (StringUtils.isBlank(filePath) || deleteTime == null || "/".equals(filePath) || "/root".equals(filePath)) {
            return;
        }
        File root = new File(filePath);
        File[] files = root.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                // 递归调用
                deleteFileByCreateTime(file.getAbsolutePath(), deleteTime);
            } else {
                Path path = Paths.get(file.getAbsolutePath());
                BasicFileAttributeView basicView = Files.getFileAttributeView(path, BasicFileAttributeView.class,
                        LinkOption.NOFOLLOW_LINKS);
                BasicFileAttributes attr = basicView.readAttributes();
                long fileCreateTime = attr.creationTime().toMillis();
                if (fileCreateTime < deleteTime) {
                    file.delete();
                }
            }
        }
    }

    /**
     * 根据字节获取文件大小
     *
     * @param size -
     *
     * @return -
     */
    public static String getByteSize(long size) {
        if (size < 1024) {
            return String.valueOf(size) + "B";
        } else {
            size = size / 1024;
        }

        if (size < 1024) {
            return String.valueOf(size) + "KB";
        } else {
            size = size / 1024;
        }
        if (size < 1024) {
            size = size * 100;
            return String.valueOf((size / 100)) + "."
                    + String.valueOf((size % 100)) + "MB";
        } else {
            //否则如果要以GB为单位的，先除于1024再作同样的处理
            size = size * 100 / 1024;
            return String.valueOf((size / 100)) + "."
                    + String.valueOf((size % 100)) + "GB";
        }
    }
}
