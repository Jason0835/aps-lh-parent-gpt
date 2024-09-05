package com.zlt.aps.controller.cx;

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
import com.zlt.aps.cx.api.domain.dto.CxQuotaSettingDto;
import com.zlt.aps.cx.api.service.ICxQuotaSettingService;
import com.zlt.aps.template.cx.CxQuotaSettingTemp;
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
 * 成型定额设定Controller
 *
 * @author chen
 * @date 2021-06-16
 */
@Controller
@RequestMapping("/cx/quota")
@Api(tags = {"成型定额设定信息接口"})
public class CxQuotaSettingController extends BaseController {
    private final String prefix = "cx/quota";

    @Autowired
    private ICxQuotaSettingService iCxQuotaSettingService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @RequiresPermissions("cx:quota:view")
    @GetMapping()
    @ApiOperation("跳转到成型定额设定信息首页")
    public String toIndex() {
        return prefix + "/quota";
    }

    /**
     * 查询成型定额设定信息维护列表
     */
    @RequiresPermissions("cx:quota:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询成型定额设定信息维护列表")
    public TableDataInfo list(CxQuotaSettingDto dto) {
        TableDataInfo list = iCxQuotaSettingService.list(dto);
        return list;
    }

    /**
     * 根据id获取成型定额设定信息维护详细信息
     */
    @RequiresPermissions("cx:quota:edit")
    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取成型定额设定信息详细信息,跳转到编辑页面")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("quotaSetting", iCxQuotaSettingService.getInfo(id));
        return prefix + "/edit";
    }

    @RequiresPermissions("cx:quota:add")
    @ApiOperation("跳转到成型定额设定新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("quotaSetting", new CxQuotaSettingDto());
        return prefix + "/edit";
    }

    /**
     * 保存成型定额设定信息维护
     */
    @RequiresPermissions("cx:quota:edit")
    @PostMapping("/edit")
    @ResponseBody
    @ApiOperation("保存成型定额设定信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(CxQuotaSettingDto dto) {
        return iCxQuotaSettingService.edit(dto);
    }

    /**
     * 删除成型定额设定信息维护
     */
    @RequiresPermissions("cx:quota:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除成型定额设定信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxQuotaSettingService.remove(arr);
    }

    /**
     * 导出成型定额设定信息
     */
    @RequiresPermissions("cx:quota:export")
    @ApiOperation("导出成型定额设定信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxQuotaSettingDto dto) throws IOException {
        List<CxQuotaSettingDto> list = iCxQuotaSettingService.exportData(dto);
        ExcelUtil<CxQuotaSettingDto> util = new ExcelUtil<>(CxQuotaSettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.cx.setting.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载模板
     */
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.cx.setting.modelName");
        ExcelUtil<CxQuotaSettingTemp> util = new ExcelUtil<>(CxQuotaSettingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("cx:quota:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.cx.setting.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<CxQuotaSettingDto> util = new ExcelUtil<>(CxQuotaSettingDto.class);
        List<CxQuotaSettingDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxQuotaSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
