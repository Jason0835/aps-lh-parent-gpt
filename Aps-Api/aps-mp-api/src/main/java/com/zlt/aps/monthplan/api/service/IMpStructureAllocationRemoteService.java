package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpStructureAllocation;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpStructureAllocationRemoteService.java
 * 描    述：IMpStructureAllocationRemoteService排产过程_结构排产前端接口
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-29
 */
@FeignClient(contextId = "IMpStructureAllocationRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMpStructureAllocationRemoteService {

    /**
     * 查询列表
     *
     * @param queryCondition 查询条件
     */
    @ApiOperation("查询列表")
    @PostMapping("/mpStructureAllocation/list")
    TableDataInfo list(@RequestBody MpStructureAllocation queryCondition);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mpStructureAllocation/checkUnique")
    String checkUnique(@RequestBody MpStructureAllocation mpStructureAllocationVO);

    /**
     * 导出排产过程_结构排产列表
     * @param fileName
     */
    @ApiOperation("导出列表")
    @PostMapping("/mpStructureAllocation/exportData/{fileName}")
    byte[] exportData(@RequestBody MpStructureAllocation queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入排产过程_结构排产数据
     */
    @ApiOperation("导入排产过程_结构排产")
    @PostMapping("/mpStructureAllocation/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 查询版本列表
     */
    @ApiOperation("查询版本列表")
    @PostMapping("/mpStructureAllocation/getVersionList")
    TableDataInfo getVersionList(@RequestBody MpStructureAllocation queryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mpStructureAllocation/save")
    AjaxResult save(@RequestBody MpStructureAllocation mpStructureAllocation);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mpStructureAllocation/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 获取日期最接近的下一个结构
     */
    @ApiOperation("获取日期最接近的下一个结构")
    @PostMapping("/mpStructureAllocation/getNextStructure")
    MpStructureAllocation getNextStructure(@RequestBody MpStructureAllocation queryVO);

    /**
     * 获取日期最接近的上一个结构
     */
    @ApiOperation("获取日期最接近的上一个结构")
    @PostMapping("/mpStructureAllocation/getPreviousStructure")
    MpStructureAllocation getPreviousStructure(@RequestBody MpStructureAllocation queryVO);

}
