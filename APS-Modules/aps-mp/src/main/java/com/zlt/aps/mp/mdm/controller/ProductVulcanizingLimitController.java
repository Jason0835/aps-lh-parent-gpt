package com.zlt.aps.mp.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.maindata.service.IProductVulcanizingLimitService;
import com.zlt.aps.monthplan.api.domain.entity.ProductVulcanizingLimit;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductVulcanizingLimitController.java
 * 描    述：基础数据-品种限制硫化机 控制层类：....
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@Api(tags = "基础数据-品种限制硫化机")
@RestController
@RequestMapping("/productVulcanizingLimit")
public class ProductVulcanizingLimitController extends BaseController<ProductVulcanizingLimit> {

    private final IProductVulcanizingLimitService productVulcanizingLimitService;

    public ProductVulcanizingLimitController(IProductVulcanizingLimitService productVulcanizingLimitService) {
        this.productVulcanizingLimitService = productVulcanizingLimitService;
    }

    /**
     * 查询基础数据-品种限制硫化机列表
     */
    @PostMapping("/list")
    @ApiOperation("查询列表")
    public TableDataInfo list(@RequestBody ProductVulcanizingLimit queryVO) {
        return null;
    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.ProductVulcanizingLimit.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @PostMapping("/save")
    @ApiOperation("保存")
    public AjaxResult save(@RequestBody ProductVulcanizingLimit billVO) {
        return null;
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.ProductVulcanizingLimit.modelName", businessType = BusinessType.DELETE)
    @DeleteMapping("/remove")
    @ApiOperation("删除")
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return null;
    }


    /**
     * 获取基础数据-品种限制硫化机详细信息
     */
    @GetMapping(value = "/{billId}")
    @ApiOperation("获取详细信息")
    public ProductVulcanizingLimit getInfo(@PathVariable("billId") Long billId) {
        return null;
    }


    /**
     * 根据集合导入基础数据-品种限制硫化机数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.ProductVulcanizingLimit.modelName", businessType = BusinessType.IMPORT)
    @PostMapping("/importData/{updateSupport}")
    @ApiOperation("导入数据")
    public AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport) throws Exception {
        return null;
    }

    /**
     * 导出列表
     */
    @Log(title = "基础数据-品种限制硫化机", businessType = BusinessType.EXPORT)
    @PostMapping("/exportData/{fileName}")
    @ApiOperation("导入数据")
    public byte[] exportData(@RequestBody ProductVulcanizingLimit queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return null;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    private void builderCondition(QueryWrapper<ProductVulcanizingLimit> queryWrapper, ProductVulcanizingLimit queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("vulcanizingMachineId")), "VULCANIZING_MACHINE_ID", queryVO.getFieldValueByFieldName("vulcanizingMachineId"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("limitType")), "LIMIT_TYPE", queryVO.getFieldValueByFieldName("limitType"));
    }

}
