package com.zlt.aps.monthplan.adjust.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.monthplan.adjust.mapper.MpAdjustResultEntityMapper;
import com.zlt.aps.monthplan.adjust.service.IMpAdjustResultService;
import com.zlt.aps.monthplan.api.domain.entity.MpAdjustResult;
import com.zlt.common.utils.PubUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;
import com.zlt.aps.monthplan.adjust.mapper.MpAdjustResultEntityMapper;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MpAdjustResultController.java
* 描    述：调整-调整结果记录 控制层类：....
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
@Api(tags = "调整-调整结果记录")
@RestController
@RequestMapping("/mpAdjustResult")
public class MpAdjustResultController extends AbstractDocBizController<MpAdjustResult> {

    @Autowired
    private IMpAdjustResultService mpAdjustResultService;

    @Autowired
    private MpAdjustResultEntityMapper entityMapper;

    /**
     * 查询调整-调整结果记录列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpAdjustResult queryVO) {
        TableDataInfo tableDataInfo = super.list(queryVO);
        sortlList(tableDataInfo.getRows());
        return tableDataInfo;
    }

    protected void sortlList(List<?> rows) {
        if (PubUtil.isEmpty(rows)) {
            return;
        }
        List<MpAdjustResult> mpAdjustResultList = (List<MpAdjustResult>) rows;
        Collections.sort(mpAdjustResultList, getSortComparator());
    }

    protected Comparator<MpAdjustResult> getSortComparator() {
        // 一级排序：结构名称升序，空值排最后
        return Comparator.comparing(MpAdjustResult::getStructureName, Comparator.nullsLast(String::compareTo))
                // 二级排序：负数排前 -> 正数次之 -> 0（含null）最后，同组内绝对值从大到小
                // 负数排前，非负数整体在后
                .thenComparing(vo -> {
                    // null统一视为0
                    Integer qty = Optional.ofNullable(vo.getTotalPlanQty()).orElse(0);
                    // 负数返回0，非负数返回1，升序实现负数排前
                    return qty < 0 ? 0 : 1;
                })
                // 非负数内部区分 正数排前，0最后
                .thenComparing(vo -> {
                    Integer qty = Optional.ofNullable(vo.getTotalPlanQty()).orElse(0);
                    // 正数返回0，0返回1，升序实现正数排前、0最后
                    return qty > 0 ? 0 : 1;
                })
                // 同分组内（负数、正数、0）按绝对值降序（从大到小）
                .thenComparing(vo -> {
                    Integer qty = Optional.ofNullable(vo.getTotalPlanQty()).orElse(0);
                    return Math.abs(qty);
                }, Comparator.reverseOrder());

    }




    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mpAdjustResult.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpAdjustResult billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mpAdjustResult.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取调整-调整结果记录详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MpAdjustResult getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入调整-调整结果记录数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mpAdjustResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "调整-调整结果记录", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpAdjustResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MpAdjustResult> listExportData(MpAdjustResult obj) {
        QueryWrapper<MpAdjustResult> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mpAdjustResultService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MpAdjustResult> queryWrapper, MpAdjustResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("adjustType")), "ADJUST_TYPE", queryVO.getFieldValueByFieldName("adjustType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cxMachineCode")), "CX_MACHINE_CODE", queryVO.getFieldValueByFieldName("cxMachineCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hasSpecialMaterial")), "HAS_SPECIAL_MATERIAL", queryVO.getFieldValueByFieldName("hasSpecialMaterial"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("totalPlanQty")), "TOTAL_PLAN_QTY", queryVO.getFieldValueByFieldName("totalPlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("beginDay")), "BEGIN_DAY", queryVO.getFieldValueByFieldName("beginDay"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("endDay")), "END_DAY", queryVO.getFieldValueByFieldName("endDay"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day1")), "DAY_1", queryVO.getFieldValueByFieldName("day1"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day2")), "DAY_2", queryVO.getFieldValueByFieldName("day2"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day3")), "DAY_3", queryVO.getFieldValueByFieldName("day3"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day4")), "DAY_4", queryVO.getFieldValueByFieldName("day4"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day5")), "DAY_5", queryVO.getFieldValueByFieldName("day5"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day6")), "DAY_6", queryVO.getFieldValueByFieldName("day6"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day7")), "DAY_7", queryVO.getFieldValueByFieldName("day7"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day8")), "DAY_8", queryVO.getFieldValueByFieldName("day8"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day9")), "DAY_9", queryVO.getFieldValueByFieldName("day9"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day10")), "DAY_10", queryVO.getFieldValueByFieldName("day10"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day11")), "DAY_11", queryVO.getFieldValueByFieldName("day11"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day12")), "DAY_12", queryVO.getFieldValueByFieldName("day12"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day13")), "DAY_13", queryVO.getFieldValueByFieldName("day13"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day14")), "DAY_14", queryVO.getFieldValueByFieldName("day14"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day15")), "DAY_15", queryVO.getFieldValueByFieldName("day15"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day16")), "DAY_16", queryVO.getFieldValueByFieldName("day16"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day17")), "DAY_17", queryVO.getFieldValueByFieldName("day17"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day18")), "DAY_18", queryVO.getFieldValueByFieldName("day18"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day19")), "DAY_19", queryVO.getFieldValueByFieldName("day19"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day20")), "DAY_20", queryVO.getFieldValueByFieldName("day20"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day21")), "DAY_21", queryVO.getFieldValueByFieldName("day21"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day22")), "DAY_22", queryVO.getFieldValueByFieldName("day22"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day23")), "DAY_23", queryVO.getFieldValueByFieldName("day23"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day24")), "DAY_24", queryVO.getFieldValueByFieldName("day24"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day25")), "DAY_25", queryVO.getFieldValueByFieldName("day25"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day26")), "DAY_26", queryVO.getFieldValueByFieldName("day26"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day27")), "DAY_27", queryVO.getFieldValueByFieldName("day27"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day28")), "DAY_28", queryVO.getFieldValueByFieldName("day28"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day29")), "DAY_29", queryVO.getFieldValueByFieldName("day29"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day30")), "DAY_30", queryVO.getFieldValueByFieldName("day30"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day31")), "DAY_31", queryVO.getFieldValueByFieldName("day31"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isLockSchedule")), "IS_LOCK_SCHEDULE", queryVO.getFieldValueByFieldName("isLockSchedule"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("version")), "VERSION", queryVO.getFieldValueByFieldName("version"));
    }


    @Override
    protected String getTypeCode(){
        return "MP0804";
    }

    /**
     * 查询版本列表
     */
    @ApiOperation("查询版本列表")
    @PostMapping("/getVersionList")
    public TableDataInfo getVersionList(@RequestBody MpAdjustResult queryVO) {
        this.startPage();
        List<MpAdjustResult> list = entityMapper.getVersionList(queryVO);
        this.clearPage();
        return this.getDataTable(list);
    }


}
