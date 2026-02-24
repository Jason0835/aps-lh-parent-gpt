package com.zlt.aps.monthplan.demand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.aps.monthplan.api.domain.entity.DpOrderOffsetDetail;
import com.zlt.aps.monthplan.demand.mapper.DpOrderOffsetDetailEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpOrderOffsetDetailService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.PubUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.core.queryformulas.QueryFormulaUtil;
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
* 文件名称：DpOrderOffsetDetailController.java
* 描    述：S1-0604订单冲减分配 控制层类：....
*@author zlt
*@date 2026-01-23
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "S1-0604订单冲减分配")
@RestController
@RequestMapping("/dpOrderOffsetDetail")
public class DpOrderOffsetDetailController extends AbstractDocBizController<DpOrderOffsetDetail> {

    @Autowired
    private IDpOrderOffsetDetailService dpOrderOffsetDetailService;

    @Autowired
    private DpOrderOffsetDetailEntityMapper entityMapper;

    /**
     * 查询S1-0604订单冲减分配列表
     */
    @RequiresPermissions( "maindata:dpOrderOffsetDetail:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody DpOrderOffsetDetail queryVO) {
        TableDataInfo tableDataInfo = super.list(queryVO);
        List<DpOrderOffsetDetail> list = (List<DpOrderOffsetDetail>) tableDataInfo.getRows();
        JsonI18nConvertUtils.conventJsonI18n(list, DpOrderOffsetDetail.class);
        return tableDataInfo;
    }

    @ApiOperation("查询需求计划版本号")
    @PostMapping("/findMonthPlanVersion")
    public AjaxResult findMonthPlanVersion(@RequestBody DpOrderOffsetDetail queryCondition){
        return AjaxResult.success(dpOrderOffsetDetailService.getOffsetVersion(queryCondition));
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.dpOrderOffsetDetail.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "maindata:dpOrderOffsetDetail:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody DpOrderOffsetDetail billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.dpOrderOffsetDetail.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "maindata:dpOrderOffsetDetail:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取S1-0604订单冲减分配详细信息
     */
    @RequiresPermissions( "maindata:dpOrderOffsetDetail:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public DpOrderOffsetDetail getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入S1-0604订单冲减分配数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "maindata:dpOrderOffsetDetail:import")
    @Log(title = "ui.data.column.dpOrderOffsetDetail.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "maindata:dpOrderOffsetDetail:export")
    @Log(title = "S1-0604订单冲减分配", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody DpOrderOffsetDetail queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<DpOrderOffsetDetail> listExportData(DpOrderOffsetDetail obj) {
        QueryWrapper<DpOrderOffsetDetail> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<DpOrderOffsetDetail> list = entityMapper.selectList(wrapper);
        //执行公式
        try {
            QueryFormulaUtil.execFormula(list, this.getQueryFormulas());
        } catch (QueryExprException e) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
        JsonI18nConvertUtils.conventJsonI18n(list, DpOrderOffsetDetail.class);
        return list;
    }

    @Override
    protected IDocService getDocService(){
        return dpOrderOffsetDetailService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<DpOrderOffsetDetail> queryWrapper, DpOrderOffsetDetail queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("areaCode")), "AREA_CODE", queryVO.getFieldValueByFieldName("areaCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("customCode")), "CUSTOM_CODE", queryVO.getFieldValueByFieldName("customCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("customName")), "CUSTOM_NAME", queryVO.getFieldValueByFieldName("customName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("customNationCode")), "CUSTOM_NATION_CODE", queryVO.getFieldValueByFieldName("customNationCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("destinationNationCode")), "DESTINATION_NATION_CODE", queryVO.getFieldValueByFieldName("destinationNationCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("poNumber")), "PO_NUMBER", queryVO.getFieldValueByFieldName("poNumber"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderQty")), "ORDER_QTY", queryVO.getFieldValueByFieldName("orderQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockQty")), "STOCK_QTY", queryVO.getFieldValueByFieldName("stockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("allocationQty")), "ALLOCATION_QTY", queryVO.getFieldValueByFieldName("allocationQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("plannedSurplus")), "PLANNED_SURPLUS", queryVO.getFieldValueByFieldName("plannedSurplus"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("produceQtyDue")), "PRODUCE_QTY_DUE", queryVO.getFieldValueByFieldName("produceQtyDue"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionQty")), "PRODUCTION_QTY", queryVO.getFieldValueByFieldName("productionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scmPriority")), "SCM_PRIORITY", queryVO.getFieldValueByFieldName("scmPriority"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("weekYear")), "WEEK_YEAR", queryVO.getFieldValueByFieldName("weekYear"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isDynamicBalance")), "IS_DYNAMIC_BALANCE", queryVO.getFieldValueByFieldName("isDynamicBalance"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isUniformity")), "IS_UNIFORMITY", queryVO.getFieldValueByFieldName("isUniformity"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deliverGoodsType")), "DELIVER_GOODS_TYPE", queryVO.getFieldValueByFieldName("deliverGoodsType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scmId")), "SCM_ID", queryVO.getFieldValueByFieldName("scmId"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isEudr")), "IS_EUDR", queryVO.getFieldValueByFieldName("isEudr"));
    }


    @Override
    protected String getTypeCode(){
        return "S1-0604";
    }


    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "areaCodeName->getcolvaluewithcondition(t_dp_area, area_name, area_code, areaCode, is_delete = 0)",
                "customNationCodeName->getcolvaluewithcondition(T_DP_NATION, nation_name, nation_code, customNationCode, is_delete = 0)",
                "destinationNationCodeName->getcolvaluewithcondition(T_DP_NATION, nation_name, nation_code, destinationNationCode, is_delete = 0)",
        };
    }

}
