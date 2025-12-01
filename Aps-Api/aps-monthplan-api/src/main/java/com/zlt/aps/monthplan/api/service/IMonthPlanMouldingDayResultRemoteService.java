package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.dto.ChangeSpecCodeMouldingDayResultParam;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanMouldingDayResult;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMonthPlanMouldingDayResultRemoteService.java
 * 描    述：IMonthPlanMouldingDayResultRemoteService分厂月生产计划排产过程-模具排产结果汇总前端接口
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
@FeignClient(contextId = "IMonthPlanMouldingDayResultRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMonthPlanMouldingDayResultRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mouldingDayResult/list")
    TableDataInfo list(@RequestBody MonthPlanMouldingDayResult QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mouldingDayResult/save")
    AjaxResult save(@RequestBody MonthPlanMouldingDayResult monthPlanMouldingDayResult);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mouldingDayResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mouldingDayResult/{id}")
    MonthPlanMouldingDayResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mouldingDayResult/checkUnique")
    String checkUnique(@RequestBody MonthPlanMouldingDayResult monthPlanMouldingDayResultVO);

    /**
     * 导出分厂月生产计划排产过程-模具排产结果汇总列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mouldingDayResult/exportData/{fileName}")
    byte[] exportData(@RequestBody MonthPlanMouldingDayResult queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入分厂月生产计划排产过程-模具排产结果汇总数据
     */
    @ApiOperation("导入分厂月生产计划排产过程-模具排产结果汇总")
    @PostMapping("/mouldingDayResult/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);


    @ApiOperation("查询对应年月+分厂+需求计划版本的分厂月计划版本")
    @PostMapping("/mouldingDayResult/productionVersionList")
    AjaxResult productionVersionList(@RequestBody MonthPlanMouldingDayResult query);

    /**
     * 对排产计划进行换硫化规格代号--背后是换成形法
     *
     * @param changeSpecCodePlan
     * @return
     */
    @ApiOperation("对排产计划进行换硫化规格代号--背后是换成形法")
    @PostMapping("/mouldingDayResult/changeSpecCode")
    AjaxResult changeSpecCode(@RequestBody ChangeSpecCodeMouldingDayResultParam changeSpecCodePlan);

    /**
     * 统计分厂月生产计划排产
     */
    @ApiOperation("统计分厂月生产计划排产")
    @PostMapping("/mouldingDayResult/statistics")
    AjaxResult statistics(@RequestBody MonthPlanMouldingDayResult QueryVO);

    /**
     * 统计每日排产的规格数及日排产总量
     *
     * @param query
     * @return
     */
    @ApiOperation("统计分厂月生产计划排产")
    @PostMapping("/mouldingDayResult/statisticsDay")
    AjaxResult getStatisticsDay(@RequestBody MonthPlanMouldingDayResult query);

    /**
     * 查询分厂月生产计划合并SKU-合并SKU
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    @ApiOperation("查询分厂月生产计划合并SKU-合并SKU")
    @PostMapping("/mouldingDayResult/listFacProduct")
    public TableDataInfo listFacProduct(@RequestBody MonthPlanMouldingDayResult queryVO);

    /**
     * 导出分厂月生产计划合并SKU-合并SKU
     *
     * @param queryVO  查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出分厂月生产计划合并SKU-合并SKU")
    @PostMapping("/mouldingDayResult/exportFacProductData/{fileName}")
    public byte[] exportFacProductData(@RequestBody MonthPlanMouldingDayResult queryVO, @PathVariable("fileName") String fileName);

    /**
     * 查询月计划排产统计
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    @ApiOperation("查询月计划排产统计")
    @PostMapping("/mouldingDayResult/listFacProductStatistics")
    public TableDataInfo listFacProductStatistics(@RequestBody MonthPlanMouldingDayResult queryVO);

    /**
     * 导出月计划排产统计
     *
     * @param queryVO  查询条件
     * @param fileName 文件名
     * @return 结果
     */
    @ApiOperation("导出月计划排产统计")
    @PostMapping("/mouldingDayResult/exportFacProductStatisticsData/{fileName}")
    public byte[] exportFacProductStatisticsData(@RequestBody MonthPlanMouldingDayResult queryVO, @PathVariable("fileName") String fileName);
}
