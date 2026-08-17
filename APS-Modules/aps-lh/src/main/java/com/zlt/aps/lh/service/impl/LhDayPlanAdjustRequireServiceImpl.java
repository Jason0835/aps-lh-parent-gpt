package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.google.common.collect.Lists;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.engine.domain.LhDayPlanAdjustVo;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.lh.api.domain.entity.LhDayPlanAdjustRequire;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.mapper.FactoryMonthPlanProductionFinalResultMapper;
import com.zlt.aps.lh.mapper.LhDayPlanAdjustRequireMapper;
import com.zlt.aps.lh.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.lh.service.ILhDayPlanAdjustRequireService;
import com.zlt.aps.maindata.mapper.MdmRawMaterialConversionEntityMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmRawMaterialConversion;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.utils.BeanCopyUtils;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化日计划调整需求服务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class LhDayPlanAdjustRequireServiceImpl extends AbstractDocService<LhDayPlanAdjustRequire>
        implements ILhDayPlanAdjustRequireService {

    private static final String FINAL_FLAG = "1";
    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int ADJUST_SLOT_COUNT = 3;
    private static final String ADJUST_ID_FIELD_TEMPLATE = "adjustId%d";
    private static final String ADJUST_QTY_FIELD_TEMPLATE = "adjustQty%d";
    private static final String ADJUST_REASON_FIELD_TEMPLATE = "adjustReason%d";

    @Resource
    private LhDayPlanAdjustRequireMapper lhDayPlanAdjustRequireMapper;

    @Resource
    private FactoryMonthPlanProductionFinalResultMapper monthPlanMapper;

    @Resource
    private MpFactoryProductionVersionMapper productionVersionMapper;

    @Resource
    private MdmRawMaterialConversionEntityMapper rawMaterialConversionMapper;

    /**
     * 查询月计划基础行，并批量回填调整明细和胎面胶 TD。
     *
     * @param queryVO 查询条件
     * @return 分页结果
     */
    @Override
    public TableDataInfo listPage(LhDayPlanAdjustRequire queryVO) {
        this.normalizeQuery(queryVO);
        String productionVersion = this.resolveFinalProductionVersion(queryVO);
        if (StringUtils.isBlank(productionVersion)) {
            return this.buildTableData(new ArrayList<LhDayPlanAdjustRequire>(0), 0);
        }

        List<FactoryMonthPlanProductionFinalResult> monthPlanList = this.queryMonthPlanList(queryVO, productionVersion);
        List<LhDayPlanAdjustRequire> allRows = this.aggregateMonthPlanRows(queryVO, productionVersion, monthPlanList);
        List<LhDayPlanAdjustRequire> pageRows = this.pageRows(allRows, queryVO.getPageNum(), queryVO.getPageSize());
        this.fillAdjustRows(pageRows);
        this.fillTreadGlueTd(pageRows, queryVO.getFactoryCode());
        pageRows.forEach(this::calculateAdjustedTotalQty);
        return this.buildTableData(pageRows, allRows.size());
    }

    /**
     * 保存当前列表行的三个调整槽位。
     *
     * @param entity 当前行数据
     */
    @Override
    public void saveRow(LhDayPlanAdjustRequire entity) {
        this.normalizeQuery(entity);
        this.validateSaveRequest(entity);
        String productionVersion = this.resolveFinalProductionVersion(entity);
        if (StringUtils.isBlank(productionVersion)) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.finalVersionMissing"));
        }

        FactoryMonthPlanProductionFinalResult sourcePlan = this.findSourceMonthPlan(entity, productionVersion);
        if (Objects.isNull(sourcePlan)) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.monthPlanMissing"));
        }

        for (int adjustIndex = 1; adjustIndex <= ADJUST_SLOT_COUNT; adjustIndex++) {
            BigDecimal adjustQty = (BigDecimal) entity.getFieldValueByFieldName(
                    String.format(ADJUST_QTY_FIELD_TEMPLATE, adjustIndex));
            String adjustReason = StringUtils.trim((String) entity.getFieldValueByFieldName(
                    String.format(ADJUST_REASON_FIELD_TEMPLATE, adjustIndex)));
            this.validateAdjustSlot(adjustIndex, adjustQty, adjustReason);
            this.saveAdjustSlot(entity, sourcePlan, adjustIndex, adjustQty, adjustReason);
        }
    }

    /**
     * 规范查询年月、工厂和分页参数。
     *
     * @param queryVO 查询条件
     */
    private void normalizeQuery(LhDayPlanAdjustRequire queryVO) {
        if (Objects.isNull(queryVO)) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.requestRequired"));
        }
        if (StringUtils.isBlank(queryVO.getFactoryCode())) {
            queryVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        queryVO.setFactoryCode(StringUtils.trim(queryVO.getFactoryCode()));
        queryVO.setMaterialCode(StringUtils.trim(queryVO.getMaterialCode()));
        queryVO.setMaterialDesc(StringUtils.trim(queryVO.getMaterialDesc()));
        queryVO.setProductStatus(StringUtils.trim(queryVO.getProductStatus()));
        if (Objects.isNull(queryVO.getYearMonth())) {
            Date currentDate = new Date();
            int currentYear = DateUtil.year(currentDate);
            int currentMonth = DateUtil.month(currentDate) + 1;
            queryVO.setYearMonth(currentYear * 100 + currentMonth);
        }
        int year = queryVO.getYearMonth() / 100;
        int month = queryVO.getYearMonth() % 100;
        if (year < 1000 || year > 9999 || month < 1 || month > 12) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.yearMonthInvalid"));
        }
        queryVO.setYear(year);
        queryVO.setMonth(month);
        if (Objects.isNull(queryVO.getPageNum()) || queryVO.getPageNum() < 1) {
            queryVO.setPageNum(DEFAULT_PAGE_NUM);
        }
        if (Objects.isNull(queryVO.getPageSize()) || queryVO.getPageSize() < 1) {
            queryVO.setPageSize(DEFAULT_PAGE_SIZE);
        }
    }

    /**
     * 获取当前年月最新定稿排产版本。
     *
     * @param queryVO 查询条件
     * @return 排产版本；不存在时返回空
     */
    private String resolveFinalProductionVersion(LhDayPlanAdjustRequire queryVO) {
        List<MpFactoryProductionVersion> versionList = productionVersionMapper.selectList(
                new LambdaQueryWrapper<MpFactoryProductionVersion>()
                        .eq(MpFactoryProductionVersion::getFactoryCode, queryVO.getFactoryCode())
                        .eq(MpFactoryProductionVersion::getYear, queryVO.getYear())
                        .eq(MpFactoryProductionVersion::getMonth, queryVO.getMonth())
                        .eq(MpFactoryProductionVersion::getIsFinal, FINAL_FLAG)
                        .orderByDesc(MpFactoryProductionVersion::getUpdateTime)
                        .orderByDesc(MpFactoryProductionVersion::getId)
                        .last("limit 1"));
        if (CollectionUtils.isEmpty(versionList)) {
            return null;
        }
        return StringUtils.trim(versionList.get(0).getProductionVersion());
    }

    /**
     * 查询有效排产版本的月计划明细。
     *
     * @param queryVO 查询条件
     * @param productionVersion 排产版本
     * @return 月计划明细
     */
    private List<FactoryMonthPlanProductionFinalResult> queryMonthPlanList(
            LhDayPlanAdjustRequire queryVO, String productionVersion) {
        return monthPlanMapper.selectList(new LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult>()
                .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, queryVO.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, queryVO.getYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, queryVO.getMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, productionVersion)
                .like(StringUtils.isNotBlank(queryVO.getMaterialCode()),
                        FactoryMonthPlanProductionFinalResult::getMaterialCode, queryVO.getMaterialCode())
                .like(StringUtils.isNotBlank(queryVO.getMaterialDesc()),
                        FactoryMonthPlanProductionFinalResult::getMaterialDesc, queryVO.getMaterialDesc())
                .eq(StringUtils.isNotBlank(queryVO.getProductStatus()),
                        FactoryMonthPlanProductionFinalResult::getProductStatus, queryVO.getProductStatus()));
    }

    /**
     * 按物料和产品状态归并月计划量。
     *
     * @param queryVO 查询条件
     * @param productionVersion 排产版本
     * @param monthPlanList 月计划明细
     * @return 归并列表
     */
    private List<LhDayPlanAdjustRequire> aggregateMonthPlanRows(
            LhDayPlanAdjustRequire queryVO,
            String productionVersion,
            List<FactoryMonthPlanProductionFinalResult> monthPlanList) {
        Map<String, LhDayPlanAdjustRequire> rowMap = new LinkedHashMap<String, LhDayPlanAdjustRequire>();
        if (CollectionUtils.isEmpty(monthPlanList)) {
            return new ArrayList<LhDayPlanAdjustRequire>(0);
        }
        monthPlanList.stream()
                .filter(Objects::nonNull)
                .filter(plan -> StringUtils.isNotBlank(plan.getMaterialCode()))
                .forEach(plan -> {
                    String materialCode = StringUtils.trim(plan.getMaterialCode());
                    String productStatus = StringUtils.trim(plan.getProductStatus());
                    String rowKey = this.buildMaterialStatusKey(materialCode, productStatus);
                    LhDayPlanAdjustRequire row = rowMap.computeIfAbsent(rowKey,
                            key -> this.createMonthPlanRow(queryVO, productionVersion, plan));
                    row.setMonthPlanQty(BigDecimalUtils.add(
                            row.getMonthPlanQty(), BigDecimalUtils.valueOf(plan.getTotalQty())));
                    if (StringUtils.isBlank(row.getMaterialDesc()) && StringUtils.isNotBlank(plan.getMaterialDesc())) {
                        row.setMaterialDesc(StringUtils.trim(plan.getMaterialDesc()));
                    }
                    if (StringUtils.isBlank(row.getMesMaterialCode())
                            && StringUtils.isNotBlank(plan.getMesMaterialCode())) {
                        row.setMesMaterialCode(StringUtils.trim(plan.getMesMaterialCode()));
                    }
                    if (Objects.nonNull(plan.getDisplaySeq())
                            && (Objects.isNull(row.getDisplaySeq()) || plan.getDisplaySeq() < row.getDisplaySeq())) {
                        row.setDisplaySeq(plan.getDisplaySeq());
                    }
                });
        return rowMap.values().stream()
                .sorted(Comparator.comparing(LhDayPlanAdjustRequire::getDisplaySeq,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LhDayPlanAdjustRequire::getMaterialCode,
                                Comparator.nullsLast(String::compareTo))
                        .thenComparing(LhDayPlanAdjustRequire::getProductStatus,
                                Comparator.nullsLast(String::compareTo)))
                .collect(Collectors.toList());
    }

    /**
     * 创建月计划列表基础行。
     *
     * @param queryVO 查询条件
     * @param productionVersion 排产版本
     * @param plan 月计划明细
     * @return 基础行
     */
    private LhDayPlanAdjustRequire createMonthPlanRow(
            LhDayPlanAdjustRequire queryVO,
            String productionVersion,
            FactoryMonthPlanProductionFinalResult plan) {
        LhDayPlanAdjustRequire row = new LhDayPlanAdjustRequire();
        row.setFactoryCode(queryVO.getFactoryCode());
        row.setYear(queryVO.getYear());
        row.setMonth(queryVO.getMonth());
        row.setYearMonth(queryVO.getYearMonth());
        row.setProductionVersion(productionVersion);
        row.setMaterialCode(StringUtils.trim(plan.getMaterialCode()));
        row.setMaterialDesc(StringUtils.trim(plan.getMaterialDesc()));
        row.setMesMaterialCode(StringUtils.trim(plan.getMesMaterialCode()));
        row.setProductStatus(StringUtils.trim(plan.getProductStatus()));
        row.setDisplaySeq(plan.getDisplaySeq());
        row.setMonthPlanQty(BigDecimal.ZERO);
        return row;
    }

    /**
     * 对归并后的列表执行内存分页。
     *
     * @param allRows 全部归并行
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @return 当前页
     */
    private List<LhDayPlanAdjustRequire> pageRows(
            List<LhDayPlanAdjustRequire> allRows, int pageNum, int pageSize) {
        int fromIndex = Math.min((pageNum - 1) * pageSize, allRows.size());
        int toIndex = Math.min(fromIndex + pageSize, allRows.size());
        return new ArrayList<LhDayPlanAdjustRequire>(allRows.subList(fromIndex, toIndex));
    }

    /**
     * 批量回填当前页三次调整。
     *
     * @param pageRows 当前页月计划行
     */
    private void fillAdjustRows(List<LhDayPlanAdjustRequire> pageRows) {
        if (CollectionUtils.isEmpty(pageRows)) {
            return;
        }
        LhDayPlanAdjustRequire firstRow = pageRows.get(0);
        Set<String> materialCodes = pageRows.stream()
                .map(LhDayPlanAdjustRequire::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        List<LhDayPlanAdjustRequire> adjustList = lhDayPlanAdjustRequireMapper.selectList(
                new LambdaQueryWrapper<LhDayPlanAdjustRequire>()
                        .eq(LhDayPlanAdjustRequire::getFactoryCode, firstRow.getFactoryCode())
                        .eq(LhDayPlanAdjustRequire::getYearMonth, firstRow.getYearMonth())
                        .in(LhDayPlanAdjustRequire::getMaterialCode, materialCodes)
                        .orderByDesc(LhDayPlanAdjustRequire::getUpdateTime)
                        .orderByDesc(LhDayPlanAdjustRequire::getId));
        Map<String, LhDayPlanAdjustRequire> rowMap = pageRows.stream()
                .collect(Collectors.toMap(
                        row -> this.buildMaterialStatusKey(row.getMaterialCode(), row.getProductStatus()),
                        row -> row,
                        (first, second) -> first,
                        LinkedHashMap::new));
        Set<String> filledSlotSet = new LinkedHashSet<String>();
        for (LhDayPlanAdjustRequire adjust : adjustList) {
            if (Objects.isNull(adjust.getAdjustCount())
                    || adjust.getAdjustCount() < 1
                    || adjust.getAdjustCount() > ADJUST_SLOT_COUNT) {
                continue;
            }
            LhDayPlanAdjustRequire row = rowMap.get(
                    this.buildMaterialStatusKey(adjust.getMaterialCode(), adjust.getProductStatus()));
            if (Objects.isNull(row)) {
                continue;
            }
            String slotKey = this.buildAdjustSlotKey(row, adjust.getAdjustCount());
            if (!filledSlotSet.add(slotKey)) {
                continue;
            }
            row.setFieldValueByFieldName(String.format(ADJUST_ID_FIELD_TEMPLATE, adjust.getAdjustCount()),
                    adjust.getId());
            row.setFieldValueByFieldName(String.format(ADJUST_QTY_FIELD_TEMPLATE, adjust.getAdjustCount()),
                    adjust.getPlanQty());
            row.setFieldValueByFieldName(String.format(ADJUST_REASON_FIELD_TEMPLATE, adjust.getAdjustCount()),
                    adjust.getReason());
        }
    }

    /**
     * 批量查询并回填胎面胶 TD。
     *
     * @param pageRows    当前页月计划行
     * @param factoryCode 工厂编码
     */
    private void fillTreadGlueTd(List<LhDayPlanAdjustRequire> pageRows, String factoryCode) {
        if (CollectionUtils.isEmpty(pageRows)) {
            return;
        }
        Set<String> materialCodes = pageRows.stream()
                .map(LhDayPlanAdjustRequire::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        Set<String> productStatuses = pageRows.stream()
                .map(LhDayPlanAdjustRequire::getProductStatus)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        if (materialCodes.isEmpty() || productStatuses.isEmpty()) {
            return;
        }
        List<MdmRawMaterialConversion> conversionList = rawMaterialConversionMapper.selectList(
                new LambdaQueryWrapper<MdmRawMaterialConversion>()
                        .eq(StringUtils.isNotBlank(factoryCode),
                                MdmRawMaterialConversion::getFactoryCode, factoryCode)
                        .in(MdmRawMaterialConversion::getMaterialCode, materialCodes)
                        .in(MdmRawMaterialConversion::getConstructionStage, productStatuses)
                        .likeRight(MdmRawMaterialConversion::getRawMaterialName, "AQT"));
        Map<String, String> treadGlueMap = conversionList.stream()
                .filter(conversion -> StringUtils.isNotBlank(conversion.getMaterialCode()))
                .filter(conversion -> StringUtils.isNotBlank(conversion.getConstructionStage()))
                .filter(conversion -> StringUtils.isNotBlank(conversion.getRawMaterialName()))
                .collect(Collectors.groupingBy(
                        conversion -> this.buildMaterialStatusKey(
                                conversion.getMaterialCode(), conversion.getConstructionStage()),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                conversion -> this.removeAqPrefix(conversion.getRawMaterialName()),
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(LinkedHashSet::new),
                                        names -> String.join(",", names)))));
        pageRows.forEach(row -> row.setTreadGlueTd(treadGlueMap.get(
                this.buildMaterialStatusKey(row.getMaterialCode(), row.getProductStatus()))));
    }

    /**
     * 计算调整后合计。
     *
     * @param row 当前列表行
     */
    private void calculateAdjustedTotalQty(LhDayPlanAdjustRequire row) {
        BigDecimal adjustedTotalQty = BigDecimalUtils.valueOf(row.getMonthPlanQty());
        for (int adjustIndex = 1; adjustIndex <= ADJUST_SLOT_COUNT; adjustIndex++) {
            BigDecimal adjustQty = (BigDecimal) row.getFieldValueByFieldName(
                    String.format(ADJUST_QTY_FIELD_TEMPLATE, adjustIndex));
            adjustedTotalQty = BigDecimalUtils.add(adjustedTotalQty, adjustQty);
        }
        row.setAdjustedTotalQty(adjustedTotalQty);
    }

    /**
     * 校验保存主键字段。
     *
     * @param entity 当前行数据
     */
    private void validateSaveRequest(LhDayPlanAdjustRequire entity) {
        if (StringUtils.isBlank(entity.getFactoryCode())) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.factoryRequired"));
        }
        if (StringUtils.isBlank(entity.getMaterialCode())) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.materialCodeRequired"));
        }
        if (StringUtils.isBlank(entity.getProductStatus())) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.productStatusRequired"));
        }
    }

    /**
     * 校验单个调整槽位的数量和原因必须同时填写或同时清空。
     *
     * @param adjustIndex 调整序号
     * @param adjustQty 调整量
     * @param adjustReason 调整原因
     */
    private void validateAdjustSlot(int adjustIndex, BigDecimal adjustQty, String adjustReason) {
        if (Objects.isNull(adjustQty) && StringUtils.isNotBlank(adjustReason)) {
            throw new IllegalArgumentException(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.adjustQtyRequired"), adjustIndex));
        }
        if (Objects.nonNull(adjustQty) && StringUtils.isBlank(adjustReason)) {
            throw new IllegalArgumentException(MessageFormat.format(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.adjustReasonRequired"), adjustIndex));
        }
    }

    /**
     * 查询保存行对应的有效月计划。
     *
     * @param entity            当前行数据
     * @param productionVersion 排产版本
     * @return 月计划来源快照
     */
    private FactoryMonthPlanProductionFinalResult findSourceMonthPlan(
            LhDayPlanAdjustRequire entity, String productionVersion) {
        List<FactoryMonthPlanProductionFinalResult> planList = monthPlanMapper.selectList(
                new LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult>()
                        .eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, entity.getFactoryCode())
                        .eq(FactoryMonthPlanProductionFinalResult::getYear, entity.getYear())
                        .eq(FactoryMonthPlanProductionFinalResult::getMonth, entity.getMonth())
                        .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, productionVersion)
                        .eq(FactoryMonthPlanProductionFinalResult::getMaterialCode, entity.getMaterialCode())
                        .eq(FactoryMonthPlanProductionFinalResult::getProductStatus, entity.getProductStatus())
                        .orderByAsc(FactoryMonthPlanProductionFinalResult::getDisplaySeq)
                        .orderByAsc(FactoryMonthPlanProductionFinalResult::getId));
        if (CollectionUtils.isEmpty(planList)) {
            return null;
        }
        FactoryMonthPlanProductionFinalResult sourcePlan = planList.get(0);
        sourcePlan.setMaterialDesc(planList.stream()
                .map(FactoryMonthPlanProductionFinalResult::getMaterialDesc)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .findFirst()
                .orElse(null));
        sourcePlan.setMesMaterialCode(planList.stream()
                .map(FactoryMonthPlanProductionFinalResult::getMesMaterialCode)
                .filter(StringUtils::isNotBlank)
                .map(StringUtils::trim)
                .findFirst()
                .orElse(null));
        return sourcePlan;
    }

    /**
     * 新增、更新或清空一个调整槽位。
     *
     * @param request      当前行请求
     * @param sourcePlan   月计划来源行
     * @param adjustIndex  调整序号
     * @param adjustQty    调整量
     * @param adjustReason 调整原因
     */
    private void saveAdjustSlot(
            LhDayPlanAdjustRequire request,
            FactoryMonthPlanProductionFinalResult sourcePlan,
            int adjustIndex,
            BigDecimal adjustQty,
            String adjustReason) {
        List<LhDayPlanAdjustRequire> existingList = lhDayPlanAdjustRequireMapper.selectList(
                new LambdaQueryWrapper<LhDayPlanAdjustRequire>()
                        .eq(LhDayPlanAdjustRequire::getFactoryCode, request.getFactoryCode())
                        .eq(LhDayPlanAdjustRequire::getYearMonth, request.getYearMonth())
                        .eq(LhDayPlanAdjustRequire::getMaterialCode, request.getMaterialCode())
                        .eq(LhDayPlanAdjustRequire::getProductStatus, request.getProductStatus())
                        .eq(LhDayPlanAdjustRequire::getAdjustCount, adjustIndex)
                        .orderByDesc(LhDayPlanAdjustRequire::getUpdateTime)
                        .orderByDesc(LhDayPlanAdjustRequire::getId));
        if (existingList.size() > 1) {
            throw new IllegalArgumentException(I18nUtil.getMessage(
                    "ui.data.alert.lhDayPlanAdjustRequire.notUnique"));
        }
        LhDayPlanAdjustRequire existing = existingList.isEmpty() ? null : existingList.get(0);
        if (Objects.isNull(adjustQty) && StringUtils.isBlank(adjustReason)) {
            if (Objects.nonNull(existing)) {
                lhDayPlanAdjustRequireMapper.deleteById(existing.getId());
            }
            return;
        }

        String currentUser = StringUtils.defaultIfBlank(SecurityUtils.getUsername(), "system");
        Date currentTime = new Date();
        LhDayPlanAdjustRequire target = Objects.isNull(existing)
                ? new LhDayPlanAdjustRequire() : existing;
        target.setFactoryCode(request.getFactoryCode());
        target.setYear(request.getYear());
        target.setMonth(request.getMonth());
        target.setYearMonth(request.getYearMonth());
        target.setProductStatus(request.getProductStatus());
        target.setMaterialCode(request.getMaterialCode());
        target.setMaterialDesc(StringUtils.trim(sourcePlan.getMaterialDesc()));
        target.setMesMaterialCode(StringUtils.trim(sourcePlan.getMesMaterialCode()));
        target.setAdjustCount(adjustIndex);
        target.setPlanQty(adjustQty);
        target.setReason(adjustReason);
        target.setAdjuster(currentUser);
        target.setUpdateBy(currentUser);
        target.setUpdateTime(currentTime);
        if (Objects.isNull(existing)) {
            target.setCreateBy(currentUser);
            target.setCreateTime(currentTime);
            target.setIsDelete(0);
            lhDayPlanAdjustRequireMapper.insert(target);
            return;
        }
        lhDayPlanAdjustRequireMapper.updateById(target);
    }

    /**
     * 生成物料和产品状态组合键。
     *
     * @param materialCode  物料编码
     * @param productStatus 产品状态/示方类型
     * @return 组合键
     */
    private String buildMaterialStatusKey(String materialCode, String productStatus) {
        return StringUtils.defaultString(materialCode).trim() + "|"
                + StringUtils.defaultString(productStatus).trim();
    }

    /**
     * 生成已回填调整槽位键。
     *
     * @param row         月计划行
     * @param adjustCount 调整序号
     * @return 槽位键
     */
    private String buildAdjustSlotKey(LhDayPlanAdjustRequire row, int adjustCount) {
        return this.buildMaterialStatusKey(row.getMaterialCode(), row.getProductStatus()) + "|" + adjustCount;
    }

    /**
     * 去掉胶名 AQ 前缀。
     *
     * @param rawMaterialName 原材料名称
     * @return 页面展示胶名
     */
    private String removeAqPrefix(String rawMaterialName) {
        String materialName = StringUtils.trim(rawMaterialName);
        return materialName.startsWith("AQ") ? materialName.substring(2) : materialName;
    }

    /**
     * 构建统一分页响应。
     *
     * @param rows  当前页数据
     * @param total 总数
     * @return 分页响应
     */
    private TableDataInfo buildTableData(List<LhDayPlanAdjustRequire> rows, long total) {
        TableDataInfo tableDataInfo = new TableDataInfo();
        tableDataInfo.setCode(HttpStatus.SUCCESS);
        tableDataInfo.setRows(rows);
        tableDataInfo.setTotal(total);
        tableDataInfo.setMsg(I18nUtil.getMessage("common.msg.base.query.success"));
        return tableDataInfo;
    }

    @Override
    public String[] getQueryFormulas() {
        return new String[0];
    }

    @Override
    public List<LhDayPlanAdjustVo> getMonthPlanLhDayAdjustList(YearMonth yearMonth, List<String> factoryList, List<String> materialCodeList) {
        if (null == yearMonth || CollectionUtils.isEmpty(factoryList) || CollectionUtils.isEmpty(materialCodeList)) {
            return Collections.emptyList();
        }
        List<LhDayPlanAdjustRequire> monthLhDayPlanAdjustRequireList = Lists.newArrayList();
        int year = yearMonth.getYear();
        int month = yearMonth.getMonthValue();
        for (String factoryCode : factoryList) {
            MpFactoryProductionVersion version = getFinalProductionVersion(factoryCode, year, month);
            List<LhDayPlanAdjustRequire> singleFactoryList = getProductionVersionYearMonthLhDayPlanAdjustInfo(version, materialCodeList);
            if (CollectionUtils.isNotEmpty(singleFactoryList)) {
                monthLhDayPlanAdjustRequireList.addAll(singleFactoryList);
            }
        }
        if (CollectionUtils.isEmpty(monthLhDayPlanAdjustRequireList)) {
            return Collections.emptyList();
        }
        List<LhDayPlanAdjustVo> resultList = BeanCopyUtils.copyBeanList(monthLhDayPlanAdjustRequireList, LhDayPlanAdjustVo.class);
        return resultList;
    }

    @Override
    protected String getDocTypeCode() {
        return "0";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("0");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "yearMonth", "materialCode", "productStatus", "adjustCount");
    }

    /**
     * 获取定稿排产版本
     *
     * @param factoryCode 分厂编码
     * @param year        年份
     * @param month       月份
     * @return 定稿排产版本，不存在返回null
     */
    private MpFactoryProductionVersion getFinalProductionVersion(String factoryCode, int year, int month) {
        if (StringUtils.isBlank(factoryCode)) {
            return null;
        }
        LambdaQueryWrapper<MpFactoryProductionVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpFactoryProductionVersion::getFactoryCode, factoryCode)
                .eq(MpFactoryProductionVersion::getYear, year)
                .eq(MpFactoryProductionVersion::getMonth, month)
                .eq(MpFactoryProductionVersion::getIsFinal, "1")
                .eq(MpFactoryProductionVersion::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                .orderByDesc(MpFactoryProductionVersion::getUpdateTime)
                .orderByDesc(MpFactoryProductionVersion::getId)
                .last("LIMIT 1");
        return productionVersionMapper.selectOne(wrapper);
    }

    /**
     * 根据排产版本号，获取对应年月的硫化日计划调整信息
     *
     * @param version          排产版本信息
     * @param materialCodeList 需要查询的Sku信息
     * @return
     */
    private List<LhDayPlanAdjustRequire> getProductionVersionYearMonthLhDayPlanAdjustInfo(MpFactoryProductionVersion version, List<String> materialCodeList) {
        if (null == version || CollectionUtils.isEmpty(materialCodeList)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<LhDayPlanAdjustRequire> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhDayPlanAdjustRequire::getFactoryCode, version.getFactoryCode())
                .eq(LhDayPlanAdjustRequire::getYear, version.getYear())
                .eq(LhDayPlanAdjustRequire::getMonth, version.getMonth())
                .in(LhDayPlanAdjustRequire::getMaterialCode, materialCodeList)
                .eq(LhDayPlanAdjustRequire::getIsDelete, DeleteFlagEnum.NORMAL.getCode());
        List<LhDayPlanAdjustRequire> dataResult = lhDayPlanAdjustRequireMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(dataResult)) {
            return Collections.emptyList();
        }
        return dataResult;
    }
}
