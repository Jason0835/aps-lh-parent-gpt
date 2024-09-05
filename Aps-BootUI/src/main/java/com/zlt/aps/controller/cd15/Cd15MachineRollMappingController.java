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
import com.zlt.aps.cd15.api.domain.dto.Cd15MachineRollMappingDto;
import com.zlt.aps.cd15.api.service.ICd15MachineRollMappingService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.cd15.Cd15MachineRollMappingTemp;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * <p>
 * 钢压大卷与机台的映射表
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-15
 */
@Api(tags = {"钢压大卷与机台的映射表"})
@Controller
@RequestMapping("/cd15/machineRollMapping")
public class Cd15MachineRollMappingController extends BaseController {
    private String prefix = "cd15/machineRollMapping";

    @Resource
    private ICd15MachineRollMappingService machineRollMappingService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @RequiresPermissions("cd15:machineRollMapping:view")
    @GetMapping()
    public String machineRollMapping() {
        return prefix + "/machineRollMapping";
    }

    @ApiOperation("根据条件查询钢压大卷与机台的映射表")
    @RequiresPermissions("cd15:machineRollMapping:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15MachineRollMappingDto dto) {
        return machineRollMappingService.listMachineRollMapping(dto);
    }

    @ApiOperation("跳转到钢压大卷与机台的映射表新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("machineRollMapping", new Cd15MachineRollMappingDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取钢压大卷与机台的映射表，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("machineRollMapping", machineRollMappingService.getBigRollColor(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改钢压大卷与机台的映射表(id为空则进行新增，id不为空则进行修改)")
    @RequiresPermissions("cd15:machineRollMapping:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueGroupOrder(Cd15MachineRollMappingDto dto) {
        return machineRollMappingService.saveMachineRollMapping(dto);
    }

    @ApiOperation("根据id判断主键是否已经存在")
    @PostMapping("/checkRollCodeUnique")
    @ResponseBody
    public String checkRollCodeUnique(Cd15MachineRollMappingDto dto) {
        return machineRollMappingService.checkMachineRollMapping(dto);
    }

    @ApiOperation("刪除钢压大卷与机台的映射表")
    @RequiresPermissions("cd15:machineRollMapping:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return machineRollMappingService.deleteMachineRollMapping(arr);
    }


    @ApiOperation("刪除全部")
    @RequiresPermissions("cd15:machineRollMapping:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return machineRollMappingService.deleteAll();
    }


    @ApiOperation("导出钢压大卷与机台的映射表")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd15MachineRollMappingDto dto) throws IOException {
        List<Cd15MachineRollMappingDto> list = machineRollMappingService.exportData(dto);
        ExcelUtil<Cd15MachineRollMappingDto> util = new ExcelUtil(Cd15MachineRollMappingDto.class);
        String fileName = I18nUtil.getMessage("ui.cd15.MachineRollMapping.column.modalName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CD15);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.cd15.MachineRollMapping.column.modalName");
        ExcelUtil<Cd15MachineRollMappingTemp> util = new ExcelUtil<>(Cd15MachineRollMappingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    @RequiresPermissions("cd15:machineRollMapping:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<Cd15MachineRollMappingDto> util = new ExcelUtil<>(Cd15MachineRollMappingDto.class);
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD15,
                I18nUtil.getMessage("ui.cd15.MachineRollMapping.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        List<Cd15MachineRollMappingDto> list = util.importExcel(in);
        // 导入
        AjaxResult ajaxResult = machineRollMappingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
