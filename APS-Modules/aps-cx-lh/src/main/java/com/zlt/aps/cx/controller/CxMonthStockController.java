package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.cx.service.ICxMonthStockService;
import com.zlt.aps.mp.api.domain.entity.CxMonthStock;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：CxMonthStockController.java
* 描    述：成型工序胎胚月结库存 控制层类：....
*@author zlt
*@date 2025-02-17
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "成型工序胎胚月结库存")
@RestController
@RequestMapping("/cxMonthStock")
public class CxMonthStockController extends AbstractDocBizController<CxMonthStock> {

    @Autowired
    private ICxMonthStockService cxMonthStockService;

    /**
     * 查询成型工序胎胚月结库存列表
     */
    @RequiresPermissions( "cx:cxMonthStock:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxMonthStock queryVO) {
        return super.list(queryVO);
    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.cxMonthStock.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "cx:cxMonthStock:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody CxMonthStock billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.cxMonthStock.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "cx:cxMonthStock:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取成型工序胎胚月结库存详细信息
     */
    @RequiresPermissions( "cx:cxMonthStock:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public CxMonthStock getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入成型工序胎胚月结库存数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "cx:cxMonthStock:import")
    @Log(title = "ui.data.column.cxMonthStock.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData/{updateSupport}")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "cx:cxMonthStock:export")
    @Log(title = "成型工序胎胚月结库存", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxMonthStock queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected IDocService getDocService(){
        return cxMonthStockService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<CxMonthStock> queryWrapper, CxMonthStock queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockMonth")), "STOCK_MONTH", queryVO.getFieldValueByFieldName("stockMonth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("bomDataVersion")), "BOM_DATA_VERSION", queryVO.getFieldValueByFieldName("bomDataVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockNum")), "STOCK_NUM", queryVO.getFieldValueByFieldName("stockNum"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("overTimeStock")), "OVER_TIME_STOCK", queryVO.getFieldValueByFieldName("overTimeStock"));
    }


    @Override
    protected String getTypeCode(){
        return "9007CX";
    }


}
