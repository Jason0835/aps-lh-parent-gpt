package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.CxSpecifyMachine;
import com.zlt.aps.cx.api.service.ICxSpecifyMachineService;
import com.zlt.aps.template.cx.CxSpecifyMachineTemp;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 定点机台Controller
 *
 * @author zlt
 * @date 2025-02-25
 */
@Api(tags = "定点机台")
@Controller
@RequestMapping("/cx/cxSpecifyMachine")
public class CxSpecifyMachineController extends BaseController {

    @Autowired
    private ICxSpecifyMachineService cxSpecifyMachineService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 根据条件查询定点机台列表
     */
    @ApiOperation("根据条件查询定点机台列表")
    @RequiresPermissions("cx:cxSpecifyMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxSpecifyMachine entity) {
        return cxSpecifyMachineService.list(entity);
    }

    /**
     * 修改或新增定点机台
     */
    @ApiOperation("修改或新增定点机台")
    @RequiresPermissions("cx:cxSpecifyMachine:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxSpecifyMachine cxSpecifyMachine) {
        AjaxResult ajaxResult = null;
        if (UserConstants.NOT_UNIQUE.equals(cxSpecifyMachineService.checkCxSpecifyMachine1Unique(cxSpecifyMachine))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cx.cxSpecifyMachine.UniqueCheck"));
        }
        if (cxSpecifyMachine.getId() != null) {
            ajaxResult = cxSpecifyMachineService.edit(cxSpecifyMachine);
        } else {
            ajaxResult = cxSpecifyMachineService.add(cxSpecifyMachine);
        }
        return ajaxResult;
    }

    /**
     * 删除定点机台
     */
    @ApiOperation("删除定点机台（id不为空）")
    @RequiresPermissions("cx:cxSpecifyMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cxSpecifyMachineService.remove(arr);
    }

    /**
     * 导出定点机台
     */
    @ApiOperation("导出定点机台")
    @RequiresPermissions("cx:cxSpecifyMachine:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxSpecifyMachine cxSpecifyMachine) throws IOException {
        List<CxSpecifyMachine> list = cxSpecifyMachineService.getList(cxSpecifyMachine);
        ExcelUtil<CxSpecifyMachine> util = new ExcelUtil(CxSpecifyMachine.class);
        String fileName = I18nUtil.getMessage("ui.cx.cxSpecifyMachine.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, cxSpecifyMachine.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载模板
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.cx.cxSpecifyMachine.export.fileName");
        ExcelUtil<CxSpecifyMachineTemp> util = new ExcelUtil<>(CxSpecifyMachineTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("cx:cxSpecifyMachine:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.cx.cxSpecifyMachine.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<CxSpecifyMachine> util = new ExcelUtil<>(CxSpecifyMachine.class);
        List<CxSpecifyMachine> list = util.importExcel(in);
        AjaxResult ajaxResult = cxSpecifyMachineService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
