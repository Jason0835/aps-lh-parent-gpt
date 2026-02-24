package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmPersonLevel;
import org.springframework.cloud.openfeign.FeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.ruoyi.common.core.web.domain.AjaxResult;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：IMdmPersonLevelService.java
 * 描    述：IMdmPersonLevelService成型机人员档配置前端接口
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-20
 */
@FeignClient(contextId = "IMdmPersonLevelRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmPersonLevelService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/mdmPersonLevel/list")
    TableDataInfo list(@RequestBody MdmPersonLevel QueryVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/mdmPersonLevel/save")
    AjaxResult save(@RequestBody MdmPersonLevel mdmPersonLevel);


    /**
     * 删除成型机人员档配置
     */
    @ApiOperation("删除成型机人员档配置")
    @DeleteMapping("/mdmPersonLevel/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmPersonLevel/{id}")
    MdmPersonLevel getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/mdmPersonLevel/checkUnique")
    String checkUnique(@RequestBody MdmPersonLevel mdmPersonLevelVO);

    /**
     * 更新成型机人员档配置
     *
     * @param mdmPersonLevel
     * @return
     */
    @ApiOperation("更新成型机人员档配置")
    @PutMapping("/mdmPersonLevel/updateMdmPersonLevel")
    AjaxResult updateMdmPersonLevel(@RequestBody MdmPersonLevel mdmPersonLevel);
}
