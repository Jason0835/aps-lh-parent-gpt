package com.zlt.aps.controller.monthplan;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.PlanOrderSortConfiguration;
import com.zlt.aps.mp.api.domain.vo.PlanOrderSortConfigurationVo;
import com.zlt.aps.mp.api.service.IPlanOrderSortConfigurationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：PlanOrderSortConfigurationController.java
 * 描    述：业务排序配置 UI控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@Api(tags = "业务排序配置")
@Controller
@RequestMapping("/monthplan/businessSortConfiguration")
public class PlanOrderSortConfigurationController extends BaseUIController<PlanOrderSortConfiguration> {

    @Autowired
    private IPlanOrderSortConfigurationService planOrderSortConfigurationService;

    /**
     * 查询库存对冲顺序数据
     */
    @ApiOperation("查询库存对冲顺序配置")
    @RequiresPermissions("monthplan:businessSortConfiguration:stockHedgingConfigurationList")
    @PostMapping("/stockHedgingConfigurationList")
    @ResponseBody
    public AjaxResult stockHedgingSortConfigList() {
        return planOrderSortConfigurationService.getStockHedgingConfigurationList();
    }

    /**
     * 查询月份排产顺序配置数据
     */
    @ApiOperation("查询月份排产顺序配置")
    @RequiresPermissions("monthplan:businessSortConfiguration:planOrderSortConfigurationList")
    @PostMapping("/planOrderSortConfigurationList")
    @ResponseBody
    public AjaxResult planOrderSortConfigurationList() {
        return planOrderSortConfigurationService.getPlanOrderSortConfigurationList();
    }

    /**
     * 保存库存对冲顺序数据
     */
    @ApiOperation("保存库存对冲顺序配置")
    @RequiresPermissions("monthplan:businessSortConfiguration:saveStockHedgingConfiguration")
    @PostMapping("/saveStockHedgingConfiguration")
    @ResponseBody
    public AjaxResult saveStockHedgingConfiguration(@RequestBody PlanOrderSortConfigurationVo planOrderSortConfigurationVo) {
        return planOrderSortConfigurationService.saveStockHedgingConfiguration(planOrderSortConfigurationVo);
    }

    /**
     * 保存月份排产顺序配置
     */
    @ApiOperation("保存月份排产顺序配置")
    @RequiresPermissions("monthplan:businessSortConfiguration:savePlanOrderConfiguration")
    @PostMapping("/savePlanOrderConfiguration")
    @ResponseBody
    public AjaxResult savePlanOrderConfiguration(@RequestBody PlanOrderSortConfigurationVo planOrderSortConfigurationVo) {
        return planOrderSortConfigurationService.savePlanOrderConfiguration(planOrderSortConfigurationVo);
    }
}
