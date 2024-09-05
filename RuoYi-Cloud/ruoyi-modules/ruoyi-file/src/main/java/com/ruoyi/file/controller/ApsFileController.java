package com.ruoyi.file.controller;

import com.ruoyi.file.service.ApsFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * APS本地文件上传下载
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/file")
public class ApsFileController {

    private static final Logger log = LoggerFactory.getLogger(ApsFileController.class);

    @Autowired
    private ApsFileService apsFileService;

    @GetMapping("/file/downloadByteFile")
    public byte[] downloadByteFile(@RequestParam("url") String url, @RequestParam("downloadType") String downloadType) throws Exception {
        return apsFileService.downloadByteFile(url, downloadType);
    }

    @PostMapping("/file/uploadByteFile")
    public String uploadByteFile(@RequestBody byte[] data, @RequestParam("uploadType") String uploadType) throws Exception {
        return apsFileService.uploadByteFile(data, uploadType);
    }

    @PostMapping("/file/uploadFile")
    public String uploadFile(@RequestPart(value = "file") MultipartFile file, @RequestParam("uploadType") String uploadType) throws Exception {
        return apsFileService.uploadFile(file, uploadType);
    }

}