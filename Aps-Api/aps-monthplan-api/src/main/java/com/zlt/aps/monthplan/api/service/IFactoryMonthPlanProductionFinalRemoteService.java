package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 分厂月份计划----月计划定稿版本-SKU服务
 *
 * @author ZLT
 * @date 20250924
 */
@FeignClient(contextId = "IFactoryMonthPlanProductionFinalRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IFactoryMonthPlanProductionFinalRemoteService {

    /**
     * 查询列表
     *
     * @param queryCondition
     * @return
     */
    @ApiOperation("查询列表")
    @PostMapping("/factoryMonthPlanFinal/list")
    TableDataInfo list(@RequestBody MonthPlanProductionFinalResult queryCondition);

    /**
     * 导入调整计划
     *
     * @param importContext 导入内容
     * @param updateSupport 是否更新
     * @return
     */
    @ApiOperation("导入分厂月排产计划-含调整及试制量试计划")
    @PostMapping("/factoryMonthPlanFinal/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 导入试制量试计划
     *
     * @param importContext 导入内容
     * @param updateSupport 是否更新
     * @return
     */
    @ApiOperation("导入试制量试计划")
    @PostMapping("/factoryMonthPlanFinal/importTrialProductionPlan")
    AjaxResult importTrialProductionPlan(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);
    /**
     * 导出排产计划
     *
     * @param prodFinal
     * @param fileName
     * @return
     */
    @ApiOperation("导出列表")
    @PostMapping("/factoryMonthPlanFinal/exportData/{fileName}")
    byte[] exportData(@RequestBody MonthPlanProductionFinalResult prodFinal, @PathVariable("fileName") String fileName);

    /**
     * 下载导入模板
     *
     * @param prodFinal
     * @param fileName
     * @return
     */
    @ApiOperation("下载导入模板")
    @PostMapping("/factoryMonthPlanFinal/importTemplate/{fileName}")
    byte[] importTemplate(@RequestBody MonthPlanProductionFinalResult prodFinal, @PathVariable("fileName") String fileName);
    /**
     * 统计分厂月生产计划排产结果
     *
     * @param prodFinal
     * @return
     */
    @ApiOperation("统计分厂月生产计划排产结果-排产结果列表")
    @PostMapping("/factoryMonthPlanFinal/statistics")
    AjaxResult statistics(@RequestBody MonthPlanProductionFinalResult prodFinal);

    /**
     * 统计每日排产的规格数及日排产总量
     *
     * @param query
     * @return
     */
    @ApiOperation("统计分厂月生产计划排产")
    @PostMapping("/factoryMonthPlanFinal/statisticsDay")
    AjaxResult getStatisticsDay(@RequestBody MonthPlanProductionFinalResult query);

    @ApiOperation("输入SAP代码后自动关联出字段")
    @PostMapping("/factoryMonthPlanFinal/linkProductInfoByProductCode")
    AjaxResult linkProductInfoByProductCode(@RequestBody MonthPlanProductionFinalResult query);

    @ApiOperation("输入订单数量后系统自动计算")
    @PostMapping("/factoryMonthPlanFinal/calculateByOrderQty")
    AjaxResult calculateByOrderQty(@RequestBody MonthPlanProductionFinalResult query);

    @ApiOperation("月计划手动调整-新增规格的增量")
    @PostMapping("/factoryMonthPlanFinal/addSpecifications")
    AjaxResult addSpecifications(@RequestBody MonthPlanProductionFinalResult query);

    @ApiOperation("月计划手动调整-编辑计划")
    @PostMapping("/factoryMonthPlanFinal/editPlan")
    AjaxResult editPlan(@RequestBody MonthPlanProductionFinalResult query);

    @ApiOperation("规格直接减量为零")
    @PostMapping("/factoryMonthPlanFinal/subtractSpecification")
    AjaxResult subtractSpecification(@RequestBody MonthPlanProductionFinalResult query);

    /**
     * 导出下载错误日志
     *
     * @param queryVO
     * @param fileName
     * @return
     */
    @ApiOperation("导出下载错误日志")
    @PostMapping("/factoryMonthPlanFinal/exportImportErrorLog/{fileName}")
    byte[] exportImportErrorLog(@RequestBody ImportErrorLog queryVO, @PathVariable("fileName") String fileName);
}
