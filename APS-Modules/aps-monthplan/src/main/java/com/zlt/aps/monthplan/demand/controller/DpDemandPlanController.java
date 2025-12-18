package com.zlt.aps.monthplan.demand.controller;


import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.tlt.aps.redissonLock.annotation.RedissonLockAnno;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.demand.service.IDpDemandPlanService;
import com.zlt.common.controller.BusiController;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
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
* 文件名称：DpDemandPlanController.java
* 描    述：需求计划 控制层类：....
*@author yelq
*@date 2025-12-12
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：yelq
*     修改内容：...
*/
@Slf4j
@Api(tags = "需求计划")
@RestController
@RequestMapping("/demandPlan")
public class DpDemandPlanController extends BusiController<DpDemandPlan>
{
    @Autowired
    private IDpDemandPlanService DpDemandPlanService;

    /**
     * 查询需求计划列表
     */
    @RequiresPermissions( "monthplan:demandPlan:list")
    @ApiOperation("查询需求计划列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody DpDemandPlan DpDemandPlan)
    {
        startPage("create_time desc");
        List<DpDemandPlan> list = DpDemandPlanService.selectDpDemandPlanList(DpDemandPlan);
        return getDataTable(list);
    }


    /**
     * 导出需求计划列表
     */
    @RequiresPermissions( "monthplan:demandPlan:export")
    @Log(title = "需求计划", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    public byte[] exportData(@RequestBody DpDemandPlan DpDemandPlan,@PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return commonExport(DpDemandPlan,fileName,response);
    }

    @Override
    public List<DpDemandPlan> listExportData(DpDemandPlan DpDemandPlan) {
        startPage("create_time desc");
        return  DpDemandPlanService.selectDpDemandPlanList(DpDemandPlan);
    }

    /**
     * 获取需求计划详细信息
     */
    @RequiresPermissions( "monthplan:demandPlan:query")
    @ApiOperation("获取需求计划详细信息")
    @GetMapping(value = "/{id}")
    public DpDemandPlan getInfo(@PathVariable("id") Long id)
    {
        return DpDemandPlanService.selectDpDemandPlanById(id);
    }

    /**
     * 新增需求计划
     */
    @Log(title = "ui.data.column.demandPlan.modelName", businessType = BusinessType.INSERT)
    @RequiresPermissions( "monthplan:demandPlan:add")
    @ApiOperation("新增需求计划")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody DpDemandPlan DpDemandPlan){
        return toAjax(DpDemandPlanService.insertDpDemandPlan(DpDemandPlan));
    }

    /**
     * 修改需求计划
     */
    @Log(title = "ui.data.column.demandPlan.modelName", businessType = BusinessType.UPDATE)
    @RequiresPermissions( "monthplan:demandPlan:edit")
    @ApiOperation("修改需求计划")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody DpDemandPlan DpDemandPlan){
        return toAjax(DpDemandPlanService.updateDpDemandPlan(DpDemandPlan));
    }

    /**
     * 删除需求计划
     */
    @Log(title = "ui.data.column.demandPlan.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "monthplan:demandPlan:remove")
    @ApiOperation("删除需求计划")
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids){
        return toAjax(DpDemandPlanService.deleteDpDemandPlanByIds(ids));
    }

    /**
     * 校验需求计划唯一性
     */
    @ApiOperation("校验需求计划唯一性")
    @PostMapping("/checkDpDemandPlanUnique")
    public String checkDpDemandPlanUnique(@RequestBody DpDemandPlan DpDemandPlan){
        return DpDemandPlanService.checkDpDemandPlanUnique(DpDemandPlan);
    }

    /**
     * 根据集合导入需求计划数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.demandPlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入需求计划数据")
    @PostMapping("/importData/{updateSupport}")
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return commonImport(importContext,updateSupport);
    }

    @Override
    public AjaxResult doImportData(List<DpDemandPlan> list, boolean updateSupport, long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return DpDemandPlanService.importData(list, updateSupport, importLogId);
    }

    @ApiOperation("生成需求计划")
    @RedissonLockAnno(uniqueMark = "redissonLock:demandPlan:createMonthRequire:",
        expressions = {"#createCondition.factoryCode", "#createCondition.year", "#createCondition.month"},
        msgKey = "ui.data.alert.createMonthRequire.run",
        waitTime = 5,
        leaseTime = 300
    )
    @PostMapping("/createMonthRequire")
    public AjaxResult createMonthRequire(@RequestBody DpDemandPlan createCondition){
        if (null == createCondition) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.condition.noEmpty"));
        }
        if (StringUtils.isBlank(createCondition.getFactoryCode()) || null == createCondition.getYear() || null == createCondition.getMonth()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        DpDemandPlanService.createMonthRequire(createCondition);
        return AjaxResult.success();
    }
}
