package com.zlt.aps.monthplan.factory.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.maindata.service.IPlanOrderSortConfigurationService;
import com.zlt.aps.monthplan.api.domain.vo.PlanOrderSortConfigurationVo;
import com.zlt.common.utils.PubUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：PlanOrderSortConfigurationController.java
 * 描    述：业务排序配置 控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-21
 */
@Slf4j
@Api(tags = "业务排序配置")
@RestController
@RequestMapping("/businessSortConfiguration")
public class PlanOrderSortConfigurationController extends BaseController {

    @Autowired
    private IPlanOrderSortConfigurationService planOrderSortConfigurationService;

    /**
     * 查询库存对冲顺序配置
     *
     * @return
     */
    @ApiOperation("查询库存对冲顺序配置")
    @PostMapping("/getStockHedgingConfigurationList")
    AjaxResult getStockHedgingConfigurationList() {
        return AjaxResult.success(planOrderSortConfigurationService.getStockHedgingConfigurationList());
    }

    /**
     * 查询月份排产配置顺序
     *
     * @return
     */
    @ApiOperation("查询月份排产配置顺序")
    @PostMapping("/getPlanOrderSortConfigurationList")
    AjaxResult getPlanOrderSortConfigurationList() {
        return AjaxResult.success(planOrderSortConfigurationService.getPlanOrderSortConfigurationList());
    }

    /**
     * 保存库存对冲顺序配置
     *
     * @return
     */
    @ApiOperation("保存库存对冲顺序配置")
    @PostMapping("/saveStockHedgingConfiguration")
    AjaxResult saveStockHedgingConfiguration(@RequestBody PlanOrderSortConfigurationVo planOrderSortConfigurationVo){
        planOrderSortConfigurationService.saveStockHedgingConfiguration(planOrderSortConfigurationVo);
        return AjaxResult.success();
    }

    /**
     * 保存月份排产顺序配置
     *
     * @return
     */
    @ApiOperation("保存月份排产顺序配置")
    @PostMapping("/savePlanOrderConfiguration")
    AjaxResult savePlanOrderConfiguration(@RequestBody PlanOrderSortConfigurationVo planOrderSortConfigurationVo){
        planOrderSortConfigurationService.savePlanOrderConfiguration(planOrderSortConfigurationVo);
        return AjaxResult.success();
    }
}
