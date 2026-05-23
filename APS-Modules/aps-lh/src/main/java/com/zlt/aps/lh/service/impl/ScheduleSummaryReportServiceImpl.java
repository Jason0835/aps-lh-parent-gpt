package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.core.domain.ExcelCellRangeAddress;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.cx.api.domain.entity.CxPrecisionPlan;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import com.zlt.aps.lh.api.domain.entity.LhShiftConfig;
import com.zlt.aps.lh.api.domain.vo.ScheduleSummaryReportVO;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.lh.mapper.CxLhScheduleResultMapper;
import com.zlt.aps.lh.mapper.CxParamConfigMapper;
import com.zlt.aps.lh.mapper.CxPrecisionPlanMapper;
import com.zlt.aps.lh.mapper.CxScheduleResultMapper;
import com.zlt.aps.lh.mapper.LhMouldChangePlanEntityMapper;
import com.zlt.aps.lh.mapper.LhPrecisionPlanMapper;
import com.zlt.aps.lh.mapper.LhShiftConfigMapper;
import com.zlt.aps.lh.mapper.MdmMaterialInfoMapper;
import com.zlt.aps.lh.service.IScheduleSummaryReportService;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.maindata.mapper.FactoryParamMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialConsumeDetailMapper;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.FactoryParam;
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
 *   <li>{.specPattern} - 规格+花纹（按示方类型分组，每组前加标题前缀，如"正规 Chinh quy：规格 花纹"）</li>
 * </ul>
 *
 * @author APS Team
 */
@Slf4j
@Service
public class ScheduleSummaryReportServiceImpl implements IScheduleSummaryReportService {

    private static final int SMALL_RUBBER_TITLE_ROW_INDEX = 6;

    private static final int SMALL_RUBBER_START_COL = 0;

    private static final int SMALL_RUBBER_END_COL = 0;

    @Resource
    private CxLhScheduleResultMapper cxLhScheduleResultMapper;

    @Resource
    private CxScheduleResultMapper cxScheduleResultMapper;

    @Resource
    private LhShiftConfigMapper lhShiftConfigMapper;

    @Resource
    private LhMouldChangePlanEntityMapper lhMouldChangePlanEntityMapper;

    @Resource
    private CxPrecisionPlanMapper cxPrecisionPlanMapper;

    @Resource
    private LhPrecisionPlanMapper lhPrecisionPlanMapper;

    @Resource
    private FactoryParamMapper factoryParamMapper;

    @Resource
    private MdmMaterialConsumeDetailMapper mdmMaterialConsumeDetailMapper;

    @Resource
    private MdmMaterialInfoMapper mdmMaterialInfoMapper;

    @Resource
    private IMpStructureAllocationRemoteService mpStructureAllocationRemoteService;

    @Resource
    private CxParamConfigMapper cxParamConfigMapper;

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

        Map<String, Object> tableMap = buildTableMap(previousDate, scheduleDate, factoryCode, classShiftTypeMap);
        // TD胶种列表也使用前一天的数据，与表头数据保持一致
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
     *
     * @param reportDate         报告日期（排程日期的前一天，如排程5/3则报告日期为5/2）
     * @param actualScheduleDate 实际排程日期（如5/3）
     * @param factoryCode        分厂编码
     * @param classShiftTypeMap  班次类型映射
     * @return 模板参数映射
     */
    private Map<String, Object> buildTableMap(Date reportDate, Date actualScheduleDate, String factoryCode,
                                              Map<Integer, String> classShiftTypeMap) {
        Map<String, Object> map = new HashMap<>(32);

        map.put("titleDate", DateUtil.format(reportDate, "MM月dd日") + "计划排产\n"
                + "Ke hoach san xuat ngay " + DateUtil.format(reportDate, "dd/MM"));

        // 成型排程结果：查询排程日期（actualScheduleDate）的数据，
        // 取3/4/5班次汇总作为报告日期（reportDate）的夜/早/中班产量
        // 排程日期5/3的数据中，3/4/5班次对应5/2的夜/早/中班
        List<CxScheduleResult> cxResults = cxScheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, actualScheduleDate)
                        .eq(CxScheduleResult::getFactoryCode, factoryCode));
        log.info("成型排程结果查询完成, 排程日期: {}, 报告日期: {}, 数量: {}",
                DateUtil.formatDate(actualScheduleDate), DateUtil.formatDate(reportDate), cxResults.size());

        BigDecimal cxNightTotal = BigDecimal.ZERO;
        BigDecimal cxMorningTotal = BigDecimal.ZERO;
        BigDecimal cxMiddleTotal = BigDecimal.ZERO;

        for (CxScheduleResult result : cxResults) {
            cxNightTotal = cxNightTotal.add(nvl(result.getClass3PlanQty()));
            cxMorningTotal = cxMorningTotal.add(nvl(result.getClass4PlanQty()));
            cxMiddleTotal = cxMiddleTotal.add(nvl(result.getClass5PlanQty()));
        }

        map.put("cxNightQty", cxNightTotal.toString());
        map.put("cxMorningQty", cxMorningTotal.toString());
        map.put("cxMiddleQty", cxMiddleTotal.toString());
        map.put("cxTotalQty", cxNightTotal.add(cxMorningTotal).add(cxMiddleTotal).toString());

        log.info("成型排程汇总 - 夜班: {}, 早班: {}, 中班: {}, 合计: {}",
                cxNightTotal, cxMorningTotal, cxMiddleTotal,
                cxNightTotal.add(cxMorningTotal).add(cxMiddleTotal));

        // 硫化排程结果：查询排程日期（actualScheduleDate）的数据
        List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> lhResults = cxLhScheduleResultMapper.selectList(
                new LambdaQueryWrapper<com.zlt.aps.cx.entity.schedule.LhScheduleResult>()
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getScheduleDate, actualScheduleDate)
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getFactoryCode, factoryCode)
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
        log.info("硫化排程结果查询完成, 日期: {}, 数量: {}", DateUtil.formatDate(actualScheduleDate), lhResults.size());

        // 成型试制/量试信息：从成型原因分析字段和硫化施工阶段字段匹配，合并去重后按逗号隔开
        String cxSetupSpecs = buildCxSetupOrTrialInfo(cxResults, "试制");
        String lhSetupSpecs = buildLhSetupOrTrialInfo(lhResults, "试制");
        map.put("cxSetupInfo", combineSpecInfo(cxSetupSpecs, lhSetupSpecs));

        String cxTrialSpecs = buildCxSetupOrTrialInfo(cxResults, "量试");
        String lhTrialSpecs = buildLhSetupOrTrialInfo(lhResults, "量试");
        map.put("cxTrialInfo", combineSpecInfo(cxTrialSpecs, lhTrialSpecs));
        // 成型规格切换：从T_MP_STRUCTURE_ALLOCATION取切换结构数据
        // 需同时传入reportDate和scheduleDate，支持非跨月和跨月两种场景
        map.put("cxSpecSwitch", buildCxSpecSwitch(reportDate, actualScheduleDate, factoryCode));

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

        // 模具交替信息：取计划日期=reportDate且排程日期=actualScheduleDate且更换类型01/02的硫化机台(去重)
        map.put("mouldChangeInfo", buildMouldChangeInfo(reportDate, actualScheduleDate, factoryCode));

        // 模具清洗日期和清洗信息：取计划日期=reportDate且排程日期=actualScheduleDate且更换类型03/04的硫化机台(去重)
        map.put("mouldCleanDate", DateUtil.format(reportDate, "MM月dd日"));
        map.put("mouldCleanInfo", buildMouldCleanInfo(reportDate, actualScheduleDate, factoryCode));

        // 成型备注：取成型精度计划的排程日期在报告日期（前一天）时间范围内要做的机台，成型精度做的时间固定在6:00~14:00
        map.put("cxRemark", buildCxRemark(reportDate, factoryCode));
        // 硫化备注：取硫化精度计划的排程日期在报告日期（前一天）时间范围内要做的机台，时间及开产时间根据参数计算
        map.put("lhRemark", buildLhRemark(reportDate, factoryCode));

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
        String result = String.join("，", allSpecs);
        return StringUtils.isNotBlank(result) ? result : "无 Không";
    }

    /**
     * 构建模具交替信息
     *
     * <p>查询计划日期区间=reportDate当天（00:00:00~23:59:59）、排程日期=actualScheduleDate、更换类型为01或02的模具交替计划，
     * 取出去重的硫化机台编码，用"；"隔开，上限15台</p>
     *
     * @param planDate           计划日期（报告日期，如5月2日）
     * @param actualScheduleDate 排程日期（如5月3日）
     * @param factoryCode        分厂编码
     * @return 去重机台编码字符串，用"；"隔开，上限15台
     */
    private String buildMouldChangeInfo(Date planDate, Date actualScheduleDate, String factoryCode) {
        Date planDateStart = LhScheduleTimeUtil.clearTime(planDate);
        Date planDateEnd = LhScheduleTimeUtil.getEndTime(planDate);
        List<LhMouldChangePlan> changePlans = lhMouldChangePlanEntityMapper.selectList(
                new LambdaQueryWrapper<LhMouldChangePlan>()
                        .eq(LhMouldChangePlan::getFactoryCode, factoryCode)
                        .ge(LhMouldChangePlan::getPlanDate, planDateStart)
                        .le(LhMouldChangePlan::getPlanDate, planDateEnd)
                        .eq(LhMouldChangePlan::getScheduleDate, actualScheduleDate)
                        .in(LhMouldChangePlan::getChangeMouldType, "01", "02")
                        .and(w -> w.eq(LhMouldChangePlan::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                                .or().isNull(LhMouldChangePlan::getIsDelete)));
        log.info("模具交替计划查询完成, 计划日期: {}~{}, 排程日期: {}, 更换类型: 01/02, 数量: {}",
                DateUtil.formatDateTime(planDateStart), DateUtil.formatDateTime(planDateEnd),
                DateUtil.formatDate(actualScheduleDate), changePlans.size());

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
     * 构建模具清洗信息
     *
     * <p>查询计划日期区间=planDate当天（00:00:00~23:59:59）、排程日期=actualScheduleDate、更换类型为03或04的模具交替计划，
     * 取出去重的硫化机台编码，用"；"隔开</p>
     *
     * @param planDate           计划日期（报告日期，如5月2日）
     * @param actualScheduleDate 排程日期（如5月3日）
     * @param factoryCode        分厂编码
     * @return 去重机台编码字符串，用"；"隔开
     */
    private String buildMouldCleanInfo(Date planDate, Date actualScheduleDate, String factoryCode) {
        Date planDateStart = LhScheduleTimeUtil.clearTime(planDate);
        Date planDateEnd = LhScheduleTimeUtil.getEndTime(planDate);
        List<LhMouldChangePlan> cleanPlans = lhMouldChangePlanEntityMapper.selectList(
                new LambdaQueryWrapper<LhMouldChangePlan>()
                        .eq(LhMouldChangePlan::getFactoryCode, factoryCode)
                        .ge(LhMouldChangePlan::getPlanDate, planDateStart)
                        .le(LhMouldChangePlan::getPlanDate, planDateEnd)
                        .eq(LhMouldChangePlan::getScheduleDate, actualScheduleDate)
                        .in(LhMouldChangePlan::getChangeMouldType, "03", "04")
                        .and(w -> w.eq(LhMouldChangePlan::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                                .or().isNull(LhMouldChangePlan::getIsDelete)));
        log.info("模具清洗计划查询完成, 计划日期: {}~{}, 排程日期: {}, 更换类型: 03/04, 数量: {}",
                DateUtil.formatDateTime(planDateStart), DateUtil.formatDateTime(planDateEnd),
                DateUtil.formatDate(actualScheduleDate), cleanPlans.size());

        if (cleanPlans.isEmpty()) {
            return "";
        }

        return cleanPlans.stream()
                .map(LhMouldChangePlan::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(";"));
    }

    /**
     * 构建列表数据（小胶种列表，使用 {.xxx} 占位符）
     *
     * <p>取数逻辑：</p>
     * <ol>
     *   <li>从系统参数表读取TD胶种类型编码（PARAM_CODE=SYS04010002）</li>
     *   <li>从原材料消耗明细表按胶种类型查对应的胎胚（CHILD_MATERIAL_NAME='AQ'+胶种类型）</li>
     *   <li>匹配本次成型排程结果中的胎胚</li>
     *   <li>通过胎胚编号关联物料主数据取规格+花纹，仅保留本次实际排产的物料</li>
     *   <li>按胶种分组，同规格多花纹用"/"隔开，不同规格用","隔开</li>
     *   <li>按示方类型（正规/量试/试制）在物料级别分组，每组规格花纹前加标题前缀，不同组用换行隔开</li>
     * </ol>
     *
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编码
     * @return 列表数据
     */
    private List<List<Map<String, Object>>> buildDataList(Date scheduleDate, String factoryCode) {
        List<Map<String, Object>> smallRubberList = new ArrayList<>();

        List<String> rubberTypeCodes = loadRubberTypeCodes(factoryCode);
        if (rubberTypeCodes.isEmpty()) {
            log.warn("未配置TD胶种类型编码（SYS04010002），小胶种列表为空");
            List<List<Map<String, Object>>> dataList = new ArrayList<>();
            dataList.add(smallRubberList);
            return dataList;
        }

        // 成型排程结果按排程日期查询，过滤掉3、4、5班次都没排计划量的胎胚
        Date cxScheduleDate = scheduleDate;
        log.info("查询成型排程结果，排程日期: {}", scheduleDate);

        List<CxScheduleResult> cxResults = cxScheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, cxScheduleDate)
                        .eq(CxScheduleResult::getFactoryCode, factoryCode));
        Set<String> scheduleEmbryoCodes = cxResults.stream()
                .filter(this::hasAnyPlanQtyInShift345)
                .map(CxScheduleResult::getEmbryoCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        // 收集已排产的物料编码集合，用于过滤物料主数据中未排产的SKU
        Set<String> scheduledMaterialCodes = cxResults.stream()
                .filter(this::hasAnyPlanQtyInShift345)
                .map(CxScheduleResult::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        log.info("已排产胎胚代码数量: {}, 已排产物料编码数量: {}", scheduleEmbryoCodes.size(), scheduledMaterialCodes.size());

        if (scheduleEmbryoCodes.isEmpty()) {
            log.warn("成型排程结果中无胎胚代码，小胶种列表为空");
            List<List<Map<String, Object>>> dataList = new ArrayList<>();
            dataList.add(smallRubberList);
            return dataList;
        }

        // 构建物料→示方类型集合映射（基于成型排程结果3/4/5班次的示方书类型，按物料级别映射）
        // 同一物料可能同时属于多种示方类型（如正规和量试），保留所有类型以便分别展示
        Map<String, Set<String>> materialRecipeTypeMap = buildMaterialRecipeTypeMap(cxResults);
        log.info("物料示方类型映射构建完成, 映射数量: {}", materialRecipeTypeMap.size());

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
        // 仅保留本次实际排产的物料（scheduledMaterialCodes过滤），避免展示未排产的SKU
        Map<String, List<MdmMaterialInfo>> materialInfoMap = new HashMap<>();
        if (!allRelevantEmbryoCodes.isEmpty()) {
            List<MdmMaterialInfo> materialInfoList = mdmMaterialInfoMapper.selectList(
                    new LambdaQueryWrapper<MdmMaterialInfo>()
                            .in(MdmMaterialInfo::getEmbryoCode, allRelevantEmbryoCodes)
                            .and(w -> w.eq(MdmMaterialInfo::getIsDelete, 0)
                                    .or().isNull(MdmMaterialInfo::getIsDelete)));
            materialInfoMap = materialInfoList.stream()
                    .filter(m -> StringUtils.isNotBlank(m.getEmbryoCode()))
                    .filter(m -> scheduledMaterialCodes.contains(m.getMaterialCode()))
                    .collect(Collectors.groupingBy(MdmMaterialInfo::getEmbryoCode, HashMap::new, Collectors.toList()));
            log.info("物料主数据查询完成(已过滤未排产SKU), 数量: {}, 胎胚分组数: {}",
                    materialInfoMap.values().stream().mapToLong(List::size).sum(), materialInfoMap.size());
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

            // 收集该胶种下所有已排产物料，按物料级别确定示方类型
            // 同一胎胚下不同物料可能有不同的示方类型（如正规和量试），需要分别展示
            List<MdmMaterialInfo> rubberTypeMaterials = new ArrayList<>();
            for (String embryoCode : scheduledEmbryos) {
                List<MdmMaterialInfo> materials = materialInfoMap.get(embryoCode);
                if (materials != null) {
                    rubberTypeMaterials.addAll(materials);
                }
            }

            if (rubberTypeMaterials.isEmpty()) {
                continue;
            }

            // 按示方类型对物料进行分组（物料级别，而非胎胚级别）
            // 同一物料可能同时属于多种示方类型，需要分别加入对应的分组
            Map<String, List<MdmMaterialInfo>> recipeTypeMaterialGroupMap = new LinkedHashMap<>();
            for (MdmMaterialInfo materialInfo : rubberTypeMaterials) {
                Set<String> recipeTypes = materialRecipeTypeMap.getOrDefault(materialInfo.getMaterialCode(), Collections.singleton("S"));
                for (String recipeType : recipeTypes) {
                    recipeTypeMaterialGroupMap.computeIfAbsent(recipeType, k -> new ArrayList<>()).add(materialInfo);
                }
            }

            // 按示方类型顺序构建规格花纹字符串：正规 → 量试 → 试制
            List<String> recipeTypeOrder = Arrays.asList("S", "T", "X");
            List<String> groupParts = new ArrayList<>();

            for (String recipeType : recipeTypeOrder) {
                List<MdmMaterialInfo> materialsForType = recipeTypeMaterialGroupMap.get(recipeType);
                if (materialsForType == null || materialsForType.isEmpty()) {
                    continue;
                }

                Map<String, Set<String>> specPatternMap = new LinkedHashMap<>();
                for (MdmMaterialInfo materialInfo : materialsForType) {
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

                if (specPatternMap.isEmpty()) {
                    continue;
                }

                List<String> specParts = new ArrayList<>();
                for (Map.Entry<String, Set<String>> entry : specPatternMap.entrySet()) {
                    StringBuilder sb = new StringBuilder(entry.getKey());
                    if (!entry.getValue().isEmpty()) {
                        sb.append(" ").append(String.join("/", entry.getValue()));
                    }
                    specParts.add(sb.toString());
                }

                String title = getRecipeTypeTitle(recipeType);
                groupParts.add(title + String.join(",", specParts));
            }

            Map<String, Object> item = new HashMap<>();
            item.put("rubberTypeName", rubberType);
            item.put("specPattern", String.join("\n", groupParts));
            item.put("height", 30);
            smallRubberList.add(item);
        }

        List<List<Map<String, Object>>> dataList = new ArrayList<>();
        dataList.add(smallRubberList);
        return dataList;
    }

    /**
     * 从系统参数表（T_MP_FACTORY_PARAM）读取TD胶种类型编码
     *
     * <p>参数编码：SYS04010002，值为逗号分隔的胶种类型（如 T101,T133,T601）</p>
     * <p>TD胶种数据取数逻辑：通过胎胚关联t_mdm_material_consume_detail，
     * 如果CHILD_MATERIAL_NAME为AQ+参数配置里key为SYS04010002的任意一个值，
     * 满足胎胚及CHILD_MATERIAL_NAME的数据即为本次要展示的TD胶种数据</p>
     *
     * @param factoryCode 分厂编码
     * @return 胶种类型列表
     */
    private List<String> loadRubberTypeCodes(String factoryCode) {
        CxParamConfig paramConfig = cxParamConfigMapper.selectOne(
                new LambdaQueryWrapper<CxParamConfig>()
                        .eq(CxParamConfig::getParamCode, "SYS04010002")
                        .eq(CxParamConfig::getIsActive, 1)
        );
        if (paramConfig == null || StringUtils.isBlank(paramConfig.getParamValue())) {
            log.warn("未找到TD胶种类型配置（成型参数配置表T_CX_PARAM_CONFIG，PARAM_CODE=SYS04010002）");
            return Collections.emptyList();
        }

        List<String> codes = Arrays.stream(paramConfig.getParamValue().split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        log.info("TD胶种类型配置（成型参数配置SYS04010002）: {}", codes);
        return codes;
    }

    /**
     * 判断成型排程结果的3、4、5班次中是否有任意一个班次排了计划量。
     * 3、4、5班次对应排程日期前一天的夜、早、中班次，
     * 三个班次都没排计划量的胎胚需要过滤掉。
     *
     * @param result 成型排程结果
     * @return true=至少一个班次有计划量，false=三个班次都没计划量
     */
    private boolean hasAnyPlanQtyInShift345(CxScheduleResult result) {
        return nvl(result.getClass3PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || nvl(result.getClass4PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || nvl(result.getClass5PlanQty()).compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 构建物料→示方类型集合映射
     *
     * <p>根据成型排程结果的3/4/5班次示方书类型确定每个物料的示方类型。
     * 同一胎胚下不同物料可能有不同的示方类型（如正规和量试），因此按物料级别映射，
     * 确保每种物料都能正确归入对应的示方类型分组。</p>
     * <p>同一物料在不同班次可能有不同的示方类型（如正规和量试同时存在），
     * 需要保留所有示方类型，以便在展示时分别列出。</p>
     *
     * @param cxResults 成型排程结果列表
     * @return 物料编码→示方类型编码集合映射（S-正规，T-量试，X-试制）
     */
    private Map<String, Set<String>> buildMaterialRecipeTypeMap(List<CxScheduleResult> cxResults) {
        Map<String, Set<String>> materialRecipeTypeMap = new HashMap<>();
        for (CxScheduleResult result : cxResults) {
            if (!hasAnyPlanQtyInShift345(result)) {
                continue;
            }
            String materialCode = result.getMaterialCode();
            if (StringUtils.isBlank(materialCode)) {
                continue;
            }

            // 收集3/4/5班次中有计划量的班次对应的示方书类型
            Set<String> recipeTypes = new HashSet<>();
            if (nvl(result.getClass3PlanQty()).compareTo(BigDecimal.ZERO) > 0) {
                recipeTypes.add(StringUtils.defaultString(result.getClass3RecipeType()));
            }
            if (nvl(result.getClass4PlanQty()).compareTo(BigDecimal.ZERO) > 0) {
                recipeTypes.add(StringUtils.defaultString(result.getClass4RecipeType()));
            }
            if (nvl(result.getClass5PlanQty()).compareTo(BigDecimal.ZERO) > 0) {
                recipeTypes.add(StringUtils.defaultString(result.getClass5RecipeType()));
            }

            // 过滤掉空字符串，保留所有有效的示方类型
            recipeTypes.removeIf(StringUtils::isBlank);
            if (recipeTypes.isEmpty()) {
                recipeTypes.add("S");
            }

            // 同一物料可能有多条排程结果记录（不同机台），合并所有示方类型
            materialRecipeTypeMap.computeIfAbsent(materialCode, k -> new HashSet<>()).addAll(recipeTypes);
        }
        return materialRecipeTypeMap;
    }

    /**
     * 根据示方类型编码获取导出标题文本
     *
     * <p>示方类型与标题的对应关系：</p>
     * <ul>
     *   <li>S（正规）→ "正规 Chinh quy："</li>
     *   <li>T（量试）→ "量试 Thi nghiem s5 luong："</li>
     *   <li>X（试制）→ "试制 Thử sản xuất："</li>
     * </ul>
     *
     * @param recipeType 示方类型编码（S-正规，T-量试，X-试制）
     * @return 标题文本
     */
    private String getRecipeTypeTitle(String recipeType) {
        switch (StringUtils.defaultString(recipeType)) {
            case "S": return "正规 Chinh quy：";
            case "T": return "量试 Thi nghiem s5 luong：";
            case "X": return "试制 Thử sản xuất：";
            default: return "正规 Chinh quy：";
        }
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
                    case 1:
                        total = total.add(nvl(result.getClass1PlanQty()));
                        break;
                    case 2:
                        total = total.add(nvl(result.getClass2PlanQty()));
                        break;
                    case 3:
                        total = total.add(nvl(result.getClass3PlanQty()));
                        break;
                    case 4:
                        total = total.add(nvl(result.getClass4PlanQty()));
                        break;
                    case 5:
                        total = total.add(nvl(result.getClass5PlanQty()));
                        break;
                    case 6:
                        total = total.add(nvl(result.getClass6PlanQty()));
                        break;
                    case 7:
                        total = total.add(nvl(result.getClass7PlanQty()));
                        break;
                    case 8:
                        total = total.add(nvl(result.getClass8PlanQty()));
                        break;
                    default:
                        break;
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
                    case 1:
                        total = total.add(nvlInt(result.getClass1PlanQty()));
                        break;
                    case 2:
                        total = total.add(nvlInt(result.getClass2PlanQty()));
                        break;
                    case 3:
                        total = total.add(nvlInt(result.getClass3PlanQty()));
                        break;
                    case 4:
                        total = total.add(nvlInt(result.getClass4PlanQty()));
                        break;
                    case 5:
                        total = total.add(nvlInt(result.getClass5PlanQty()));
                        break;
                    case 6:
                        total = total.add(nvlInt(result.getClass6PlanQty()));
                        break;
                    case 7:
                        total = total.add(nvlInt(result.getClass7PlanQty()));
                        break;
                    case 8:
                        total = total.add(nvlInt(result.getClass8PlanQty()));
                        break;
                    default:
                        break;
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
                    case 1:
                        qty = nvlInt(result.getClass1PlanQty());
                        break;
                    case 2:
                        qty = nvlInt(result.getClass2PlanQty());
                        break;
                    case 3:
                        qty = nvlInt(result.getClass3PlanQty());
                        break;
                    case 4:
                        qty = nvlInt(result.getClass4PlanQty());
                        break;
                    case 5:
                        qty = nvlInt(result.getClass5PlanQty());
                        break;
                    case 6:
                        qty = nvlInt(result.getClass6PlanQty());
                        break;
                    case 7:
                        qty = nvlInt(result.getClass7PlanQty());
                        break;
                    case 8:
                        qty = nvlInt(result.getClass8PlanQty());
                        break;
                    default:
                        break;
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
     *
     * <p>取成型精度计划的排程日期在报告日期（前一天）时间范围内要做的机台，
     * 成型精度做的时间固定在6:00~14:00</p>
     *
     * @param reportDate  报告日期（排程日期的前一天）
     * @param factoryCode 分厂编码
     * @return 成型备注字符串，格式如："机台A、机台B 做精度 6:00-14:00"
     */
    private String buildCxRemark(Date reportDate, String factoryCode) {
        Date dayStart = LhScheduleTimeUtil.clearTime(reportDate);
        Date dayEnd = LhScheduleTimeUtil.getEndTime(reportDate);

        List<CxPrecisionPlan> precisionPlans = cxPrecisionPlanMapper.selectList(
                new LambdaQueryWrapper<CxPrecisionPlan>()
                        .eq(CxPrecisionPlan::getFactoryCode, factoryCode)
                        .ge(CxPrecisionPlan::getScheduleDate, dayStart)
                        .le(CxPrecisionPlan::getScheduleDate, dayEnd));
        log.info("成型精度计划查询完成, 报告日期(前一天): {}, 分厂: {}, 数量: {}",
                DateUtil.formatDate(reportDate), factoryCode, precisionPlans.size());

        if (precisionPlans.isEmpty()) {
            return "";
        }

        List<String> machineCodes = precisionPlans.stream()
                .map(CxPrecisionPlan::getMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        if (machineCodes.isEmpty()) {
            return "";
        }

        String machineStr = String.join("、", machineCodes);
        return machineStr + " 6:00-14:00精度校验";
    }

    /**
     * 构建硫化备注信息
     *
     * <p>取硫化精度计划的排程日期在报告日期（前一天）时间范围内要做的机台，
     * 硫化精度做的时间及开产时间根据以下三个参数来定：</p>
     * <ul>
     *   <li>胶囊预热时间（小时）SYS0307009，如：2.5</li>
     *   <li>保养开始小时 SYS0307002，如：8</li>
     *   <li>保养耗时（小时）SYS0307001，如：7</li>
     * </ul>
     * <p>开产为保养完后胶囊预热完后开产。
     * 例如：保养8:00开始，保养7小时到15:00结束，胶囊预热2.5小时，开产时间17:30</p>
     *
     * @param reportDate  报告日期（排程日期的前一天）
     * @param factoryCode 分厂编码
     * @return 硫化备注字符串，格式如："机台A、机台B 做精度 8:00-15:00，开产17:30"
     */
    private String buildLhRemark(Date reportDate, String factoryCode) {
        Date dayStart = LhScheduleTimeUtil.clearTime(reportDate);
        Date dayEnd = LhScheduleTimeUtil.getEndTime(reportDate);

        List<LhPrecisionPlan> precisionPlans = lhPrecisionPlanMapper.selectList(
                new LambdaQueryWrapper<LhPrecisionPlan>()
                        .eq(LhPrecisionPlan::getFactoryCode, factoryCode)
                        .ge(LhPrecisionPlan::getScheduleDate, dayStart)
                        .le(LhPrecisionPlan::getScheduleDate, dayEnd));
        log.info("硫化精度计划查询完成, 报告日期(前一天): {}, 分厂: {}, 数量: {}",
                DateUtil.formatDate(reportDate), factoryCode, precisionPlans.size());

        if (precisionPlans.isEmpty()) {
            return "";
        }

        List<String> machineCodes = precisionPlans.stream()
                .map(LhPrecisionPlan::getMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        if (machineCodes.isEmpty()) {
            return "";
        }

        String maintenanceStartHourStr = loadFactoryParamValue(factoryCode, null, "SYS0307002");
        String maintenanceDurationStr = loadFactoryParamValue(factoryCode, null, "SYS0307001");
        String capsulePreheatStr = loadFactoryParamValue(factoryCode, null, "SYS0307009");

        int maintenanceStartHour = 8;
        int maintenanceDuration = 7;
        double capsulePreheatHours = 2.5;

        try {
            if (StringUtils.isNotBlank(maintenanceStartHourStr)) {
                maintenanceStartHour = Integer.parseInt(maintenanceStartHourStr.trim());
            }
        } catch (NumberFormatException e) {
            log.warn("解析保养开始小时参数（SYS0307002）失败: {}, 使用默认值8", maintenanceStartHourStr);
        }
        try {
            if (StringUtils.isNotBlank(maintenanceDurationStr)) {
                maintenanceDuration = Integer.parseInt(maintenanceDurationStr.trim());
            }
        } catch (NumberFormatException e) {
            log.warn("解析保养耗时参数（SYS0307001）失败: {}, 使用默认值7", maintenanceDurationStr);
        }
        try {
            if (StringUtils.isNotBlank(capsulePreheatStr)) {
                capsulePreheatHours = Double.parseDouble(capsulePreheatStr.trim());
            }
        } catch (NumberFormatException e) {
            log.warn("解析胶囊预热时间参数（SYS0307009）失败: {}, 使用默认值2.5", capsulePreheatStr);
        }

        int maintenanceEndHour = maintenanceStartHour + maintenanceDuration;
        String maintenanceTimeRange = maintenanceStartHour + ":00-" + maintenanceEndHour + ":00";

        double productionStartTotalHours = maintenanceEndHour + capsulePreheatHours;
        int productionStartHour = (int) productionStartTotalHours;
        int productionStartMinute = (int) Math.round((productionStartTotalHours - productionStartHour) * 60);
        String productionStartTime = String.format("%d:%02d", productionStartHour, productionStartMinute);

        log.info("硫化备注时间计算 - 保养开始: {}小时, 保养耗时: {}小时, 胶囊预热: {}小时, 保养时段: {}, 开产时间: {}",
                maintenanceStartHour, maintenanceDuration, capsulePreheatHours,
                maintenanceTimeRange, productionStartTime);

        String machineStr = String.join("、", machineCodes);
        return machineStr + maintenanceTimeRange + " 维保," + productionStartTime + "开产";
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
     * 从T_MP_STRUCTURE_ALLOCATION表获取结构排产数据，按成型机台分组，
     * 找到结束日等于前一天日号的前结构，以及开始日等于前一天日号或排程日期日号的后结构，
     * 只展示第二天切换的数据，展示格式为"前结构 换 后结构"，多个切换用"；"隔开。
     *
     * <p>两种场景：</p>
     * <ul>
     *   <li>非跨月场景：排程日期21号，取5月转产数据，找endDay=20的前结构，
     *       后结构先查beginDay=20，20号没有再查beginDay=21</li>
     *   <li>跨月场景：排程日期6月1号，取6月转产数据，找endDay=1且后结构beginDay=1</li>
     * </ul>
     *
     * @param reportDate   报告日期（排程日期的前一天）
     * @param scheduleDate 排程日期
     * @param factoryCode  分厂编码
     * @return 规格切换信息字符串，如"结构A 换 结构B；结构C 换 结构D"
     */
    private String buildCxSpecSwitch(Date reportDate, Date scheduleDate, String factoryCode) {
        LocalDate localReportDate = DateUtil.toLocalDateTime(reportDate).toLocalDate();
        LocalDate localScheduleDate = DateUtil.toLocalDateTime(scheduleDate).toLocalDate();

        // 跨月场景：排程日期是1号时，直接取排程日期所在月的转产数据；否则取前一天所在月的数据
        LocalDate queryMonthBase = localScheduleDate.getDayOfMonth() == 1
                ? localScheduleDate : localReportDate;

        MpStructureAllocation structureQuery = new MpStructureAllocation();
        structureQuery.setFactoryCode(factoryCode);
        structureQuery.setYear(queryMonthBase.getYear());
        structureQuery.setMonth(queryMonthBase.getMonthValue());

        List<MpStructureAllocation> structureList = queryStructureAllocationList(structureQuery);
        if (structureList.isEmpty()) {
            log.info("成型规格切换：未查询到结构排产数据, 查询年月: {}-{}, 分厂: {}",
                    queryMonthBase.getYear(), queryMonthBase.getMonthValue(), factoryCode);
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
        int reportDayOfMonth = localReportDate.getDayOfMonth();
        int scheduleDayOfMonth = localScheduleDate.getDayOfMonth();

        for (Map.Entry<String, List<MpStructureAllocation>> entry : machineGroupMap.entrySet()) {
            List<MpStructureAllocation> structures = entry.getValue().stream()
                    .sorted(Comparator.comparing(MpStructureAllocation::getBeginDay, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            // 同一机台必须有2条及以上转产数据才说明有结构切换
            if (structures.size() < 2) {
                continue;
            }

            // 查找前结构：结束日等于前一天日号（跨月场景下为排程日期日号1号）
            int prevEndDay = localScheduleDate.getDayOfMonth() == 1
                    ? scheduleDayOfMonth : reportDayOfMonth;
            MpStructureAllocation prevStructure = null;
            for (MpStructureAllocation s : structures) {
                if (s.getEndDay() != null && s.getEndDay() == prevEndDay) {
                    prevStructure = s;
                    break;
                }
            }
            if (prevStructure == null) {
                continue;
            }

            // 查找后结构：先查开始日等于前一天日号，没有再查开始日等于排程日期日号
            MpStructureAllocation nextStructure = findNextStructure(structures, prevStructure,
                    reportDayOfMonth, scheduleDayOfMonth);
            if (nextStructure == null) {
                continue;
            }

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
     * 查找下一个结构切换数据。
     * 优先查找开始日等于前一天日号的结构，若没有则查找开始日等于排程日期日号的结构。
     *
     * @param structures       按beginDay排序的转产数据列表
     * @param prevStructure    前结构（已确定结束日的前结构）
     * @param reportDayOfMonth 前一天的日号
     * @param scheduleDayOfMonth 排程日期的日号
     * @return 下一个结构切换数据，未找到返回null
     */
    private MpStructureAllocation findNextStructure(List<MpStructureAllocation> structures,
                                                    MpStructureAllocation prevStructure,
                                                    int reportDayOfMonth,
                                                    int scheduleDayOfMonth) {
        // 优先查找开始日等于前一天日号的结构
        for (MpStructureAllocation s : structures) {
            if (s == prevStructure) {
                continue;
            }
            if (s.getBeginDay() != null && s.getBeginDay() == reportDayOfMonth) {
                return s;
            }
        }
        // 前一天日号没有匹配，再查找开始日等于排程日期日号的结构
        for (MpStructureAllocation s : structures) {
            if (s == prevStructure) {
                continue;
            }
            if (s.getBeginDay() != null && s.getBeginDay() == scheduleDayOfMonth) {
                return s;
            }
        }
        return null;
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

    /**
     * 从系统参数表（T_MP_FACTORY_PARAM）加载参数值
     *
     * @param factoryCode     分厂编码
     * @param productTypeCode 产品品类编码（可为null）
     * @param paramCode       参数编码
     * @return 参数值，未找到返回null
     */
    private String loadFactoryParamValue(String factoryCode, String productTypeCode, String paramCode) {
        try {
            LambdaQueryWrapper<FactoryParam> wrapper = new LambdaQueryWrapper<FactoryParam>()
                    .eq(FactoryParam::getFactoryCode, factoryCode)
                    .eq(FactoryParam::getParamCode, paramCode)
                    .eq(FactoryParam::getIsDelete, "0");
            if (productTypeCode != null) {
                wrapper.eq(FactoryParam::getProductTypeCode, productTypeCode);
            }
            FactoryParam param = factoryParamMapper.selectOne(wrapper);
            if (param != null && param.getParamValue() != null && !param.getParamValue().trim().isEmpty()) {
                return param.getParamValue().trim();
            }
        } catch (Exception e) {
            log.warn("从T_MP_FACTORY_PARAM加载参数失败: factoryCode={}, paramCode={}, error={}",
                    factoryCode, paramCode, e.getMessage());
        }
        return null;
    }
}
