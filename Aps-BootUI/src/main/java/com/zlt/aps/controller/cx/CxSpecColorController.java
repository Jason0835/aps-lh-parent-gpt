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
import com.zlt.aps.cx.api.domain.dto.CxSpecColorDto;
import com.zlt.aps.cx.api.service.ICxSpecColorService;
import com.zlt.aps.template.cx.CxSpecColorTemp;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
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
 * 规格字体颜色设置Controller
 *
 * @author chen
 * @date 2021-08-21
 */
@Api(tags = "规格字体颜色设置")
@Controller
@RequestMapping("/cx/specColor")
public class CxSpecColorController extends BaseController {

    private final String prefix = "cx/specColor";
    @Autowired
    private ICxSpecColorService iCxSpecColorService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:specColor:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/specColor";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxSpecColor", new CxSpecColorDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxSpecColor", iCxSpecColorService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询规格字体颜色设置列表
     */
    @ApiOperation("根据条件查询规格字体颜色设置列表")
    @RequiresPermissions("cx:specColor:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxSpecColorDto dto) {
        return iCxSpecColorService.list(dto);
    }

    /**
     * 修改或新增规格字体颜色设置
     */
    @ApiOperation("修改或新增规格字体颜色设置")
    @RequiresPermissions("cx:specColor:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxSpecColorDto dto) {
        AjaxResult ajaxResult = null;
        if (StringUtils.isBlank(dto.getColorCode())) {
            dto.setColorCode("#000000");
        }
        if (dto.getId() != null) {
            ajaxResult = iCxSpecColorService.edit(dto);
        } else {
            ajaxResult = iCxSpecColorService.add(dto);
        }
        return ajaxResult;
    }

    /**
     * 删除规格字体颜色设置
     */
    @ApiOperation("删除规格字体颜色设置（id不为空）")
    @RequiresPermissions("cx:specColor:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxSpecColorService.remove(arr);
    }

    @ApiOperation("校验规格字体颜色设置唯一性")
    @PostMapping("/checkSpecCodeUnique")
    @ResponseBody
    public String checkCxSpecColorUnique(CxSpecColorDto dto) {
        return iCxSpecColorService.checkCxSpecColorUnique(dto);
    }

    /**
     * 导出规格字体颜色设置
     */
    @ApiOperation("导出规格字体颜色设置")
    @RequiresPermissions("cx:specColor:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxSpecColorDto dto) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.specColor.modelName");
        List<CxSpecColorDto> list = iCxSpecColorService.getList(dto);
        ExcelUtil<CxSpecColorDto> util = new ExcelUtil<>(CxSpecColorDto.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, dto.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载模板
     *
     * @param response 下载的模板文件
     * @throws IOException 异常
     */
    @ApiOperation("下载模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.specColor.modelName");
        ExcelUtil<CxSpecColorTemp> util = new ExcelUtil<>(CxSpecColorTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     *
     * @param file          要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("cx:specColor:import")
    @ApiOperation("数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.specColor.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<CxSpecColorDto> util = new ExcelUtil<>(CxSpecColorDto.class);
        List<CxSpecColorDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxSpecColorService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
