package com.zlt.mix.controller.setting;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

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
import com.zlt.mix.setting.api.domain.entity.FactoryGlueAreaRelation;
import com.zlt.mix.setting.api.service.IFactoryGlueAreaRelationService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 分厂胶料与密炼区对应关系Controller
 * @author zlt
 * @date 2022-11-22
 */
@Api(tags = "分厂胶料与密炼区对应关系")
@Controller
@RequestMapping("/setting/factoryGlueAreaRelation")
public class FactoryGlueAreaRelationController extends BaseController {

    @Autowired
    private IFactoryGlueAreaRelationService iFactoryGlueAreaRelationService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    private final String prefix = "setting/factoryGlueAreaRelation";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:factoryGlueAreaRelation:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/factoryGlueAreaRelation";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("tFactoryGlueAreaRelation", new FactoryGlueAreaRelation());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("tFactoryGlueAreaRelation", iFactoryGlueAreaRelationService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询分厂胶料与密炼区对应关系列表
     */
    @ApiOperation("根据条件查询分厂胶料与密炼区对应关系列表")
    @RequiresPermissions("setting:factoryGlueAreaRelation:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(FactoryGlueAreaRelation entity) {
        return iFactoryGlueAreaRelationService.list(entity);
    }

    /**
     * 修改或新增分厂胶料与密炼区对应关系
     */
    @ApiOperation("修改或新增分厂胶料与密炼区对应关系")
    @RequiresPermissions("setting:factoryGlueAreaRelation:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult editSave(FactoryGlueAreaRelation tFactoryGlueAreaRelation) {
        String unique = this.checkFactoryGlueAreaRelationUnique(tFactoryGlueAreaRelation);
        if (unique.equals(ZltConstant.NOT_UNIQUE)) {
            return AjaxResult.error(I18nUtil.getMessage("setting.factoryGlueAreaRelation.database.unique"));
        }
        AjaxResult ajaxResult = null;
        if (tFactoryGlueAreaRelation.getId() != null){
            ajaxResult = iFactoryGlueAreaRelationService.edit(tFactoryGlueAreaRelation);
        } else{
            ajaxResult = iFactoryGlueAreaRelationService.add(tFactoryGlueAreaRelation);
        }
        return ajaxResult;
    }

    /**
     * 删除分厂胶料与密炼区对应关系
     */
    @ApiOperation("删除分厂胶料与密炼区对应关系（id不为空）")
    @RequiresPermissions("setting:factoryGlueAreaRelation:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iFactoryGlueAreaRelationService.remove(arr);
    }

    /**
     * 校验分厂胶料与密炼区对应关系唯一性
     */
    @ApiOperation("校验分厂胶料与密炼区对应关系唯一性")
    @PostMapping("/checkFactoryGlueAreaRelationUnique")
    @ResponseBody
    public String checkFactoryGlueAreaRelationUnique(FactoryGlueAreaRelation tFactoryGlueAreaRelation) {
        return iFactoryGlueAreaRelationService.checkFactoryGlueAreaRelationUnique(tFactoryGlueAreaRelation);
    }

    /**
     * 导出分厂胶料与密炼区对应关系
     */
    @ApiOperation("导出分厂胶料与密炼区对应关系")
    @RequiresPermissions("setting:factoryGlueAreaRelation:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,FactoryGlueAreaRelation tFactoryGlueAreaRelation) throws IOException {
        String fileName = I18nUtil.getMessage("setting.factoryGlueAreaRelation.modelName");
        List<FactoryGlueAreaRelation> list = iFactoryGlueAreaRelationService.getList(tFactoryGlueAreaRelation);
        ExcelUtil<FactoryGlueAreaRelation> util = new ExcelUtil<>(FactoryGlueAreaRelation.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, tFactoryGlueAreaRelation.toString(),ZltConstant.PROCEDURE_CODE_SETTING);
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
        String fileName = I18nUtil.getMessage("setting.factoryGlueAreaRelation.modelName");
        ExcelUtil<FactoryGlueAreaRelation> util = new ExcelUtil<>(FactoryGlueAreaRelation.class);
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
    @RequiresPermissions("setting:factoryGlueAreaRelation:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_SETTING,
                I18nUtil.getMessage("setting.factoryGlueAreaRelation.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        ExcelUtil<FactoryGlueAreaRelation> util = new ExcelUtil<>(FactoryGlueAreaRelation.class);
        InputStream in = new ByteArrayInputStream(data);
        List<FactoryGlueAreaRelation> list = util.importExcel(in);
        AjaxResult ajaxResult = iFactoryGlueAreaRelationService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

}
