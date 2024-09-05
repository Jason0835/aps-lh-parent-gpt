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
import com.zlt.aps.cx.api.domain.dto.CxLossSettingDto;
import com.zlt.aps.cx.api.service.ICxLossSettingService;
import com.zlt.aps.template.cx.CxLossSettingTemp;
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
 * 成型损耗率设定Controller
 *
 * @author chen
 * @date 2021-07-19
 */
@Api(tags = "成型损耗率设定")
@Controller
@RequestMapping("/cx/loss")
public class CxLossSettingController extends BaseController {

    private final String prefix = "cx/loss";

    @Autowired
    private ICxLossSettingService iCxLossSettingService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;


    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:loss:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/loss";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("cxLossSetting", new CxLossSettingDto());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("cxLossSetting", iCxLossSettingService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询成型损耗率设定列表
     */
    @ApiOperation("根据条件查询成型损耗率设定列表")
    @RequiresPermissions("cx:loss:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(CxLossSettingDto dto) {
        return iCxLossSettingService.list(dto);
    }

    /**
     * 修改或新增成型损耗率设定
     */
    @ApiOperation("修改或新增成型损耗率设定")
    @RequiresPermissions("cx:loss:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(CxLossSettingDto dto) {
        AjaxResult ajaxResult = null;
        if (dto.getId() != null) {
            ajaxResult = iCxLossSettingService.edit(dto);
        } else {
            ajaxResult = iCxLossSettingService.add(dto);
        }
        return ajaxResult;
    }

    /**
     * 删除成型损耗率设定
     */
    @ApiOperation("删除成型损耗率设定（id不为空）")
    @RequiresPermissions("cx:loss:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iCxLossSettingService.remove(arr);
    }


    @ApiOperation("校验成型损耗率设定唯一性")
    @PostMapping("/checkCxLossSettingUnique")
    @ResponseBody
    public String checkCxLossSettingUnique(CxLossSettingDto dto) {
        return iCxLossSettingService.checkCxLossSettingUnique(dto);
    }


    /**
     * 导出成型损耗率设定
     */
    @ApiOperation("导出成型损耗率设定")
    @RequiresPermissions("cx:loss:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, CxLossSettingDto dto) throws IOException {
        List<CxLossSettingDto> list = iCxLossSettingService.getList(dto);
        ExcelUtil<CxLossSettingDto> util = new ExcelUtil<>(CxLossSettingDto.class);
        String fileName = I18nUtil.getMessage("ui.data.column.cx.loss.modelName");
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
        String fileName = I18nUtil.getMessage("ui.data.column.cx.loss.modelName");
        ExcelUtil<CxLossSettingTemp> util = new ExcelUtil<>(CxLossSettingTemp.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("cx:loss:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.cx.loss.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        ExcelUtil<CxLossSettingDto> util = new ExcelUtil<>(CxLossSettingDto.class);
        List<CxLossSettingDto> list = util.importExcel(in);
        AjaxResult ajaxResult = iCxLossSettingService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
