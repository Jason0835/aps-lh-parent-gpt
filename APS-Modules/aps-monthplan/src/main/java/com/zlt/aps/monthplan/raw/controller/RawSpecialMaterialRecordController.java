package com.zlt.aps.monthplan.raw.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.RawSpecialMaterialRecordEntityMapper;
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

import com.zlt.aps.monthplan.api.domain.entity.RawSpecialMaterialRecord;
import com.zlt.aps.maindata.service.IRawSpecialMaterialRecordService;

import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：RawSpecialMaterialRecordController.java
* 描    述：特殊材料清单 控制层类：....
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
@Api(tags = "特殊材料清单")
@RestController
@RequestMapping("/rawSpecialMaterialRecord")
public class RawSpecialMaterialRecordController extends AbstractDocBizController<RawSpecialMaterialRecord> {

    @Autowired
    private IRawSpecialMaterialRecordService rawSpecialMaterialRecordService;

    @Autowired
    private RawSpecialMaterialRecordEntityMapper entityMapper;

    /**
     * 查询特殊材料清单列表
     */
    @RequiresPermissions( "maindata:rawSpecialMaterialRecord:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody RawSpecialMaterialRecord queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.rawSpecialMaterialRecord.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "maindata:rawSpecialMaterialRecord:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody RawSpecialMaterialRecord billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.rawSpecialMaterialRecord.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "maindata:rawSpecialMaterialRecord:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取特殊材料清单详细信息
     */
    @RequiresPermissions( "maindata:rawSpecialMaterialRecord:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public RawSpecialMaterialRecord getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入特殊材料清单数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "maindata:rawSpecialMaterialRecord:import")
    @Log(title = "ui.data.column.rawSpecialMaterialRecord.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "maindata:rawSpecialMaterialRecord:export")
    @Log(title = "特殊材料清单", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody RawSpecialMaterialRecord queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<RawSpecialMaterialRecord> listExportData(RawSpecialMaterialRecord obj) {
        QueryWrapper<RawSpecialMaterialRecord> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return rawSpecialMaterialRecordService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<RawSpecialMaterialRecord> queryWrapper, RawSpecialMaterialRecord queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("rubberSpec")), "RUBBER_SPEC", queryVO.getFieldValueByFieldName("rubberSpec"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialType")), "MATERIAL_TYPE", queryVO.getFieldValueByFieldName("materialType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("quota")), "QUOTA", queryVO.getFieldValueByFieldName("quota"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("unit")), "UNIT", queryVO.getFieldValueByFieldName("unit"));
    }


    @Override
    protected String getTypeCode(){
        return "RAW9005";
    }


}
