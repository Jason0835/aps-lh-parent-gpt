package com.zlt.aps.mdm.controller;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.mdm.service.IMdmMustFinishPlanService;
import com.zlt.aps.mdm.api.domain.entity.MdmMustFinishPlan;
import com.zlt.common.controller.BusiController;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMustFinishPlanController.java
 * 描    述：必须保证的客户月计划 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-25
 */
@Slf4j
@Api(tags = "必须保证的客户月计划")
@RestController
@RequestMapping("/mustFinishPlan")
public class MdmMustFinishPlanController extends BusiController<MdmMustFinishPlan> {
    @Autowired
    private IMdmMustFinishPlanService mdmMustFinishPlanService;

    /**
     * 查询必须保证的客户月计划列表
     */
    @RequiresPermissions("mdm:mustFinishPlan:list")
    @ApiOperation("查询必须保证的客户月计划列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MdmMustFinishPlan mdmMustFinishPlan) {
        startPage();
        List<MdmMustFinishPlan> list = mdmMustFinishPlanService.selectMdmMustFinishPlanList(mdmMustFinishPlan);
        return getDataTable(list);
    }


    /**
     * 导出必须保证的客户月计划列表
     */
    @RequiresPermissions("mdm:mustFinishPlan:export")
    @Log(title = "ui.data.column.mustFinishPlan.modelName", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody MdmMustFinishPlan mdmMustFinishPlan, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return commonExport(mdmMustFinishPlan, fileName, response);
    }

    @Override
    public List<MdmMustFinishPlan> listExportData(MdmMustFinishPlan mdmMustFinishPlan) {
        startPage();
        return mdmMustFinishPlanService.selectMdmMustFinishPlanList(mdmMustFinishPlan);
    }

    /**
     * 获取必须保证的客户月计划详细信息
     */
    @ApiOperation("获取必须保证的客户月计划详细信息")
    @GetMapping(value = "/{id}")
    public MdmMustFinishPlan getInfo(@PathVariable("id") Long id) {
        return mdmMustFinishPlanService.selectMdmMustFinishPlanById(id);
    }

    /**
     * 新增必须保证的客户月计划
     */
    @Log(title = "ui.data.column.mustFinishPlan.modelName", businessType = BusinessType.INSERT)
    @RequiresPermissions("mdm:mustFinishPlan:add")
    @ApiOperation("新增必须保证的客户月计划")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MdmMustFinishPlan mdmMustFinishPlan) {
        return toAjax(mdmMustFinishPlanService.insertMdmMustFinishPlan(mdmMustFinishPlan));
    }

    /**
     * 修改必须保证的客户月计划
     */
    @Log(title = "ui.data.column.mustFinishPlan.modelName", businessType = BusinessType.UPDATE)
    @RequiresPermissions("mdm:mustFinishPlan:edit")
    @ApiOperation("修改必须保证的客户月计划")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MdmMustFinishPlan mdmMustFinishPlan) {
        return toAjax(mdmMustFinishPlanService.updateMdmMustFinishPlan(mdmMustFinishPlan));
    }

    /**
     * 删除必须保证的客户月计划
     */
    @Log(title = "ui.data.column.mustFinishPlan.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions("mdm:mustFinishPlan:remove")
    @ApiOperation("删除必须保证的客户月计划")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(mdmMustFinishPlanService.deleteMdmMustFinishPlanByIds(ids));
    }

    /**
     * 校验必须保证的客户月计划唯一性
     */
    @ApiOperation("校验必须保证的客户月计划唯一性")
    @PostMapping("/checkMdmMustFinishPlanUnique")
    public String checkMdmMustFinishPlanUnique(@RequestBody MdmMustFinishPlan mdmMustFinishPlan) {
        return mdmMustFinishPlanService.checkMdmMustFinishPlanUnique(mdmMustFinishPlan);
    }

    /**
     * 根据集合导入必须保证的客户月计划数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mustFinishPlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入必须保证的客户月计划数据")
    @PostMapping("/importData/{updateSupport}")
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return commonImport(importContext, updateSupport);
    }

    @Override
    public AjaxResult doImportData(List<MdmMustFinishPlan> list, boolean updateSupport, long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return mdmMustFinishPlanService.importData(list, updateSupport, importLogId);
    }
}
