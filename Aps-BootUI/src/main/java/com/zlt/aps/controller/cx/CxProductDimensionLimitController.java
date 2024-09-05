package com.zlt.aps.controller.cx;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Value;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.zlt.aps.common.utils.ImportUtil;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.io.*;
import com.ruoyi.common4ui.utils.file.FileUtils;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.zlt.file.encryptbyll.FileEncryptUtils;

import com.zlt.aps.cx.api.domain.entity.CxProductDimensionLimit;
import com.zlt.aps.cx.api.service.ICxProductDimensionLimitService;

/**
 * 成型投产班次同寸口硫化班次限定设置Controller
 * @author zlt
 * @date 2022-01-08
 */
@Api(tags = "成型投产班次同寸口硫化班次限定设置")
@Controller
@RequestMapping("/cx/dimensionLimit")
public class CxProductDimensionLimitController extends BaseController {

    @Autowired
    private ICxProductDimensionLimitService iCxProductDimensionLimitService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "cx/dimensionLimit";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:dimensionLimit:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/dimensionLimit";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxProductDimensionLimit", new CxProductDimensionLimit());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxProductDimensionLimit", iCxProductDimensionLimitService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询成型投产班次同寸口硫化班次限定设置列表
     */
    @ApiOperation("根据条件查询成型投产班次同寸口硫化班次限定设置列表")
    @RequiresPermissions("cx:dimensionLimit:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxProductDimensionLimit entity) {
        return iCxProductDimensionLimitService.list(entity);
    }

    /**
     * 修改或新增成型投产班次同寸口硫化班次限定设置
     */
    @ApiOperation("修改或新增成型投产班次同寸口硫化班次限定设置")
    @RequiresPermissions("cx:dimensionLimit:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxProductDimensionLimit cxProductDimensionLimit) {
        AjaxResult ajaxResult = null;

        //唯一性校验
        if(UserConstants.NOT_UNIQUE.equals(iCxProductDimensionLimitService.checkCxProductDimensionLimitUnique(cxProductDimensionLimit))){
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.dimensionLimit.checkUnique"));
        }
        if (cxProductDimensionLimit.getId() != null){
            ajaxResult = iCxProductDimensionLimitService.edit(cxProductDimensionLimit);
        } else{
            ajaxResult = iCxProductDimensionLimitService.add(cxProductDimensionLimit);
        }
        return ajaxResult;
    }

    /**
     * 删除成型投产班次同寸口硫化班次限定设置
     */
    @ApiOperation("删除成型投产班次同寸口硫化班次限定设置（id不为空）")
    @RequiresPermissions("cx:dimensionLimit:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxProductDimensionLimitService.remove(arr);
    }

    /**
     * 校验成型投产班次同寸口硫化班次限定设置唯一性
     */
    @ApiOperation("校验成型投产班次同寸口硫化班次限定设置唯一性")
    @PostMapping("/checkCxProductDimensionLimitUnique")
    @ResponseBody
    public String checkCxProductDimensionLimitUnique(CxProductDimensionLimit cxProductDimensionLimit) {
        return iCxProductDimensionLimitService.checkCxProductDimensionLimitUnique(cxProductDimensionLimit);
    }

    /**
     * 导出成型投产班次同寸口硫化班次限定设置
     */
    @ApiOperation("导出成型投产班次同寸口硫化班次限定设置")
    @RequiresPermissions("cx:dimensionLimit:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,CxProductDimensionLimit cxProductDimensionLimit) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.dimensionLimit.modelName");
        List<CxProductDimensionLimit> list = iCxProductDimensionLimitService.getList(cxProductDimensionLimit);
        ExcelUtil<CxProductDimensionLimit> util = new ExcelUtil<>(CxProductDimensionLimit. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, cxProductDimensionLimit.toString(),"ApsConstant.PROCEDURE_CODE_XXX");
        iExportLogService.add(exportLog);
    }

    /**
     * 下载导入模板
     *
     * @param response 下载的模板文件
     * @throws IOException 异常
     */
    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.dimensionLimit.modelName");
        ExcelUtil<CxProductDimensionLimit> util = new ExcelUtil<>(CxProductDimensionLimit.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file 要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("cx:dimensionLimit:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, "ApsConstant.PROCEDURE_CODE_XXX",
                I18nUtil.getMessage("ui.data.column.dimensionLimit.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<CxProductDimensionLimit> util = new ExcelUtil<>(CxProductDimensionLimit.class);
        InputStream in = new ByteArrayInputStream(data);
        List<CxProductDimensionLimit> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxProductDimensionLimitService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
