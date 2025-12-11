package com.zlt.aps.monthplan.demand.controller;


import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.service.IMpMonthlySaleQtyService;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthlySaleQty;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MpMonthlySaleQtyController.java
* 描    述：月均销量 控制层类：....
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
@Api(tags = "月均销量")
@RestController
@RequestMapping("/monthlySaleQty")
public class MpMonthlySaleQtyController extends BusiController<MpMonthlySaleQty>
{
    @Autowired
    private IMpMonthlySaleQtyService mpMonthlySaleQtyService;

    /**
     * 导出月均销量列表
     */
    @RequiresPermissions( "monthplan:monthlySaleQty:export")
    @Log(title = "月均销量", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody MpMonthlySaleQty mpMonthlySaleQty,@PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return commonExport(mpMonthlySaleQty,fileName,response);
    }

    /**
     * 新增月均销量
     */
    @Log(title = "ui.data.column.monthlySaleQty.modelName", businessType = BusinessType.INSERT)
    @RequiresPermissions( "monthplan:monthlySaleQty:add")
    @ApiOperation("新增月均销量")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MpMonthlySaleQty mpMonthlySaleQty){
        return toAjax(mpMonthlySaleQtyService.save(mpMonthlySaleQty));
    }

    /**
     * 修改月均销量
     */
    @Log(title = "ui.data.column.monthlySaleQty.modelName", businessType = BusinessType.UPDATE)
    @RequiresPermissions( "monthplan:monthlySaleQty:edit")
    @ApiOperation("修改月均销量")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MpMonthlySaleQty mpMonthlySaleQty){
        return toAjax(mpMonthlySaleQtyService.save(mpMonthlySaleQty));
    }

    /**
     * 删除月均销量
     */
    @Log(title = "ui.data.column.monthlySaleQty.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "monthplan:monthlySaleQty:remove")
    @ApiOperation("删除月均销量")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable List<Long> ids){
        return toAjax(mpMonthlySaleQtyService.removeByIds(ids));
    }

    /**
     * 校验月均销量唯一性
     */
    @ApiOperation("校验月均销量唯一性")
    @PostMapping("/checkMpMonthlySaleQtyUnique")
    public String checkMpMonthlySaleQtyUnique(@RequestBody MpMonthlySaleQty mpMonthlySaleQty){
        return mpMonthlySaleQtyService.checkUnique(mpMonthlySaleQty);
    }

    /**
     * 根据集合导入月均销量数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.monthlySaleQty.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入月均销量数据")
    @PostMapping("/importData/{updateSupport}")
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return commonImport(importContext,updateSupport);
    }

    @Override
    public AjaxResult doImportData(List<MpMonthlySaleQty> list, boolean updateSupport, long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mpMonthlySaleQtyService.importData(list, updateSupport, importLogId);
    }
}
