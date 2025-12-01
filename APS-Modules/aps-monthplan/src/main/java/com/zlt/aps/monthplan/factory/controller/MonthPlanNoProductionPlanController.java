package com.zlt.aps.monthplan.factory.controller;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionPlan;
import com.zlt.aps.monthplan.factory.service.IMonthPlanNoProductionPlanService;
import com.zlt.common.controller.BusiController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MonthPlanNoProductionPlanController.java
* 描    述：分厂月生产计划排产过程-未排产计划 控制层类：....
*@author zlt
*@date 2025-03-21
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "分厂月生产计划排产过程-未排产计划")
@RestController
@RequestMapping("/monthPlanNoProductionPlan")
public class MonthPlanNoProductionPlanController extends BusiController<MonthPlanNoProductionPlan> {

    @Autowired
    private IMonthPlanNoProductionPlanService monthPlanNoProductionPlanService;

    /**
     * 查询分厂月生产计划排产过程-未排产计划列表
     */
    @RequiresPermissions("monthplan:monthPlanNoProductionPlan:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MonthPlanNoProductionPlan queryVO) {
        startPage(getOrderBy());
        List<MonthPlanNoProductionPlan> list = monthPlanNoProductionPlanService.selectList(queryVO);
        return getDataTable(list);
    }

    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "monthplan:monthPlanNoProductionPlan:export")
    @Log(title = "分厂月生产计划排产过程-未排产计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody MonthPlanNoProductionPlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.commonExport(queryVO, fileName, response);
    }

    @Override
    protected List<MonthPlanNoProductionPlan> listExportData(MonthPlanNoProductionPlan query) {
        startPage(getOrderBy());
        return monthPlanNoProductionPlanService.selectList(query);
    }


}
