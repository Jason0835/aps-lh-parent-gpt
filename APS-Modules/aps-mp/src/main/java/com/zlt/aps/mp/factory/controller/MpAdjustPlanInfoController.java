package com.zlt.aps.mp.factory.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.mp.api.domain.entity.MpAdjustPlanRequireInfo;
import com.zlt.aps.mp.factory.service.IMpAdjustPlanRequireInfoService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpAdjustPlanInfoController.java
 * 描    述：S2-0611.计划调整需求信息 控制层类：....
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
@Api(tags = "计划调整需求信息")
@RestController
@RequestMapping("/adjustPlanRequireInfo")
@RequiredArgsConstructor
public class MpAdjustPlanInfoController extends AbstractDocBizController<MpAdjustPlanRequireInfo> {

    private final IMpAdjustPlanRequireInfoService mpAdjustPlanInfoService;

    /** 查询计划调整需求信息列表 */
    @ApiOperation("查询计划调整需求信息列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpAdjustPlanRequireInfo queryVO) {
        return super.list(queryVO);
    }

    /** 新增计划调整需求信息 */
    @Log(title = "ui.data.column.mpAdjustPlanInfo.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增计划调整需求信息")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody MpAdjustPlanRequireInfo entity) {
        return super.save(entity);
    }

    /** 编辑计划调整需求信息 */
    @Log(title = "ui.data.column.mpAdjustPlanInfo.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑计划调整需求信息")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody MpAdjustPlanRequireInfo entity) {
        return super.save(entity);
    }

    /** 删除计划调整需求信息 */
    @Log(title = "ui.data.column.mpAdjustPlanInfo.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除计划调整需求信息")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /** 获取计划调整需求信息详情 */
    @ApiOperation("获取计划调整需求信息详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public MpAdjustPlanRequireInfo getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /** 导入计划调整需求信息 */
    @Log(title = "ui.data.column.mpAdjustPlanInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入计划调整需求信息")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /** 导出计划调整需求信息 */
    @Log(title = "ui.data.column.mpAdjustPlanInfo.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出计划调整需求信息")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpAdjustPlanRequireInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
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
     * @param queryWrapper 查询包装器
     * @param queryVO      查询条件
     */
    @Override
    protected void builderCondition(QueryWrapper<MpAdjustPlanRequireInfo> queryWrapper, MpAdjustPlanRequireInfo queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getStructureName()), "STRUCTURE_NAME", queryVO.getStructureName());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMesMaterialCode()), "MES_MATERIAL_CODE", queryVO.getMesMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMaterialCode()), "MATERIAL_CODE", queryVO.getMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMaterialDesc()), "MATERIAL_DESC", queryVO.getMaterialDesc());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getArea()), "AREA", queryVO.getArea());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getPlanAdjustType()), "PLAN_ADJUST_TYPE", queryVO.getPlanAdjustType());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getAdjustReason()), "ADJUST_REASON", queryVO.getAdjustReason());
        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getAdjustDateStart()), "ADJUST_DATE", queryVO.getAdjustDateStart());
        queryWrapper.le(PubUtil.isNotEmpty(queryVO.getAdjustDateEnd()), "ADJUST_DATE", queryVO.getAdjustDateEnd());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getRemark()), "REMARK", queryVO.getRemark());
    }

    @Override
    protected String getTypeCode() {
        return "S2-0801";
    }

    @Override
    protected String getOrderBy() {
        return "ADJUST_DATE,STRUCTURE_NAME,MATERIAL_DESC";
    }
}
