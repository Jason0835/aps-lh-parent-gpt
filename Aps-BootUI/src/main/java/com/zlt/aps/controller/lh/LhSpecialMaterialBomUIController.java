package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.lh.api.domain.entity.LhSpecialMaterialBom;
import com.zlt.aps.lh.api.service.ILhSpecialMaterialBomRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * 特殊物料清单配置 UI控制层
 *
 * @author zlt
 * @date 2026-05-06
 */
@Slf4j
@Api(tags = "特殊物料清单配置管理")
@Controller
@RequestMapping("/lh/lhSpecialMaterialBom")
public class LhSpecialMaterialBomUIController extends BaseUIController<LhSpecialMaterialBom> {

    @Autowired
    private ILhSpecialMaterialBomRemoteService iLhSpecialMaterialBomService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @PostMapping("/list")
    @RequiresPermissions("lh:lhSpecialMaterialBom:list")
    @ResponseBody
    public TableDataInfo list(LhSpecialMaterialBom lhSpecialMaterialBom) {
        return iLhSpecialMaterialBomService.list(lhSpecialMaterialBom);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions({"lh:lhSpecialMaterialBom:edit", "lh:lhSpecialMaterialBom:add"})
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(LhSpecialMaterialBom lhSpecialMaterialBom) {
        if (UserConstants.NOT_UNIQUE.equals(iLhSpecialMaterialBomService.checkUnique(lhSpecialMaterialBom))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhSpecialMaterialBom.notUnique"));
        }
        // 分类冲突校验：同一物料/结构下不能同时存在芯片胎、19.5寸宽基和22.5寸宽基
        String categoryConflict = iLhSpecialMaterialBomService.checkCategoryConflict(lhSpecialMaterialBom);
        if (categoryConflict != null) {
            return AjaxResult.error(categoryConflict);
        }
        return iLhSpecialMaterialBomService.save(lhSpecialMaterialBom);
    }

    /**
     * 删除特殊物料清单配置
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("lh:lhSpecialMaterialBom:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(@RequestParam String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhSpecialMaterialBomService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(LhSpecialMaterialBom lhSpecialMaterialBom) {
        return iLhSpecialMaterialBomService.checkUnique(lhSpecialMaterialBom);
    }

    /**
     * 校验分类冲突
     */
    @ApiOperation("校验分类冲突")
    @PostMapping("/checkCategoryConflict")
    @ResponseBody
    public String checkCategoryConflict(LhSpecialMaterialBom lhSpecialMaterialBom) {
        return iLhSpecialMaterialBomService.checkCategoryConflict(lhSpecialMaterialBom);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称
     */
    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    /**
     * 继承时重写方法
     */
    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 继承时重写方法
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.lhSpecialMaterialBom.modelName");
    }

    /**
     * 下载导入模板
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<LhSpecialMaterialBom> util = new ExcelUtil<>(LhSpecialMaterialBom.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * 数据导出
     */
    @ApiOperation("数据导出")
    @RequiresPermissions("lh:lhSpecialMaterialBom:export")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, LhSpecialMaterialBom entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iLhSpecialMaterialBomService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 数据导入
     */
    @RequiresPermissions("lh:lhSpecialMaterialBom:import")
    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("数据导入")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        AjaxResult ajaxResult = iLhSpecialMaterialBomService.importData(context, updateSupport);
        return ajaxResult;
    }
}
