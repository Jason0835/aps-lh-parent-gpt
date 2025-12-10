package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMouldShellInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMpMouldShellInfoRemoteService.java
 * 描    述：IMpMouldShellInfoRemoteService模壳台账前端接口
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
@FeignClient(contextId = "IMdmMouldShellInfoRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmMouldShellInfoRemoteService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mpMouldShellInfo/list")
    TableDataInfo list(@RequestBody MdmMouldShellInfo QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mpMouldShellInfo/save")
    AjaxResult save(@RequestBody MdmMouldShellInfo mdmMouldShellInfo);


    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/mpMouldShellInfo/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mpMouldShellInfo/{id}")
    MdmMouldShellInfo getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mpMouldShellInfo/checkUnique")
    String checkUnique(@RequestBody MdmMouldShellInfo mdmMouldShellInfoVO);

    /**
     * 导出模壳台账列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/mpMouldShellInfo/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmMouldShellInfo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入模壳台账数据
     */
    @ApiOperation("导入模壳台账")
    @PostMapping("/mpMouldShellInfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 抓取MES数据
     *
     * @return 结果
     */
    @ApiOperation("抓取MES数据")
    @PostMapping("/mpMouldShellInfo/mesCapture")
    AjaxResult mesCapture();
}
