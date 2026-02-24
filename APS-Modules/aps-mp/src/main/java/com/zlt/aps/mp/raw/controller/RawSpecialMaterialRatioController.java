package com.zlt.aps.mp.raw.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.RawSpecialMaterialRatioEntityMapper;
import com.zlt.common.utils.PubUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
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

import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialRatio;
import com.zlt.aps.maindata.service.IRawSpecialMaterialRatioService;

import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：RawSpecialMaterialRatioController.java
* 描    述：特殊材料批次比例 控制层类：....
*@author zlt
*@date 2025-12-10
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "特殊材料批次比例")
@RestController
@RequestMapping("/rawSpecialMaterialRatio")
public class RawSpecialMaterialRatioController extends AbstractDocBizController<RawSpecialMaterialRatio> {

    @Autowired
    private IRawSpecialMaterialRatioService rawSpecialMaterialRatioService;

    @Autowired
    private RawSpecialMaterialRatioEntityMapper entityMapper;

    /**
     * 查询特殊材料批次比例列表
     */
    @RequiresPermissions( "maindata:rawSpecialMaterialRatio:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody RawSpecialMaterialRatio queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.rawSpecialMaterialRatio.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "maindata:rawSpecialMaterialRatio:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody RawSpecialMaterialRatio billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.rawSpecialMaterialRatio.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "maindata:rawSpecialMaterialRatio:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取特殊材料批次比例详细信息
     */
    @RequiresPermissions( "maindata:rawSpecialMaterialRatio:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public RawSpecialMaterialRatio getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入特殊材料批次比例数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "maindata:rawSpecialMaterialRatio:import")
    @Log(title = "ui.data.column.rawSpecialMaterialRatio.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "maindata:rawSpecialMaterialRatio:export")
    @Log(title = "特殊材料批次比例", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody RawSpecialMaterialRatio queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<RawSpecialMaterialRatio> listExportData(RawSpecialMaterialRatio obj) {
        QueryWrapper<RawSpecialMaterialRatio> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return rawSpecialMaterialRatioService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<RawSpecialMaterialRatio> queryWrapper, RawSpecialMaterialRatio queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("standardLength")), "STANDARD_LENGTH", queryVO.getFieldValueByFieldName("standardLength"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("ratio")), "RATIO", queryVO.getFieldValueByFieldName("ratio"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("unit")), "UNIT", queryVO.getFieldValueByFieldName("unit"));
    }


    @Override
    protected String getTypeCode(){
        return "RAW9002";
    }


}
