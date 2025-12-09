package com.zlt.aps.monthplan.demand.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.monthplan.api.domain.entity.SalesOrderPool;
import com.zlt.aps.monthplan.demand.mapper.SalesOrderPoolEntityMapper;
import com.zlt.aps.monthplan.demand.service.ISalesOrderPoolService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;
import com.zlt.common.utils.PubUtil;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：SalesOrderPoolController.java
* 描    述：销售订单池 控制层类：....
*@author zlt
*@date 2025-12-04
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "销售订单池")
@RestController
@RequestMapping("/SalesOrderPool")
public class SalesOrderPoolController extends AbstractDocBizController<SalesOrderPool> {

    @Autowired
    private ISalesOrderPoolService salesOrderPoolService;

    @Autowired
    private SalesOrderPoolEntityMapper entityMapper;

    /**
     * 查询销售订单池列表
     */
    @RequiresPermissions( "monthplan:SalesOrderPool:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody SalesOrderPool queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.SalesOrderPool.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "monthplan:SalesOrderPool:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody SalesOrderPool billVO){
        return super.save(billVO);
    }

    /**
     *  批量修改同PO号的销售优先级
     */
    @Log(title = "ui.data.column.SalesOrderPool.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "monthplan:SalesOrderPool:save")
    @ApiOperation("批量修改同PO号的销售优先级")
    @PostMapping("/editBySalCodePo")
    public AjaxResult editBySalCodePo(@RequestBody SalesOrderPool billVO){
        return salesOrderPoolService.editBySalCodePo(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.SalesOrderPool.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "monthplan:SalesOrderPool:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取销售订单池详细信息
     */
    @RequiresPermissions( "monthplan:SalesOrderPool:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public SalesOrderPool getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入销售订单池数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "monthplan:SalesOrderPool:import")
    @Log(title = "ui.data.column.SalesOrderPool.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "monthplan:SalesOrderPool:export")
    @Log(title = "销售订单池", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody SalesOrderPool queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<SalesOrderPool> listExportData(SalesOrderPool obj) {
        QueryWrapper<SalesOrderPool> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }
    
    /**
     * 检查SCM数据
     */
    @RequiresPermissions( "monthplan:SalesOrderPool:getSCMData")
    @ApiOperation("检查SCM数据")
    @PostMapping("/checkSCMData")
    public AjaxResult checkSCMData(){
    	// TODO 接口实现后调用抓取接口
        return AjaxResult.success("北美区域未识别到优先级，是否继续", "1");
    }
    
    /**
     * 抓取SCM数据
     */
    @RequiresPermissions( "monthplan:SalesOrderPool:getSCMData")
    @ApiOperation("抓取SCM数据")
    @PostMapping("/getSCMData")
    public AjaxResult getSCMData(){
    	// TODO 接口实现后调用抓取接口
        return AjaxResult.success();
    }


    @Override
    protected IDocService getDocService(){
        return salesOrderPoolService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<SalesOrderPool> queryWrapper, SalesOrderPool queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productType")), "PRODUCT_TYPE", queryVO.getFieldValueByFieldName("productType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderPriority")), "ORDER_PRIORITY", queryVO.getFieldValueByFieldName("orderPriority"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("area")), "AREA", queryVO.getFieldValueByFieldName("area"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("salCode")), "SAL_CODE", queryVO.getFieldValueByFieldName("salCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("salNCode")), "SAL_N_CODE", queryVO.getFieldValueByFieldName("salNCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("natCode")), "NAT_CODE", queryVO.getFieldValueByFieldName("natCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("salCodePo")), "SAL_CODE_PO", queryVO.getFieldValueByFieldName("salCodePo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("billDate")), "BILL_DATE", queryVO.getFieldValueByFieldName("billDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("oriMaterialCode")), "ORI_MATERIAL_CODE", queryVO.getFieldValueByFieldName("oriMaterialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("ordQty")), "ORD_QTY", queryVO.getFieldValueByFieldName("ordQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("weekYear")), "WEEK_YEAR", queryVO.getFieldValueByFieldName("weekYear"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dynamicBalance")), "DYNAMIC_BALANCE", queryVO.getFieldValueByFieldName("dynamicBalance"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("uniformity")), "UNIFORMITY", queryVO.getFieldValueByFieldName("uniformity"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("eudr")), "EUDR", queryVO.getFieldValueByFieldName("eudr"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deliverGoodsType")), "DELIVER_GOODS_TYPE", queryVO.getFieldValueByFieldName("deliverGoodsType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scmPriority")), "SCM_PRIORITY", queryVO.getFieldValueByFieldName("scmPriority"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scmDetailId")), "SCM_DETAIL_ID", queryVO.getFieldValueByFieldName("scmDetailId"));
    }


    @Override
    protected String getTypeCode(){
        return "MP099";
    }


}
