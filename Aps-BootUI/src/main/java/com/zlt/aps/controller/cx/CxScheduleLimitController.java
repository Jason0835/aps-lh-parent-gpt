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
import com.zlt.aps.cx.api.domain.dto.CxScheduleLimitDto;
import com.zlt.aps.cx.api.service.ICxScheduleLimitService;
import com.zlt.aps.template.cx.CxScheduleLimitTemp;
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
 * <p>
 * 成型排产限制信息维护 前端控制器
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-16
 */
@Controller
@RequestMapping("/cx/limit")
@Api(tags = {"成型排产限制信息接口"})
public class CxScheduleLimitController extends BaseController {

    private final String prefix = "cx/limit";

    @Autowired
    private ICxScheduleLimitService iCxScheduleLimitService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    @RequiresPermissions("cx:limit:view")
    @GetMapping()
    @ApiOperation("跳转到成型排产限制信息首页")
    public String toIndex() {
        return prefix + "/limit";
    }

    /**
     * 查询成型排产限制信息维护列表
     */
    @RequiresPermissions("cx:limit:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询成型排产限制信息维护列表")
    public TableDataInfo list(CxScheduleLimitDto dto) {
        TableDataInfo list = iCxScheduleLimitService.list(dto);
        return list;
    }

    /**
     * 根据id获取成型排产限制信息维护详细信息
     */
    @RequiresPermissions("cx:limit:edit")
    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取成型排产限制信息详细信息,跳转到编辑页面")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("ScheduleLimit", iCxScheduleLimitService.getInfo(id));
        return prefix + "/edit";
    }

    @RequiresPermissions("cx:limit:add")
    @ApiOperation("跳转到成型排产限制新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("ScheduleLimit", new CxScheduleLimitDto());
        return prefix + "/edit";
    }

    /**
     * 保存成型排产限制信息维护
     */
    @RequiresPermissions("cx:limit:edit")
    @PostMapping("/edit")
    @ResponseBody
    @ApiOperation("保存成型排产限制信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(CxScheduleLimitDto dto) {
        return iCxScheduleLimitService.edit(dto);
    }

    /**
     * 删除成型排产限制信息维护
     */
    @RequiresPermissions("cx:limit:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除成型排产限制信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxScheduleLimitService.remove(arr);
    }

    /**
     * 导出成型排产限制信息
     */
    @RequiresPermissions("cx:limit:export")
    @ApiOperation("导出成型排产限制信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxScheduleLimitDto dto) throws IOException {
        List<CxScheduleLimitDto> list = iCxScheduleLimitService.exportData(dto);
        ExcelUtil<CxScheduleLimitDto> util = new ExcelUtil<>(CxScheduleLimitDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.cx.limit.modelName");
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
        String fileName = I18nUtil.getMessage("ui.data.column.cx.limit.modelName");
        ExcelUtil<CxScheduleLimitTemp> util = new ExcelUtil<>(CxScheduleLimitTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("cx:limit:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.cx.limit.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<CxScheduleLimitDto> util = new ExcelUtil<>(CxScheduleLimitDto.class);
        List<CxScheduleLimitDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxScheduleLimitService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
