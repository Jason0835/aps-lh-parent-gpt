package com.zlt.aps.controller.tc;


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
import com.zlt.aps.tc.api.domain.dto.TcSpecifyMachineDto;
import com.zlt.aps.tc.api.service.ITcSpecifyMachineService;
import com.zlt.aps.template.tc.TcSpecifyMachineTemp;
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

@Api(tags = {"胎侧定点机台维护接口"})
@Controller
@RequestMapping("/tc/specifyMachine")
public class TcSpecifyMachineController extends BaseController {

    private String prefix = "tc/specifyMachine";

    @Resource
    private ITcSpecifyMachineService iTcSpecifyMachineService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    @RequiresPermissions("tc:specifyMachine:view")
    @GetMapping()
    public String specifyMachine() {
        return prefix + "/specifyMachine";
    }

    @ApiOperation("根据条件查询定点机台列表")
//    @RequiresPermissions("tc:specifyMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(TcSpecifyMachineDto dto) {
        return iTcSpecifyMachineService.listSpecifyMachine(dto);
    }

    @ApiOperation("跳转到定点机台新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("specifyMachine", new TcSpecifyMachineDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取定点机台信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("specifyMachine", iTcSpecifyMachineService.getSpecifyMachine(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改定点机台(id为空则进行新增，id不为空则进行修改)")
//    @RequiresPermissions("tc:specifyMachine:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveSpecifyMachine(TcSpecifyMachineDto dto) {
        return iTcSpecifyMachineService.saveSpecifyMachine(dto);
    }

    @ApiOperation("刪除定点机台")
//    @RequiresPermissions("tc:specifyMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iTcSpecifyMachineService.deleteSpecifyMachine(arr);
    }

    @ApiOperation("刪除全部定点机台")
    @RequiresPermissions("tc:specifyMachine:removeAll")
    @PostMapping("/removeAll")
    @ResponseBody
    public AjaxResult removeAll() {
        return iTcSpecifyMachineService.deleteAllSpecifyMachine();
    }

    @ApiOperation("导出定点机台")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, TcSpecifyMachineDto dto) throws IOException {
        List<TcSpecifyMachineDto> list = iTcSpecifyMachineService.exportData(dto);
        ExcelUtil<TcSpecifyMachineDto> util = new ExcelUtil(TcSpecifyMachineDto.class);
        String fileName = I18nUtil.getMessage("ui.tc.specifyMachine.column.modalName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_TC);
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
        String fileName = I18nUtil.getMessage("ui.tc.specifyMachine.column.modalName");
        ExcelUtil<TcSpecifyMachineTemp> util = new ExcelUtil<>(TcSpecifyMachineTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     *
     * @param file
     * @param updateSupport
     * @throws Exception
     */
    @RequiresPermissions("tc:specifyMachine:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_TC,
                I18nUtil.getMessage("ui.tc.specifyMachine.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //解析文件
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<TcSpecifyMachineDto> util = new ExcelUtil<>(TcSpecifyMachineDto.class);
        List<TcSpecifyMachineDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iTcSpecifyMachineService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
