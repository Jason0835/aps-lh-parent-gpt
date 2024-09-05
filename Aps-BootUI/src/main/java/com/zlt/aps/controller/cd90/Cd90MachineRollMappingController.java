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
import com.zlt.aps.cd90.api.domain.dto.Cd90MachineRollMappingDto;
import com.zlt.aps.cd90.api.service.ICd90MachineRollMappingService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.cd90.Cd90MachineRollMappingTemp;
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
 * 90度裁断帘布大卷与机台的映射表
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-16
 */
@Api(tags = {"纤维压延帘布大卷与机台的映射表"})
@Controller
@RequestMapping("/cd90/machineRollMapping")
public class Cd90MachineRollMappingController extends BaseController {
    private String prefix = "cd90/machineRollMapping";

    @Resource
    private ICd90MachineRollMappingService machineRollMappingService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    @RequiresPermissions("cd90:machineRollMapping:view")
    @GetMapping()
    public String machineRollMapping() {
        return prefix + "/machineRollMapping";
    }

    @ApiOperation("根据条件查询帘布大卷与机台的映射表列表")
    @RequiresPermissions("cd90:machineRollMapping:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90MachineRollMappingDto dto) {
        return machineRollMappingService.listMachineRollMapping(dto);
    }

    @ApiOperation("跳转到帘布大卷与机台的映射表新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("machineRollMapping", new Cd90MachineRollMappingDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取帘布大卷与机台的映射表，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("machineRollMapping", machineRollMappingService.getBigRollColor(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改帘布大卷与机台的映射表(id为空则进行新增，id不为空则进行修改)")
    @RequiresPermissions("cd90:machineRollMapping:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueGroupOrder(Cd90MachineRollMappingDto dto) {
        return machineRollMappingService.saveMachineRollMapping(dto);
    }

    @ApiOperation("根据id判断主键是否已经存在")
    @PostMapping("/checkRollCodeUnique")
    @ResponseBody
    public String checkRollCodeUnique(Cd90MachineRollMappingDto dto) {
        return machineRollMappingService.checkMachineRollMapping(dto);
    }

    @ApiOperation("刪除帘布大卷与机台的映射表")
    @RequiresPermissions("cd90:machineRollMapping:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return machineRollMappingService.deleteMachineRollMapping(arr);
    }

    @ApiOperation("刪除全部")
    @RequiresPermissions("cd90:machineRollMapping:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return machineRollMappingService.deleteAll();
    }


    @ApiOperation("导出帘布大卷与机台的映射表")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd90MachineRollMappingDto dto) throws IOException {
        List<Cd90MachineRollMappingDto> list = machineRollMappingService.exportData(dto);
        ExcelUtil<Cd90MachineRollMappingDto> util = new ExcelUtil(Cd90MachineRollMappingDto.class);
        String fileName = I18nUtil.getMessage("ui.cd90.MachineRollMapping.column.modalName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CD90);
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
        String fileName = I18nUtil.getMessage("ui.cd90.MachineRollMapping.column.modalName");
        ExcelUtil<Cd90MachineRollMappingTemp> util = new ExcelUtil<>(Cd90MachineRollMappingTemp.class);
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
    @RequiresPermissions("cd90:machineRollMapping:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD90, I18nUtil.getMessage("ui.cd90.MachineRollMapping.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<Cd90MachineRollMappingDto> util = new ExcelUtil<>(Cd90MachineRollMappingDto.class);
        List<Cd90MachineRollMappingDto> list = util.importExcel(in);
        AjaxResult ajaxResult = machineRollMappingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
