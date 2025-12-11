package com.zlt.aps.monthplan.demand.controller;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.service.IMpHistorySaleRecordService;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleRecord;
import com.zlt.common.controller.BusiController;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import com.ruoyi.common.core.web.page.TableDataInfo;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MpHistorySaleRecordController.java
* 描    述：历史销售记录 控制层类：....
*@author yelq
*@date 2025-12-11
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：yelq
*     修改内容：...
*/
@Slf4j
@Api(tags = "历史销售记录")
@RestController
@RequestMapping("/historySaleRecord")
public class MpHistorySaleRecordController extends BusiController<MpHistorySaleRecord>
{
    @Autowired
    private IMpHistorySaleRecordService mpHistorySaleRecordService;

    /**
     * 查询历史销售记录列表
     */
    @RequiresPermissions( "maindata:historySaleRecord:list")
    @ApiOperation("查询历史销售记录列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MpHistorySaleRecord mpHistorySaleRecord)
    {
        startPage("create_time desc");
        List<MpHistorySaleRecord> list = mpHistorySaleRecordService.selectMpHistorySaleRecordList(mpHistorySaleRecord);
        return getDataTable(list);
    }


    /**
     * 导出历史销售记录列表
     */
    @RequiresPermissions( "maindata:historySaleRecord:export")
    @Log(title = "历史销售记录", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody MpHistorySaleRecord mpHistorySaleRecord,@PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return commonExport(mpHistorySaleRecord,fileName,response);
    }

    @Override
    public List<MpHistorySaleRecord> listExportData(MpHistorySaleRecord mpHistorySaleRecord) {
        startPage("create_time desc");
        return  mpHistorySaleRecordService.selectMpHistorySaleRecordList(mpHistorySaleRecord);
    }

    /**
     * 获取历史销售记录详细信息
     */
    @RequiresPermissions( "maindata:historySaleRecord:query")
    @ApiOperation("获取历史销售记录详细信息")
    @GetMapping(value = "/{id}")
    public MpHistorySaleRecord getInfo(@PathVariable("id") Long id)
    {
        return mpHistorySaleRecordService.selectMpHistorySaleRecordById(id);
    }

    /**
     * 新增历史销售记录
     */
    @Log(title = "ui.data.column.historySaleRecord.modelName", businessType = BusinessType.INSERT)
    @RequiresPermissions( "maindata:historySaleRecord:add")
    @ApiOperation("新增历史销售记录")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MpHistorySaleRecord mpHistorySaleRecord){
        return toAjax(mpHistorySaleRecordService.insertMpHistorySaleRecord(mpHistorySaleRecord));
    }

    /**
     * 修改历史销售记录
     */
    @Log(title = "ui.data.column.historySaleRecord.modelName", businessType = BusinessType.UPDATE)
    @RequiresPermissions( "maindata:historySaleRecord:edit")
    @ApiOperation("修改历史销售记录")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MpHistorySaleRecord mpHistorySaleRecord){
        return toAjax(mpHistorySaleRecordService.updateMpHistorySaleRecord(mpHistorySaleRecord));
    }

    /**
     * 删除历史销售记录
     */
    @Log(title = "ui.data.column.historySaleRecord.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "maindata:historySaleRecord:remove")
    @ApiOperation("删除历史销售记录")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(mpHistorySaleRecordService.deleteMpHistorySaleRecordByIds(ids));
    }

    /**
     * 校验历史销售记录唯一性
     */
    @ApiOperation("校验历史销售记录唯一性")
    @PostMapping("/checkMpHistorySaleRecordUnique")
    public String checkMpHistorySaleRecordUnique(@RequestBody MpHistorySaleRecord mpHistorySaleRecord){
        return mpHistorySaleRecordService.checkMpHistorySaleRecordUnique(mpHistorySaleRecord);
    }

    /**
     * 根据集合导入历史销售记录数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.historySaleRecord.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入历史销售记录数据")
    @PostMapping("/importData/{updateSupport}")
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return commonImport(importContext,updateSupport);
    }

    @Override
    public AjaxResult doImportData(List<MpHistorySaleRecord> list, boolean updateSupport, long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mpHistorySaleRecordService.importData(list, updateSupport, importLogId);
    }
}
