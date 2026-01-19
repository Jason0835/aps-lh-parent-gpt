package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.MpMonthPlanMonitorEntityMapper;
import com.zlt.aps.maindata.service.IMpMonthPlanMonitorService;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanMonitor;
import com.zlt.common.utils.PubUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import lombok.extern.slf4j.Slf4j;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MpMonthPlanMonitorController.java
* 描    述：月度硫化监控 控制层类：....
*@author zlt
*@date 2025-12-24
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "月度硫化监控")
@RestController
@RequestMapping("/mpMonthPlanMonitor")
public class MpMonthPlanMonitorController extends AbstractDocBizController<MpMonthPlanMonitor> {

    @Autowired
    private IMpMonthPlanMonitorService mpMonthPlanMonitorService;

    @Autowired
    private MpMonthPlanMonitorEntityMapper entityMapper;

    /**
     * 查询月度硫化监控列表
     */
    @RequiresPermissions( "monthplan:mpMonthPlanMonitor:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpMonthPlanMonitor queryVO) {
        startPage(getOrderBy(queryVO));
        List<MpMonthPlanMonitor> list = entityMapper.listReport(queryVO);
        clearPage();
        return getDataTable(list);
    
    }

    @Override
    protected String getOrderBy() {
        return "onboard_date desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mpMonthPlanMonitor.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "monthplan:mpMonthPlanMonitor:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpMonthPlanMonitor billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mpMonthPlanMonitor.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "monthplan:mpMonthPlanMonitor:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取月度硫化监控详细信息
     */
    @RequiresPermissions( "monthplan:mpMonthPlanMonitor:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MpMonthPlanMonitor getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入月度硫化监控数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "monthplan:mpMonthPlanMonitor:import")
    @Log(title = "ui.data.column.mpMonthPlanMonitor.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "monthplan:mpMonthPlanMonitor:export")
    @Log(title = "月度硫化监控", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpMonthPlanMonitor queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MpMonthPlanMonitor> listExportData(MpMonthPlanMonitor obj) {
        return entityMapper.listReport(obj);
    }

    @Override
    protected IDocService getDocService(){
        return mpMonthPlanMonitorService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MpMonthPlanMonitor> queryWrapper, MpMonthPlanMonitor queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("yearMonth")), "YEAR_MONTH", queryVO.getFieldValueByFieldName("yearMonth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", queryVO.getFieldValueByFieldName("productionVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productStatus")), "PRODUCT_STATUS", queryVO.getFieldValueByFieldName("productStatus"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainMaterialDesc")), "MAIN_MATERIAL_DESC", queryVO.getFieldValueByFieldName("mainMaterialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainPattern")), "MAIN_PATTERN", queryVO.getFieldValueByFieldName("mainPattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldQty")), "MOULD_QTY", queryVO.getFieldValueByFieldName("mouldQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("netDemandQty")), "NET_DEMAND_QTY", queryVO.getFieldValueByFieldName("netDemandQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleQty")), "SCHEDULE_QTY", queryVO.getFieldValueByFieldName("scheduleQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("onboardDate")), "ONBOARD_DATE", queryVO.getFieldValueByFieldName("onboardDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("unqualifiedQty")), "UNQUALIFIED_QTY", queryVO.getFieldValueByFieldName("unqualifiedQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionQty")), "PRODUCTION_QTY", queryVO.getFieldValueByFieldName("productionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMargin")), "LH_MARGIN", queryVO.getFieldValueByFieldName("lhMargin"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("expectedCloseDay")), "EXPECTED_CLOSE_DAY", queryVO.getFieldValueByFieldName("expectedCloseDay"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("expectedCloseDate")), "EXPECTED_CLOSE_DATE", queryVO.getFieldValueByFieldName("expectedCloseDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planCloseDate")), "PLAN_CLOSE_DATE", queryVO.getFieldValueByFieldName("planCloseDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("diffDay")), "DIFF_DAY", queryVO.getFieldValueByFieldName("diffDay"));
    }


    @Override
    protected String getTypeCode(){
        return "MONTH0612";
    }
}
