package com.ruoyi.file.service;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.file.MimeTypeUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * APS本地文件上传下载实现类
 *
 * @author ruoyi
 */
@Service
public class ApsFileServiceImpl implements ApsFileService {

    private static final Logger log = LoggerFactory.getLogger(ApsFileServiceImpl.class);

    @Value("${uploadFilePath.exportFile}")
    private String uploadFilePath;

    @Value("${uploadFilePath.importFile}")
    private String importFilePath;

    @Value("${uploadFilePath.imageFile}")
    private String imageFilePath;

    /**
     * 下载byte[]文件
     *
     * @param url,downloadType
     * @param downloadType
     * @return
     * @throws Exception
     */
    @Override
    public byte[] downloadByteFile(String url, String downloadType)  {
        byte[] buffer = null;
        File file = null;
        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        try {

            if ("export".equals(downloadType)) {
                if(!new File(uploadFilePath).exists()) {
                    //目录不存在，则先创建
                    new File(uploadFilePath).mkdirs();
                }
                file = new File(uploadFilePath + url);
            } else if ("import".equals(downloadType)) {
                if(!new File(importFilePath).exists()) {
                    //目录不存在，则先创建
                    new File(importFilePath).mkdirs();
                }
                file = new File(importFilePath + url);
            } else if ("image".equals(downloadType)) {
                if(!new File(imageFilePath).exists()) {
                    //目录不存在，则先创建
                    new File(imageFilePath).mkdirs();
                }
                file = new File(imageFilePath + url);
            }
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            buffer = bos.toByteArray();
        } catch (Exception e) {
            log.error(e.getMessage());
        } finally {
            try {
                fis.close();
                bos.close();
            } catch (IOException e) {
            }
        }
        return buffer;
    }

    /**
     * 上传byte[]文件
     *
     * @param data
     * @return
     * @throws Exception
     */
    @Override
    public String uploadByteFile(byte[] data, String uploadType) throws Exception {
        String pathFileName = "/" + DateUtils.getDate() + "/" + UUID.randomUUID().toString() + ".xlsx";
        String absolutePath = uploadFilePath + pathFileName;

        if ("export".equals(uploadType)) {
            absolutePath = uploadFilePath + pathFileName;
        } else if ("import".equals(uploadType)) {
            absolutePath = importFilePath + pathFileName;
        } else if ("image".equals(uploadType)) {
            absolutePath = imageFilePath + pathFileName;
        }

        FileOutputStream fileOutputStream = null;
        try {
            File desc = new File(absolutePath);
            if (!desc.exists()) {
                if (!desc.getParentFile().exists()) {
                    desc.getParentFile().mkdirs();
                }
            }
            fileOutputStream = new FileOutputStream(absolutePath);
            IOUtils.write(data, fileOutputStream);
            fileOutputStream.close();
        } catch (Exception e) {
            throw new Exception("File upload failed:"+e);
        } finally {
            IOUtils.closeQuietly(fileOutputStream);
        }
        return pathFileName;
    }

    /**
     * MultipartFile文件上传接口
     *
     * @param MultipartFile,uploadType
     * @return 文件路径
     * @throws Exception
     */
    public String uploadFile(MultipartFile file, String uploadType) throws Exception {

        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        if (StringUtils.isEmpty(extension)) {
            extension = MimeTypeUtils.getExtension(file.getContentType());
        }

        String pathFileName = "/" + DateUtils.getDate() + "/" + UUID.randomUUID().toString() + "." + extension;
        String absolutePath = uploadFilePath + pathFileName;

        if ("export".equals(uploadType)) {
            absolutePath = uploadFilePath + pathFileName;
        } else if ("import".equals(uploadType)) {
            absolutePath = importFilePath + pathFileName;
        } else if ("image".equals(uploadType)) {
            absolutePath = imageFilePath + pathFileName;
        }

        FileOutputStream fileOutputStream = null;
        try {
            File desc = new File(absolutePath);
            if (!desc.exists()) {
                if (!desc.getParentFile().exists()) {
                    desc.getParentFile().mkdirs();
                }
            }
            fileOutputStream = new FileOutputStream(absolutePath);
            IOUtils.write(file.getBytes(), fileOutputStream);
            fileOutputStream.close();
        } catch (Exception e) {
            throw new Exception("File upload failed:"+e);
        } finally {
            IOUtils.closeQuietly(fileOutputStream);
        }
        return pathFileName;
    }

}
