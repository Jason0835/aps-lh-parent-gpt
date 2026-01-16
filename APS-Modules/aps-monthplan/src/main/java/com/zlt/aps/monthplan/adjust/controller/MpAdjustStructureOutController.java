package com.zlt.aps.monthplan.adjust.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.monthplan.adjust.mapper.MpAdjustStructureOutEntityMapper;
import com.zlt.aps.monthplan.adjust.service.IMpAdjustStructureOutService;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustStructureOut;
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


import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MpAdjustStructureOutController.java
* 描    述：调整-结构调整记录 控制层类：....
*@author zlt
*@date 2025-12-19
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "调整-结构调整记录")
@RestController
@RequestMapping("/mpAdjustStructureOut")
public class MpAdjustStructureOutController extends AbstractDocBizController<MpAdjustStructureOut> {

    @Autowired
    private IMpAdjustStructureOutService mpAdjustStructureOutService;

    @Autowired
    private MpAdjustStructureOutEntityMapper entityMapper;

    /**
     * 查询调整-结构调整记录列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpAdjustStructureOut queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mpAdjustStructureOut.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpAdjustStructureOut billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mpAdjustStructureOut.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取调整-结构调整记录详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MpAdjustStructureOut getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入调整-结构调整记录数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mpAdjustStructureOut.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "调整-结构调整记录", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpAdjustStructureOut queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MpAdjustStructureOut> listExportData(MpAdjustStructureOut obj) {
        QueryWrapper<MpAdjustStructureOut> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mpAdjustStructureOutService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MpAdjustStructureOut> queryWrapper, MpAdjustStructureOut queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("version")), "VERSION", queryVO.getFieldValueByFieldName("version"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduledMachines")), "SCHEDULED_MACHINES", queryVO.getFieldValueByFieldName("scheduledMachines"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hasSpecialMaterial")), "HAS_SPECIAL_MATERIAL", queryVO.getFieldValueByFieldName("hasSpecialMaterial"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("previousNetQty")), "PREVIOUS_NET_QTY", queryVO.getFieldValueByFieldName("previousNetQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("currentNetQty")), "CURRENT_NET_QTY", queryVO.getFieldValueByFieldName("currentNetQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("netQtyChange")), "NET_QTY_CHANGE", queryVO.getFieldValueByFieldName("netQtyChange"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthScheduledQty")), "MONTH_SCHEDULED_QTY", queryVO.getFieldValueByFieldName("monthScheduledQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pendingQty")), "PENDING_QTY", queryVO.getFieldValueByFieldName("pendingQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("confirmAdjustQty")), "CONFIRM_ADJUST_QTY", queryVO.getFieldValueByFieldName("confirmAdjustQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("adjustPriority")), "ADJUST_PRIORITY", queryVO.getFieldValueByFieldName("adjustPriority"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("actualAdjustQty")), "ACTUAL_ADJUST_QTY", queryVO.getFieldValueByFieldName("actualAdjustQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("adjustReason")), "ADJUST_REASON", queryVO.getFieldValueByFieldName("adjustReason"));
    }


    @Override
    protected String getTypeCode(){
        return "MP0806";
    }

    /**
     * 查询版本列表
     */
    @ApiOperation("查询版本列表")
    @PostMapping("/getVersionList")
    public TableDataInfo getVersionList(@RequestBody MpAdjustStructureOut queryVO) {
        this.startPage();
        List<MpAdjustStructureOut> list = entityMapper.getVersionList(queryVO);
        this.clearPage();
        return this.getDataTable(list);
    }


}
