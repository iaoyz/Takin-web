package io.shulie.takin.web.entrypoint.controller.file;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.pamirs.takin.entity.domain.dto.file.FileDTO;
import com.pamirs.takin.entity.domain.vo.file.FileDeleteVO;
import io.shulie.takin.utils.file.FileManagerHelper;
import io.shulie.takin.utils.json.JsonHelper;
import io.shulie.takin.web.common.constant.RemoteConstant;
import io.shulie.takin.web.common.domain.WebResponse;
import io.shulie.takin.web.common.http.HttpWebClient;
import io.shulie.takin.web.common.util.FileUtil;
import io.shulie.takin.web.common.vo.FileWrapperVO;
import io.shulie.takin.web.common.util.CommonUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author qianshui
 * @date 2020/4/17 下午5:50
 */
@RestController
@RequestMapping("/api/file")
@Api(tags = "文件管理")
@Slf4j
public class FileController {

    @Autowired
    private HttpWebClient httpWebClient;

    @Value("${file.upload.user.data.dir:/data/tmp}")
    private String fileDir;
    @Value("${file.upload.script.path:/data/nfs_dir}")
    private String fileScriptPath;

    @PostMapping("/upload")
    @ApiOperation(value = "文件上传")
    public WebResponse upload(List<MultipartFile> file) {
        if (file == null || file.size() == 0) {
            return WebResponse.fail("上传文件不能为空");
        }
        FileWrapperVO wrapperVO = new FileWrapperVO();
        wrapperVO.setFile(FileUtil.convertMultipartFileList(file));
        wrapperVO.setRequestUrl(RemoteConstant.FILE_UPLOAD_URL);
        wrapperVO.setHttpMethod(HttpMethod.POST);
        WebResponse webResponse = httpWebClient.requestFile(wrapperVO);
        FileUtil.deleteTempFile(file);
        return webResponse;
    }

    @PostMapping("/attachment/upload")
    @ApiOperation(value = "文件上传")
    public WebResponse uploadAttachment(List<MultipartFile> file) {
        if (file == null || file.size() == 0) {
            return WebResponse.fail("上传文件不能为空");
        }
        FileWrapperVO wrapperVO = new FileWrapperVO();
        wrapperVO.setFile(FileUtil.convertMultipartFileList(file));
        wrapperVO.setRequestUrl(RemoteConstant.FILE_UPLOAD_URL);
        wrapperVO.setHttpMethod(HttpMethod.POST);
        WebResponse webResponse = httpWebClient.requestFile(wrapperVO);
        FileUtil.deleteTempFile(file);
        String jsonString = JsonHelper.bean2Json(webResponse.getData());
        List<FileDTO> dtoList = JsonHelper.json2List(jsonString, FileDTO.class);
        if (CollectionUtils.isNotEmpty(dtoList)) {
            for (FileDTO fileDTO : dtoList) {
                fileDTO.setFileType(2);
            }
        }
        return WebResponse.success(dtoList);
    }

    @DeleteMapping
    @ApiOperation(value = "文件删除")
    public WebResponse delete(@RequestBody FileDeleteVO vo) {
        if (vo.getUploadId() == null) {
            return WebResponse.fail("删除文件不能为空");
        }
        vo.setRequestUrl(RemoteConstant.FILE_URL);
        vo.setHttpMethod(HttpMethod.DELETE);
        return httpWebClient.request(vo);
    }

    @ApiOperation("|_ 文件下载")
    @GetMapping("/download")
    public void download(@RequestParam("filePath") String filePath, HttpServletResponse response) {
        if (!this.filePathValidate(filePath)) {
            log.error("非法下载路径文件，禁止下载：{}", filePath);
            return;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("文件不存在，地址：{}", filePath);
            return;
        }
        CommonUtil.zeroCopyDownload(file, response);
    }

    @ApiOperation("文件下载")
    @GetMapping(value = "/downloadFileByPath")
    public void downloadFileByPath(@RequestParam("filePath") String filePath, HttpServletResponse response) {
        this.download(filePath, response);
        // 删除文件
        //cn.hutool.core.io.FileUtil.del(filePath);
    }

    /**
     * 文件路径是否管理策略
     *
     * @param filePath 文件路径
     * @return 是/否
     */
    private boolean filePathValidate(String filePath) {
        return this.pathInit().stream().anyMatch(filePath::startsWith);
    }

    /**
     * 文件路径初始化
     *
     * @return 文件路径列表
     */
    private List<String> pathInit() {
        List<String> arrayList = new ArrayList<>();
        // 新版
        //arrayList.add(ConfigServerHelper.getValueByKey(ConfigServerKeyEnum.TAKIN_FILE_UPLOAD_USER_DATA_DIR));
        //arrayList.add(ConfigServerHelper.getValueByKey(ConfigServerKeyEnum.TAKIN_FILE_UPLOAD_TMP_PATH));
        //arrayList.add(ConfigServerHelper.getValueByKey(ConfigServerKeyEnum.TAKIN_FILE_UPLOAD_SCRIPT_PATH));
        //arrayList.add(ConfigServerHelper.getValueByKey(ConfigServerKeyEnum.TAKIN_DATA_PATH));
        // 旧版
        arrayList.add(fileDir);
        arrayList.add(fileScriptPath);
        return arrayList;
    }

}
