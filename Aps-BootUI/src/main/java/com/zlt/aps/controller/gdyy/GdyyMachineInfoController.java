package com.zlt.aps.controller.gdyy;

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
import com.zlt.aps.gdyy.api.domain.entity.GdyyMachineInfo;
import com.zlt.aps.gdyy.api.service.IGdyyMachineInfoService;
import com.zlt.aps.template.gdyy.GdyyMachineInfoTemp;
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
 * 钢带压延机台信息Controller
 *
 * @author zlt
 * @date 2021-05-28
 */
@Api(tags = "钢带压延机台信息维护接口")
@Controller
@RequestMapping("/gdyy/machine")
public class GdyyMachineInfoController extends BaseController {

    private final String prefix = "gdyy/machine";

    @Autowired
    private IGdyyMachineInfoService machineInfoService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    /**
     * 列表跳转至machine页面
     *
     * @return
     */
    @RequiresPermissions("gdyy:machine:view")
    @GetMapping()
    public String operlog() {

        return prefix + "/machine";
    }

    /**
     * 新增跳转至add页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("machineInfo", new GdyyMachineInfo());
        return prefix + "/edit";
    }

    /**
     * 查询钢带压延机台信息列表
     */
    @ApiOperation("根据条件查询钢带压延机台信息")
    @RequiresPermissions("gdyy:machine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(GdyyMachineInfo machineInfo) {
        return machineInfoService.list(machineInfo);
    }

    /**
     * 修改钢带压延机台信息前获取对象
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("machineInfo", machineInfoService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改钢带压延机台信息（id不为空）")
    @RequiresPermissions({"gdyy:machine:edit", "gdyy:machine:add"})
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(GdyyMachineInfo machineInfo) {
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
     * 删除钢带压延机台信息
     */
    @ApiOperation("删除钢带压延机台信息（id不为空）")
    @RequiresPermissions("gdyy:machine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return machineInfoService.remove(arr);
    }

    @ApiOperation("根据code判断钢带压延机台是否已经存在")
    @PostMapping("/checkMachineCodeUnique")
    @ResponseBody
    public String checkMachineCodeUnique(GdyyMachineInfo machineInfo) {
        String result = machineInfoService.checkMachineCodeUnique(machineInfo);
        return result;
    }

    @ApiOperation("导出钢带压延机台信息")
    @RequiresPermissions("gdyy:machine:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GdyyMachineInfo machineInfo) throws IOException {
        List<GdyyMachineInfo> list = machineInfoService.exportList(machineInfo);
        ExcelUtil<GdyyMachineInfo> util = new ExcelUtil<>(GdyyMachineInfo.class);
        String fileName = I18nUtil.getMessage("ui.gdyy.machine.export.fileName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, machineInfo.toString(), ApsConstant.PROCEDURE_CODE_GDYY);
        iExportLogService.add(exportLog);
    }


    /**
     * 下载模板
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.gdyy.machine.export.fileName");
        ExcelUtil<GdyyMachineInfoTemp> util = new ExcelUtil<>(GdyyMachineInfoTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("gdyy:machine:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_GDYY, I18nUtil.getMessage("ui.gdyy.machine.export.fileName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<GdyyMachineInfo> util = new ExcelUtil<>(GdyyMachineInfo.class);
        List<GdyyMachineInfo> list = util.importExcel(in);
        AjaxResult ajaxResult = machineInfoService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
