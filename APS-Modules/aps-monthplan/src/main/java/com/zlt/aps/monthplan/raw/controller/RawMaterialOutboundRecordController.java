package com.zlt.aps.monthplan.raw.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.RawMaterialRequirePlanEntityMapper;
import com.zlt.common.utils.PubUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import com.zlt.aps.monthplan.api.domain.entity.RawMaterialOutboundRecord;
import com.zlt.aps.maindata.service.IRawMaterialOutboundRecordService;

import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;
import com.zlt.aps.maindata.mapper.RawMaterialOutboundRecordEntityMapper;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：RawMaterialOutboundRecordController.java
* 描    述：原材料出库量 控制层类：....
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
@Api(tags = "原材料出库量")
@RestController
@RequestMapping("/rawMaterialOutboundRecord")
public class RawMaterialOutboundRecordController extends AbstractDocBizController<RawMaterialOutboundRecord> {

    @Autowired
    private IRawMaterialOutboundRecordService rawMaterialOutboundRecordService;

    @Autowired
    private RawMaterialOutboundRecordEntityMapper entityMapper;

    /**
     * 查询原材料出库量列表
     */
    @RequiresPermissions( "maindata:rawMaterialOutboundRecord:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody RawMaterialOutboundRecord queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.rawMaterialOutboundRecord.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "maindata:rawMaterialOutboundRecord:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody RawMaterialOutboundRecord billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.rawMaterialOutboundRecord.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "maindata:rawMaterialOutboundRecord:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取原材料出库量详细信息
     */
    @RequiresPermissions( "maindata:rawMaterialOutboundRecord:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public RawMaterialOutboundRecord getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入原材料出库量数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "maindata:rawMaterialOutboundRecord:import")
    @Log(title = "ui.data.column.rawMaterialOutboundRecord.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "maindata:rawMaterialOutboundRecord:export")
    @Log(title = "原材料出库量", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody RawMaterialOutboundRecord queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<RawMaterialOutboundRecord> listExportData(RawMaterialOutboundRecord obj) {
        QueryWrapper<RawMaterialOutboundRecord> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return rawMaterialOutboundRecordService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<RawMaterialOutboundRecord> queryWrapper, RawMaterialOutboundRecord queryVO) {
        // 精确查询的字段
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialType")), "MATERIAL_TYPE", queryVO.getFieldValueByFieldName("materialType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("outboundDate")), "OUTBOUND_DATE", queryVO.getFieldValueByFieldName("outboundDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("outboundQty")), "OUTBOUND_QTY", queryVO.getFieldValueByFieldName("outboundQty"));

        // 模糊查询的字段
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("unit")), "UNIT", queryVO.getFieldValueByFieldName("unit"));
    }


    @Override
    protected String getTypeCode(){
        return "RAW9004";
    }


    /**
     * MES抓取
     */
    @Log(title = "ui.data.column.rawMaterialOutboundRecord.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "maindata:rawMaterialOutboundRecord:catch")
    @ApiOperation("MES抓取")
    @PostMapping("/mesCatch")
    public AjaxResult mesCatch(){
        //todo 对接接口
        return AjaxResult.success();
    }


}
