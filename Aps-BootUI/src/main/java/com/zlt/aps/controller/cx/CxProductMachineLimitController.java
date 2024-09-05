package com.zlt.aps.controller.cx;

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

import com.zlt.aps.cx.api.domain.entity.CxProductMachineLimit;
import com.zlt.aps.cx.api.service.ICxProductMachineLimitService;

/**
 * 成型投产班次同机台硫化班次限定设置Controller
 * @author zlt
 * @date 2022-01-08
 */
@Api(tags = "成型投产班次同机台硫化班次限定设置")
@Controller
@RequestMapping("/cx/machineLimit")
public class CxProductMachineLimitController extends BaseController {

    @Autowired
    private ICxProductMachineLimitService iCxProductMachineLimitService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "cx/machineLimit";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:machineLimit:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/machineLimit";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxProductMachineLimit", new CxProductMachineLimit());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxProductMachineLimit", iCxProductMachineLimitService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询成型投产班次同机台硫化班次限定设置列表
     */
    @ApiOperation("根据条件查询成型投产班次同机台硫化班次限定设置列表")
    @RequiresPermissions("cx:machineLimit:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxProductMachineLimit entity) {
        return iCxProductMachineLimitService.list(entity);
    }

    /**
     * 修改或新增成型投产班次同机台硫化班次限定设置
     */
    @ApiOperation("修改或新增成型投产班次同机台硫化班次限定设置")
    @RequiresPermissions("cx:machineLimit:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxProductMachineLimit cxProductMachineLimit) {
        AjaxResult ajaxResult = null;
        //唯一性校验
        if(UserConstants.NOT_UNIQUE.equals(iCxProductMachineLimitService.checkCxProductMachineLimitUnique(cxProductMachineLimit))){
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.machineLimit.checkUnique"));
        }
        if (cxProductMachineLimit.getId() != null){
            ajaxResult = iCxProductMachineLimitService.edit(cxProductMachineLimit);
        } else{
            ajaxResult = iCxProductMachineLimitService.add(cxProductMachineLimit);
        }
        return ajaxResult;
    }

    /**
     * 删除成型投产班次同机台硫化班次限定设置
     */
    @ApiOperation("删除成型投产班次同机台硫化班次限定设置（id不为空）")
    @RequiresPermissions("cx:machineLimit:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxProductMachineLimitService.remove(arr);
    }

    /**
     * 校验成型投产班次同机台硫化班次限定设置唯一性
     */
    @ApiOperation("校验成型投产班次同机台硫化班次限定设置唯一性")
    @PostMapping("/checkCxProductMachineLimitUnique")
    @ResponseBody
    public String checkCxProductMachineLimitUnique(CxProductMachineLimit cxProductMachineLimit) {
        return iCxProductMachineLimitService.checkCxProductMachineLimitUnique(cxProductMachineLimit);
    }

    /**
     * 导出成型投产班次同机台硫化班次限定设置
     */
    @ApiOperation("导出成型投产班次同机台硫化班次限定设置")
    @RequiresPermissions("cx:machineLimit:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,CxProductMachineLimit cxProductMachineLimit) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.machineLimit.modelName");
        List<CxProductMachineLimit> list = iCxProductMachineLimitService.getList(cxProductMachineLimit);
        ExcelUtil<CxProductMachineLimit> util = new ExcelUtil<>(CxProductMachineLimit. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, cxProductMachineLimit.toString(),"ApsConstant.PROCEDURE_CODE_XXX");
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
        String fileName = I18nUtil.getMessage("ui.data.column.machineLimit.modelName");
        ExcelUtil<CxProductMachineLimit> util = new ExcelUtil<>(CxProductMachineLimit.class);
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
    @RequiresPermissions("cx:machineLimit:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, "ApsConstant.PROCEDURE_CODE_XXX",
                I18nUtil.getMessage("ui.data.column.machineLimit.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<CxProductMachineLimit> util = new ExcelUtil<>(CxProductMachineLimit.class);
        InputStream in = new ByteArrayInputStream(data);
        List<CxProductMachineLimit> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxProductMachineLimitService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
