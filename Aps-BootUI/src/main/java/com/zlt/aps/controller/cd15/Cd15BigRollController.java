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
import com.zlt.aps.cd15.api.domain.dto.Cd15BigRollDto;
import com.zlt.aps.cd15.api.service.ICd15BigRollService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.cd15.Cd15BigRollTemp;
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

@Api(tags = {"15度裁断钢压大卷信息接口"})
@Controller
@RequestMapping("/cd15/bigRoll")
public class Cd15BigRollController extends BaseController {

    private String prefix = "cd15/bigRoll";

    @Resource
    private ICd15BigRollService iCd15BigRollService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @RequiresPermissions("cd15:bigRoll:view")
    @GetMapping()
    public String bigRoll() {
        return prefix + "/bigRoll";
    }

    @ApiOperation("根据条件查询钢压大卷信息列表")
//    @RequiresPermissions("cd15:bigRoll:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15BigRollDto dto) {
        return iCd15BigRollService.listBigRoll(dto);
    }

    @ApiOperation("跳转到钢压大卷信息新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("bigRoll", new Cd15BigRollDto());
        return prefix + "/edit";
    }

    @ApiOperation("获取钢压大卷信息信息，跳转到编辑页面")
    @GetMapping("/edit/{id}")
    public String edit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("bigRoll", iCd15BigRollService.getBigRoll(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改钢压大卷信息(id为空则进行新增，id不为空则进行修改)")
//    @RequiresPermissions("cd15:bigRoll:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveBigRoll(Cd15BigRollDto dto) {
        return iCd15BigRollService.saveBigRoll(dto);
    }

    @ApiOperation("根据code判断钢压大卷代号是否已经存在")
    @PostMapping("/checkBigRollCodeUnique")
    @ResponseBody
    public String checkBigRollCodeUnique(Cd15BigRollDto dto) {
        return iCd15BigRollService.checkBigRollCodeUnique(dto);
    }

    @ApiOperation("刪除钢压大卷信息")
//    @RequiresPermissions("cd15:bigRoll:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCd15BigRollService.deleteBigRoll(arr);
    }

    @ApiOperation("导出钢压大卷信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd15BigRollDto dto) throws IOException {
        List<Cd15BigRollDto> list = iCd15BigRollService.exportData(dto);
        ExcelUtil<Cd15BigRollDto> util = new ExcelUtil(Cd15BigRollDto.class);
        String fileName = I18nUtil.getMessage("ui.cd15.bigRoll.column.modalName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CD15);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.cd15.bigRoll.column.modalName");
        ExcelUtil<Cd15BigRollTemp> util = new ExcelUtil<>(Cd15BigRollTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    @RequiresPermissions("cd15:bigRoll:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<Cd15BigRollDto> util = new ExcelUtil<>(Cd15BigRollDto.class);

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);

        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD15,
                I18nUtil.getMessage("ui.cd15.bigRoll.column.modalName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        List<Cd15BigRollDto> list = util.importExcel(in);
        // 导入
        AjaxResult ajaxResult = iCd15BigRollService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
