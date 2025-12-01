package com.zlt.aps.controller.monthplan;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMouldingProductParamDto;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMouldConfiguration;
import com.zlt.aps.monthplan.api.service.IProductionMouldConfigurationRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.Logical;
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
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductionMouldConfigurationUIController.java
 * 描    述：模具正在生产的品种 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-28
 */
@Slf4j
@Api(tags = "模具正在生产的品种")
@Controller
@RequestMapping("/monthplan/productionMouldConfiguration")
public class ProductionMouldConfigurationUIController extends BaseUIController<ProductionMouldConfiguration> {

    @Autowired
    private IProductionMouldConfigurationRemoteService iProductionMouldConfigurationService;

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:productionMouldConfiguration:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(ProductionMouldConfiguration productionMouldConfiguration) {
        return iProductionMouldConfigurationService.list(productionMouldConfiguration);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions(value = {"monthplan:productionMouldConfiguration:edit", "monthplan:productionMouldConfiguration:add"}, logical = Logical.OR)
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(ProductionMouldConfiguration productionMouldConfiguration) {
        if (UserConstants.NOT_UNIQUE.equals(iProductionMouldConfigurationService.checkUnique(productionMouldConfiguration))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.productionMouldConfiguration.checkUnique"));
        }

        return iProductionMouldConfigurationService.save(productionMouldConfiguration);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("生成-正在生产的品种")
    @RequiresPermissions("monthplan:productionMouldConfiguration:building")
    @PostMapping("/buildMouldingProduct")
    @ResponseBody
    public AjaxResult buildMouldingProduct(@RequestBody FactoryMouldingProductParamDto param) {
        if (null == param || StringUtils.isEmpty(param.getFactoryCode()) || null == param.getVulcanizingDate()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.productionMouldConfiguration.param.noEmpty"));
        }

        return iProductionMouldConfigurationService.buildMouldingProduct(param);
    }

    /**
     * 删除模具正在生产的品种
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:productionMouldConfiguration:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iProductionMouldConfigurationService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验模具正在生产的品种唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(ProductionMouldConfiguration productionMouldConfiguration) {
        return iProductionMouldConfigurationService.checkUnique(productionMouldConfiguration);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.productionMouldConfiguration.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<ProductionMouldConfiguration> util = new ExcelUtil<>(ProductionMouldConfiguration.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, ProductionMouldConfiguration entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iProductionMouldConfigurationService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

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
        AjaxResult ajaxResult = iProductionMouldConfigurationService.importData(context, false);
        return ajaxResult;
    }
}
