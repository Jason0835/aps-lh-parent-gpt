package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.ProductALevel;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IProductALevelRemoteService.java
 * 描    述：IProductALevelRemoteService基础数据-SAP-OEE率前端接口
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@FeignClient(contextId = "IProductALevelRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IProductALevelRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/productDamage/list")
    TableDataInfo list(@RequestBody ProductALevel QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/productDamage/save")
    AjaxResult save(@RequestBody ProductALevel productALevel);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/productDamage/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/productDamage/{id}")
    ProductALevel getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/productDamage/checkUnique")
    String checkUnique(@RequestBody ProductALevel productALevelVO);

    /**
     * 导出基础数据-SAP-OEE率列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/productDamage/exportData/{fileName}")
    byte[] exportData(@RequestBody ProductALevel queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入基础数据-SAP-OEE率数据
     */
    @ApiOperation("导入基础数据-SAP-OEE率")
    @PostMapping("/productDamage/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 不备货
     * @param ids 集合
     * @param year 年
     * @param month 月
     * @return 结果
     */
    @ApiOperation("不备货")
    @PostMapping("/productDamage/noStockUp")
    public AjaxResult noStockUp(@RequestBody List<Long> ids, @RequestParam("year") Integer year, @RequestParam("month") Integer month);
}
