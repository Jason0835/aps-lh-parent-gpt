package com.zlt.aps.controller.nc;


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
import com.zlt.aps.nc.api.domain.dto.NcMouthPlateDto;
import com.zlt.aps.nc.api.service.INcMouthPlateService;
import com.zlt.aps.template.nc.NcMouthPlateTemp;
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
 * 内衬口型板信息维护 前端控制器
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-01
 */
@Controller
@RequestMapping("/nc/mouthPlate")
@Api(tags = {"内衬口型板信息接口"})
public class NcMouthPlateController extends BaseController {

    private final String prefix = "nc/mouthPlate";

    @Autowired
    private INcMouthPlateService iNcMouthPlateService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    @RequiresPermissions("nc:mouthPlate:view")
    @GetMapping()
    @ApiOperation("跳转到口型板信息首页")
    public String toIndex() {
        return prefix + "/mouthPlate";
    }

    /**
     * 查询内衬口型板信息维护列表
     */
    @RequiresPermissions("nc:mouthPlate:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询内衬口型板信息维护列表")
    public TableDataInfo list(NcMouthPlateDto dto) {
        TableDataInfo list = iNcMouthPlateService.list(dto);
        return list;
    }

    /**
     * 根据id获取内衬口型板信息维护详细信息
     */
    @GetMapping(value = "/edit/{id}")
    @ApiOperation("获取内衬口型板信息详细信息,跳转到编辑页面")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "int", value = "主键id", paramType = "query")
    })
    public String getInfo(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("MouthPlate", iNcMouthPlateService.getInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("跳转到内衬口型板新增页面")
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("MouthPlate", new NcMouthPlateDto());
        return prefix + "/edit";
    }

    /**
     * 保存内衬口型板信息维护
     */
    @RequiresPermissions("nc:mouthPlate:edit")
    @PostMapping("/edit")
    @ResponseBody
    @ApiOperation("保存内衬口型板信息（id为空则新增，id不为空则修改）")
    public AjaxResult edit(NcMouthPlateDto dto) {
        return iNcMouthPlateService.edit(dto);
    }

    /**
     * 删除内衬口型板信息维护
     */
    @RequiresPermissions("nc:mouthPlate:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除内衬口型板信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", dataType = "Array", value = "id数组", paramType = "query")
    })
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iNcMouthPlateService.remove(arr);
    }

    /**
     * 导出内衬口型板信息
     */
    @RequiresPermissions("nc:mouthPlate:export")
    @ApiOperation("导出内衬口型板信息")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, NcMouthPlateDto dto) throws IOException {
        List<NcMouthPlateDto> list = iNcMouthPlateService.exportData(dto);
        ExcelUtil<NcMouthPlateDto> util = new ExcelUtil<>(NcMouthPlateDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.nc.mouthPlate.modelName");
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_NC);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.nc.mouthPlate.modelName");
        ExcelUtil<NcMouthPlateTemp> util = new ExcelUtil<>(NcMouthPlateTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("nc:mouthPlate:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<NcMouthPlateDto> util = new ExcelUtil<>(NcMouthPlateDto.class);
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_NC,
                I18nUtil.getMessage("ui.data.column.nc.mouthPlate.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        //解析文件
        InputStream in = new ByteArrayInputStream(data);
        List<NcMouthPlateDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iNcMouthPlateService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存导入失败详细信息
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }
}
