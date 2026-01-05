package com.zlt.aps.monthplan.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.maindata.mapper.MdmSkuLhCapacityEntityMapper;
import com.zlt.aps.maindata.service.IMdmSkuLhCapacityService;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuLhCapacity;
import com.zlt.common.exception.QueryExprException;
import com.zlt.common.utils.PubUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.zlt.core.queryformulas.QueryFormulaUtil;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MdmSkuLhCapacityController.java
* 描    述：SKU日硫化产能 控制层类：....
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
@Api(tags = "SKU日硫化产能")
@RestController
@RequestMapping("/mdmSkuLhCapacity")
public class MdmSkuLhCapacityController extends AbstractDocBizController<MdmSkuLhCapacity> {

    @Autowired
    private IMdmSkuLhCapacityService mdmSkuLhCapacityService;

    @Autowired
    private MdmSkuLhCapacityEntityMapper entityMapper;

    /**
     * 查询SKU日硫化产能列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmSkuLhCapacity queryVO) {
        TableDataInfo tableDataInfo = super.list(queryVO);
        try {
            QueryFormulaUtil.execFormula(tableDataInfo.getRows(), this.getQueryFormulas());
        } catch (QueryExprException e) {
            throw new ServiceException("执行查询公式时发生错误.");
        }
        // 计算APS日硫化量
        calculateApsCapacity(tableDataInfo.getRows());
        return tableDataInfo;
    }


    /**
     * 计算APS日硫化量
     */
    private void calculateApsCapacity(List<?> sourceList) {
        if (PubUtil.isEmpty(sourceList)) {
            return;
        }
        List<MdmSkuLhCapacity> mdmSkuLhCapacityList = (List<MdmSkuLhCapacity>) sourceList;
        // 计算APS日硫化量：APS日硫化量 = 24 * 60 / 硫化总时间(min)
        mdmSkuLhCapacityList.stream()
                .filter(skuCapacity -> {
                    Integer sum = skuCapacity.getSumVulcanization();
                    return sum != null && sum > 0;
                })
                .forEach(skuCapacity -> {
                    Integer sumVulcanization = skuCapacity.getSumVulcanization();
                    double divisionResult = (double) ApsConstant.MINUTES_PER_DAY / sumVulcanization;
                    double ceilResult = Math.ceil(divisionResult);
                    skuCapacity.setApsCapacity(Convert.toInt(ceilResult));
                });
            // 设置默认值
            mdmSkuLhCapacityList.stream()
                    .filter(skuCapacity -> {
                        Integer sum = skuCapacity.getSumVulcanization();
                        return sum == null || sum <= 0;
                    })
                    .forEach(skuCapacity -> skuCapacity.setApsCapacity(0));
    }


    @Override
    protected String[] getQueryFormulas() {
        return new String[]{
                "materialDesc -> getcolvalue(T_MDM_MATERIAL_INFO, MATERIAL_DESC, MATERIAL_CODE, materialCode)"
        };
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmSkuLhCapacity.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmSkuLhCapacity billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmSkuLhCapacity.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取SKU日硫化产能详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmSkuLhCapacity getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入SKU日硫化产能数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmSkuLhCapacity.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "SKU日硫化产能", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmSkuLhCapacity queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmSkuLhCapacity> listExportData(MdmSkuLhCapacity obj) {
        QueryWrapper<MdmSkuLhCapacity> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<MdmSkuLhCapacity> resultList = entityMapper.selectList(wrapper);
        // 计算APS日硫化量
        calculateApsCapacity(resultList);
        return resultList;
    }

    @Override
    protected IDocService getDocService(){
        return mdmSkuLhCapacityService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmSkuLhCapacity> queryWrapper, MdmSkuLhCapacity queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("classCapacity")), "CLASS_CAPACITY", queryVO.getFieldValueByFieldName("classCapacity"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesCapacity")), "MES_CAPACITY", queryVO.getFieldValueByFieldName("mesCapacity"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("standardCapacity")), "STANDARD_CAPACITY", queryVO.getFieldValueByFieldName("standardCapacity"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("apsCapacity")), "APS_CAPACITY", queryVO.getFieldValueByFieldName("apsCapacity"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sumVulcanization")), "SUM_VULCANIZATION", queryVO.getFieldValueByFieldName("sumVulcanization"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("vulcanizationTime")), "VULCANIZATION_TIME", queryVO.getFieldValueByFieldName("vulcanizationTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mechanicalTime")), "MECHANICAL_TIME", queryVO.getFieldValueByFieldName("mechanicalTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("checkTime")), "CHECK_TIME", queryVO.getFieldValueByFieldName("checkTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("clearTime")), "CLEAR_TIME", queryVO.getFieldValueByFieldName("clearTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionTime")), "PRODUCTION_TIME", queryVO.getFieldValueByFieldName("productionTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("standardTime")), "STANDARD_TIME", queryVO.getFieldValueByFieldName("standardTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dineTime")), "DINE_TIME", queryVO.getFieldValueByFieldName("dineTime"));
    }


    @Override
    protected String getTypeCode(){
        return "MDM0135";
    }


}
