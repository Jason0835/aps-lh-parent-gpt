package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.cx.mapper.entity.CxScheduleStopInfoEntityMapper;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleStopInfo;
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


import com.zlt.aps.cx.service.ICxScheduleStopInfoService;

import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：CxScheduleStopInfoController.java
* 描    述：成型机台自动停排信息 控制层类：....
*@author zlt
*@date 2025-03-11
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "成型机台自动停排信息")
@RestController
@RequestMapping("/cxScheduleStopInfo")
public class CxScheduleStopInfoController extends AbstractDocBizController<CxScheduleStopInfo> {

    @Autowired
    private ICxScheduleStopInfoService cxScheduleStopInfoService;

    @Autowired
    private CxScheduleStopInfoEntityMapper entityMapper;

    /**
     * 查询成型机台自动停排信息列表
     */
    @RequiresPermissions( "cx:cxScheduleStopInfo:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxScheduleStopInfo queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.cxScheduleStopInfo.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @RequiresPermissions( "cx:cxScheduleStopInfo:save")
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody CxScheduleStopInfo billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.cxScheduleStopInfo.modelName", businessType = BusinessType.DELETE)
    @RequiresPermissions( "cx:cxScheduleStopInfo:remove")
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取成型机台自动停排信息详细信息
     */
    @RequiresPermissions( "cx:cxScheduleStopInfo:query")
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public CxScheduleStopInfo getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入成型机台自动停排信息数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @RequiresPermissions( "cx:cxScheduleStopInfo:import")
    @Log(title = "ui.data.column.cxScheduleStopInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @RequiresPermissions( "cx:cxScheduleStopInfo:export")
    @Log(title = "成型机台自动停排信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxScheduleStopInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<CxScheduleStopInfo> listExportData(CxScheduleStopInfo obj) {
        QueryWrapper<CxScheduleStopInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return cxScheduleStopInfoService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<CxScheduleStopInfo> queryWrapper, CxScheduleStopInfo queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cxBatchNo")), "CX_BATCH_NO", queryVO.getFieldValueByFieldName("cxBatchNo"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("spec")), "SPEC", queryVO.getFieldValueByFieldName("spec"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("sapCode")), "SAP_CODE", queryVO.getFieldValueByFieldName("sapCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderNo")), "ORDER_NO", queryVO.getFieldValueByFieldName("orderNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stopReason")), "STOP_REASON", queryVO.getFieldValueByFieldName("stopReason"));
    }


    @Override
    protected String getTypeCode(){
        return "CX9210";
    }


}
