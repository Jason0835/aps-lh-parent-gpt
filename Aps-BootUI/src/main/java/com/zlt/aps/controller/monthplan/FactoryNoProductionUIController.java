package com.zlt.aps.controller.monthplan;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.ai.DifyFileUploader;
import com.zlt.aps.monthplan.api.domain.entity.FactoryNoProduction;
import com.zlt.aps.monthplan.api.service.IFactoryNoProductionRemoteService;
import com.zlt.common.utils.PubUtil;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static com.zlt.aps.ai.AiContents.DIFY_API_KEY;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：FactoryNoProductionController.java
 * 描    述：基础数据-分厂不排产 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-26
 */
@Slf4j
@Api(tags = "基础数据-分厂不排产")
@Controller
@RequestMapping("/monthplan/factoryNoProduction")
public class FactoryNoProductionUIController extends BaseUIController<FactoryNoProduction> {

    @Autowired
    private IFactoryNoProductionRemoteService factoryNoProductionService;

    public static final String IMG_DIFY_API_KEY = "app-QHWU8MyWZHB4bAXXXf9tQp0Q";
    public static final String MP3_DIFY_API_KEY = "app-ROYlNjafJal1RbKWEufW8rIG";
    public static final String MP3 = "mp3";
    public static final String PNG = "png";
    /**
     * 根据条件查询分厂不排产品种列表
     */
    @ApiOperation("根据条件查询分厂不排产品种列表")
//    @RequiresPermissions("monthplan:factoryNoProduction:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(FactoryNoProduction entity) {
        return factoryNoProductionService.list(entity);
    }

    /**
     * 修改或新增分厂不排产品种
     */
    @ApiOperation("修改或新增分厂不排产品种")
//    @RequiresPermissions("monthplan:factoryNoProduction:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(FactoryNoProduction factoryNoProduction) {
        String productCode = factoryNoProduction.getProductCode();
        String factoryCode = factoryNoProduction.getFactoryCode();
        if (StringUtils.isBlank(productCode) || StringUtils.isBlank(factoryCode)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.factoryNoProduction.checkData.empty"));
        }
        if (factoryNoProduction.getId() != null) {
            return factoryNoProductionService.edit(factoryNoProduction);
        }
//        String unique = checkDocFactoryNotProductionEntityUnique(factoryNoProduction);
//        if (StringUtils.equals(unique, "1")) {
//            return AjaxResult.error(I18nUtil.getMessage("ui.factoryNoProduction.unique"));
//        }
        return factoryNoProductionService.add(factoryNoProduction);
    }

    /**
     * 删除分厂不排产品种
     */
    @ApiOperation("删除分厂不排产品种（id不为空）")
    @RequiresPermissions("monthplan:factoryNoProduction:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return factoryNoProductionService.remove(arr);
    }

    /**
     * 校验分厂不排产品种唯一性
     */
    @ApiOperation("校验分厂不排产品种唯一性")
    @PostMapping("/checkDocFactoryNotProductionEntityUnique")
    @ResponseBody
    public String checkDocFactoryNotProductionEntityUnique(FactoryNoProduction factoryNoProduction) {
        return factoryNoProductionService.checkFactoryNoProductionUnique(factoryNoProduction);
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<FactoryNoProduction> util = new ExcelUtil<>(FactoryNoProduction.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @Override
    public String getProcedureCode() {
        return "0";
    }


    @Override
    public String getFunctionName() {
        return getExportTemplateFileName();
    }

    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.factoryNoProduction.modelName");
    }

    @Override
    @ResponseBody
    @PostMapping({"/importData"})
    @ApiOperation("导入分厂不排产设定配置")
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名为空，无法转换");
        }

        String suffix = FileUtil.getSuffix(originalFilename);
        AjaxResult ajaxResult;
        File destFile;
        if (PNG.equals(suffix)){
            destFile = transferToFile(file, originalFilename);
            ajaxResult = importDataForAIWithImg(destFile);
        }else if (MP3.equals(suffix)){
            destFile = transferToFile(file, originalFilename);
            ajaxResult = importDataForAIWithMp3(destFile);
        }else {
            ajaxResult = factoryNoProductionService.importData(context, updateSupport);
        }
        return ajaxResult;
    }

    /**
     * 图片AI识别，导入
     * @param tempFile
     * @return
     * @throws IOException
     */
    private AjaxResult importDataForAIWithImg(File tempFile) throws IOException {
        AjaxResult ajaxResult;

        String fileId = DifyFileUploader.uploadFile(tempFile,IMG_DIFY_API_KEY);
        System.out.println("文件上传成功，File ID: " + fileId);

        // 使用上传的文件调用LLM
        String response = DifyFileUploader.callLLMWithFile(fileId,null,IMG_DIFY_API_KEY);
        System.out.println("LLM处理结果: " + response);
        if(response.startsWith("{") && response.endsWith("}")) {
            response = response.substring(1, response.length()-1);
        }
        List<FactoryNoProduction> factoryNoProductionList = JSONUtil.toList(response,FactoryNoProduction.class);
        ajaxResult = factoryNoProductionService.importDataForAI(factoryNoProductionList);
        return ajaxResult;
    }

    /**
     * 音频AI识别，导入
     * @param tempFile
     * @return
     * @throws IOException
     */
    private AjaxResult importDataForAIWithMp3(File tempFile) throws IOException {
        AjaxResult ajaxResult;

        String fileId = DifyFileUploader.uploadFile(tempFile,MP3_DIFY_API_KEY);
        System.out.println("文件上传成功，File ID: " + fileId);

        // 使用上传的文件调用LLM
        String response = DifyFileUploader.callLLMWithFile(fileId,null,MP3_DIFY_API_KEY,"audio");
        System.out.println("LLM处理结果: " + response);
        if(response.startsWith("{") && response.endsWith("}")) {
            response = response.substring(1, response.length()-1);
        }
        List<FactoryNoProduction> factoryNoProductionList = JSONUtil.toList(response,FactoryNoProduction.class);
        ajaxResult = factoryNoProductionService.importDataForAI(factoryNoProductionList);
        return ajaxResult;
    }

    /**
     * 转换文件类型
     * @param file
     * @param originalFilename
     * @return
     * @throws IOException
     */
    private File transferToFile(MultipartFile file, String originalFilename) throws IOException {
        // 创建临时文件
        File tempFile = FileUtil.createTempFile(
                FileUtil.getPrefix(originalFilename),
                "." + FileUtil.getSuffix(originalFilename),
                null,
                true
        );

        // 获取文件字节数组并写入临时文件
        byte[] bytes = file.getBytes();
        FileUtil.writeBytes(bytes, tempFile);
        return tempFile;
    }
}
