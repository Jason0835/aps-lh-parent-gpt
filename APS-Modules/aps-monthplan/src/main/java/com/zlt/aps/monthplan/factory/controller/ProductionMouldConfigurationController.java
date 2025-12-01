package com.zlt.aps.monthplan.factory.controller;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.Logical;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMouldingProductParamDto;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMouldConfiguration;
import com.zlt.aps.monthplan.factory.service.IProductionMouldConfigurationService;
import com.zlt.common.controller.BusiController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductionMouldConfigurationController.java
 * 描    述：模具正在生产的品种 控制层类：....
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
@RestController
@RequiredArgsConstructor
@RequestMapping("/productionMouldConfiguration")
public class ProductionMouldConfigurationController extends BusiController<ProductionMouldConfiguration> {

    private final IProductionMouldConfigurationService productionMouldConfigurationService;

    /**
     * 查询模具正在生产的品种列表
     */
    @RequiresPermissions("monthplan:productionMouldConfiguration:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody ProductionMouldConfiguration queryVO) {
        try {
            startPage(getOrderBy());
            List<ProductionMouldConfiguration> list = productionMouldConfigurationService.selectList(queryVO);
            return getDataTable(list);
        } finally {
            this.clearPage();
        }
    }

    protected String getOrderBy() {
        return "year desc,month desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.productionMouldConfiguration.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions(value = {"monthplan:productionMouldConfiguration:add", "monthplan:productionMouldConfiguration:edit"}, logical = Logical.OR)
    @ApiOperation("保存")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody ProductionMouldConfiguration billVO) {
        return productionMouldConfigurationService.saveConfiguration(billVO);
    }

    /**
     * 生产模具正在生产的品种
     *
     * @param param
     * @return
     */
    @ApiOperation("生成模具正在生产的品种")
    @PostMapping("/buildMouldingProduct")
    public AjaxResult buildMouldingProduct(@RequestBody FactoryMouldingProductParamDto param) {
        if (null == param || StringUtils.isEmpty(param.getFactoryCode()) || null == param.getVulcanizingDate()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.productionMouldConfiguration.param.noEmpty"));
        }
        return productionMouldConfigurationService.buildMouldingProduct(param);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.productionMouldConfiguration.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions("monthplan:productionMouldConfiguration:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return toAjax(productionMouldConfigurationService.removeByIds(ids));
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody ProductionMouldConfiguration billVO) {
        return productionMouldConfigurationService.checkUnique(billVO);
    }

    /**
     * 根据集合导入模具正在生产的品种数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions("monthplan:productionMouldConfiguration:import")
    @Log(title = "ui.data.column.productionMouldConfiguration.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.commonImport(importContext, updateSupport);
    }

    @Override
    protected AjaxResult doImportData(List<ProductionMouldConfiguration> list, boolean updateSupport, long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return productionMouldConfigurationService.doImportData(list, updateSupport, importLogId);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("monthplan:productionMouldConfiguration:export")
    @Log(title = "模具正在生产的品种", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody ProductionMouldConfiguration queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.commonExport(queryVO, fileName, response);
    }

    @Override
    protected List<ProductionMouldConfiguration> listExportData(ProductionMouldConfiguration query) {
        try {
            startPage(getOrderBy());
            return productionMouldConfigurationService.selectList(query);
        } finally {
            this.clearPage();
        }
    }

}
