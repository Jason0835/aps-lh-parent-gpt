package com.zlt.aps.controller.cd15;

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
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.service.ICd15MachineInfoService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.cd15.Cd15MachineInfoTemp;
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
 * 15°裁断机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "15°裁断机台信息维护接口")
@Controller
@RequestMapping("/cd15/machine")
public class Cd15MachineInfoController extends BaseController {
    @Autowired
    private ICd15MachineInfoService machineInfoService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    private String prefix = "cd15/machine";

    /**
     * 列表跳转至machine页面
     *
     * @return
     */
    @RequiresPermissions("cd15:machine:view")
    @GetMapping()
    public String operlog() {

        return prefix + "/machine";
    }

    /**
     * 新增跳转至add页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("machineInfo", new Cd15MachineInfo());
        return prefix + "/edit";
    }

    /**
     * 查询15°裁断机台信息列表
     */
    @ApiOperation("根据条件查询15°裁断机台信息")
    @RequiresPermissions("cd15:machine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15MachineInfo machineInfo) {
        return machineInfoService.list(machineInfo);
    }

    /**
     * 修改15°裁断机台信息前获取对象
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("machineInfo", machineInfoService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改15°裁断机台信息（id不为空）")
    @RequiresPermissions("cd15:machine:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(Cd15MachineInfo machineInfo) {
        AjaxResult ajaxResult = null;
        //id为空则是新增操作，否则是编辑
        if (machineInfo.getId() != null) {
            ajaxResult = machineInfoService.edit(machineInfo);
        } else {
            ajaxResult = machineInfoService.add(machineInfo);
        }
        return ajaxResult;
    }

    /**
     * 删除15°裁断机台信息
     */
    @ApiOperation("删除15°裁断机台信息（id不为空）")
    @RequiresPermissions("cd15:machine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return machineInfoService.remove(arr);
    }

    @ApiOperation("根据code判断15°裁断机台是否已经存在")
    @PostMapping("/checkMachineCodeUnique")
    @ResponseBody
    public String checkMachineCodeUnique(Cd15MachineInfo machineInfo) {
        String result = machineInfoService.checkMachineCodeUnique(machineInfo);
        return result;
    }

    @ApiOperation("导出15°裁断机台信息")
    @RequiresPermissions("cd15:machine:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd15MachineInfo machineInfo) throws IOException {
        List<Cd15MachineInfo> list = machineInfoService.exportList(machineInfo);
        ExcelUtil<Cd15MachineInfo> util = new ExcelUtil(Cd15MachineInfo.class);
        String fileName = I18nUtil.getMessage("ui.cd15.machine.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, machineInfo.toString(), ApsConstant.PROCEDURE_CODE_CD15);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.cd15.machine.export.fileName");
        ExcelUtil<Cd15MachineInfoTemp> util = new ExcelUtil<>(Cd15MachineInfoTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    @RequiresPermissions("cd15:machine:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<Cd15MachineInfo> util = new ExcelUtil<>(Cd15MachineInfo.class);
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD15,
                I18nUtil.getMessage("ui.cd15.machine.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        List<Cd15MachineInfo> list = util.importExcel(in);
        // 导入
        AjaxResult ajaxResult = machineInfoService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
