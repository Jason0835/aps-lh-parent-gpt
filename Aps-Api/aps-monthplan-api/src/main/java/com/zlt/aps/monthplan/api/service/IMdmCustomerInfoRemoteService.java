package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmCustomerInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmCustomerInfoRemoteService.java
 * 描    述：IMdmCustomerInfoRemoteService客户信息前端接口
 *@author zlt
 *@date 2025-03-04
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@FeignClient(contextId = "IMdmCustomerInfoRemoteService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmCustomerInfoRemoteService {

    /**
     * 查询列表
     * @param QueryVO 参数
     * @return 结果
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmCustomerInfo/list")
    TableDataInfo list(@RequestBody MdmCustomerInfo QueryVO);

    /**
    * 保存
    */
    @ApiOperation("保存")
    @PostMapping("/mdmCustomerInfo/save")
    AjaxResult save(@RequestBody MdmCustomerInfo mdmCustomerInfo);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mdmCustomerInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmCustomerInfo/{id}")
    MdmCustomerInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmCustomerInfo/checkUnique")
    String checkUnique(@RequestBody MdmCustomerInfo mdmCustomerInfoVO);

    /**
     * 导出客户信息列表
    */
    @ApiOperation("导出列表")
    @PostMapping("/mdmCustomerInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmCustomerInfo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入客户信息数据
     */
    @ApiOperation("导入客户信息")
    @PostMapping("/mdmCustomerInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

}
