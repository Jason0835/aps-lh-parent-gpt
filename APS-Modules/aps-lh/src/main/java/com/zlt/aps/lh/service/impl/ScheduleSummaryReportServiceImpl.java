package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.core.domain.ExcelCellRangeAddress;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanPlan;
import com.zlt.aps.lh.api.domain.entity.LhShiftConfig;
import com.zlt.aps.lh.api.domain.vo.ScheduleSummaryReportVO;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.lh.mapper.CxLhScheduleResultMapper;
import com.zlt.aps.lh.mapper.CxParamConfigMapper;
import com.zlt.aps.lh.mapper.CxScheduleResultMapper;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhMouldCleanPlanMapper;
import com.zlt.aps.lh.mapper.LhShiftConfigMapper;
import com.zlt.aps.lh.mapper.MdmMaterialInfoMapper;
import com.zlt.aps.lh.service.IScheduleSummaryReportService;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.maindata.mapper.MdmMaterialConsumeDetailMapper;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.service.IMpStructureAllocationRemoteService;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 排产小结报表服务实现
 *
 * <p>基于Excel模板生成排产小结报表，使用 {@link ExcelUtils#writeMultiList} 填充占位符。</p>
 *
 * <p>模板占位符说明：</p>
 * <ul>
 *   <li>普通占位符: {keyName} - 固定位置值替换</li>
 *   <li>列表占位符: {.keyName} - 列表数据循环行（小胶种）</li>
 * </ul>
 *
 * <p>普通占位符清单：</p>
 * <ul>
 *   <li>{titleDate} - 标题日期（中文+越南语两行）</li>
 *   <li>{cxNightQty}/{cxMorningQty}/{cxMiddleQty}/{cxTotalQty} - 成型各班产量</li>
 *   <li>{cxSetupInfo} - 试制规格（成型原因分析字段匹配"试制" + 硫化施工阶段=01）</li>
 *   <li>{cxTrialInfo} - 量试规格（成型原因分析字段匹配"量试" + 硫化施工阶段=02）</li>
 *   <li>{cxSpecSwitch} - 成型规格切换</li>
 *   <li>{lhNightQty}/{lhMorningQty}/{lhMiddleQty}/{lhTotalQty} - 硫化各班产量</li>
 *   <li>{lhNightMachines}/{lhMorningMachines}/{lhMiddleMachines}/{lhTotalMachines} - 硫化各班开动机台数</li>
 *   <li>{mouldCleanDate} - 模具清洗日期（查询排程日期前一天，如"14日"）</li>
 *   <li>{mouldChangeInfo} - 模具交替机台信息（去重；隔开，上限15台）</li>
 *   <li>{mouldCleanInfo} - 模具清洗机台信息（去重；隔开）</li>
 *   <li>{cxRemark} - 成型备注</li>
 *   <li>{lhRemark} - 硫化备注</li>
 * </ul>
 *
 * <p>列表占位符清单：</p>
 * <ul>
 *   <li>{.rubberTypeName} - 胶种名称</li>
 *   <li>{.specPattern} - 规格+花纹</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Service
public class ScheduleSummaryReportServiceImpl implements IScheduleSummaryReportService {

    private static final int SMALL_RUBBER_TITLE_ROW_INDEX = 6;

    private static final int SMALL_RUBBER_START_COL = 1;

    private static final int SMALL_RUBBER_END_COL = 1;

    @Resource
    private CxLhScheduleResultMapper cxLhScheduleResultMapper;

    @Resource
    private CxScheduleResultMapper cxScheduleResultMapper;

    @Resource
    private LhShiftConfigMapper lhShiftConfigMapper;

    @Resource
    private LhMouldCleanPlanMapper lhMouldCleanPlanMapper;

    @Resource
    private LhMouldChangePlanEntityMapper lhMouldChangePlanEntityMapper;

    @Resource
    private CxParamConfigMapper cxParamConfigMapper;

    @Resource
    private MdmMaterialConsumeDetailMapper mdmMaterialConsumeDetailMapper;

    @Resource
    private MdmMaterialInfoMapper mdmMaterialInfoMapper;

    @Resource
    private IMpStructureAllocationRemoteService mpStructureAllocationRemoteService;

    @Override
    public byte[] exportScheduleSummaryReport(ScheduleSummaryReportVO queryVO) {
        if (queryVO == null || StringUtils.isBlank(queryVO.getScheduleDate())) {
            throw new ServiceException("排程日期不能为空");
        }

        Date scheduleDate = DateUtil.parse(queryVO.getScheduleDate(), "yyyy-MM-dd");
        scheduleDate = LhScheduleTimeUtil.clearTime(scheduleDate);
        String factoryCode = StringUtils.defaultString(queryVO.getFactoryCode(), FactoryConstant.DEFAULT_FACTORY_CODE);

        Map<String, Object> exportData = buildScheduleSummaryExportData(scheduleDate, factoryCode);

        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/scheduleSummaryReport.xlsx");

        if (inputStream == null) {
            throw new ServiceException("排产小结模板文件不存在");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> tableMap = (Map<String, Object>) exportData.get("tableMap");
        @SuppressWarnings("unchecked")
        List<List<Map<String, Object>>> dataList = (List<List<Map<String, Object>>>) exportData.get("dataList");

        return ExcelUtils.writeMultiList(inputStream, 0, tableMap, dataList);
    }

    /**
     * 构建排产小结导出数据（tableMap和dataList），
     * 供外部调用方将排产小结作为子sheet嵌入到多sheet导出流程中。
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编码
     * @return 包含 tableMap（模板占位符映射）和 dataList（列表数据）的Map
     */
    @Override
    public Map<String, Object> buildScheduleSummaryExportData(Date scheduleDate, String factoryCode) {
        scheduleDate = LhScheduleTimeUtil.clearTime(scheduleDate);
        factoryCode = StringUtils.defaultString(factoryCode, FactoryConstant.DEFAULT_FACTORY_CODE);

        // 排产小结导出查询排程日期的前一天数据
        Date previousDate = LhScheduleTimeUtil.addDays(scheduleDate, -1);
        log.info("构建排产小结导出数据, 排程日期: {}, 实际查询日期(前一天): {}, 分厂: {}",
                DateUtil.formatDate(scheduleDate), DateUtil.formatDate(previousDate), factoryCode);

        List<LhShiftConfig> shiftConfigs = loadShiftConfigs(factoryCode);
        Map<Integer, String> classShiftTypeMap = buildClassShiftTypeMap(shiftConfigs);

        int maxDateOffset = shiftConfigs.stream()
                .map(LhShiftConfig::getDateOffset)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        Map<String, Object> tableMap = buildTableMap(previousDate, factoryCode, classShiftTypeMap, maxDateOffset);
        List<List<Map<String, Object>>> dataList = buildDataList(previousDate, factoryCode);

        // 小胶种列表数据处理：无数据时隐藏第7行，有数据时合并B7到B列结束行
        List<Map<String, Object>> smallRubberList = dataList.isEmpty() ? Collections.emptyList() : dataList.get(0);
        if (smallRubberList.isEmpty()) {
            // 小胶种列表无数据，隐藏第7行（索引6）
            List<Integer> hiddenRows = new ArrayList<>();
            hiddenRows.add(SMALL_RUBBER_TITLE_ROW_INDEX);
            tableMap.put(ExcelUtils.HIDDEN_ROWS, hiddenRows);
        } else {
            // 小胶种列表有数据，合并B7到B列结束行
            List<ExcelCellRangeAddress> rangeAddressList = new ArrayList<>();
            int endRowIndex = SMALL_RUBBER_TITLE_ROW_INDEX + smallRubberList.size() - 1;
            rangeAddressList.add(new ExcelCellRangeAddress(
                    SMALL_RUBBER_TITLE_ROW_INDEX,
                    endRowIndex,
                    SMALL_RUBBER_START_COL,
                    SMALL_RUBBER_END_COL));
            tableMap.put(ExcelUtils.RANGE_ADDRESS, rangeAddressList);
        }

        Map<String, Object> result = new HashMap<>(4);
        result.put("tableMap", tableMap);
        result.put("dataList", dataList);
        return result;
    }

    /**
     * 构建模板参数映射表（普通占位符）
     */
    private Map<String, Object> buildTableMap(Date scheduleDate, String factoryCode,
                                              Map<Integer, String> classShiftTypeMap, int maxDateOffset) {
        Map<String, Object> map = new HashMap<>(32);

        map.put("titleDate", DateUtil.format(scheduleDate, "MM月dd日") + "计划排产\n"
                + "Ke hoach san xuat ngay " + DateUtil.format(scheduleDate, "dd/MM"));

        // 成型排程结果：CxScheduleResult没有isDelete字段，无需过滤
        List<CxScheduleResult> cxResults = cxScheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, scheduleDate)
                        .eq(CxScheduleResult::getFactoryCode, factoryCode));
        log.info("成型排程结果查询完成, 日期: {}, 数量: {}", DateUtil.formatDate(scheduleDate), cxResults.size());

        BigDecimal cxNightTotal = BigDecimal.ZERO;
        BigDecimal cxMorningTotal = BigDecimal.ZERO;
        BigDecimal cxMiddleTotal = BigDecimal.ZERO;

        for (CxScheduleResult result : cxResults) {
            cxNightTotal = cxNightTotal.add(sumCxQtyByShiftType(result, classShiftTypeMap, "01"));
            cxMorningTotal = cxMorningTotal.add(sumCxQtyByShiftType(result, classShiftTypeMap, "02"));
            cxMiddleTotal = cxMiddleTotal.add(sumCxQtyByShiftType(result, classShiftTypeMap, "03"));
        }

        map.put("cxNightQty", cxNightTotal.toString());
        map.put("cxMorningQty", cxMorningTotal.toString());
        map.put("cxMiddleQty", cxMiddleTotal.toString());
        map.put("cxTotalQty", cxNightTotal.add(cxMorningTotal).add(cxMiddleTotal).toString());

        log.info("成型排程汇总 - 夜班: {}, 早班: {}, 中班: {}, 合计: {}",
                cxNightTotal, cxMorningTotal, cxMiddleTotal,
                cxNightTotal.add(cxMorningTotal).add(cxMiddleTotal));

        // 硫化排程结果：cx-lh-api的LhScheduleResult有isDelete字段，需要过滤
        List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> lhResults = cxLhScheduleResultMapper.selectList(
                new LambdaQueryWrapper<com.zlt.aps.cx.entity.schedule.LhScheduleResult>()
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getScheduleDate, scheduleDate)
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getFactoryCode, factoryCode)
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
        log.info("硫化排程结果查询完成, 日期: {}, 数量: {}", DateUtil.formatDate(scheduleDate), lhResults.size());

        // 成型试制/量试信息：从成型原因分析字段和硫化施工阶段字段匹配，合并去重后按逗号隔开
        String cxSetupSpecs = buildCxSetupOrTrialInfo(cxResults, "试制");
        String lhSetupSpecs = buildLhSetupOrTrialInfo(lhResults, "试制");
        map.put("cxSetupInfo", combineSpecInfo(cxSetupSpecs, lhSetupSpecs));

        String cxTrialSpecs = buildCxSetupOrTrialInfo(cxResults, "量试");
        String lhTrialSpecs = buildLhSetupOrTrialInfo(lhResults, "量试");
        map.put("cxTrialInfo", combineSpecInfo(cxTrialSpecs, lhTrialSpecs));
        // 成型规格切换：参考成型日计划生成页面的成型结构切换导出逻辑，从T_MP_STRUCTURE_ALLOCATION取切换结构数据
        map.put("cxSpecSwitch", buildCxSpecSwitch(scheduleDate, factoryCode));

        BigDecimal lhNightTotal = BigDecimal.ZERO;
        BigDecimal lhMorningTotal = BigDecimal.ZERO;
        BigDecimal lhMiddleTotal = BigDecimal.ZERO;

        for (com.zlt.aps.cx.entity.schedule.LhScheduleResult result : lhResults) {
            lhNightTotal = lhNightTotal.add(sumLhQtyByShiftType(result, classShiftTypeMap, "01"));
            lhMorningTotal = lhMorningTotal.add(sumLhQtyByShiftType(result, classShiftTypeMap, "02"));
            lhMiddleTotal = lhMiddleTotal.add(sumLhQtyByShiftType(result, classShiftTypeMap, "03"));
        }

        long nightMachines = countLhMachinesByShiftType(lhResults, classShiftTypeMap, "01");
        long morningMachines = countLhMachinesByShiftType(lhResults, classShiftTypeMap, "02");
        long middleMachines = countLhMachinesByShiftType(lhResults, classShiftTypeMap, "03");

        map.put("lhNightQty", lhNightTotal.toString());
        map.put("lhMorningQty", lhMorningTotal.toString());
        map.put("lhMiddleQty", lhMiddleTotal.toString());
        map.put("lhTotalQty", lhNightTotal.add(lhMorningTotal).add(lhMiddleTotal).toString());
        map.put("lhNightMachines", String.valueOf(nightMachines));
        map.put("lhMorningMachines", String.valueOf(morningMachines));
        map.put("lhMiddleMachines", String.valueOf(middleMachines));
        map.put("lhTotalMachines", String.valueOf(nightMachines + morningMachines + middleMachines));

        log.info("硫化排程汇总 - 夜班: {}, 早班: {}, 中班: {}, 合计: {}, 开动机台 - 夜: {}, 早: {}, 中: {}, 合计: {}",
                lhNightTotal, lhMorningTotal, lhMiddleTotal,
                lhNightTotal.add(lhMorningTotal).add(lhMiddleTotal),
                nightMachines, morningMachines, middleMachines,
                nightMachines + morningMachines + middleMachines);

        // 模具交模信息：从换模计划表查询，需过滤isDelete
        map.put("mouldChangeInfo", buildMouldChangeInfo(scheduleDate, factoryCode));

        // 模具清洗日期和清洗信息：参考LhBaseDataServiceImpl的查询方式
        map.put("mouldCleanDate", buildMouldCleanDate(scheduleDate, factoryCode, maxDateOffset));
        map.put("mouldCleanInfo", buildMouldCleanInfo(scheduleDate, factoryCode, maxDateOffset));

        // 成型/硫化备注
        map.put("cxRemark", buildCxRemark(cxResults));
        map.put("lhRemark", buildLhRemark(lhResults));

        return map;
    }

    /**
     * 构建成型试制/量试信息
     *
     * <p>遍历成型排程结果的所有班次原因分析字段，
     * 若包含关键字则取该条记录的物料描述（规格），去重后用"，"隔开</p>
     *
     * @param cxResults 成型排程结果列表
     * @param keyword   关键字（"试制"或"量试"）
     * @return 匹配到的规格描述，多个用"，"隔开；无匹配返回空字符串
     */
    private String buildCxSetupOrTrialInfo(List<CxScheduleResult> cxResults, String keyword) {
        Set<String> matchedSpecs = new LinkedHashSet<>();
        for (CxScheduleResult result : cxResults) {
            if (containsKeywordInAnyAnalysis(result, keyword)) {
                String specDesc = StringUtils.defaultString(result.getMaterialDesc()).trim();
                if (StringUtils.isNotBlank(specDesc)) {
                    matchedSpecs.add(specDesc);
                }
            }
        }
        return String.join("，", matchedSpecs);
    }

    /**
     * 判断成型排程结果的任意一班原因分析字段是否包含指定关键字
     */
    private boolean containsKeywordInAnyAnalysis(CxScheduleResult result, String keyword) {
        return StringUtils.contains(result.getClass1Analysis(), keyword)
                || StringUtils.contains(result.getClass2Analysis(), keyword)
                || StringUtils.contains(result.getClass3Analysis(), keyword)
                || StringUtils.contains(result.getClass4Analysis(), keyword)
                || StringUtils.contains(result.getClass5Analysis(), keyword)
                || StringUtils.contains(result.getClass6Analysis(), keyword)
                || StringUtils.contains(result.getClass7Analysis(), keyword)
                || StringUtils.contains(result.getClass8Analysis(), keyword);
    }

    /**
     * 构建硫化试制/量试信息
     *
     * <p>遍历硫化排程结果，根据施工阶段（constructionStage）字段匹配试制或量试，
     * 取出对应记录的规格描述（specDesc），去重后用"，"隔开</p>
     *
     * @param lhResults 硫化排程结果列表
     * @param keyword   关键字（"试制"或"量试"）
     * @return 匹配到的规格描述，多个用"，"隔开；无匹配返回空字符串
     */
    private String buildLhSetupOrTrialInfo(List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> lhResults, String keyword) {
        String targetCode;
        if ("试制".equals(keyword)) {
            targetCode = ConstructionStageEnum.MEASUREMENT.getStage();
        } else {
            targetCode = ConstructionStageEnum.TRIAL_PRODUCTION.getStage();
        }

        Set<String> matchedSpecs = new LinkedHashSet<>();
        for (com.zlt.aps.cx.entity.schedule.LhScheduleResult result : lhResults) {
            if (targetCode.equals(result.getConstructionStage())) {
                String specDesc = StringUtils.defaultString(result.getSpecDesc()).trim();
                if (StringUtils.isNotBlank(specDesc)) {
                    matchedSpecs.add(specDesc);
                }
            }
        }
        return String.join("，", matchedSpecs);
    }

    /**
     * 合并成型和硫化的试制/量试规格信息，去重后用"，"隔开
     *
     * @param cxInfo 成型试制/量试规格信息
     * @param lhInfo 硫化试制/量试规格信息
     * @return 合并后的规格信息
     */
    private String combineSpecInfo(String cxInfo, String lhInfo) {
        Set<String> allSpecs = new LinkedHashSet<>();
        if (StringUtils.isNotBlank(cxInfo)) {
            Arrays.stream(cxInfo.split("，"))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .forEach(allSpecs::add);
        }
        if (StringUtils.isNotBlank(lhInfo)) {
            Arrays.stream(lhInfo.split("，"))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .forEach(allSpecs::add);
        }
        return String.join("，", allSpecs);
    }

    /**
     * 构建模具交模信息
     *
     * <p>查询排程日期对应的换模计划，取出去重的机台编码，逗号隔开</p>
     * <p>参考LhBaseDataServiceImpl的查询方式，添加isDelete过滤</p>
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编码
     * @return 去重机台编码字符串，用"；"隔开，上限15台
     */
    private String buildMouldChangeInfo(Date scheduleDate, String factoryCode) {
        List<LhMouldChangePlan> changePlans = lhMouldChangePlanEntityMapper.selectList(
                new LambdaQueryWrapper<LhMouldChangePlan>()
                        .eq(LhMouldChangePlan::getFactoryCode, factoryCode)
                        .eq(LhMouldChangePlan::getScheduleDate, scheduleDate)
                        .and(w -> w.eq(LhMouldChangePlan::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                                .or().isNull(LhMouldChangePlan::getIsDelete)));
        log.info("模具交替计划查询完成, 日期: {}, 数量: {}", DateUtil.formatDate(scheduleDate), changePlans.size());

        if (changePlans.isEmpty()) {
            return "";
        }

        return changePlans.stream()
                .map(LhMouldChangePlan::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .limit(15)
                .collect(Collectors.joining(";"));
    }

    /**
     * 构建模具清洗日期
     *
     * <p>查询前一天日期范围内的清洗计划，返回清洗日期（如"14日"）</p>
     *
     * @param scheduleDate 查询日期（排程日期的前一天）
     * @param factoryCode  分厂编码
     * @param maxDateOffset 最大日期偏移量（来自班次配置）
     * @return 清洗日期字符串
     */
    private String buildMouldCleanDate(Date scheduleDate, String factoryCode, int maxDateOffset) {
        Date rangeStart = LhScheduleTimeUtil.clearTime(scheduleDate);
        Date rangeEnd = LhScheduleTimeUtil.addDays(rangeStart, maxDateOffset + 1);

        List<LhMouldCleanPlan> cleanPlans = lhMouldCleanPlanMapper.selectList(
                new LambdaQueryWrapper<LhMouldCleanPlan>()
                        .eq(LhMouldCleanPlan::getFactoryCode, factoryCode)
                        .ge(LhMouldCleanPlan::getCleanTime, rangeStart)
                        .lt(LhMouldCleanPlan::getCleanTime, rangeEnd)
                        .and(w -> w.eq(LhMouldCleanPlan::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                                .or().isNull(LhMouldCleanPlan::getIsDelete))
                        .orderByAsc(LhMouldCleanPlan::getCleanTime));
        log.info("模具清洗计划查询完成, 日期范围: {} ~ {}, 数量: {}",
                DateUtil.formatDateTime(rangeStart), DateUtil.formatDateTime(rangeEnd), cleanPlans.size());

        if (cleanPlans.isEmpty()) {
            return "";
        }

        return DateUtil.format(scheduleDate, "dd日");
    }

    /**
     * 构建模具清洗信息
     *
     * <p>查询前一天日期范围内的清洗计划，取出去重的机台编码，用"；"隔开</p>
     *
     * @param scheduleDate 查询日期（排程日期的前一天）
     * @param factoryCode  分厂编码
     * @param maxDateOffset 最大日期偏移量（来自班次配置）
     * @return 去重机台编码字符串，用"；"隔开
     */
    private String buildMouldCleanInfo(Date scheduleDate, String factoryCode, int maxDateOffset) {
        Date rangeStart = LhScheduleTimeUtil.clearTime(scheduleDate);
        Date rangeEnd = LhScheduleTimeUtil.addDays(rangeStart, maxDateOffset + 1);

        List<LhMouldCleanPlan> cleanPlans = lhMouldCleanPlanMapper.selectList(
                new LambdaQueryWrapper<LhMouldCleanPlan>()
                        .eq(LhMouldCleanPlan::getFactoryCode, factoryCode)
                        .ge(LhMouldCleanPlan::getCleanTime, rangeStart)
                        .lt(LhMouldCleanPlan::getCleanTime, rangeEnd)
                        .and(w -> w.eq(LhMouldCleanPlan::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                                .or().isNull(LhMouldCleanPlan::getIsDelete))
                        .orderByAsc(LhMouldCleanPlan::getCleanTime));

        if (cleanPlans.isEmpty()) {
            return "";
        }

        return cleanPlans.stream()
                .map(LhMouldCleanPlan::getLhCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(";"));
    }

    /**
     * 构建列表数据（小胶种列表，使用 {.xxx} 占位符）
     *
     * <p>取数逻辑：</p>
     * <ol>
     *   <li>从成型参数配置表读取胶种类型编码（PARAM_CODE=RUBBER_TYPE_CODES）</li>
     *   <li>从原材料消耗明细表按胶种类型查对应的胎胚（CHILD_MATERIAL_NAME='AQ'+胶种类型）</li>
     *   <li>匹配本次成型排程结果中的胎胚</li>
     *   <li>通过胎胚编号关联物料主数据取规格+花纹</li>
     *   <li>按胶种分组，同规格多花纹用"/"隔开，不同规格用","隔开</li>
     * </ol>
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编码
     * @return 列表数据
     */
    private List<List<Map<String, Object>>> buildDataList(Date scheduleDate, String factoryCode) {
        List<Map<String, Object>> smallRubberList = new ArrayList<>();

        List<String> rubberTypeCodes = loadRubberTypeCodes();
        if (rubberTypeCodes.isEmpty()) {
            log.warn("未配置胶种类型编码（RUBBER_TYPE_CODES），小胶种列表为空");
            List<List<Map<String, Object>>> dataList = new ArrayList<>();
            dataList.add(smallRubberList);
            return dataList;
        }

        // 查询本次成型排程结果的胎胚列表
        List<CxScheduleResult> cxResults = cxScheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, scheduleDate)
                        .eq(CxScheduleResult::getFactoryCode, factoryCode));
        Set<String> scheduleEmbryoCodes = cxResults.stream()
                .map(CxScheduleResult::getEmbryoCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        if (scheduleEmbryoCodes.isEmpty()) {
            log.warn("成型排程结果中无胎胚代码，小胶种列表为空");
            List<List<Map<String, Object>>> dataList = new ArrayList<>();
            dataList.add(smallRubberList);
            return dataList;
        }

        // 按胶种类型查询对应的胎胚，构建胶种→胎胚映射
        Map<String, Set<String>> rubberTypeEmbryoMap = new LinkedHashMap<>();
        for (String rubberType : rubberTypeCodes) {
            String childMaterialName = "AQ" + rubberType;
            List<MdmMaterialConsumeDetail> consumeDetails = mdmMaterialConsumeDetailMapper.selectList(
                    new LambdaQueryWrapper<MdmMaterialConsumeDetail>()
                            .eq(MdmMaterialConsumeDetail::getFactoryCode, factoryCode)
                            .eq(MdmMaterialConsumeDetail::getChildMaterialName, childMaterialName)
                            .and(w -> w.eq(MdmMaterialConsumeDetail::getIsDelete, 0)
                                    .or().isNull(MdmMaterialConsumeDetail::getIsDelete)));
            log.info("胶种类型[{}]消耗明细查询, CHILD_MATERIAL_NAME: {}, 数量: {}",
                    rubberType, childMaterialName, consumeDetails.size());

            Set<String> embryoCodes = consumeDetails.stream()
                    .map(MdmMaterialConsumeDetail::getEmbryoCode)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            rubberTypeEmbryoMap.put(rubberType, embryoCodes);
        }

        // 查询物料主数据，构建胎胚→物料列表映射
        Set<String> allRelevantEmbryoCodes = rubberTypeEmbryoMap.values().stream()
                .flatMap(Set::stream)
                .filter(scheduleEmbryoCodes::contains)
                .collect(Collectors.toSet());

        log.info("需要查询物料主数据的胎胚代码数量: {}", allRelevantEmbryoCodes.size());

        // 查询物料主数据，构建胎胚→物料列表映射（一个胎胚代码可能对应多条物料记录）
        Map<String, List<MdmMaterialInfo>> materialInfoMap = new HashMap<>();
        if (!allRelevantEmbryoCodes.isEmpty()) {
            List<MdmMaterialInfo> materialInfoList = mdmMaterialInfoMapper.selectList(
                    new LambdaQueryWrapper<MdmMaterialInfo>()
                            .in(MdmMaterialInfo::getEmbryoCode, allRelevantEmbryoCodes)
                            .and(w -> w.eq(MdmMaterialInfo::getIsDelete, 0)
                                    .or().isNull(MdmMaterialInfo::getIsDelete)));
            materialInfoMap = materialInfoList.stream()
                    .filter(m -> StringUtils.isNotBlank(m.getEmbryoCode()))
                    .collect(Collectors.groupingBy(MdmMaterialInfo::getEmbryoCode, HashMap::new, Collectors.toList()));
            log.info("物料主数据查询完成, 数量: {}, 胎胚分组数: {}", materialInfoList.size(), materialInfoMap.size());
        }

        // 按胶种分组构建列表数据
        for (String rubberType : rubberTypeCodes) {
            Set<String> embryoCodes = rubberTypeEmbryoMap.getOrDefault(rubberType, Collections.emptySet());

            Set<String> scheduledEmbryos = embryoCodes.stream()
                    .filter(scheduleEmbryoCodes::contains)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            if (scheduledEmbryos.isEmpty()) {
                continue;
            }

            Map<String, Set<String>> specPatternMap = new LinkedHashMap<>();
            for (String embryoCode : scheduledEmbryos) {
                List<MdmMaterialInfo> materialInfoList = materialInfoMap.get(embryoCode);
                if (materialInfoList == null || materialInfoList.isEmpty()) {
                    log.warn("胎胚代码[{}]未找到物料主数据", embryoCode);
                    continue;
                }
                for (MdmMaterialInfo materialInfo : materialInfoList) {
                    String spec = StringUtils.defaultString(materialInfo.getSpecifications()).trim();
                    String pattern = StringUtils.defaultString(materialInfo.getPattern()).trim();
                    if (StringUtils.isBlank(spec)) {
                        continue;
                    }
                    specPatternMap.computeIfAbsent(spec, k -> new LinkedHashSet<>());
                    if (StringUtils.isNotBlank(pattern)) {
                        specPatternMap.get(spec).add(pattern);
                    }
                }
            }

            List<String> specParts = new ArrayList<>();
            for (Map.Entry<String, Set<String>> entry : specPatternMap.entrySet()) {
                StringBuilder sb = new StringBuilder(entry.getKey());
                if (!entry.getValue().isEmpty()) {
                    sb.append(" ").append(String.join("/", entry.getValue()));
                }
                specParts.add(sb.toString());
            }

            Map<String, Object> item = new HashMap<>();
            item.put("rubberTypeName", rubberType);
            item.put("specPattern", String.join(",", specParts));
            smallRubberList.add(item);
        }

        List<List<Map<String, Object>>> dataList = new ArrayList<>();
        dataList.add(smallRubberList);
        return dataList;
    }

    /**
     * 从成型参数配置表读取胶种类型编码
     *
     * <p>参数编码：RUBBER_TYPE_CODES，值为逗号分隔的胶种类型（如 T101,T133,T601）</p>
     *
     * @return 胶种类型列表
     */
    private List<String> loadRubberTypeCodes() {
        CxParamConfig config = cxParamConfigMapper.selectOne(
                new LambdaQueryWrapper<CxParamConfig>()
                        .eq(CxParamConfig::getParamCode, "RUBBER_TYPE_CODES")
                        .eq(CxParamConfig::getIsActive, 1));

        if (config == null || StringUtils.isBlank(config.getParamValue())) {
            log.warn("未找到胶种类型配置（PARAM_CODE=RUBBER_TYPE_CODES）");
            return Collections.emptyList();
        }

        List<String> codes = Arrays.stream(config.getParamValue().split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        log.info("胶种类型配置: {}", codes);
        return codes;
    }

    /**
     * 按班次类型汇总成型产量
     */
    private BigDecimal sumCxQtyByShiftType(CxScheduleResult result, Map<Integer, String> classShiftTypeMap, String shiftType) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, String> entry : classShiftTypeMap.entrySet()) {
            if (shiftType.equals(entry.getValue())) {
                int shiftIndex = entry.getKey();
                switch (shiftIndex) {
                    case 1: total = total.add(nvl(result.getClass1PlanQty())); break;
                    case 2: total = total.add(nvl(result.getClass2PlanQty())); break;
                    case 3: total = total.add(nvl(result.getClass3PlanQty())); break;
                    case 4: total = total.add(nvl(result.getClass4PlanQty())); break;
                    case 5: total = total.add(nvl(result.getClass5PlanQty())); break;
                    case 6: total = total.add(nvl(result.getClass6PlanQty())); break;
                    case 7: total = total.add(nvl(result.getClass7PlanQty())); break;
                    case 8: total = total.add(nvl(result.getClass8PlanQty())); break;
                    default: break;
                }
            }
        }
        return total;
    }

    /**
     * 按班次类型汇总硫化产量
     */
    private BigDecimal sumLhQtyByShiftType(com.zlt.aps.cx.entity.schedule.LhScheduleResult result,
                                           Map<Integer, String> classShiftTypeMap, String shiftType) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, String> entry : classShiftTypeMap.entrySet()) {
            if (shiftType.equals(entry.getValue())) {
                int shiftIndex = entry.getKey();
                switch (shiftIndex) {
                    case 1: total = total.add(nvlInt(result.getClass1PlanQty())); break;
                    case 2: total = total.add(nvlInt(result.getClass2PlanQty())); break;
                    case 3: total = total.add(nvlInt(result.getClass3PlanQty())); break;
                    case 4: total = total.add(nvlInt(result.getClass4PlanQty())); break;
                    case 5: total = total.add(nvlInt(result.getClass5PlanQty())); break;
                    case 6: total = total.add(nvlInt(result.getClass6PlanQty())); break;
                    case 7: total = total.add(nvlInt(result.getClass7PlanQty())); break;
                    case 8: total = total.add(nvlInt(result.getClass8PlanQty())); break;
                    default: break;
                }
            }
        }
        return total;
    }

    /**
     * 统计某班次类型下的硫化开动机台数
     */
    private long countLhMachinesByShiftType(List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> results,
                                            Map<Integer, String> classShiftTypeMap, String shiftType) {
        return results.stream()
                .filter(r -> hasNonZeroQtyForShiftType(r, classShiftTypeMap, shiftType))
                .count();
    }

    /**
     * 判断某台机器在指定班次类型下是否有计划产量
     */
    private boolean hasNonZeroQtyForShiftType(com.zlt.aps.cx.entity.schedule.LhScheduleResult result,
                                              Map<Integer, String> classShiftTypeMap, String shiftType) {
        for (Map.Entry<Integer, String> entry : classShiftTypeMap.entrySet()) {
            if (shiftType.equals(entry.getValue())) {
                int shiftIndex = entry.getKey();
                BigDecimal qty = BigDecimal.ZERO;
                switch (shiftIndex) {
                    case 1: qty = nvlInt(result.getClass1PlanQty()); break;
                    case 2: qty = nvlInt(result.getClass2PlanQty()); break;
                    case 3: qty = nvlInt(result.getClass3PlanQty()); break;
                    case 4: qty = nvlInt(result.getClass4PlanQty()); break;
                    case 5: qty = nvlInt(result.getClass5PlanQty()); break;
                    case 6: qty = nvlInt(result.getClass6PlanQty()); break;
                    case 7: qty = nvlInt(result.getClass7PlanQty()); break;
                    case 8: qty = nvlInt(result.getClass8PlanQty()); break;
                    default: break;
                }
                if (qty.compareTo(BigDecimal.ZERO) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private BigDecimal nvl(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private BigDecimal nvlInt(Integer val) {
        return val != null ? new BigDecimal(val) : BigDecimal.ZERO;
    }

    /**
     * 构建成型备注信息
     */
    private String buildCxRemark(List<CxScheduleResult> cxResults) {
        return cxResults.stream()
                .map(CxScheduleResult::getSpecialRequirements)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；"));
    }

    /**
     * 构建硫化备注信息
     */
    private String buildLhRemark(List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> lhResults) {
        return lhResults.stream()
                .map(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getRemark)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；"));
    }

    /**
     * 加载班次配置列表
     */
    private List<LhShiftConfig> loadShiftConfigs(String factoryCode) {
        return lhShiftConfigMapper.selectList(
                new LambdaQueryWrapper<LhShiftConfig>()
                        .eq(LhShiftConfig::getFactoryCode, factoryCode)
                        .eq(LhShiftConfig::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                        .orderByAsc(LhShiftConfig::getShiftIndex));
    }

    /**
     * 构建班次类型映射：班次序号 → 班次类型（早班/中班/夜班）
     */
    private Map<Integer, String> buildClassShiftTypeMap(List<LhShiftConfig> shiftConfigs) {
        Map<Integer, String> map = new LinkedHashMap<>();
        for (LhShiftConfig config : shiftConfigs) {
            if (config.getShiftIndex() != null && StringUtils.isNotBlank(config.getShiftType())) {
                map.put(config.getShiftIndex(), config.getShiftType());
            }
        }
        log.info("班次配置映射: {}", map);
        return map;
    }

    /**
     * 构建成型规格切换信息。
     * 参考成型日计划生成页面的成型结构切换导出逻辑（CxScheduleResultServiceImpl.buildStructureChangeDataListV2），
     * 从T_MP_STRUCTURE_ALLOCATION表获取结构排产数据，
     * 按成型机台分组，根据排程日期找到当前正在运行的结构和下一个切换结构，
     * 展示格式为"前结构 换 后结构"，多个切换用"；"隔开。
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编码
     * @return 规格切换信息字符串，如"结构A 换 结构B；结构C 换 结构D"
     */
    private String buildCxSpecSwitch(Date scheduleDate, String factoryCode) {
        LocalDate localScheduleDate = DateUtil.toLocalDateTime(scheduleDate).toLocalDate();

        MpStructureAllocation structureQuery = new MpStructureAllocation();
        structureQuery.setFactoryCode(factoryCode);
        structureQuery.setYear(localScheduleDate.getYear());
        structureQuery.setMonth(localScheduleDate.getMonthValue());

        List<MpStructureAllocation> structureList = queryStructureAllocationList(structureQuery);
        if (structureList.isEmpty()) {
            log.info("成型规格切换：未查询到结构排产数据, 日期: {}, 分厂: {}", DateUtil.formatDate(scheduleDate), factoryCode);
            return "";
        }

        Map<String, List<MpStructureAllocation>> machineGroupMap = structureList.stream()
                .filter(Objects::nonNull)
                .filter(s -> StringUtils.isNotBlank(s.getCxMachineCode()))
                .collect(Collectors.groupingBy(
                        s -> s.getCxMachineCode().trim(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<String> switchList = new ArrayList<>();
        int scheduleDayOfMonth = localScheduleDate.getDayOfMonth();

        for (Map.Entry<String, List<MpStructureAllocation>> entry : machineGroupMap.entrySet()) {
            List<MpStructureAllocation> structures = entry.getValue().stream()
                    .sorted(Comparator.comparing(MpStructureAllocation::getBeginDay, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            int currentIndex = -1;
            for (int i = 0; i < structures.size(); i++) {
                MpStructureAllocation s = structures.get(i);
                if (s.getBeginDay() != null && s.getEndDay() != null
                        && scheduleDayOfMonth >= s.getBeginDay() && scheduleDayOfMonth <= s.getEndDay()) {
                    currentIndex = i;
                    break;
                }
            }

            if (currentIndex == -1 || currentIndex >= structures.size() - 1) {
                continue;
            }

            MpStructureAllocation prevStructure = structures.get(currentIndex);
            MpStructureAllocation nextStructure = structures.get(currentIndex + 1);

            String prevStructureName = StringUtils.defaultString(prevStructure.getStructureName()).trim();
            String nextStructureName = StringUtils.defaultString(nextStructure.getStructureName()).trim();

            if (StringUtils.isNotBlank(prevStructureName) && StringUtils.isNotBlank(nextStructureName)) {
                switchList.add(prevStructureName + " 换 " + nextStructureName);
            }
        }

        String result = String.join("；", switchList);
        log.info("成型规格切换: {}", result);
        return result;
    }

    /**
     * 通过Feign远程调用查询结构排产数据列表，
     * 并将返回的LinkedHashMap转换为MpStructureAllocation实体列表。
     *
     * @param structureQuery 查询条件（含factoryCode、year、month）
     * @return MpStructureAllocation实体列表
     */
    private List<MpStructureAllocation> queryStructureAllocationList(MpStructureAllocation structureQuery) {
        try {
            TableDataInfo structureDataInfo = mpStructureAllocationRemoteService.list(structureQuery);
            if (structureDataInfo == null || structureDataInfo.getRows() == null) {
                return Collections.emptyList();
            }
            return convertToMpStructureAllocationList(structureDataInfo.getRows());
        } catch (Exception e) {
            log.error("查询结构排产数据失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 将Feign远程调用返回的LinkedHashMap列表转换为MpStructureAllocation实体列表。
     * Feign反序列化泛型丢失，TableDataInfo.getRows()中的元素实际类型为LinkedHashMap，
     * 需使用ObjectMapper.convertValue进行类型转换。
     *
     * @param rows Feign远程调用返回的行数据列表
     * @return MpStructureAllocation实体列表
     */
    private List<MpStructureAllocation> convertToMpStructureAllocationList(List<?> rows) {
        List<MpStructureAllocation> entityList = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return entityList;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        for (Object obj : rows) {
            if (obj instanceof MpStructureAllocation) {
                entityList.add((MpStructureAllocation) obj);
            } else if (obj instanceof Map) {
                MpStructureAllocation entity = objectMapper.convertValue(obj, MpStructureAllocation.class);
                entityList.add(entity);
            }
        }
        return entityList;
    }
}
