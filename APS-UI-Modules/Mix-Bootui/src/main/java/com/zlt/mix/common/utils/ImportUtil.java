package com.zlt.mix.common.utils;

import com.alibaba.fastjson.JSONArray;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.GatewayConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.file.FileUploadUtils;
import com.ruoyi.common.core.utils.file.FileUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.file.api.service.ISimpleFileService;
import com.zlt.mix.common.core.utils.ExcelUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Objects;

/**
 * @author: Chen
 * @since: 2021/8/2 9:10
 */
public class ImportUtil {

    private static final Logger log = LoggerFactory.getLogger(com.zlt.mix.common.utils.ImportUtil.class);

    private static ISimpleFileService iSimpleFileService;

    //构造器注入iSimpleFileService（在配置类ExcelUtilConfig调用此构造方法）
    public ImportUtil(ISimpleFileService iSimpleFileService) {
        com.zlt.mix.common.utils.ImportUtil.iSimpleFileService = iSimpleFileService;
    }

    /**
     * 获取导入记录并上传导入的文件
     *
     * @param file           导入的文件
     * @param importFilePath 上传的导入文件地址
     * @param procedureCode  导入记录的生产代码
     * @param functionName   导入记录的功能名称
     * @return 导入记录
     */
    public static ImportLog getImportLogAndUploadFile(MultipartFile file, String importFilePath, String procedureCode, String functionName) {
        //初始值定义
        String templateFilePath = importFilePath + "/template" + DateUtils.dateTimeNow() + ".xlsx";
        FileOutputStream fileOutputStream = null;
        FileInputStream fileInputStream = null;
        ImportLog importLog = new ImportLog();
        try {
            //写临时文件
            File desc = new File(templateFilePath);
            if (!desc.exists()) {
                if (!desc.getParentFile().exists()) {
                    boolean mkdirs = desc.getParentFile().mkdirs();
                }
            }
            fileOutputStream = new FileOutputStream(templateFilePath);
            IOUtils.write(file.getBytes(), fileOutputStream);
            fileOutputStream.close();

            //将临时文件上传到指定路径
            fileInputStream = new FileInputStream(templateFilePath);
            FileItem fileItem = ExcelUtil.createFileItem(fileInputStream, "1.xlsx");
            MultipartFile tempFile = new CommonsMultipartFile(fileItem);
            String pathFileName = FileUploadUtils.upload(importFilePath, tempFile);
            // 保存导入记录
            importLog.setProcedureCode(procedureCode);
            importLog.setFunctionCode(Objects.requireNonNull(ServletUtils.getRequest()).getRequestURI().split("/")[2]);
            importLog.setFunctionName(functionName);
            importLog.setFileName(file.getOriginalFilename());
            importLog.setFileUrl(pathFileName);
            //删除临时文件
            FileUtils.deleteFile(templateFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        } finally {
            IOUtils.closeQuietly(fileOutputStream, fileInputStream);
        }
        return importLog;
    }

    /**
     * 获取导入记录并上传导入的文件
     *
     * @param procedureCode  导入记录的生产代码
     * @param functionName   导入记录的功能名称
     * @param fileName       原始文件名
     * @return 导入记录
     */
    public static ImportLog getImportLogAndUploadFile(byte[] data, String procedureCode, String functionName, String fileName) {
        //上传文件
        String pathFileName = "";
        try{
            // pathFileName = iSimpleFileService.uploadByteFile(data,"import");
        }catch (Exception e){
            log.error("File upload error:"+e);
        }
        //针对服务调用异常，网关会优先拦截处理异常，所以在此捕获不到异常，异常信息会返回到pathFileName,pathFileName设异常值会影响导出页面
        if (pathFileName.indexOf(":500") >= 0) {
            pathFileName = null;
        }

        // 保存导入记录
        ImportLog importLog = new ImportLog();
        importLog.setProcedureCode(procedureCode);
        importLog.setFunctionCode(Objects.requireNonNull(ServletUtils.getRequest()).getRequestURI().split("/")[2]);
        importLog.setFunctionName(functionName);
        importLog.setFileName(fileName);
        importLog.setFileUrl(pathFileName);
        return importLog;
    }

    /**
     * 更新导入成功数量，失败数量，并给返回的 msg格式化
     *
     * @param importLog  要更新的导入记录
     * @param ajaxResult 返回的结果
     */
    public static void updateImportLogAndFormatMsg(ImportLog importLog, AjaxResult ajaxResult, IImportLogService iImportLogService) {
        // 根据返回的 msg 更新成功数，失败数，并给 msg格式化
        String[] message = ajaxResult.get(GatewayConstants.MSG_TAG).toString().split(",");
        switch (message.length) {
            case 2:
                importLog.setSuccessNum(Long.valueOf(message[1]));
                importLog.setFailNum(0L);
                ajaxResult.put(GatewayConstants.MSG_TAG, StringUtils.format(message[0], message[1]));
                break;
            case 3:
                importLog.setSuccessNum(Long.valueOf(message[1]));
                importLog.setFailNum(Long.valueOf(message[2]));
                ajaxResult.put(GatewayConstants.MSG_TAG, StringUtils.format(message[0], message[1], message[2]));
                break;
            default:
                break;
        }
        iImportLogService.edit(importLog);
    }

    /**
     * 根据返回结果保存导入错误详细信息
     *
     * @param ajaxResult             返回结果
     * @param iImportErrorLogService 保存接口
     */
    public static void saveImportErrorLogs(AjaxResult ajaxResult, IImportErrorLogService iImportErrorLogService) {
        if (ajaxResult.get(Constants.CODE).equals(HttpStatus.ERROR)) {
            // 导入有失败，保存导入失败记录
            List<ImportErrorLog> importErrorLogs = StringUtils.cast(ajaxResult.get(Constants.DATA));
            if (CollectionUtils.isNotEmpty(importErrorLogs)) {
                String listTxt = JSONArray.toJSONString(importErrorLogs);
                List<ImportErrorLog> importErrorLogList = JSONArray.parseArray(listTxt, ImportErrorLog.class);
                iImportErrorLogService.insertImportErrorLogList(importErrorLogList);
            }
        }
    }
}
