package com.zlt.mix.controller.setting;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.schedule.api.domain.entity.GlueCollectPlan;
import com.zlt.mix.setting.api.domain.entity.MesBasMaterial;
import com.zlt.mix.setting.api.service.IMesBasMaterialService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import javax.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import org.apache.poi.ss.usermodel.Workbook;
import com.zlt.mix.common.utils.ExportUtil;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.zlt.mix.common.utils.ImportUtil;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.io.*;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.zlt.file.encryptbyll.FileEncryptUtils;


/**
 * 物料Controller
 * @author Joran.zhang
 * @date 2022-05-30
 */
@Api(tags = "物料")
@Controller
@RequestMapping("/setting/material")
public class MesBasMaterialController extends BaseController {

    @Resource
    private IMesBasMaterialService iMesBasMaterialService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;

    private final String prefix = "setting/material";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:material:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/material";
    }

    @ApiOperation("根据条件查询物料列表")
    @RequiresPermissions("setting:material:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listMesBasMaterial(MesBasMaterial entity) {
        return iMesBasMaterialService.listMesBasMaterial(entity);
    }

    /**
     * 跳转至选物料类型
     */
    @GetMapping("/toChooseGlue/{id}")
    public String toChooseMachine(@PathVariable("id") Long id, ModelMap modelMap) {
        modelMap.put("mesBasMaterial", iMesBasMaterialService.getMesBasMaterialInfo(id));
        return prefix + "/chooseGlue";
    }

    /**
     * 汇总胶料需求计划选机台
     */
    @ApiOperation("汇总胶料需求计划选机台")
    @RequiresPermissions("setting:material:chooseGlue")
    @PostMapping("/chooseGlue")
    @ResponseBody
    public AjaxResult chooseGlue(MesBasMaterial mesBasMaterial) {
        return iMesBasMaterialService.chooseGlue(mesBasMaterial);
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    @RequiresPermissions("setting:material:edit")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mesBasMaterial", iMesBasMaterialService.getMesBasMaterialInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("修改或新增物料")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveMesBasMaterial(MesBasMaterial mesBasMaterial) {
        return iMesBasMaterialService.saveMesBasMaterial(mesBasMaterial);
    }

    @ApiOperation("删除物料（id不为空）")
    @RequiresPermissions("setting:material:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeMesBasMaterial(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMesBasMaterialService.deleteMesBasMaterial(arr);
    }

    @ApiOperation("校验物料唯一性")
    @PostMapping("/checkMesBasMaterialUnique")
    @ResponseBody
    public String checkMesBasMaterialUnique(MesBasMaterial mesBasMaterial) {
        return iMesBasMaterialService.checkMesBasMaterialUnique(mesBasMaterial);
    }

    /**
     * 导出物料
     */
    @ApiOperation("导出物料")
    @RequiresPermissions("setting:material:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,MesBasMaterial mesBasMaterial) throws IOException {
        String fileName = I18nUtil.getMessage("setting.material.modelName");
        List<MesBasMaterial> list = iMesBasMaterialService.exportData(mesBasMaterial);
        ExcelUtil<MesBasMaterial> util = new ExcelUtil<>(MesBasMaterial. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, mesBasMaterial.toString(),ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.material.modelName");
        ExcelUtil<MesBasMaterial> util = new ExcelUtil<>(MesBasMaterial.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file 要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("setting:material:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.material.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        ExcelUtil<MesBasMaterial> util = new ExcelUtil<>(MesBasMaterial.class);
        List<MesBasMaterial> list = util.importExcel(in);
        //导入数据
        AjaxResult ajaxResult = iMesBasMaterialService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
