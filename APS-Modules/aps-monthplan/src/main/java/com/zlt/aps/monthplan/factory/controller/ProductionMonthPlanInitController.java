package com.zlt.aps.monthplan.factory.controller;

import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMonthPlanInit;
import com.zlt.aps.monthplan.factory.service.IProductionMonthPlanInitService;
import com.zlt.common.controller.BusiController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductionMonthPlanInitController.java
 * 描    述：分厂月生产计划排产过程-计划初始化 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-17
 */
@Slf4j
@Api(tags = "分厂月生产计划排产过程-计划初始化")
@RestController
@RequestMapping("/productionMonthPlanInit")
@RequiredArgsConstructor
public class ProductionMonthPlanInitController extends BusiController<ProductionMonthPlanInit> {

    private final IProductionMonthPlanInitService productionMonthPlanInitService;

    /**
     * 查询分厂月生产计划排产过程-计划初始化列表
     */
    @RequiresPermissions("monthplan:productionMonthPlanInit:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody ProductionMonthPlanInit queryVO) {
        try {
            startPage();
            List<ProductionMonthPlanInit> list = productionMonthPlanInitService.selectList(queryVO);
            return getDataTable(list);
        } finally {
            PageUtils.clearPage();
        }
    }

    /**
     //  * 保存
     //  */
    // @Log(title = "ui.data.column.productionMonthPlanInit.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    // @RequiresPermissions("monthplan:productionMonthPlanInit:save")
    // @ApiOperation("保存")
    // @PostMapping("/save")
    // @Override
    // public AjaxResult save(@RequestBody ProductionMonthPlanInit billVO) {
    //     return super.save(billVO);
    // }
    //
    // /**
    //  * 删除
    //  */
    // @Log(title = "ui.data.column.productionMonthPlanInit.modelName", businessType = BusinessType.DELETE)
    // @RequiresPermissions("monthplan:productionMonthPlanInit:remove")
    // @ApiOperation("删除")
    // @DeleteMapping("/remove")
    // @Override
    // public AjaxResult removeByIds(@RequestBody List<Long> ids) {
    //     return super.removeByIds(ids);
    // }


    // /**
    //  * 获取分厂月生产计划排产过程-计划初始化详细信息
    //  */
    // @RequiresPermissions("monthplan:productionMonthPlanInit:query")
    // @ApiOperation("获取详细信息")
    // @GetMapping(value = "/{billId}")
    // @Override
    // public ProductionMonthPlanInit getInfo(@PathVariable("billId") Long billId) {
    //     return super.getInfo(billId);
    // }


    // /**
    //  * 根据集合导入分厂月生产计划排产过程-计划初始化数据
    //  *
    //  * @param importContext 导入上下文
    //  * @param updateSupport 已存在记录是否更新
    //  * @return 结果
    //  */
    // @RequiresPermissions("monthplan:productionMonthPlanInit:import")
    // @Log(title = "ui.data.column.productionMonthPlanInit.modelName", businessType = BusinessType.IMPORT)
    // @ApiOperation("导入数据")
    // @PostMapping("/importData")
    // @Override
    // public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
    //     return super.importData(importContext, updateSupport);
    // }

    /**
     * 导出列表
     */
    @RequiresPermissions("monthplan:productionMonthPlanInit:export")
    @Log(title = "分厂月生产计划排产过程-计划初始化", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody ProductionMonthPlanInit queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.commonExport(queryVO, fileName, response);
    }

    @Override
    protected List<ProductionMonthPlanInit> listExportData(ProductionMonthPlanInit obj) {
        return productionMonthPlanInitService.selectList(obj);
    }
}
