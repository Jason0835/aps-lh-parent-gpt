package com.zlt.aps.controller.tm;


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
import com.zlt.aps.template.tm.TmSpecifyMachineTemp;
import com.zlt.aps.tm.api.domain.dto.TmSpecifyMachineDto;
import com.zlt.aps.tm.api.service.ITmSpecifyMachineService;
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

@Api(tags = {"胎面定点机台维护接口"})
@Controller
@RequestMapping("/tm/specifyMachine")
public class TmSpecifyMachineController extends BaseController {

    private final String prefix = "tm/specifyMachine";

    @Resource
    private ITmSpecifyMachineService iTmSpecifyMachineService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @RequiresPermissions("tm:specifyMachine:view")
    @GetMapping()
    public String specifyMachine() {
        return prefix + "/specifyMachine";
    }

    @ApiOperation("根据条件查询定点机台列表")
    @RequiresPermissions("tm:specifyMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TmSpecifyMachineDto dto) {
        return iTmSpecifyMachineService.listSpecifyMachine(dto);
    }

    @ApiOperation("跳转到定点机台新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("specifyMachine", new TmSpecifyMachineDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取定点机台信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("specifyMachine", iTmSpecifyMachineService.getSpecifyMachine(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改定点机台(id为空则进行新增，id不为空则进行修改)")
    @RequiresPermissions("tm:specifyMachine:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveSpecifyMachine(TmSpecifyMachineDto dto) {
        return iTmSpecifyMachineService.saveSpecifyMachine(dto);
    }

    @ApiOperation("刪除定点机台")
    @RequiresPermissions("tm:specifyMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTmSpecifyMachineService.deleteSpecifyMachine(arr);
    }

    @ApiOperation("刪除全部定点机台")
    @RequiresPermissions("tm:specifyMachine:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iTmSpecifyMachineService.deleteAllSpecifyMachine();
    }

    @ApiOperation("导出定点机台")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TmSpecifyMachineDto dto) throws IOException {
        List<TmSpecifyMachineDto> list = iTmSpecifyMachineService.exportData(dto);
        ExcelUtil<TmSpecifyMachineDto> util = new ExcelUtil(TmSpecifyMachineDto.class);
        String fileName = I18nUtil.getMessage("ui.tm.specifyMachine.column.modalName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_TM);
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
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.tm.specifyMachine.column.modalName");
        ExcelUtil<TmSpecifyMachineTemp> util = new ExcelUtil<>(TmSpecifyMachineTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @return
     * @throws Exception
     */
    @RequiresPermissions("tm:specifyMachine:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TM,
                I18nUtil.getMessage("ui.tm.specifyMachine.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TmSpecifyMachineDto> util = new ExcelUtil<>(TmSpecifyMachineDto.class);
        List<TmSpecifyMachineDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iTmSpecifyMachineService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }


}
