package com.zlt.aps.mp.factory.controller;

import com.ruoyi.common.core.utils.PageUtils;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.mp.api.domain.entity.ProductionMonthPlanInit;
import com.zlt.aps.mp.factory.service.IProductionMonthPlanInitService;
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
 * 描    述：工厂月计划初始化 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20251205
 */
@Slf4j
@Api(tags = "工厂月计划初始化业务-服务类")
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
            List<ProductionMonthPlanInit> list = productionMonthPlanInitService.getDataList(queryVO);
            return getDataTable(list);
        } finally {
            PageUtils.clearPage();
        }
    }

    /**
     * 导出列表
     */
    @RequiresPermissions("monthplan:productionMonthPlanInit:export")
    @Log(title = "工厂月计划初始化", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody ProductionMonthPlanInit condition, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.commonExport(condition, fileName, response);
    }

    @Override
    protected List<ProductionMonthPlanInit> listExportData(ProductionMonthPlanInit obj) {
        return productionMonthPlanInitService.getDataList(obj);
    }
}
