package com.zlt.aps.mp.factory.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.MpAdjustPlanRequireInfo;
import com.zlt.aps.mp.factory.dto.FactoryMonthPlanMouldDayResultExportVo;
import com.zlt.aps.mp.factory.service.IMpAdjustPlanRequireInfoService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustPlanInfoController.java
 * 描    述：S2-0604.排产结果-生产计划排产结果 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 20260716
 */
@Slf4j
@RestController
@Api(tags = ".计划调整需求信息")
@RequiredArgsConstructor
@RequestMapping("/adjustPlanRequireInfo")
public class MpAdjustPlanInfoController extends AbstractDocBizController<MpAdjustPlanRequireInfo> {

    private final IMpAdjustPlanRequireInfoService mpAdjustPlanInfoService;

    private final IExportLogService iExportLogService;

    /**
     * 查询S2-0801.计划调整信息查询列表
     */
    @Override
    @ApiOperation("查询列表")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody MpAdjustPlanRequireInfo queryVO) {
        TableDataInfo tableDataInfo = super.list(queryVO);
        return tableDataInfo;
    }

    @Override
    protected String getOrderBy() {
        return "ADJUST_DATE,STRUCTURE_NAME,MATERIAL_DESC";
    }

    /**
     * 保存
     */
    @Override
    @ApiOperation("保存")
    @PostMapping("/save")
    @Log(title = "ui.data.column.factoryMonthPlanMouldDayResult.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    public AjaxResult save(@RequestBody MpAdjustPlanRequireInfo billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Override
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Log(title = "ui.data.column.factoryMonthPlanMouldDayResult.modelName", businessType = BusinessType.DELETE)
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取S2-0604.排产结果-生产计划排产结果详细信息
     */
    @Override
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    public MpAdjustPlanRequireInfo getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入S2-0604.排产结果-生产计划排产结果数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Override
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Log(title = "ui.data.column.factoryMonthPlanMouldDayResult.modelName", businessType = BusinessType.IMPORT)
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 保存导出日志
     *
     * @param queryVO   查询条件
     * @param fileName  导出文件名
     * @param beginTime 导出开始时间
     * @param list      导出数据
     */
    private void saveExportLog(FactoryMonthPlanMouldDayResult queryVO, String fileName, Date beginTime,
                               List<FactoryMonthPlanMouldDayResultExportVo> list) {
        Date endTime = DateUtils.getNowDate();
        ExportLog exportLog = new ExportLog();
        exportLog.setProcedureCode("0");
        exportLog.setExportParams(queryVO.toString());
        String uri = ServletUtils.getRequest().getRequestURI();
        exportLog.setFunctionCode(uri.split("/")[1]);
        exportLog.setFunctionName(fileName);
        exportLog.setFileName(fileName + ".xlsx");
        exportLog.setRowCount(list.size());
        exportLog.setBeginTime(beginTime);
        exportLog.setEndTime(endTime);
        exportLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        this.iExportLogService.add(exportLog);
    }

    @Override
    protected List<MpAdjustPlanRequireInfo> listExportData(MpAdjustPlanRequireInfo obj) {
        QueryWrapper<MpAdjustPlanRequireInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.orderByAsc("ADJUST_DATE", "MATERIAL_DESC");
        return mpAdjustPlanInfoService.getListByCondition(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mpAdjustPlanInfoService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MpAdjustPlanRequireInfo> queryWrapper, MpAdjustPlanRequireInfo queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getStructureName());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getMesMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getMaterialDesc());

        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("area")), "AREA", queryVO.getArea());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planAdjustType")), "PLAN_ADJUST_TYPE", queryVO.getPlanAdjustType());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("adjustReason")), "ADJUST_REASON", queryVO.getAdjustReason());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("remark")), "REMARK", queryVO.getFieldValueByFieldName("remark"));
    }

    @Override
    protected String getTypeCode() {
        return "S2-0801";
    }

}
