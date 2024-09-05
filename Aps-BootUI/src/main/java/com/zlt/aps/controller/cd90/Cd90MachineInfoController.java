package com.zlt.aps.controller.cd90;

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
import com.zlt.aps.cd90.api.domain.entity.Cd90MachineInfo;
import com.zlt.aps.cd90.api.service.ICd90MachineInfoService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.cd90.Cd90MachineInfoTemp;
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
 * 90°裁断机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "90°裁断机台信息维护接口")
@Controller
@RequestMapping("/cd90/machine")
public class Cd90MachineInfoController extends BaseController {
    @Autowired
    private ICd90MachineInfoService machineInfoService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    private String prefix = "cd90/machine";

    /**
     * 列表跳转至machine页面
     *
     * @return
     */
    @RequiresPermissions("cd90:machine:view")
    @GetMapping()
    public String operlog() {

        return prefix + "/machine";
    }

    /**
     * 新增跳转至add页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("machineInfo", new Cd90MachineInfo());
        return prefix + "/edit";
    }

    /**
     * 查询90°裁断机台信息列表
     */
    @ApiOperation("根据条件查询90°裁断机台信息")
    @RequiresPermissions("cd90:machine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90MachineInfo machineInfo) {
        return machineInfoService.list(machineInfo);
    }

    /**
     * 修改90°裁断机台信息前获取对象
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("machineInfo", machineInfoService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改90°裁断机台信息（id不为空）")
    @RequiresPermissions("cd90:machine:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(Cd90MachineInfo machineInfo) {
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
     * 删除90°裁断机台信息
     */
    @ApiOperation("删除90°裁断机台信息（id不为空）")
    @RequiresPermissions("cd90:machine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return machineInfoService.remove(arr);
    }

    @ApiOperation("根据code判断90°裁断机台是否已经存在")
    @PostMapping("/checkMachineCodeUnique")
    @ResponseBody
    public String checkMachineCodeUnique(Cd90MachineInfo machineInfo) {
        String result = machineInfoService.checkMachineCodeUnique(machineInfo);
        return result;
    }

    @ApiOperation("导出90°裁断机台信息")
    @RequiresPermissions("cd90:machine:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd90MachineInfo machineInfo) throws IOException {
        List<Cd90MachineInfo> list = machineInfoService.exportList(machineInfo);
        ExcelUtil<Cd90MachineInfo> util = new ExcelUtil(Cd90MachineInfo.class);
        String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, machineInfo.toString(), ApsConstant.PROCEDURE_CODE_CD90);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载模板
     *
     * @param response
     * @throws IOException
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
        ExcelUtil<Cd90MachineInfoTemp> util = new ExcelUtil<>(Cd90MachineInfoTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("cd90:machine:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD90, I18nUtil.getMessage("ui.cd90.bigRoll.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<Cd90MachineInfo> util = new ExcelUtil<>(Cd90MachineInfo.class);
        List<Cd90MachineInfo> list = util.importExcel(in);
        AjaxResult ajaxResult = machineInfoService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
