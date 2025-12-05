package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.text.Convert;
import com.tlt.aps.constant.StringConstant;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.mapper.ProductMinConfigurationMapper;
import com.zlt.aps.maindata.service.IProductMinConfigurationService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.ProductMinConfiguration;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：ProductMinConfigurationController.java
* 描    述：最小批量 控制层类：....
*@author ZLT
*@date 2025-02-26
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：ZLT
*     修改内容：...
*/
@Slf4j
@Api(tags = "最小批量")
@RestController
@RequestMapping("/productMinConfiguration")
public class ProductMinConfigurationController extends AbstractDocBizController<ProductMinConfiguration> {

    @Autowired
    private IProductMinConfigurationService productMinConfigurationService;

    @Autowired
    private ProductMinConfigurationMapper productMinConfigurationMapper;

    @Autowired
    private MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;

    /**
     * 查询最小批量列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody ProductMinConfiguration queryVO) {
        this.startPage(this.getOrderBy());
        QueryWrapper<ProductMinConfiguration> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<ProductMinConfiguration> list = productMinConfigurationMapper.selectList(wrapper);
        return getDataTable(list);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.productMinConfiguration.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody ProductMinConfiguration billVO) {
        if (Objects.nonNull(billVO.getId())) {
            return toAjax(productMinConfigurationService.save(billVO));
        }
        List<ProductMinConfiguration> list = new ArrayList<>();
        if (!StringConstant.ALL_MATCH.equals(billVO.getProductCode())) {
            String productCode = billVO.getProductCode();
            String[] pcs = Convert.toStrArray(productCode);
            for (String item : pcs) {
                LambdaQueryWrapper<MdmMaterialInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(MdmMaterialInfo::getMaterialCode, item);
                MdmMaterialInfo productInfo = mdmMaterialInfoEntityMapper.selectOne(wrapper);

                ProductMinConfiguration cinfo = new ProductMinConfiguration();
                cinfo.setFactoryCode(billVO.getFactoryCode());
                cinfo.setProductCode(item);
                cinfo.setProductType(productInfo.getProductTypeName());
                cinfo.setProductDesc(productInfo.getSpecifications());
                cinfo.setMinQty(billVO.getMinQty());
                cinfo.setUpQty(billVO.getUpQty());
                list.add(cinfo);
            }
        } else {
            list.add(billVO);
        }
        for (ProductMinConfiguration productMinConfiguration : list) {
            productMinConfigurationService.checkUnique(productMinConfiguration);
        }
        int save = productMinConfigurationService.save(list);
        return toAjax(save);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.productMinConfiguration.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取最小批量详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public ProductMinConfiguration getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入最小批量数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.productMinConfiguration.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "最小批量", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody ProductMinConfiguration queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<ProductMinConfiguration> listExportData(ProductMinConfiguration queryVO) {
        QueryWrapper<ProductMinConfiguration> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        return productMinConfigurationMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return productMinConfigurationService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<ProductMinConfiguration> queryWrapper, ProductMinConfiguration queryVO) {
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productDesc")), "PRODUCT_DESC", queryVO.getFieldValueByFieldName("productDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productType")), "PRODUCT_TYPE", queryVO.getFieldValueByFieldName("productType"));
    }

    @Override
    protected String getTypeCode(){
        return "0138";
    }

}
