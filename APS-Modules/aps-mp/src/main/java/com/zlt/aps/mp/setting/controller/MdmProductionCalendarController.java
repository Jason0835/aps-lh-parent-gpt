package com.zlt.aps.mp.setting.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.service.IMdmProductionCalendarService;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductionCalendar;
import com.zlt.common.controller.BusiController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductionCalendarController.java
 * 描    述：生产日历 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-17
 */
@Slf4j
@Api(tags = "生产日历")
@RestController
@RequestMapping("/productionCalendar")
public class MdmProductionCalendarController extends BusiController {
    @Autowired
    private IMdmProductionCalendarService mdmProductionCalendarService;

    /**
     * 查询生产日历列表
     */
    // @RequiresPermissions("maindata:productionCalendar:list")
    @RequiresPermissions("lean:productioncalendar:list")
    @ApiOperation("查询生产日历列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MdmProductionCalendar mdmProductionCalendar) {
        startPage("create_time desc");
        List<MdmProductionCalendar> list = mdmProductionCalendarService.selectMdmProductionCalendarList(mdmProductionCalendar);
        return getDataTable(list);
    }


    // /**
    //  * 导出生产日历列表
    //  */
    // @RequiresPermissions("maindata:productionCalendar:export")
    // @Log(title = "生产日历", businessType = BusinessType.EXPORT)
    // @PostMapping("/exportData/{fileName}")
    // public byte[] exportData(@RequestBody MdmProductionCalendar mdmProductionCalendar, @PathVariable("fileName") String fileName,
    //                          HttpServletResponse response) throws IOException {
    //     return commonExport(mdmProductionCalendar, fileName, response);
    // }

    // @Override
    // public List<MdmProductionCalendar> listExportData(MdmProductionCalendar mdmProductionCalendar) {
    //     startPage("create_time desc");
    //     return mdmProductionCalendarService.selectMdmProductionCalendarList(mdmProductionCalendar);
    // }

    /**
     * 获取生产日历详细信息
     */
    @ApiOperation("获取生产日历详细信息")
    @GetMapping(value = "/{id}")
    public MdmProductionCalendar getInfo(@PathVariable("id") Long id) {
        return mdmProductionCalendarService.selectMdmProductionCalendarById(id);
    }

    /**
     * 新增生产日历
     */
    @Log(title = "ui.data.column.productionCalendar.modelName", businessType = BusinessType.INSERT)
    // @RequiresPermissions("maindata:productionCalendar:add")
    @RequiresPermissions("lean:productioncalendar:add")
    @ApiOperation("新增生产日历")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MdmProductionCalendar mdmProductionCalendar) {
        return toAjax(mdmProductionCalendarService.insertMdmProductionCalendar(mdmProductionCalendar));
    }

    /**
     * 修改生产日历
     */
    @Log(title = "ui.data.column.productionCalendar.modelName", businessType = BusinessType.UPDATE)
    // @RequiresPermissions("maindata:productionCalendar:edit")
    @RequiresPermissions("lean:productioncalendar:edit")
    @ApiOperation("修改生产日历")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MdmProductionCalendar mdmProductionCalendar) {
        return toAjax(mdmProductionCalendarService.updateMdmProductionCalendar(mdmProductionCalendar));
    }

    /**
     * 删除生产日历
     */
    @Log(title = "ui.data.column.productionCalendar.modelName", businessType = BusinessType.DELETE)
    // @RequiresPermissions("maindata:productionCalendar:remove")
    @RequiresPermissions("lean:productioncalendar:remove")
    @ApiOperation("删除生产日历")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(mdmProductionCalendarService.deleteMdmProductionCalendarByIds(ids));
    }

    /**
     * 校验生产日历唯一性
     */
    @ApiOperation("校验生产日历唯一性")
    @PostMapping("/checkMdmProductionCalendarUnique")
    public String checkMdmProductionCalendarUnique(@RequestBody MdmProductionCalendar mdmProductionCalendar) {
        return mdmProductionCalendarService.checkMdmProductionCalendarUnique(mdmProductionCalendar);
    }

    // /**
    //  * 根据集合导入生产日历数据
    //  *
    //  * @param importContext 导入上下文
    //  * @param updateSupport 已存在记录是否更新
    //  * @return 结果
    //  */
    // @Log(title = "ui.data.column.productionCalendar.modelName", businessType = BusinessType.IMPORT)
    // @ApiOperation("导入生产日历数据")
    // @PostMapping("/importData/{updateSupport}")
    // public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
    //     return commonImport(importContext, updateSupport);
    // }

    // @Override
    // public AjaxResult doImportData(List<MdmProductionCalendar> list, boolean updateSupport, long importLogId) {
    //     if (CollectionUtils.isEmpty(list)) {
    //         return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
    //     }
    //     return mdmProductionCalendarService.importData(list, updateSupport, importLogId);
    // }
}
