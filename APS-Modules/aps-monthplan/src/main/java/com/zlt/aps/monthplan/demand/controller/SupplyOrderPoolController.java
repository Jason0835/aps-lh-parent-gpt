package com.zlt.aps.monthplan.demand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tlt.aps.redissonLock.annotation.RedissonLockAnno;
import com.zlt.aps.monthplan.api.domain.entity.SupplyOrderPool;
import com.zlt.aps.monthplan.demand.mapper.SupplyOrderPoolEntityMapper;
import com.zlt.aps.monthplan.demand.service.ISupplyOrderPoolService;
import com.zlt.common.utils.PubUtil;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：SupplyOrderPoolController.java
* 描    述：供应链订单池 控制层类：....
*@author yelq
*@date 2025-12-22
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：yelq
*     修改内容：...
*/
@Slf4j
@Api(tags = "供应链订单池")
@RestController
@RequestMapping("/supplyOrderPool")
public class SupplyOrderPoolController extends AbstractDocBizController<SupplyOrderPool> {

    @Autowired
    private ISupplyOrderPoolService supplyOrderPoolService;

    @Autowired
    private SupplyOrderPoolEntityMapper entityMapper;

    /**
     * 查询供应链订单池列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody SupplyOrderPool queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.supplyOrderPool.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody SupplyOrderPool billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.supplyOrderPool.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取供应链订单池详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public SupplyOrderPool getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入供应链订单池数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.supplyOrderPool.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "供应链订单池", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody SupplyOrderPool queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<SupplyOrderPool> listExportData(SupplyOrderPool obj) {
        QueryWrapper<SupplyOrderPool> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @ApiOperation("生成周期排产储备")
    @RedissonLockAnno(uniqueMark = "redissonLock:supplyOrderPool:createCycleStockUp:",
        msgKey = "ui.data.alert.createCycleStockUp.run",
        waitTime = 5,
        leaseTime = 300
    )
    @PostMapping("/createCycleStockUp")
    public AjaxResult createCycleStockUp(@RequestBody SupplyOrderPool supplyOrderPool){
        supplyOrderPoolService.createCycleStockUp(supplyOrderPool);
        return AjaxResult.success();
    }

    @ApiOperation("生成常规储备")
    @RedissonLockAnno(uniqueMark = "redissonLock:supplyOrderPool:createPrecedentStockUp:",
        msgKey = "ui.data.alert.createPrecedentStockUp.run",
        waitTime = 5,
        leaseTime = 300
    )
    @PostMapping("/createPrecedentStockUp")
    public AjaxResult createPrecedentStockUp(@RequestBody SupplyOrderPool supplyOrderPool){
        supplyOrderPoolService.createPrecedentStockUp(supplyOrderPool);
        return AjaxResult.success();
    }

    /**
     * 输入NC物料编码，带出对应信息
     */
    @ApiOperation("输入NC物料编码，带出对应信息")
    @PostMapping("/queryRelationByMaterialCode")
    public AjaxResult queryRelationByMaterialCode(@RequestBody SupplyOrderPool supplyOrderPool)
    {
        return AjaxResult.success(supplyOrderPoolService.queryRelationByMaterialCode(supplyOrderPool));
    }

    @Override
    protected IDocService getDocService(){
        return supplyOrderPoolService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<SupplyOrderPool> queryWrapper, SupplyOrderPool queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCategory")), "PRODUCT_CATEGORY", queryVO.getFieldValueByFieldName("productCategory"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderType")), "ORDER_TYPE", queryVO.getFieldValueByFieldName("orderType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("saleArea")), "SALE_AREA", queryVO.getFieldValueByFieldName("saleArea"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("qty")), "QTY", queryVO.getFieldValueByFieldName("qty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("threeAverageQty")), "THREE_AVERAGE_QTY", queryVO.getFieldValueByFieldName("threeAverageQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sixAverageQty")), "SIX_AVERAGE_QTY", queryVO.getFieldValueByFieldName("sixAverageQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deliveryFrequency")), "DELIVERY_FREQUENCY", queryVO.getFieldValueByFieldName("deliveryFrequency"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureFrequency")), "STRUCTURE_FREQUENCY", queryVO.getFieldValueByFieldName("structureFrequency"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("threeOverdueStockQty")), "THREE_OVERDUE_STOCK_QTY", queryVO.getFieldValueByFieldName("threeOverdueStockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sixOverdueStockQty")), "SIX_OVERDUE_STOCK_QTY", queryVO.getFieldValueByFieldName("sixOverdueStockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("nightOverdueStockQty")), "NIGHT_OVERDUE_STOCK_QTY", queryVO.getFieldValueByFieldName("nightOverdueStockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("twelveOverdueStockQty")), "TWELVE_OVERDUE_STOCK_QTY", queryVO.getFieldValueByFieldName("twelveOverdueStockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockLimit")), "STOCK_LIMIT", queryVO.getFieldValueByFieldName("stockLimit"));
    }


    @Override
    protected String getTypeCode(){
        return "2025122214";
    }


}
