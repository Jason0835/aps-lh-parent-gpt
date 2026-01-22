package com.zlt.aps.itf.finereport;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;

import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 分厂月度计划控制台业务
 *
 * @author ZLT
 * @date 20250213
 */
@FeignClient(contextId = "IFinereportService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.itf:/itf}")
public interface IFinereportService {
    /**
     * 帆软报表公共页面
     *
     * @return 结果集合
     */
    @ApiOperation("报表预览公共页面")
    @GetMapping("/finereport/reportView/{reportCode}")
    AjaxResult reportView(@PathVariable("reportCode") String reportCode);
    
    /**
     * 同步已计划未发货数据
     *
     * @param planedNotShipParamVo 查询条件
     * @return 结果集合
     */
    @ApiOperation("同步已计划未发货数据")
    @GetMapping("/finereport/inventoryAgeAnalysis")
    AjaxResult inventoryAgeAnalysis();

    /**
     * 查询单胎总重报表
     *
     * @return 结果集合
     */
    @ApiOperation("查询单胎总重报表")
    @GetMapping("/finereport/singleTireTotalWeight")
    AjaxResult singleTireTotalWeight();

    /**
     * 越南工厂成型机数据报表
     *
     * @return 结果
     */
    @ApiOperation("越南工厂成型机数据报表")
    @GetMapping("/finereport/factoryMoldingMachine")
    AjaxResult factoryMoldingMachine();

    /**
     * 越南工厂硫化机数据报表
     *
     * @return 结果
     */
    @ApiOperation("越南工厂硫化机数据报表")
    @GetMapping("/finereport/factoryVulcanizingMachine")
    AjaxResult factoryVulcanizingMachine();

    /**
     * 越南工厂结构在机数据报表
     *
     * @return 结果
     */
    @ApiOperation("越南工厂结构在机数据报表")
    @GetMapping("/finereport/productionStructure")
    AjaxResult productionStructure();

    /**
     * 越南工厂年度产量报表
     */
    @ApiOperation("越南工厂年度产量报表")
    @GetMapping("/finereport/productionYear")
    AjaxResult productionYear();

    /**
     * 越南工厂结构在机数据报表
     *
     * @return 结果
     */
    @ApiOperation("订单冲减分配报表")
    @GetMapping("/finereport/orderOffset")
    AjaxResult orderOffset();
}
