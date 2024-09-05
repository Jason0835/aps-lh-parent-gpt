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
 * @date 2021-07-21
 */
@Api(tags = "定点机台")
@Controller
@RequestMapping("/cx/cxSpecifyMachine")
public class CxSpecifyMachineController extends BaseController {

    @Autowired
    private ICxSpecifyMachineService iCxSpecifyMachine1Service;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    private String prefix = "cx/cxSpecifyMachine";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:cxSpecifyMachine:view")
    @GetMapping()
    public String operlog() {
        return prefix + "/cxSpecifyMachine";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxSpecifyMachine1", new CxSpecifyMachine());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxSpecifyMachine1", iCxSpecifyMachine1Service.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询定点机台列表
     */
    @ApiOperation("根据条件查询定点机台列表")
    @RequiresPermissions("cx:cxSpecifyMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxSpecifyMachine entity) {
        return iCxSpecifyMachine1Service.list(entity);
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
        if (UserConstants.NOT_UNIQUE.equals(iCxSpecifyMachine1Service.checkCxSpecifyMachine1Unique(cxSpecifyMachine))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.cx.cxSpecifyMachine.UniqueCheck"));
        }
        if (cxSpecifyMachine.getId() != null) {
            ajaxResult = iCxSpecifyMachine1Service.edit(cxSpecifyMachine);
        } else {
            ajaxResult = iCxSpecifyMachine1Service.add(cxSpecifyMachine);
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
        return iCxSpecifyMachine1Service.remove(arr);
    }

    /**
     * 导出定点机台
     */
    @ApiOperation("导出定点机台")
    @RequiresPermissions("cx:cxSpecifyMachine:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxSpecifyMachine cxSpecifyMachine) throws IOException {
        List<CxSpecifyMachine> list = iCxSpecifyMachine1Service.getList(cxSpecifyMachine);
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
        AjaxResult ajaxResult = iCxSpecifyMachine1Service.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }


}
