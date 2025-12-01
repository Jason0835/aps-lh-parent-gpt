package com.zlt.mix.controller.setting;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.common.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.SettingFormulaInfo;
import com.zlt.mix.setting.api.service.ISettingFormulaInfoService;
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
import java.util.List;

/**
 * @author Liam
 * @date 2022-03-22
 */
@Controller
@RequestMapping("/setting/formulaInfo")
@Api(tags = {"配方信息管理接口"})
public class SettingFormulaInfoController extends BaseController {
    private final String prefix = "setting/formulaInfo";

    @Autowired
    private ISettingFormulaInfoService iSettingFormulaInfoService;

    @Autowired
    private IExportLogService iExportLogService;

    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    @RequiresPermissions("setting:formulaInfo:view")
    @GetMapping()
    @ApiOperation("跳转到配方信息首页")
    public String toIndex() {
        return prefix + "/formulaInfo";
    }


    @RequiresPermissions("setting:formulaInfo:list")
    @PostMapping("/list")
    @ResponseBody
    @ApiOperation("查询配方信息维护列表")
    public TableDataInfo list(SettingFormulaInfo entity) {
        return iSettingFormulaInfoService.list(entity);
    }

    @GetMapping("/edit/{id}")
    @ApiOperation("获取配方信息详细信息，跳转到编辑页面")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "id", dataType = "Long", value = "主键id", paramType = "query")
    )
    public String getInfo(@PathVariable("id") Long id, ModelMap modelMap) {
        modelMap.put("formulaInfo", iSettingFormulaInfoService.getInfo(id));
        return prefix + "/edit";
    }

    @GetMapping("/add")
    @ApiOperation("跳转到配方信息新增页面")
    public String toAdd(ModelMap modelMap) {
        modelMap.put("formulaInfo", new SettingFormulaInfo());
        return prefix + "/edit";
    }

    @RequiresPermissions("setting:formulaInfo:edit")
    @PostMapping("/save")
    @ResponseBody
    @ApiOperation("保存配方信息（id为空则新增，id不为空则修改）")
    public AjaxResult save(SettingFormulaInfo entity) {
        return iSettingFormulaInfoService.save(entity);
    }

    @RequiresPermissions("setting:formulaInfo:remove")
    @PostMapping("/remove")
    @ResponseBody
    @ApiOperation("删除配方信息")
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iSettingFormulaInfoService.remove(arr);
    }

    @RequiresPermissions("setting:formulaInfo:export")
    @GetMapping("/export")
    @ResponseBody
    @ApiOperation("导出配方信息")
    public void export(HttpServletResponse response, SettingFormulaInfo entity) throws IOException {
        List<SettingFormulaInfo> list = iSettingFormulaInfoService.export(entity);
        ExcelUtil<SettingFormulaInfo> excelUtil = new ExcelUtil<>(SettingFormulaInfo.class);
        String fileName = I18nUtil.getMessage("setting.formulaInfo.modelName");
        Workbook workbook = excelUtil.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, entity.toString(), ZltConstant.PROCEDURE_CODE_SETTING);
        iExportLogService.add(exportLog);
    }

    @GetMapping("/importTemplate")
    @ResponseBody
    @ApiOperation("导入配方信息的模板Excel")
    public void importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("setting.formulaInfo.modelName");
        ExcelUtil<SettingFormulaInfo> excelUtil = new ExcelUtil<>(SettingFormulaInfo.class);
        excelUtil.exportExcel(response, null, fileName, fileName);
    }

    @RequiresPermissions("setting:formulaInfo:import")
    @PostMapping("/importData")
    @ResponseBody
    @ApiOperation("导入配方信息")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //根据文件是否加密，返回字节数组
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        //上传到文件服务器并返回导入对象
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.formulaInfo.modelName"), file.getOriginalFilename());

        //保存导入日志
        importLog = iImportLogService.add(importLog);

        //解析成List
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ExcelUtil<SettingFormulaInfo> excelUtil = new ExcelUtil<>(SettingFormulaInfo.class);
        List<SettingFormulaInfo> list = excelUtil.importExcel(in);
        AjaxResult ajaxResult = iSettingFormulaInfoService.importData(list, updateSupport, importLog.getId());

        //更新日志成功数和失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);

        //保存导入失败详细记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);

        return ajaxResult;
    }

    @PostMapping("/checkGlueUnique")
    @ApiOperation("判断胶料名称是否已经存在")
    @ResponseBody
    public String checkGlueUnique(SettingFormulaInfo entity) {
        return iSettingFormulaInfoService.checkGlueUnique(entity);
    }

}
