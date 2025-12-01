package com.ruoyi.file.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * APS本地文件上传下载对外接口
 *
 * @author ruoyi
 */
@FeignClient(contextId = "iApsFileService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.file:file}")
public interface IApsFileService {

    /**
     * 下载文件
     *
     * @param workbook,downloadType
     * @return byte[]
     */
    @GetMapping("/file/downloadByteFile")
    public byte[] downloadByteFile(@RequestParam("url") String url, @RequestParam("downloadType") String downloadType) throws Exception;

    /**
     * 上传byte[]文件
     *
     * @param data,uploadType
     * @return FilePathName
     */
    @PostMapping("/file/uploadByteFile")
    public String uploadByteFile(@RequestBody byte[] data, @RequestParam("uploadType") String uploadType) throws Exception;

    /**
     * 上传MultipartFile文件
     *
     * @param file,uploadType
     * @return FilePathName
     */
    @PostMapping(value = "/file/uploadFile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadFile(@RequestPart(value = "file") MultipartFile file, @RequestParam("uploadType") String uploadType) throws Exception;

}
