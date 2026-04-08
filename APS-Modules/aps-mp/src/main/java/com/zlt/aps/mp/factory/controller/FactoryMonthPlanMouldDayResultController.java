package com.zlt.aps.mp.factory.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.utils.ApsNumberUtils;
import com.zlt.aps.mp.api.domain.entity.DpDemandPlan;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.demand.mapper.DpDemandPlanEntityMapper;
import com.zlt.aps.mp.factory.dto.FactoryMonthPlanMouldDayResultExportVo;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanMouldDayResultEntityMapper;
import com.zlt.aps.mp.factory.service.IFactoryMonthPlanMouldDayResultService;
import com.zlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：FactoryMonthPlanMouldDayResultController.java
* 描    述：S2-0604.排产结果-生产计划排产结果 控制层类：....
*@author zlt
*@date 2025-12-31
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "S2-0604.排产结果-生产计划排产结果")
@RestController
@RequestMapping("/factoryMonthPlanMouldDayResult")
public class FactoryMonthPlanMouldDayResultController extends AbstractDocBizController<FactoryMonthPlanMouldDayResult> {

    @Autowired
    private IFactoryMonthPlanMouldDayResultService factoryMonthPlanMouldDayResultService;

    @Autowired
    private DpDemandPlanEntityMapper dpDemandPlanEntityMapper;

    @Autowired
    private FactoryMonthPlanMouldDayResultEntityMapper entityMapper;

    @Autowired
    private IExportLogService iExportLogService;

    /**
     * 查询S2-0604.排产结果-生产计划排产结果列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody FactoryMonthPlanMouldDayResult queryVO) {
        TableDataInfo tableDataInfo = super.list(queryVO);
        calculateDayVulcanizationQty(tableDataInfo.getRows());
        // 禅道：21394，从需求计划重算净需求，未排查量
        this.calculateDemandQty(tableDataInfo, queryVO);
        return tableDataInfo;
    }

    /**
     * 从需求计划重算净需求，未排查量
     * @param tableDataInfo
     */
    private void calculateDemandQty(TableDataInfo tableDataInfo, FactoryMonthPlanMouldDayResult queryVO) {
        List<?> rows = tableDataInfo.getRows();
        LambdaQueryWrapper<DpDemandPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DpDemandPlan::getMonthPlanVersion, queryVO.getMonthPlanVersion());
        Map<String, Integer> demandPlanMap = dpDemandPlanEntityMapper.selectList(queryWrapper).stream()
                .collect(Collectors.toMap(DpDemandPlan::getMaterialCode, DpDemandPlan::getNetQty,
                        (q1, q2) -> ApsNumberUtils.safeAdd(q1, q2)));
        for (Object item: rows) {
            FactoryMonthPlanMouldDayResult result = (FactoryMonthPlanMouldDayResult)item;
            String materialCode = result.getMaterialCode();
            Integer netQty = demandPlanMap.getOrDefault(materialCode, 0);
            Integer totalQty = ApsNumberUtils.intValue(result.getTotalQty());
            Integer diffQty = netQty > totalQty? netQty - totalQty: 0;
            result.setFactProdReqQty(netQty);
            result.setDifferenceQty(diffQty);
        }
    }

    /**
     * 计算机台日硫化量
     */
    private void calculateDayVulcanizationQty(List<?> sourceList) {
        if (PubUtil.isEmpty(sourceList)) {
            return;
        }
        List<FactoryMonthPlanMouldDayResult> list = (List<FactoryMonthPlanMouldDayResult>) sourceList;
        // 计算机台日硫化量 = 日硫化量（单模）* 2
        list.stream()
                .forEach(vo -> {
                    Integer dayVulcanizationQty = Convert.toInt(vo.getDayVulcanizationQty(),0);
                    vo.setDayVulcanizationQty(dayVulcanizationQty * 2);
                });
    }

    @Override
    protected String getOrderBy() {
        return "PRO_SIZE,STRUCTURE_NAME,MOULD_CAVITY_QTY DESC,TYPE_BLOCK_QTY DESC,MAIN_PATTERN";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.factoryMonthPlanMouldDayResult.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody FactoryMonthPlanMouldDayResult billVO){
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.factoryMonthPlanMouldDayResult.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }


    /**
     * 获取S2-0604.排产结果-生产计划排产结果详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public FactoryMonthPlanMouldDayResult getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入S2-0604.排产结果-生产计划排产结果数据
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.factoryMonthPlanMouldDayResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "S2-0604.排产结果-生产计划排产结果", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody FactoryMonthPlanMouldDayResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<FactoryMonthPlanMouldDayResultExportVo> list = factoryMonthPlanMouldDayResultService.getExportList(queryVO, false);
        byte[] resultBytes = factoryMonthPlanMouldDayResultService.getFactoryMonthPlanMouldDayResultExportByte(queryVO, list);
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
        return resultBytes;
    }

    /**
     * 全物料导出
     */
    @Log(title = "S2-0604.排产结果-全物料导出", businessType = BusinessType.EXPORT)
    @ApiOperation("全物料导出")
    @PostMapping("/exportAllMaterial/{fileName}")
    public byte[] exportAllMaterial(@RequestBody FactoryMonthPlanMouldDayResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        Date beginTime = DateUtils.getNowDate();
        List<FactoryMonthPlanMouldDayResultExportVo> list = factoryMonthPlanMouldDayResultService.getExportList(queryVO, true);
        byte[] resultBytes = factoryMonthPlanMouldDayResultService.getFactoryMonthPlanMouldDayResultExportByte(queryVO, list);
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
        return resultBytes;
    }

    @Override
    protected List<FactoryMonthPlanMouldDayResult> listExportData(FactoryMonthPlanMouldDayResult obj) {
        QueryWrapper<FactoryMonthPlanMouldDayResult> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.orderByAsc("STRUCTURE_NAME", "MAIN_PATTERN","MAIN_MATERIAL_DESC");
        List<FactoryMonthPlanMouldDayResult> list = entityMapper.selectList(wrapper);
        calculateDayVulcanizationQty(list);
        this.translationList(list);
        return list;
    }

    private void translationList(List<FactoryMonthPlanMouldDayResult> list) {
        Locale locale = I18nUtil.getLocaleFromRedis();
        list.forEach(mouldDayResult -> {
            String reason = mouldDayResult.getReason();
            if (StringUtils.isNotBlank(reason)) {
                if (reason.contains("|")) {
                    String[] split = reason.split("\\|");
                    List<String> reasonList = new ArrayList<>(split.length);
                    for (String reasonI18n : split) {
                        String convertValue = JsonI18nConvertUtils.getConvertValue(reasonI18n, locale);
                        reasonList.add(convertValue);
                    }
                    mouldDayResult.setReason(String.join(",", reasonList));
                } else {
                    String convertValue = JsonI18nConvertUtils.getConvertValue(reason, locale);
                    mouldDayResult.setReason(convertValue);
                }
            }
        });
    }

    @Override
    protected IDocService getDocService(){
        return factoryMonthPlanMouldDayResultService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<FactoryMonthPlanMouldDayResult> queryWrapper, FactoryMonthPlanMouldDayResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("yearMonth")), "YEAR_MONTH", queryVO.getFieldValueByFieldName("yearMonth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", queryVO.getFieldValueByFieldName("productionVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planType")), "PLAN_TYPE", queryVO.getFieldValueByFieldName("planType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productStatus")), "PRODUCT_STATUS", queryVO.getFieldValueByFieldName("productStatus"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureName")), "STRUCTURE_NAME", queryVO.getFieldValueByFieldName("structureName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainMaterialDesc")), "MAIN_MATERIAL_DESC", queryVO.getFieldValueByFieldName("mainMaterialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dynamicBalanceQty")), "DYNAMIC_BALANCE_QTY", queryVO.getFieldValueByFieldName("dynamicBalanceQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("uniformityQty")), "UNIFORMITY_QTY", queryVO.getFieldValueByFieldName("uniformityQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("constructionStage")), "CONSTRUCTION_STAGE", queryVO.getFieldValueByFieldName("constructionStage"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mainPattern")), "MAIN_PATTERN", queryVO.getFieldValueByFieldName("mainPattern"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldCavityQty")), "MOULD_CAVITY_QTY", queryVO.getFieldValueByFieldName("mouldCavityQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("typeBlockQty")), "TYPE_BLOCK_QTY", queryVO.getFieldValueByFieldName("typeBlockQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("heightQty")), "HEIGHT_QTY", queryVO.getFieldValueByFieldName("heightQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("averageSaleQty")), "AVERAGE_SALE_QTY", queryVO.getFieldValueByFieldName("averageSaleQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("inventorySalesRatio")), "INVENTORY_SALES_RATIO", queryVO.getFieldValueByFieldName("inventorySalesRatio"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dayVulcanizationQty")), "DAY_VULCANIZATION_QTY", queryVO.getFieldValueByFieldName("dayVulcanizationQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cxMachineCode")), "CX_MACHINE_CODE", queryVO.getFieldValueByFieldName("cxMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldChangeInfo")), "MOULD_CHANGE_INFO", queryVO.getFieldValueByFieldName("mouldChangeInfo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isImport")), "IS_IMPORT", queryVO.getFieldValueByFieldName("isImport"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionSequence")), "PRODUCTION_SEQUENCE", queryVO.getFieldValueByFieldName("productionSequence"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("curingTime")), "CURING_TIME", queryVO.getFieldValueByFieldName("curingTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("prodReqPlan")), "PROD_REQ_PLAN", queryVO.getFieldValueByFieldName("prodReqPlan"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factProdReqQty")), "FACT_PROD_REQ_QTY", queryVO.getFieldValueByFieldName("factProdReqQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("totalQty")), "TOTAL_QTY", queryVO.getFieldValueByFieldName("totalQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("heightProductionQty")), "HEIGHT_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("heightProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("midProductionQty")), "MID_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("midProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cycleProductionQty")), "CYCLE_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("cycleProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("conventionProductionQty")), "CONVENTION_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("conventionProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("postponeProductionQty")), "POSTPONE_PRODUCTION_QTY", queryVO.getFieldValueByFieldName("postponeProductionQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("differenceQty")), "DIFFERENCE_QTY", queryVO.getFieldValueByFieldName("differenceQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("reason")), "REASON", queryVO.getFieldValueByFieldName("reason"));
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
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("totalVulcanizationMinutes")), "TOTAL_VULCANIZATION_MINUTES", queryVO.getFieldValueByFieldName("totalVulcanizationMinutes"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("displaySeq")), "DISPLAY_SEQ", queryVO.getFieldValueByFieldName("displaySeq"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCategory")), "PRODUCT_CATEGORY", queryVO.getFieldValueByFieldName("productCategory"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("remark")), "REMARK", queryVO.getFieldValueByFieldName("remark"));
    }


    @Override
    protected String getTypeCode(){
        return "11";
    }


}
