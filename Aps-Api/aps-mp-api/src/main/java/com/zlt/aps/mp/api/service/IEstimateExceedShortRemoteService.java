package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.EstimateExceedShort;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IEstimateExceedShortRemoteService.java
 * 描    述：IEstimateExceedShortRemoteService预计超欠产前端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-18
 */
@FeignClient(contextId = "IEstimateExceedShortRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IEstimateExceedShortRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/estimateExceedShort/list")
    TableDataInfo list(@RequestBody EstimateExceedShort QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/estimateExceedShort/save")
    AjaxResult save(@RequestBody EstimateExceedShort estimateExceedShort);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/estimateExceedShort/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/estimateExceedShort/{id}")
    EstimateExceedShort getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/estimateExceedShort/checkUnique")
    String checkUnique(@RequestBody EstimateExceedShort estimateExceedShortVO);

    /**
     * 导出预计超欠产列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/estimateExceedShort/exportData/{fileName}")
    byte[] exportData(@RequestBody EstimateExceedShort queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入预计超欠产数据
     */
    @ApiOperation("导入预计超欠产")
    @PostMapping("/estimateExceedShort/importData/{updateSupport}")
    AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport);

    // /**
    //  * 导入预计超欠产数据
    //  */
    // @ApiOperation("导入预计超欠产")
    // @PostMapping("/estimateExceedShort/importData/{updateSupport}/{importLogId}")
    // public AjaxResult importData(@RequestBody List<EstimateExceedShort> list, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("importLogId") Long importLogId);

    
    @ApiOperation("查询列表")
    @PostMapping("/estimateExceedShort/getList")
    List<EstimateExceedShort> getList(@RequestBody EstimateExceedShort entity);

    @ApiOperation("修改预计超欠数")
    @PostMapping("/estimateExceedShort/updateExceedShortQty")
    AjaxResult updateExceedShortQty(@RequestBody EstimateExceedShort estimateExceedShort);
}
