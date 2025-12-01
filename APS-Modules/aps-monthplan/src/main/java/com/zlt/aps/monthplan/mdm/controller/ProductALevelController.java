package com.zlt.aps.monthplan.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.maindata.service.IProductALevelService;
import com.zlt.aps.monthplan.api.domain.entity.ProductALevel;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：ProductALevelController.java
* 描    述：基础数据-SAP-OEE率 控制层类：....
*@author ZLT
*@date 2025-02-20
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：ZLT
*     修改内容：...
*/
@Slf4j
@Api(tags = "基础数据-SAP-OEE率")
@RestController
@RequestMapping("/productDamage")
public class ProductALevelController extends AbstractDocBizController<ProductALevel> {

    @Autowired
    private IProductALevelService productALevelService;

    /**
     * 查询基础数据-SAP-OEE率列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody ProductALevel queryVO) {
        startPage("create_time desc");
        List<ProductALevel> list = productALevelService.selectDocProductALevelList(queryVO);
        //执行公式
        try {
            QueryFormulaUtil.execFormula(list, this.getQueryFormulas());
        } catch (QueryExprException e) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
        return getDataTable(list);
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.ProductALevel.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody ProductALevel billVO){
        return toAjax(baseDao.save(billVO));
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.ProductALevel.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return toAjax(baseDao.deleteByIds(ProductALevel.class, ids));
    }

    /**
     * 获取基础数据-SAP-OEE率详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public ProductALevel getInfo(@PathVariable("billId") Long billId) {
        ProductALevel productALevel = baseDao.selectById(ProductALevel.class, billId);
        //执行公式
        try {
            QueryFormulaUtil.execFormula(Collections.singletonList(productALevel), this.getQueryFormulas());
        } catch (QueryExprException e) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
        return productALevel;
    }


    /**
     * 根据集合导入基础数据-SAP-OEE率数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.ProductALevel.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "基础数据-SAP-OEE率", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody ProductALevel queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return commonExport(queryVO, fileName,response);
    }

    @Override
//    @DataAuth(docFields = {"PRODUCT_TYPE_CODE", "FACTORY_CODE"}, docTypes = {DocTypeEnum.PRODUCT_NAME, DocTypeEnum.FACTORY_CODE})
    public List<ProductALevel> listExportData(ProductALevel productALevel) {
        startPage("create_time desc");
        return productALevelService.selectDocProductALevelList(productALevel);
    }

    @Override
    protected IDocService getDocService(){
        return productALevelService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<ProductALevel> queryWrapper, ProductALevel queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("aLevel")), "A_LEVEL", queryVO.getFieldValueByFieldName("aLevel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isStockUp")), "IS_STOCK_UP", queryVO.getFieldValueByFieldName("isStockUp"));

        boolean flag1 = PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productDesc"));
        String addSql = "";
        if (flag1) {
            addSql += "and b.PRODUCT_DESC LIKE %" + queryVO.getFieldValueByFieldName("productDesc") + "%";
        }
        boolean flag2 = PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize"));
        if (flag2) {
            addSql += "and b.PRO_SIZE = " + queryVO.getFieldValueByFieldName("proSize");
        }
        boolean flag3 = PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern"));
        if (flag3) {
            addSql += "and b.PATTERN LIKE %" + queryVO.getFieldValueByFieldName("pattern") + "%";
        }
        boolean flag4 = PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand"));
        if (flag4) {
            addSql += "and b.BRAND = " + queryVO.getFieldValueByFieldName("brand");
        }
        queryWrapper.exists(
                StringUtils.isNotBlank(addSql),
                " SELECT 1 FROM T_MDM_PRODUCT_INFO b WHERE b.PRODUCT_CODE = T_MDM_PRODUCT_A_LEVEL.PRODUCT_CODE and b.FACTORY_CODE = T_MDM_PRODUCT_A_LEVEL.FACTORY_CODE "
                + addSql
        );
    }

    @Override
    protected String getTypeCode(){
        return "DOC0104";
    }

    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "productDesc,proSize,pattern,brand->getcolsvalue(T_MDM_PRODUCT_INFO, [PRODUCT_DESC,PRO_SIZE,PATTERN,BRAND], PRODUCT_CODE, productCode)",
        };
    }

    /**
     * 不备货
     * @param ids 集合
     * @param year 年
     * @param month 月
     * @return 结果
     */
    @ApiOperation("不备货")
    @PostMapping("/noStockUp")
    public AjaxResult noStockUp(@RequestBody List<Long> ids, @RequestParam("year") Integer year, @RequestParam("month") Integer month) {
        return productALevelService.noStockUp(ids, year, month);
    }
}
