package com.zlt.aps.monthplan.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmStockUpPlan;
import com.zlt.aps.monthplan.api.domain.vo.MdmStockUpPlanVo;
import com.zlt.aps.monthplan.api.domain.vo.QueryCalcStockingParamVo;
import com.zlt.aps.monthplan.api.domain.vo.StockUpPlanExcelVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmStockUpPlanService.java
 * 描    述：IMdmStockUpPlanService备货计划前端接口
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-18
 */
@FeignClient(contextId = "IMdmStockUpPlanService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmStockUpPlanService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmStockUpPlan/list")
    TableDataInfo list(@RequestBody MdmStockUpPlanVo QueryVO);

    /**
     * 生成备货计划
     */
    @ApiOperation("生成备货计划")
    @PostMapping("/mdmStockUpPlan/createStockUpPlan")
    AjaxResult createStockUpPlan(@RequestBody QueryCalcStockingParamVo queryCalcStockingParamVo);

    /**
     * 修改备货计划
     */
    @ApiOperation("修改备货计划")
    @PostMapping("/mdmStockUpPlan/saveStockUpPlan")
    AjaxResult saveStockUpPlan(@RequestBody MdmStockUpPlanVo updateStockUpPlan);

    /**
     * 导入备货计划数据
     */
    @ApiOperation("导入备货计划")
    @PostMapping("/mdmStockUpPlan/importData")
    AjaxResult importData(@RequestBody List<StockUpPlanExcelVo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    @ApiOperation("导出列表")
    @PostMapping("/mdmStockUpPlan/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmStockUpPlan entity, @PathVariable("fileName") String fileName);
}
