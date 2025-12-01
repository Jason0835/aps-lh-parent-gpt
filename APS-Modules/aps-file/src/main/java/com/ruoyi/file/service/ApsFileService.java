package com.ruoyi.file.service;

import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

/**
 * APS本地文件上传下载接口
 *
 * @author ruoyi
 */
public interface ApsFileService {
    /**
     * 下载文件接口
     *
     * @param url,downloadType
     * @return byte[]
     * @throws Exception
     */
    public  byte[] downloadByteFile(String url,String downloadType) throws Exception;

    /**
     * byte[]文件上传接口
     *
     * @param byte[],uploadType
     * @return 文件路径
     * @throws Exception
     */
    public String uploadByteFile(byte[] data,String uploadType) throws Exception;

    /**
     * MultipartFile文件上传接口
     *
     * @param MultipartFile,uploadType
     * @return 文件路径
     * @throws Exception
     */
    public String uploadFile(MultipartFile file,String uploadType) throws Exception;
}
