package com.zlt.aps.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.mdm.mapper.MdmSkuConstructionRefEntityMapper;
import com.zlt.aps.mdm.service.IMdmSkuConstructionRefService;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuConstructionRef;
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
* 文件名称：MdmSkuConstructionRefController.java
* 描    述：SKU与施工（示方书）关系 控制层类：....
*@author zlt
*@date 2025-12-06
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "SKU与施工（示方书）关系")
@RestController
@RequestMapping("/mdmSkuConstructionRef")
public class MdmSkuConstructionRefController extends AbstractDocBizController<MdmSkuConstructionRef> {

    @Autowired
    private IMdmSkuConstructionRefService mdmSkuConstructionRefService;

    @Autowired
    private MdmSkuConstructionRefEntityMapper entityMapper;

    /**
     * 查询SKU与施工（示方书）关系列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmSkuConstructionRef queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmSkuConstructionRef.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmSkuConstructionRef billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmSkuConstructionRef.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "mdm:mdmSkuConstructionRef:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取SKU与施工（示方书）关系详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmSkuConstructionRef getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入SKU与施工（示方书）关系数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmSkuConstructionRef.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "SKU与施工（示方书）关系", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmSkuConstructionRef queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmSkuConstructionRef> listExportData(MdmSkuConstructionRef obj) {
        QueryWrapper<MdmSkuConstructionRef> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mdmSkuConstructionRefService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmSkuConstructionRef> queryWrapper, MdmSkuConstructionRef queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("constructionCode")), "CONSTRUCTION_CODE", queryVO.getFieldValueByFieldName("constructionCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", queryVO.getFieldValueByFieldName("productionVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldMethod")), "MOULD_METHOD", queryVO.getFieldValueByFieldName("mouldMethod"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("bomVersion")), "BOM_VERSION", queryVO.getFieldValueByFieldName("bomVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldClampingPressure")), "MOULD_CLAMPING_PRESSURE", queryVO.getFieldValueByFieldName("mouldClampingPressure"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldCavity")), "MOULD_CAVITY", queryVO.getFieldValueByFieldName("mouldCavity"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("curingTime")), "CURING_TIME", queryVO.getFieldValueByFieldName("curingTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hydraulicPressureCuringTime")), "HYDRAULIC_PRESSURE_CURING_TIME", queryVO.getFieldValueByFieldName("hydraulicPressureCuringTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("curingTime2")), "CURING_TIME2", queryVO.getFieldValueByFieldName("curingTime2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hydraulicPressureCuringTime2")), "HYDRAULIC_PRESSURE_CURING_TIME2", queryVO.getFieldValueByFieldName("hydraulicPressureCuringTime2"));
    }


    @Override
    protected String getTypeCode(){
        return "MDM0123";
    }

    /**
     * 抓取MES数据
     * @return 结果
     */
    @ApiOperation("抓取MES数据")
    @PostMapping("/mesCapture")
    public AjaxResult mesCapture() {
        // TODO...
        return AjaxResult.success();
    }

    /**
     * 更新胎胚描述到物料表
     *
     * @param queryVO 参数
     * @return 结果
     */
    @ApiOperation("更新胎胚描述到物料表")
    @PostMapping("/updateMainMaterialDescToMaterialInfo")
    public AjaxResult updateMainMaterialDescToMaterialInfo(@RequestBody MdmSkuConstructionRef queryVO) {
        return mdmSkuConstructionRefService.updateMainMaterialDescToMaterialInfo(queryVO);
    }
}
