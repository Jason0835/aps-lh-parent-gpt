package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdFinalQueryDto;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanRequireStock;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanDayProductionInfoVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanProdFinalVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 分厂月份计划----月计划定稿版本服务
 *
 * @author ZLT
 * @date 20250211
 */
@FeignClient(contextId = "IFactoryMonthPlanProdFinalRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IFactoryMonthPlanProdFinalRemoteService {

    /**
     * 查询列表
     *
     * @param QueryVO
     * @return
     */
    @ApiOperation("查询列表")
    @PostMapping("/factoryMonthPlanProdFinal/list")
    TableDataInfo list(@RequestBody FactoryMonthPlanProdFinal QueryVO);

    /**
     * 根据查询条件，获取对应的月计划定稿数据
     *
     * @param queryCondition
     * @return
     */
    @ApiOperation("根据查询条件，获取对应的月计划定稿数据")
    @PostMapping("/factoryMonthPlanProdFinal/getProdResult")
    List<FactoryMonthPlanProdFinalVo> getProdResult(@RequestBody FactoryMonthPlanProdFinalQueryDto queryCondition);

    /**
     * 根据查询条件，获取某日的月计划排产数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    @ApiOperation("根据查询条件，获取某日的月计划排产数据")
    @PostMapping("/factoryMonthPlanProdFinal/getDayProductionInfo")
    List<FactoryMonthPlanDayProductionInfoVo> getMonthPlanProductionInfo(@RequestBody FactoryMonthPlanProdFinalQueryDto queryCondition);

    /**
     * 根据查询条件，获取某日对应的月计划排产数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    @ApiOperation("根据查询条件，获取对应的月计划排产数据")
    @PostMapping("/factoryMonthPlanProdFinal/getMonthPlanProdResult")
    List<FactoryMonthPlanProdFinalVo> getMonthPlanProdResult(@RequestBody FactoryMonthPlanProdFinalQueryDto queryCondition);


    /**
     * 获取销售需求计划
     *
     * @return
     */
    @ApiOperation("根据查询条件，获取销售需求计划")
    @PostMapping("/factoryMonthPlanProdFinal/getSaleMonthPlanRequireStock")
    List<MonthPlanRequireStock> getSaleMonthPlanRequireStock(@RequestParam("monthPlanVersion") String monthPlanVersion);
    /**
     * 定稿
     *
     * @param factoryMonthPlanProdFinal
     * @return
     */
    @ApiOperation("定稿 - 年月+分厂+需求计划版本+分厂月计划版本")
    @PostMapping("/factoryMonthPlanProdFinal/finalized")
    AjaxResult finalized(@RequestBody FactoryMonthPlanProdFinal factoryMonthPlanProdFinal);

    /**
     * 导入调整计划
     *
     * @param importContext 导入内容
     * @param updateSupport 是否更新
     * @return
     */
    @ApiOperation("导入分厂月生产计划排产过程-模具排产结果汇总")
    @PostMapping("/factoryMonthPlanProdFinal/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 导出排产计划
     *
     * @param prodFinal
     * @param fileName
     * @return
     */
    @ApiOperation("导出列表")
    @PostMapping("/factoryMonthPlanProdFinal/exportData/{fileName}")
    byte[] exportData(@RequestBody FactoryMonthPlanProdFinal prodFinal, @PathVariable("fileName") String fileName);

    /**
     * 统计分厂月生产计划排产结果
     *
     * @param prodFinal
     * @return
     */
    @ApiOperation("统计分厂月生产计划排产结果-排产结果列表")
    @PostMapping("/factoryMonthPlanProdFinal/statistics")
    AjaxResult statistics(@RequestBody FactoryMonthPlanProdFinal prodFinal);

    /**
     * 统计每日排产的规格数及日排产总量
     *
     * @param query
     * @return
     */
    @ApiOperation("统计分厂月生产计划排产")
    @PostMapping("/factoryMonthPlanProdFinal/statisticsDay")
    AjaxResult getStatisticsDay(@RequestBody FactoryMonthPlanProdFinal query);

    /**
     * 获取月份排产模式--Date 不为空则表示非自然月排产，Date为空表示自然月排产
     *
     * @param condition 查询条件
     * @return
     */
    @ApiOperation("获取月份排产模式--Date 不为空则表示非自然月排产，Date为空表示自然月排产")
    @PostMapping("/factoryMonthPlanProdFinal/getProductionMonthType")
    AjaxResult getProductionMonthType(@RequestBody FactoryMonthPlanProdFinal condition);

    /**
     * 下发月计划
     *
     * @param factoryMonthPlanProdFinal 参数
     * @return 结果
     */
    @ApiOperation("下发月计划 - 年月+分厂+需求计划版本+分厂月计划版本")
    @PostMapping("/factoryMonthPlanProdFinal/issueMonthPlan")
    public AjaxResult issueMonthPlan(@RequestBody FactoryMonthPlanProdFinal factoryMonthPlanProdFinal);
}
