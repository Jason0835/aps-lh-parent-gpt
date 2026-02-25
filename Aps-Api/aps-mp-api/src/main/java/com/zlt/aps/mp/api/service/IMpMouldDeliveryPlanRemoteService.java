package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MpMouldDeliveryPlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpMouldDeliveryPlanRemoteService.java
 * 描    述：IMpMouldDeliveryPlanRemoteService模具到货计划前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-05
 */
@FeignClient(contextId = "IMpMouldDeliveryPlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpMouldDeliveryPlanRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mpMouldDeliveryPlan/list")
    TableDataInfo list(@RequestBody MpMouldDeliveryPlan QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mpMouldDeliveryPlan/save")
    AjaxResult save(@RequestBody MpMouldDeliveryPlan mpMouldDeliveryPlan);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mpMouldDeliveryPlan/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mpMouldDeliveryPlan/{id}")
    MpMouldDeliveryPlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mpMouldDeliveryPlan/checkUnique")
    String checkUnique(@RequestBody MpMouldDeliveryPlan mpMouldDeliveryPlanVO);

    /**
     * 导出模具到货计划列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mpMouldDeliveryPlan/exportData/{fileName}")
    byte[] exportData(@RequestBody MpMouldDeliveryPlan queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入模具到货计划数据
     */
    @ApiOperation("导入模具到货计划")
    @PostMapping("/mpMouldDeliveryPlan/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 根据计划发货日期获取计划上机日期
     *
     * @param entity 计划发货日期
     * @return 结果
     */
    @ApiOperation("根据计划发货日期获取计划上机日期")
    @PostMapping("/mpMouldDeliveryPlan/getBoardingDate")
    AjaxResult getBoardingDate(@RequestBody MpMouldDeliveryPlan entity);

    /**
     * 更新主花纹到物料表
     *
     * @param queryVO 参数
     * @return 结果
     */
    @ApiOperation("更新主花纹到物料表")
    @PostMapping("/mpMouldDeliveryPlan/updateMainPatternToMaterial")
    public AjaxResult updateMainPatternToMaterial(@RequestBody MpMouldDeliveryPlan queryVO);
}
