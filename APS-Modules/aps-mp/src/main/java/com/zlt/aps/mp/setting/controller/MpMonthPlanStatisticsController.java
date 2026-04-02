package com.zlt.aps.mp.setting.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.common.core.constant.BusiConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MpMonthPlanStatisticsEntityMapper;
import com.zlt.aps.maindata.service.IMpMonthPlanStatisticsService;
import com.zlt.aps.mp.api.domain.capacity.MpDailyCapacityLimitVo;
import com.zlt.aps.mp.api.domain.entity.MpAdjustResult;
import com.zlt.aps.mp.api.domain.entity.MpMonthPlanStatistics;
import com.zlt.aps.mp.api.domain.vo.MpDayProductionStatisticsDetailVo;
import com.zlt.aps.mp.engine.constant.ProductionConstant;
import com.zlt.common.utils.PubUtil;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：MpMonthPlanStatisticsController.java
* 描    述：S2-0612.最终排产计划统计 控制层类：....
*@author zlt
*@date 2026-02-05
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "S2-0612.最终排产计划统计")
@RestController
@RequestMapping("/mpMonthPlanStatistics")
public class MpMonthPlanStatisticsController extends AbstractDocBizController<MpMonthPlanStatistics> {

    @Autowired
    private IMpMonthPlanStatisticsService mpMonthPlanStatisticsService;

    @Autowired
    private MpMonthPlanStatisticsEntityMapper entityMapper;

    /**
     * 查询S2-0612.最终排产计划统计列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MpMonthPlanStatistics queryVO) {
        queryVO.setTempFlag(null);
        TableDataInfo tableDataInfo = super.list(queryVO);
        filterByGroup(tableDataInfo, queryVO.getTempFlag());
        handleZeroToNull(tableDataInfo.getRows());
        return tableDataInfo;
    }



    /**
     * 按规则筛选数据：
     * 1. 按 structureName + structureType 分组
     * 2. 组内优先取 tempFlag=1 的数据
     * 3. 无1则取组内第一条数据
     *
     * @param tableDataInfo 原始数据集合
     * @param tempFlag 临时标识
     * @return
     */
    public void filterByGroup(TableDataInfo tableDataInfo, String tempFlag) {
        // 1. 空集合直接返回空列表，避免空指针
        if (PubUtil.isEmpty(tableDataInfo.getRows())) {
            return;
        }

        List<MpMonthPlanStatistics> sourceList = (List<MpMonthPlanStatistics>) tableDataInfo.getRows();
        String targetTempFlag = StringUtils.isEmpty(tempFlag) ? YesOrNoEnum.YES.getCode() : tempFlag;

        // 2. 双字段分组：key = structureName + structureType
        Map<AbstractMap.SimpleEntry<String, String>, List<MpMonthPlanStatistics>> groupMap = sourceList.stream()
                .collect(Collectors.groupingBy(
                        item -> new AbstractMap.SimpleEntry<>(
                                item.getStructureName(),
                                item.getStructureType()
                        )
                ));

        // 3. 遍历每组数据，执行优先级筛选
        List<MpMonthPlanStatistics> resultList = groupMap.values().stream()
                .map(groupData -> {
                    // 优先查找 tempFlag = 0 的数据
                    Optional<MpMonthPlanStatistics> flagZeroData = groupData.stream()
                            .filter(item -> targetTempFlag.equals(item.getTempFlag()))
                            .findFirst();
                    // 有0则取0，无0则取组内第一条数据
                    return flagZeroData.orElse(groupData.get(0));
                })
                .collect(Collectors.toList());
        tableDataInfo.setRows(resultList);
        tableDataInfo.setTotal(Convert.toLong(resultList.size()));
    }



    /**
     * 将字段中值为0的字段设为null
     * @param rows
     */
    private void handleZeroToNull(List<?> rows) {
        if (PubUtil.isEmpty(rows)) {
            return;
        }
        List<MpMonthPlanStatistics> monthPlanStatisticsList = (List<MpMonthPlanStatistics>) rows;
        for (MpMonthPlanStatistics statistics: monthPlanStatisticsList) {
            for (int day = ProductionConstant.MONTH_START_DAY; day <= ProductionConstant.MONTH_MAX_DAY; day++) {
                setDayField(statistics, day);
            }
        }
    }

    /**
     * 设置时间相关字段
     * @param statistics
     * @param day
     */
    private void setDayField(MpMonthPlanStatistics statistics, int day) {
        String dayVale = Convert.toStr(statistics.getFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + day), null);
        if (dayVale != null) {
            MpDayProductionStatisticsDetailVo dayProductionStatisticsDetailVo = JSONUtil.toBean(dayVale, MpDayProductionStatisticsDetailVo.class);
            if (dayProductionStatisticsDetailVo.getLhMachines() == null && dayProductionStatisticsDetailVo.getEmbryoCount() == null
                    && dayProductionStatisticsDetailVo.getChangeMould() == null) {
                return;
            }
            if (Convert.toInt(dayProductionStatisticsDetailVo.getLhMachines(), 0).equals(0)) {
                dayProductionStatisticsDetailVo.setLhMachines(null);
            }
            if (Convert.toInt(dayProductionStatisticsDetailVo.getEmbryoCount(), 0).equals(0)) {
                dayProductionStatisticsDetailVo.setEmbryoCount(null);
            }
            statistics.setFieldValueByFieldName(BusiConstant.WeekRollAdjust.FIELD_PREFIX_DAY + day, JSONObject.toJSONString(dayProductionStatisticsDetailVo));
        }
    }



    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mpMonthPlanStatistics.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MpMonthPlanStatistics billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mpMonthPlanStatistics.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取S2-0612.最终排产计划统计详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MpMonthPlanStatistics getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入S2-0612.最终排产计划统计数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mpMonthPlanStatistics.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "S2-0612.最终排产计划统计", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MpMonthPlanStatistics queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MpMonthPlanStatistics> listExportData(MpMonthPlanStatistics obj) {
        QueryWrapper<MpMonthPlanStatistics> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return mpMonthPlanStatisticsService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MpMonthPlanStatistics> queryWrapper, MpMonthPlanStatistics queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("yearMonth")), "`YEAR_MONTH`", queryVO.getFieldValueByFieldName("yearMonth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lastMonthPlanVersion")), "LAST_MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("lastMonthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", queryVO.getFieldValueByFieldName("productionVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tempFlag")), "TEMP_FLAG", queryVO.getFieldValueByFieldName("tempFlag"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureType")), "STRUCTURE_TYPE", queryVO.getFieldValueByFieldName("structureType"));
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
    }


    @Override
    protected String getTypeCode(){
        return "s2-0612";
    }


}
