package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMustFinishPlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmMustFinishPlanRemoteService.java
 * 描    述：IMdmMustFinishPlanRemoteService必须保证的客户月计划前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-25
 */
@FeignClient(contextId = "IMdmMustFinishPlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmMustFinishPlanRemoteService {

    /**
     * 查询必须保证的客户月计划列表
     */
    @ApiOperation("查询必须保证的客户月计划列表")
    @PostMapping("/mustFinishPlan/list")
    TableDataInfo list(@RequestBody MdmMustFinishPlan mdmMustFinishPlan);

    /**
     * 新增必须保证的客户月计划
     */
    @ApiOperation("新增必须保证的客户月计划")
    @PostMapping("/mustFinishPlan/add")
    AjaxResult add(@RequestBody MdmMustFinishPlan mdmMustFinishPlan);

    /**
     * 修改必须保证的客户月计划
     */
    @ApiOperation("修改必须保证的客户月计划")
    @PostMapping("/mustFinishPlan/edit")
    AjaxResult edit(@RequestBody MdmMustFinishPlan mdmMustFinishPlan);

    /**
     * 删除必须保证的客户月计划
     */
    @ApiOperation("删除必须保证的客户月计划")
    @DeleteMapping("/mustFinishPlan/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mustFinishPlan/{id}")
    MdmMustFinishPlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验必须保证的客户月计划唯一性
     */
    @ApiOperation("校验必须保证的客户月计划唯一性")
    @PostMapping("/mustFinishPlan/checkMdmMustFinishPlanUnique")
    String checkMdmMustFinishPlanUnique(@RequestBody MdmMustFinishPlan mdmMustFinishPlan);

    /**
     * 导出必须保证的客户月计划列表
     */
    @ApiOperation("导出必须保证的客户月计划列表")
    @PostMapping("/mustFinishPlan/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmMustFinishPlan mdmMustFinishPlan, @PathVariable("fileName") String fileName);

    /**
     * 导入必须保证的客户月计划数据
     */
    @ApiOperation("导入必须保证的客户月计划")
    @PostMapping("/mustFinishPlan/importData/{updateSupport}")
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport);

}
