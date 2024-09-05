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
import com.zlt.aps.cd15.api.domain.dto.Cd15QuotaSettingDto;
import com.zlt.aps.cd15.api.service.ICd15QuotaSettingService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.template.cd15.Cd15QuotaSettingTemp;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
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
 * 15度裁断定额设定Controller
 *
 * @author chen
 * @date 2021-06-28
 */
@Api(tags = "15度裁断定额设定")
@Controller
@RequestMapping("/cd15/quota")
public class Cd15QuotaSettingController extends BaseController {

    private final String prefix = "cd15/quota";
    @Autowired
    private ICd15QuotaSettingService iCd15QuotaSettingService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cd15:quota:view")
    @ApiOperation("跳转到15度裁断定额设定信息首页")
    @GetMapping()
    public String toIndex() {
        return prefix + "/quota";
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转到90度裁断定额设定新增页")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("quotaSetting", new Cd15QuotaSettingDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @ApiOperation("跳转到90度裁断定额设定编辑页")
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("quotaSetting", iCd15QuotaSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询15度裁断定额设定列表
     */
    @ApiOperation("根据条件查询15度裁断定额设定列表")
    @RequiresPermissions("cd15:quota:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(Cd15QuotaSettingDto dto) {
        return iCd15QuotaSettingService.list(dto);
    }

    /**
     * 修改或新增15度裁断定额设定
     */
    @ApiOperation("修改或新增15度裁断定额设定")
    @RequiresPermissions("cd15:quota:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(Cd15QuotaSettingDto dto) {
        return iCd15QuotaSettingService.edit(dto);
    }

    /**
     * 删除15度裁断定额设定
     */
    @ApiOperation("删除15度裁断定额设定（id不为空）")
    @RequiresPermissions("cd15:quota:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCd15QuotaSettingService.remove(arr);
    }

    /**
     * 导出15度裁断定额设定
     */
    @ApiOperation("导出15度裁断定额设定")
    @RequiresPermissions("cd15:quota:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, Cd15QuotaSettingDto dto) throws IOException {
        List<Cd15QuotaSettingDto> list = iCd15QuotaSettingService.exportData(dto);
        ExcelUtil<Cd15QuotaSettingDto> util = new ExcelUtil<>(Cd15QuotaSettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.cd15.setting.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CD15);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.cd15.setting.modelName");
        ExcelUtil<Cd15QuotaSettingTemp> util = new ExcelUtil<>(Cd15QuotaSettingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
    }

    @RequiresPermissions("cd15:quota:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<Cd15QuotaSettingDto> util = new ExcelUtil<>(Cd15QuotaSettingDto.class);
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CD15,
                I18nUtil.getMessage("ui.data.column.cd15.setting.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        List<Cd15QuotaSettingDto> list = util.importExcel(in);
        // 导入
        AjaxResult ajaxResult = iCd15QuotaSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入错误详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
