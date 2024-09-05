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
import com.zlt.aps.cd90.api.domain.dto.Cd90SpecifyMachineDto;
import com.zlt.aps.cd90.api.service.ICd90SpecifyMachineService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.cd90.Cd90SpecifyMachineTemp;
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

@Api(tags = {"90度裁断定点机台维护接口"})
@Controller
@RequestMapping("/cd90/specifyMachine")
public class Cd90SpecifyMachineController extends BaseController {

    private String prefix = "cd90/specifyMachine";

    @Resource
    private ICd90SpecifyMachineService iCd90SpecifyMachineService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    @RequiresPermissions("cd90:specifyMachine:view")
    @GetMapping()
    public String specifyMachine() {
        return prefix + "/specifyMachine";
    }

    @ApiOperation("根据条件查询定点机台列表")
//    @RequiresPermissions("cd90:specifyMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd90SpecifyMachineDto dto) {
        return iCd90SpecifyMachineService.listSpecifyMachine(dto);
    }

    @ApiOperation("跳转到定点机台新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("specifyMachine", new Cd90SpecifyMachineDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取定点机台信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("specifyMachine", iCd90SpecifyMachineService.getSpecifyMachine(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改定点机台(id为空则进行新增，id不为空则进行修改)")
//    @RequiresPermissions("cd90:specifyMachine:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveSpecifyMachine(Cd90SpecifyMachineDto dto) {
        return iCd90SpecifyMachineService.saveSpecifyMachine(dto);
    }

    @ApiOperation("刪除定点机台")
//    @RequiresPermissions("cd90:specifyMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCd90SpecifyMachineService.deleteSpecifyMachine(arr);
    }

    @ApiOperation("刪除全部定点机台")
    @RequiresPermissions("cd90:specifyMachine:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iCd90SpecifyMachineService.deleteAllSpecifyMachine();
    }

    @ApiOperation("导出定点机台")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd90SpecifyMachineDto dto) throws IOException {
        List<Cd90SpecifyMachineDto> list = iCd90SpecifyMachineService.exportData(dto);
        ExcelUtil<Cd90SpecifyMachineDto> util = new ExcelUtil(Cd90SpecifyMachineDto.class);
        String fileName = I18nUtil.getMessage("ui.cd90.specifyMachine.column.modalName");
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
        String fileName = I18nUtil.getMessage("ui.cd90.specifyMachine.column.modalName");
        ExcelUtil<Cd90SpecifyMachineTemp> util = new ExcelUtil<>(Cd90SpecifyMachineTemp.class);
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
    @RequiresPermissions("cd90:specifyMachine:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD90, I18nUtil.getMessage("ui.cd90.specifyMachine.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<Cd90SpecifyMachineDto> util = new ExcelUtil<>(Cd90SpecifyMachineDto.class);
        List<Cd90SpecifyMachineDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iCd90SpecifyMachineService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
