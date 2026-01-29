package com.zlt.aps.monthplan.check.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.zlt.aps.monthplan.api.domain.entity.MpCheckItemRecord;
import com.zlt.aps.monthplan.check.service.IMpCheckItemRecordService;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.ruoyi.common.core.web.page.TableDataInfo;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MpCheckItemRecordController.java
* 描    述：S2-1202 检测项记录 控制层类：....
*@author hsc
*@date 2026-01-29
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：hsc
*     修改内容：...
*/
@Slf4j
@Api(tags = "S2-1202 检测项记录")
@RestController
@RequestMapping("/checkItemRecord")
public class MpCheckItemRecordController extends BaseController<MpCheckItemRecord>
{
    @Autowired
    private IMpCheckItemRecordService mpCheckItemRecordService;

    /**
     * 查询S2-1202 检测项记录列表
     */
    @ApiOperation("查询S2-1202 检测项记录列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MpCheckItemRecord mpCheckItemRecord)
    {
        startPage("create_time desc");
        List<MpCheckItemRecord> list = mpCheckItemRecordService.selectMpCheckItemRecordList(mpCheckItemRecord);
        return getDataTable(list);
    }


    /**
     * 获取S2-1202 检测项记录详细信息
     */
    @ApiOperation("获取S2-1202 检测项记录详细信息")
    @GetMapping(value = "/{id}")
    public MpCheckItemRecord getInfo(@PathVariable("id") Long id)
    {
        return mpCheckItemRecordService.selectMpCheckItemRecordById(id);
    }

    /**
     * 新增S2-1202 检测项记录
     */
    @Log(title = "ui.data.column.checkItemRecord.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增S2-1202 检测项记录")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MpCheckItemRecord mpCheckItemRecord){
        return toAjax(mpCheckItemRecordService.insertMpCheckItemRecord(mpCheckItemRecord));
    }

    /**
     * 修改S2-1202 检测项记录
     */
    @Log(title = "ui.data.column.checkItemRecord.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改S2-1202 检测项记录")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MpCheckItemRecord mpCheckItemRecord){
        return toAjax(mpCheckItemRecordService.updateMpCheckItemRecord(mpCheckItemRecord));
    }

    /**
     * 删除S2-1202 检测项记录
     */
    @Log(title = "ui.data.column.checkItemRecord.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除S2-1202 检测项记录")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(mpCheckItemRecordService.deleteMpCheckItemRecordByIds(ids));
    }

    /**
     * 校验S2-1202 检测项记录唯一性
     */
    @ApiOperation("校验S2-1202 检测项记录唯一性")
    @PostMapping("/checkMpCheckItemRecordUnique")
    public String checkMpCheckItemRecordUnique(@RequestBody MpCheckItemRecord mpCheckItemRecord){
        return mpCheckItemRecordService.checkMpCheckItemRecordUnique(mpCheckItemRecord);
    }

}
