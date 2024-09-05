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
import com.zlt.aps.cx.api.domain.dto.CxHolidaySettingDto;
import com.zlt.aps.cx.api.service.ICxHolidaySettingService;
import com.zlt.aps.template.cx.CxHolidaySettingTemp;
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
 * 假日设定Controller
 *
 * @author chen
 * @date 2021-06-30
 */
@Api(tags = "假日设定")
@Controller
@RequestMapping("/cx/holiday")
public class CxHolidaySettingController extends BaseController {

    @Autowired
    private ICxHolidaySettingService iCxHolidaySettingService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    private String prefix = "cx/holiday";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:holiday:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/holiday";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxHolidaySetting", new CxHolidaySettingDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxHolidaySetting", iCxHolidaySettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询假日设定列表
     */
    @ApiOperation("根据条件查询假日设定列表")
    @RequiresPermissions("cx:holiday:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxHolidaySettingDto dto) {
        return iCxHolidaySettingService.list(dto);
    }

    /**
     * 修改或新增假日设定
     */
    @ApiOperation("修改或新增假日设定")
    @RequiresPermissions("cx:holiday:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxHolidaySettingDto dto) {
        if (iCxHolidaySettingService.checkUnique(dto).size() > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.holiday.notUnique"));
        }
        return iCxHolidaySettingService.edit(dto);
    }

    /**
     * 删除假日设定
     */
    @ApiOperation("删除假日设定（id不为空）")
    @RequiresPermissions("cx:holiday:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxHolidaySettingService.remove(arr);
    }

    /**
     * 导出假日设定
     */
    @ApiOperation("导出假日设定")
    @RequiresPermissions("cx:holiday:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxHolidaySettingDto dto) throws IOException {
        List<CxHolidaySettingDto> list = iCxHolidaySettingService.exportData(dto);
        ExcelUtil<CxHolidaySettingDto> util = new ExcelUtil<>(CxHolidaySettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.cx.holiday.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CX);
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
        String fileName = I18nUtil.getMessage("ui.data.column.cx.holiday.modelName");
        ExcelUtil<CxHolidaySettingTemp> util = new ExcelUtil<>(CxHolidaySettingTemp.class);
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
    @RequiresPermissions("cx:holiday:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.cx.holiday.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<CxHolidaySettingDto> util = new ExcelUtil<>(CxHolidaySettingDto.class);
        List<CxHolidaySettingDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxHolidaySettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
