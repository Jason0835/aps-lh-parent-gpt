package com.zlt.aps.mp.adjust.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.utils.PageUtils;
import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.mp.adjust.mapper.MpAdjustStructureInEntityMapper;
import com.zlt.aps.mp.adjust.service.IMpAdjustStructureInService;
import com.zlt.aps.mp.api.domain.entity.MpAdjustStructureIn;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MpAdjustStructureInController.java
* 描    述：调整-结构内调整记录 控制层类：....
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
@Api(tags = "调整-结构内调整记录")
@RestController
@RequestMapping("/mpAdjustStructureIn")
public class MpAdjustStructureInController extends AbstractDocBizController<MpAdjustStructureIn> {

    @Autowired
    private IMpAdjustStructureInService mpAdjustStructureInService;

    @Autowired
    private MpAdjustStructureInEntityMapper entityMapper;

    /**
     * 查询调整-结构内调整记录列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpAdjustStructureIn queryVO) {
        PageUtils.clearPage();
        TableDataInfo tableDataInfo = super.list(queryVO);
        sortlList(tableDataInfo.getRows());
        return tableDataInfo;
    }

    protected void sortlList(List<?> rows) {
        if (PubUtil.isEmpty(rows)) {
            return;
        }
        List<MpAdjustStructureIn> mpAdjustStructureInList = (List<MpAdjustStructureIn>) rows;
        Collections.sort(mpAdjustStructureInList, getSortComparator());
    }

    protected Comparator<MpAdjustStructureIn> getSortComparator() {
        // 定义施工阶段自定义排序权重：正式(03) -> 试制(01) -> 量试(02) -> 无工艺(00)，空值排最后
        Map<String, Integer> stageSortWeights = new HashMap<>();
        // 正式：权重1
        stageSortWeights.put(ConstructionStageEnum.FORMAL_PRODUCTION.getStage(), 1);
        // 试制：权重2
        stageSortWeights.put(ConstructionStageEnum.MEASUREMENT.getStage(), 2);
        // 量试：权重3
        stageSortWeights.put(ConstructionStageEnum.TRIAL_PRODUCTION.getStage(), 3);
        // 无施工：权重4
        stageSortWeights.put(ConstructionStageEnum.NO_CONSTRUCTION.getStage(), 4);
        // 一级排序：结构名称升序，空值排最后
        return Comparator.comparing(MpAdjustStructureIn::getStructureName, Comparator.nullsLast(String::compareTo))
                // 二级排序：施工阶段按自定义权重升序（权重小排前）
                .thenComparing(vo -> stageSortWeights.getOrDefault(vo.getConstructionStage(), 5))
                // 三级排序：负数排前 -> 正数次之 -> 0（含null）最后，同组内绝对值从大到小
                // 负数排前，非负数整体在后
                .thenComparing(vo -> {
                    // null统一视为0
                    Integer qty = Optional.ofNullable(vo.getPendingQty()).orElse(0);
                    // 负数返回0，非负数返回1，升序实现负数排前
                    return qty < 0 ? 0 : 1;
                })
                // 非负数内部区分 正数排前，0最后
                .thenComparing(vo -> {
                    Integer qty = Optional.ofNullable(vo.getPendingQty()).orElse(0);
                    // 正数返回0，0返回1，升序实现正数排前、0最后
                    return qty > 0 ? 0 : 1;
                })
                // 同分组内（负数、正数、0）按绝对值降序（从大到小）
                .thenComparing(vo -> {
                    Integer qty = Optional.ofNullable(vo.getPendingQty()).orElse(0);
                    return Math.abs(qty);
                }, Comparator.reverseOrder());

    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.mpAdjustStructureIn.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpAdjustStructureIn billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mpAdjustStructureIn.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取调整-结构内调整记录详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MpAdjustStructureIn getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入调整-结构内调整记录数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mpAdjustStructureIn.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "调整-结构内调整记录", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpAdjustStructureIn queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MpAdjustStructureIn> listExportData(MpAdjustStructureIn obj) {
        QueryWrapper<MpAdjustStructureIn> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mpAdjustStructureInService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MpAdjustStructureIn> queryWrapper, MpAdjustStructureIn queryVO) {
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
        return "MP0802";
    }


    /**
     * 查询版本列表
     */
    @ApiOperation("查询版本列表")
    @PostMapping("/getVersionList")
    public TableDataInfo getVersionList(@RequestBody MpAdjustStructureIn queryVO) {
        this.startPage();
        List<MpAdjustStructureIn> list = entityMapper.getVersionList(queryVO);
        this.clearPage();
        return this.getDataTable(list);
    }


}
