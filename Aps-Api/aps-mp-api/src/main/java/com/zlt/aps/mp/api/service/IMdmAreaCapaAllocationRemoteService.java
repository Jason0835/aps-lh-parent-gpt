package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmAreaCapaAllocation;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmAreaCapaAllocationRemoteService.java
 * 描    述：IMdmAreaCapaAllocationRemoteService区域产能分配前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-08
 */
@FeignClient(contextId = "IMdmAreaCapaAllocationRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmAreaCapaAllocationRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmAreaCapaAllocation/list")
    TableDataInfo list(@RequestBody MdmAreaCapaAllocation QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmAreaCapaAllocation/save")
    AjaxResult save(@RequestBody MdmAreaCapaAllocation mdmAreaCapaAllocation);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmAreaCapaAllocation/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmAreaCapaAllocation/{id}")
    MdmAreaCapaAllocation getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmAreaCapaAllocation/checkUnique")
    String checkUnique(@RequestBody MdmAreaCapaAllocation mdmAreaCapaAllocationVO);

    /**
     * 导出区域产能分配列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mdmAreaCapaAllocation/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmAreaCapaAllocation queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入区域产能分配数据
     */
    @ApiOperation("导入区域产能分配")
    @PostMapping("/mdmAreaCapaAllocation/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 复制
     *
     * @param entity 参数
     * @return 结果
     */
    @ApiOperation("复制")
    @PostMapping("/mdmAreaCapaAllocation/copy")
    AjaxResult copy(@RequestBody MdmAreaCapaAllocation entity);

    /**
     * 复制前校验
     *
     * @param entity 参数
     * @return 结果
     */
    @ApiOperation("复制前校验")
    @PostMapping("/mdmAreaCapaAllocation/checkBeforeCopy")
    AjaxResult checkBeforeCopy(@RequestBody MdmAreaCapaAllocation entity);

    /**
     * 获取总产能分配
     *
     * @param entity 参数
     * @return 结果
     */
    @ApiOperation("获取总产能分配")
    @PostMapping("/mdmAreaCapaAllocation/getSumCapacityAllocation")
    public AjaxResult getSumCapacityAllocation(@RequestBody MdmAreaCapaAllocation entity);
}
