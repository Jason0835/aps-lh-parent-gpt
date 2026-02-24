package com.tlt.aps.utils;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.*;
import java.nio.file.Files;

/**
 * 文件转换类
 */
public class MultipartFileUtils {

    /**
     * file转MultipartFile
     * @param file
     * @return
     */
    public static MultipartFile getMultipartFile(File file,String fileName) {
        FileItem item = new DiskFileItemFactory().createItem("file"
                , MediaType.MULTIPART_FORM_DATA_VALUE
                , true
                , fileName);
        try (InputStream input = new FileInputStream(file);
             OutputStream os = item.getOutputStream()) {
            // 流转移
            IOUtils.copy(input, os);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid file: " + e, e);
        }
        item.setFieldName(fileName);
        return new CommonsMultipartFile(item);
    }
    /**
     * file转MultipartFile
     * @param file
     * @return
     */
    public static MultipartFile getMultipartFile(File file) {
        FileItem item = new DiskFileItemFactory().createItem("file"
                , MediaType.MULTIPART_FORM_DATA_VALUE
                , true
                , file.getName());
        try (InputStream input = new FileInputStream(file);
             OutputStream os = item.getOutputStream()) {
            // 流转移
            IOUtils.copy(input, os);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid file: " + e, e);
        }

        return new CommonsMultipartFile(item);
    }

    /**
     * 流转MultipartFile
     * @param inputStream
     * @param tempFileName
     * @return
     * @throws IOException
     */
    public static MultipartFile convertInputStreamToMultipartFile(InputStream inputStream,String tempFileName) throws IOException {
        // 创建一个临时文件
        File tempFile = File.createTempFile(tempFileName, ".tmp");
        try (OutputStream outputStream = Files.newOutputStream(tempFile.toPath())) {
            byte[] buffer = new byte[1024];
            int length;
            // 从 InputStream 中读取数据，并写入到临时文件中
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        }
       return getMultipartFile(tempFile);
    }


}
