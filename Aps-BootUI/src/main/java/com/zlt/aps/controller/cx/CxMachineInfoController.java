package com.zlt.aps.controller.cx;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import com.zlt.aps.cx.api.service.ICxMachineInfoService;
import com.zlt.aps.template.cx.CxMachineInfoTemp;
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
 * 成型机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "成型机台信息维护接口")
@Controller
@RequestMapping("/cx/machine")
public class CxMachineInfoController extends BaseController {
    @Autowired
    private ICxMachineInfoService cxMachineInfoService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    private String prefix = "cx/machine";

    /**
     * 列表跳转至machine页面
     *
     * @return
     */
    @RequiresPermissions("cx:machine:view")
    @GetMapping()
    public String operlog() {

        return prefix + "/machine";
    }

    /**
     * 新增跳转至add页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxMachineInfo", new CxMachineInfo());
        return prefix + "/edit";
    }

    /**
     * 查询成型机台信息列表
     */
    @ApiOperation("根据条件查询成型机台信息")
    @RequiresPermissions("cx:machine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxMachineInfo cxMachineInfo) {
        return cxMachineInfoService.list(cxMachineInfo);
    }

    /**
     * 修改成型机台信息前获取对象
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxMachineInfo", cxMachineInfoService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改成型机台信息（id不为空）")
    @RequiresPermissions("cx:machine:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxMachineInfo cxMachineInfo) {
        AjaxResult ajaxResult = null;
        //id为空则是新增操作，否则是编辑
        if (cxMachineInfo.getId() != null) {
            ajaxResult = cxMachineInfoService.edit(cxMachineInfo);
        } else {
            ajaxResult = cxMachineInfoService.add(cxMachineInfo);
        }
        return ajaxResult;
    }

    /**
     * 删除成型机台信息
     */
    @ApiOperation("删除成型机台信息（id不为空）")
    @RequiresPermissions("cx:machine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return cxMachineInfoService.remove(arr);
    }

    @ApiOperation("根据code判断成型机台是否已经存在")
    @PostMapping("/checkMachineCodeUnique")
    @ResponseBody
    public String checkMachineCodeUnique(CxMachineInfo cxMachineInfo) {
        String result = cxMachineInfoService.checkMachineCodeUnique(cxMachineInfo);
        return result;
    }

    @ApiOperation("导出内衬机台信息")
    @RequiresPermissions("cx:machine:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxMachineInfo machineInfo) throws IOException {
        List<CxMachineInfo> list = cxMachineInfoService.exportList(machineInfo);
        ExcelUtil<CxMachineInfo> util = new ExcelUtil(CxMachineInfo.class);
        String fileName = I18nUtil.getMessage("ui.cx.machine.export.sheetName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, machineInfo.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载模板
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.cx.machine.export.sheetName");
        ExcelUtil<CxMachineInfoTemp> util = new ExcelUtil<>(CxMachineInfoTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("cx:machine:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.cx.machine.export.sheetName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<CxMachineInfo> util = new ExcelUtil<>(CxMachineInfo.class);
        List<CxMachineInfo> list = util.importExcel(in);
        AjaxResult ajaxResult = cxMachineInfoService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
