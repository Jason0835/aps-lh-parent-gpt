package com.zlt.aps.mp.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.mapper.ProductMoldingLimitMapper;
import com.zlt.aps.maindata.service.IProductMoldingLimitService;
import com.zlt.aps.mp.api.domain.entity.ProductMoldingLimit;
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
 * 文件名称：ProductMoldingLimitController.java
 * 描    述：基础数据-品种限制成型机 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@Api(tags = "基础数据-品种限制成型机")
@RestController
@RequestMapping("/productMoldingLimit")
public class ProductMoldingLimitController extends AbstractDocBizController<ProductMoldingLimit> {

    private final IProductMoldingLimitService productMoldingLimitService;

    public ProductMoldingLimitController(IProductMoldingLimitService productMoldingLimitService) {
        this.productMoldingLimitService = productMoldingLimitService;
    }

    @Autowired
    private ProductMoldingLimitMapper entityMapper;

    /**
     * 查询基础数据-品种限制成型机列表
     */
    @PostMapping("/list")
    @ApiOperation("查询列表")
    public TableDataInfo list(@RequestBody ProductMoldingLimit queryVO) {
        this.startPage(this.getOrderBy());
        QueryWrapper<ProductMoldingLimit> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<ProductMoldingLimit> list = entityMapper.selectList(wrapper);
        return getDataTable(list);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.ProductMoldingLimit.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/save")
    @ApiOperation("保存")
    @Override
    public AjaxResult save(@RequestBody ProductMoldingLimit billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.ProductMoldingLimit.modelName", businessType = BusinessType.DELETE)
    @DeleteMapping("/remove")
    @ApiOperation("删除")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取基础数据-品种限制成型机详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public ProductMoldingLimit getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入基础数据-品种限制成型机数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.ProductMoldingLimit.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData")
    @ApiOperation("导入数据")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "基础数据-品种限制成型机", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    @ApiOperation("导入数据")
    @Override
    public byte[] exportData(@RequestBody ProductMoldingLimit queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<ProductMoldingLimit> listExportData(ProductMoldingLimit obj) {
        QueryWrapper<ProductMoldingLimit> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return productMoldingLimitService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<ProductMoldingLimit> queryWrapper, ProductMoldingLimit queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sapCode")), "SAP_CODE", queryVO.getFieldValueByFieldName("sapCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineCode")), "MACHINE_CODE", queryVO.getFieldValueByFieldName("machineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("jobType")), "JOB_TYPE", queryVO.getFieldValueByFieldName("jobType"));
    }

    @Override
    protected String getTypeCode() {
        return "0124";
    }

}
