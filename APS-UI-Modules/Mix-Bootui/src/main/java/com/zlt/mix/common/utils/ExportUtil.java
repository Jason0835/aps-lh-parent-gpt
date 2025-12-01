package com.zlt.mix.common.utils;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.file.api.service.ISimpleFileService;
import com.zlt.mix.common.core.utils.ExcelUtil;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ExportUtil {

    private static final Logger log = LoggerFactory.getLogger(com.zlt.mix.common.utils.ExportUtil.class);


    private static ISimpleFileService iSimpleFileService;

    //构造器注入iSimpleFileService（在配置类ExcelUtilConfig调用此构造方法）
    public ExportUtil(ISimpleFileService iSimpleFileService) {
        com.zlt.mix.common.utils.ExportUtil.iSimpleFileService = iSimpleFileService;
    }


    /**
     * 根据Workbook上传并且导出Excel
     *
     * @param response       浏览器响应
     * @param workbook       生成Excel的工作簿
     * @param fileName       导出记录的文件名称
     * @param exportParams   导出记录的查询参数
     * @param procedureCode  导出记录的工序
     * @return ExportLog 导出记录
     */
    public static ExportLog uploadAndExportExcel(HttpServletResponse response, Workbook workbook, String fileName, String exportParams, String procedureCode) {
        ExportLog exportLog = new ExportLog();
        OutputStream outputStream = null;
        ByteArrayOutputStream bos =null;
        try {
            //workbook转字节数组
            bos = new ByteArrayOutputStream();
            workbook.write(bos);
            byte[] data = bos.toByteArray();

            if (data != null) {
                //上传文件
                String pathFileName = "" ;
                try {
                    pathFileName = iSimpleFileService.uploadByteFile(data, "export");
                } catch (Exception e) {
                    log.error("File upload error:" + e);
                }
                //针对服务调用异常，网关会优先拦截处理异常，所以在此捕获不到异常，异常信息会返回到pathFileName,pathFileName设异常值会影响导出页面
                if (pathFileName.indexOf(":500") >= 0) {
                    pathFileName = null;
                }

                //将字节数组写到浏览器
                outputStream = response.getOutputStream();
                IOUtils.write(data, outputStream);
                outputStream.close();

                //新增文件上传记录
                exportLog.setProcedureCode(procedureCode);
                exportLog.setExportParams(exportParams);
                String uri = ServletUtils.getRequest().getRequestURI();
                exportLog.setFunctionCode(uri.split("/")[2]);
                exportLog.setFunctionName(fileName);
                exportLog.setFileName(fileName + ".xlsx");
                exportLog.setFileUrl(pathFileName);
            }

        } catch (Exception e) {
            log.error("上传文件异常", e);
        } finally {
            IOUtils.closeQuietly(outputStream,bos,workbook);
        }
        return exportLog;
    }

    /**
     * 根据byte[]上传并导出Excel文件
     *
     * @param response       浏览器响应
     * @param data           字节流
     * @param fileName       导出指定的文件名，不需要后缀
     * @param exportParams   导出记录的查询参数
     * @param procedureCode  导出记录的工序
     * @return ExportLog 导出记录
     */
    public static ExportLog uploadAndExportExcelByByte(HttpServletResponse response, byte[] data, String fileName, String exportParams, String procedureCode) throws IOException {
        //初始值定义
        OutputStream outputStream = null;
        ExportLog exportLog = new ExportLog();
        ExcelUtil.setResponseHeader(response, fileName);
        try {
            //上传文件
            String pathFileName = "" ;
            try {
                pathFileName = iSimpleFileService.uploadByteFile(data, "export");
            } catch (Exception e) {
                log.error("File upload error:" + e);
            }
            //针对服务调用异常，网关会优先拦截处理异常，所以在此捕获不到异常，异常信息会返回到pathFileName,pathFileName设异常值会影响导出页面
            if (pathFileName.indexOf(":500") >= 0) {
                pathFileName = null;
            }

            //将文件写到浏览器
            outputStream = response.getOutputStream();
            IOUtils.write(data, outputStream);
            outputStream.close();

            //新增文件上传记录
            exportLog.setProcedureCode(procedureCode);
            exportLog.setExportParams(exportParams);
            String uri = ServletUtils.getRequest().getRequestURI();
            exportLog.setFunctionCode(uri.split("/")[2]);
            exportLog.setFunctionName(fileName);
            exportLog.setFileName(fileName + "-" + DateUtils.dateTimeNow() + ".xlsx");
            exportLog.setFileUrl(pathFileName);
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
        return exportLog;
    }

    /**
     * 上传文件
     * @param data
     * @return
     * @throws IOException
     */
    public static String uploadExcelByByte(byte[] data) throws IOException {
        String pathFileName = "" ;
        if(data==null){
            return null;
        }
        try {
            pathFileName = iSimpleFileService.uploadByteFile(data, "export");
        } catch (Exception e) {
            log.error("File upload error:" + e);
        }
        return pathFileName;
    }

}
