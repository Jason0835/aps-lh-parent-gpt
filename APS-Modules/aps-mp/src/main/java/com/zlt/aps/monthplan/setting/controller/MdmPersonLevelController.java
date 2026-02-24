package com.zlt.aps.monthplan.setting.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.service.IMdmPersonLevelService;
import com.zlt.aps.monthplan.api.domain.entity.MdmPersonLevel;
import lombok.extern.slf4j.Slf4j;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;


import com.ruoyi.common.core.web.page.TableDataInfo;


/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmPersonLevelController.java
 * 描    述：成型机人员档配置 控制层类：....
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
@Slf4j
@Api(tags = "成型机人员档配置")
@RestController
@RequestMapping("/mdmPersonLevel")
public class MdmPersonLevelController extends BaseController {

    @Autowired
    private IMdmPersonLevelService mdmPersonLevelService;

    /**
     * 查询成型机人员档配置列表
     */
    @RequiresPermissions("monthplan:mdmPersonLevel:list")
    @ApiOperation("查询成型机人员档配置列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MdmPersonLevel mdmPersonLevel) {
        startPage("create_time desc");
        List<MdmPersonLevel> list = mdmPersonLevelService.selectMdmPersonLevelList(mdmPersonLevel);
        return getDataTable(list);
    }

    /**
     * 获取成型机人员档配置详细信息
     */
    @RequiresPermissions("monthplan:mdmPersonLevel:query")
    @ApiOperation("获取成型机人员档配置详细信息")
    @GetMapping(value = "/{id}")
    public MdmPersonLevel getInfo(@PathVariable("id") Long id) {
        return mdmPersonLevelService.selectMdmPersonLevelById(id);
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody MdmPersonLevel mdmPersonLevel) {
        return mdmPersonLevelService.checkUnique(mdmPersonLevel);
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("保存成型机人员档配置")
    @PostMapping("/save")
    public AjaxResult save(@RequestBody MdmPersonLevel mdmPersonLevel) {
        return AjaxResult.success(mdmPersonLevelService.saveOrUpdate(mdmPersonLevel));
    }

    /**
     * 删除成型机人员档配置
     */
    @RequiresPermissions("monthplan:mdmPersonLevel:remove")
    @ApiOperation("删除成型机人员档配置")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(mdmPersonLevelService.removeByIds(Arrays.asList(ids)));
    }

    /**
     * 校验唯一性
     */
    @ApiOperation("更新成型机人员档配置")
    @PutMapping("/updateMdmPersonLevel")
    public AjaxResult updateMdmPersonLevel(@RequestBody MdmPersonLevel mdmPersonLevel) {
        return AjaxResult.success(mdmPersonLevelService.updateById(mdmPersonLevel));
    }
}
