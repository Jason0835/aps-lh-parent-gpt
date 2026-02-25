package com.zlt.aps.mp.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.zlt.aps.mp.api.domain.vo.PlanOrderSortConfigurationVo;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import com.ruoyi.common.core.web.domain.AjaxResult;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IPlanOrderSortConfigurationService.java
 * 描    述：IPlanOrderSortConfigurationService业务排序配置前端接口
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
@FeignClient(contextId = "IPlanOrderSortConfigurationService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IPlanOrderSortConfigurationService {

    /**
     * 查询库存对冲顺序配置
     *
     * @return
     */
    @ApiOperation("查询库存对冲顺序配置")
    @PostMapping("/businessSortConfiguration/getStockHedgingConfigurationList")
    AjaxResult getStockHedgingConfigurationList();

    /**
     * 获取月份排产配置顺序
     *
     * @return
     */
    @ApiOperation("获取月份排产配置顺序")
    @PostMapping("/businessSortConfiguration/getPlanOrderSortConfigurationList")
    AjaxResult getPlanOrderSortConfigurationList();

    /**
     * 保存库存对冲顺序
     *
     * @return
     */
    @ApiOperation("保存库存对冲顺序配置")
    @PostMapping("/businessSortConfiguration/saveStockHedgingConfiguration")
    AjaxResult saveStockHedgingConfiguration(@RequestBody PlanOrderSortConfigurationVo planOrderSortConfigurationVo);

    /**
     * 保存月份排产顺序配置
     *
     * @return
     */
    @ApiOperation("保存月份排产顺序配置")
    @PostMapping("/businessSortConfiguration/savePlanOrderConfiguration")
    AjaxResult savePlanOrderConfiguration(@RequestBody PlanOrderSortConfigurationVo planOrderSortConfigurationVo);
}
