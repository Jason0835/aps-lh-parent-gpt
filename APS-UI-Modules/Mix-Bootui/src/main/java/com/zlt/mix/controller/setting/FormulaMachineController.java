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
import com.ruoyi.common.utils.StringUtils;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.CollectionUtil;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.common.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.dto.MachineOrderDto;
import com.zlt.mix.setting.api.domain.entity.FormulaMachine;
import com.zlt.mix.setting.api.service.IFormulaMachineService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 配方与机台对应Controller
 *
 * @author Gim
 * @date 2022-03-28
 */
@Api(tags = "配方与机台对应")
@Controller
@RequestMapping("/setting/formulaMachine")
public class FormulaMachineController extends BaseController {

    @Resource
    private IFormulaMachineService iFormulaMachineService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/formulaMachine";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:formulaMachine:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/formulaMachine";
    }

    @ApiOperation("根据条件查询配方与机台对应列表")
    @RequiresPermissions("setting:formulaMachine:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listFormulaMachine(FormulaMachine entity) {
        return iFormulaMachineService.listFormulaMachine(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        mmap.put("formulaMachine", new FormulaMachine());
        mmap.put("editType", "0");
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("formulaMachine", iFormulaMachineService.getFormulaMachineInfo(id));
        mmap.put("editType", "1");
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增配方与机台对应")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveFormulaMachine(FormulaMachine formulaMachine) {
        List<MachineOrderDto> machineOrderList = formulaMachine.getMachineOrderList();
        if (CollectionUtil.isEmpty(machineOrderList)) {
            return AjaxResult.error(I18nUtil.getMessage("setting.formulaMachine.mustAddOneRecord"));
        }
        for (MachineOrderDto machineOrderDto : machineOrderList) {
            if (StringUtils.isBlank(machineOrderDto.getMachineCode())) {
                return AjaxResult.error(I18nUtil.getMessage("setting.formulaMachine.machineIsNull"));
            }
            if (machineOrderDto.getMachineOrder() == null) {
                return AjaxResult.error(I18nUtil.getMessage("setting.formulaMachine.orderIsNull"));
            }
        }
        return iFormulaMachineService.saveFormulaMachine(formulaMachine);
    }

    @ApiOperation("删除配方与机台对应（id不为空）")
    @RequiresPermissions("setting:formulaMachine:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeFormulaMachine(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iFormulaMachineService.deleteFormulaMachine(arr);
    }

    @ApiOperation("校验配方与机台对应唯一性")
    @PostMapping("/checkFormulaMachineUnique")
    @ResponseBody
    public String checkFormulaMachineUnique(FormulaMachine formulaMachine) {
        return iFormulaMachineService.checkFormulaMachineUnique(formulaMachine);
    }

    /**
     * 导出配方与机台对应
     */
    @ApiOperation("导出配方与机台对应")
    @RequiresPermissions("setting:formulaMachine:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, FormulaMachine formulaMachine) throws IOException {
        String fileName = I18nUtil.getMessage("setting.formulaMachine.modelName");
        List<FormulaMachine> list = iFormulaMachineService.exportData(formulaMachine);
        ExcelUtil<FormulaMachine> util = new ExcelUtil<>(FormulaMachine.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, formulaMachine.toString(), ZltConstant.PROCEDURE_CODE_SETTING);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载导入模板
     *
     * @param response 下载的模板文件
     * @throws IOException 异常
     */
    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("setting.formulaMachine.modelName");
        ExcelUtil<FormulaMachine> util = new ExcelUtil<>(FormulaMachine.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file          要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("setting:formulaMachine:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.formulaMachine.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<FormulaMachine> util = new ExcelUtil<>(FormulaMachine.class);
        List<FormulaMachine> list = util.importExcel(in);

        //导入数据
        AjaxResult ajaxResult = iFormulaMachineService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    @ApiOperation("根据密炼区和胶料名称进行精确查询")
    @PostMapping("/getFormulaMachineList")
    @ResponseBody
    public AjaxResult getFormulaMachineList(FormulaMachine formulaMachine) {
        ArrayList<FormulaMachine> machineList = iFormulaMachineService.getFormulaMachineList(formulaMachine);
        return AjaxResult.success(machineList);
    }

    @ApiOperation("根据条件查询配方对应机台的列表")
    @PostMapping("/getRecipeMachineList")
    @ResponseBody
    public AjaxResult getRecipeMachineList(FormulaMachine entity) {
    	ArrayList<FormulaMachine> machineList = iFormulaMachineService.listRecipeMachine(entity);
        return AjaxResult.success(machineList);
    }
}
