package com.zlt.aps.monthplan.raw.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.RawMaterialRequirePlanEntityMapper;
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

import com.zlt.aps.monthplan.api.domain.entity.RawMaterialRequirePlan;
import com.zlt.aps.maindata.service.IRawMaterialRequirePlanService;

import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：RawMaterialRequirePlanController.java
* 描    述：原材料需求计划 控制层类：....
*@author zlt
*@date 2025-12-08
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "原材料需求计划")
@RestController
@RequestMapping("/rawMaterialRequirePlan")
public class RawMaterialRequirePlanController extends AbstractDocBizController<RawMaterialRequirePlan> {

    @Autowired
    private IRawMaterialRequirePlanService rawMaterialRequirePlanService;

    @Autowired
    private RawMaterialRequirePlanEntityMapper entityMapper;

    /**
     * 查询原材料需求计划列表
     */
    @RequiresPermissions( "maindata:rawMaterialRequirePlan:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody RawMaterialRequirePlan queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.rawMaterialRequirePlan.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "maindata:rawMaterialRequirePlan:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody RawMaterialRequirePlan billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.rawMaterialRequirePlan.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "maindata:rawMaterialRequirePlan:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取原材料需求计划详细信息
     */
    @RequiresPermissions( "maindata:rawMaterialRequirePlan:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public RawMaterialRequirePlan getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入原材料需求计划数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "maindata:rawMaterialRequirePlan:import")
    @Log(title = "ui.data.column.rawMaterialRequirePlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "maindata:rawMaterialRequirePlan:export")
    @Log(title = "原材料需求计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody RawMaterialRequirePlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<RawMaterialRequirePlan> listExportData(RawMaterialRequirePlan obj) {
        QueryWrapper<RawMaterialRequirePlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return rawMaterialRequirePlanService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<RawMaterialRequirePlan> queryWrapper, RawMaterialRequirePlan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("category")), "CATEGORY", queryVO.getFieldValueByFieldName("category"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("curMonthQty")), "CUR_MONTH_QTY", queryVO.getFieldValueByFieldName("curMonthQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("curMonthRudrQty")), "CUR_MONTH_RUDR_QTY", queryVO.getFieldValueByFieldName("curMonthRudrQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tMonthQty")), "T_MONTH_QTY", queryVO.getFieldValueByFieldName("tMonthQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tMonthEudrQty")), "T_MONTH_EUDR_QTY", queryVO.getFieldValueByFieldName("tMonthEudrQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("t1MonthQty")), "T1_MONTH_QTY", queryVO.getFieldValueByFieldName("t1MonthQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("t1MonthEudrQty")), "T1_MONTH_EUDR_QTY", queryVO.getFieldValueByFieldName("t1MonthEudrQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("t2MonthQty")), "T2_MONTH_QTY", queryVO.getFieldValueByFieldName("t2MonthQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("t2MonthEudrQty")), "T2_MONTH_EUDR_QTY", queryVO.getFieldValueByFieldName("t2MonthEudrQty"));
    }


    @Override
    protected String getTypeCode(){
        return "RAW9003";
    }


}
