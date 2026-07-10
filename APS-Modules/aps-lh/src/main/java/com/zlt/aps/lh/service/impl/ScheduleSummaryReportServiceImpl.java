package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.core.domain.ExcelCellRangeAddress;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.cx.entity.config.CxParamConfig;
import com.zlt.aps.cx.entity.schedule.CxPrecisionPlan;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
import com.zlt.aps.lh.api.domain.entity.LhPrecisionPlan;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhShiftConfig;
import com.zlt.aps.lh.api.domain.vo.ScheduleSummaryReportVO;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.api.enums.MouldChangeTypeEnum;
import com.zlt.aps.lh.mapper.*;
import com.zlt.aps.lh.service.IScheduleSummaryReportService;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.util.LhSingleControlMachineUtil;
import com.zlt.aps.lh.util.MachineStatusUtil;
import com.zlt.aps.maindata.mapper.FactoryParamMapper;
import com.zlt.aps.maindata.mapper.LhMachineInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialConsumeDetailMapper;
import com.zlt.aps.mdm.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.FactoryParam;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.service.IMpStructureAllocationRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
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
 *   <li>{cxSetupInfo} - 试制规格（成型3/4/5班次有排计划量且示方书类型=X的物料描述）</li>
 *   <li>{cxTrialInfo} - 量试规格（成型3/4/5班次有排计划量且示方书类型=T的物料描述）</li>
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
 *   <li>{.specPattern} - 主胎胚描述（按示方类型分组，每组前加标题前缀，如"正规 Chinh quy：胎胚描述"，多个用","隔开）</li>
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

    /**
     * 新模板（T+2）小胶种列表起始列：G 列（索引6）
     * 原模板（T+1）小胶种列表在 A 列（索引0），新模板在 G 列左右分布
     */
    private static final int SMALL_RUBBER_START_COL_T2 = 6;

    private static final int SMALL_RUBBER_END_COL_T2 = 6;

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
    private LhMachineInfoEntityMapper lhMachineInfoEntityMapper;

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

        // 排产小结导出：排程日期为T+1日，一条排程记录包含8个班次数据
        // T+1区域取class3/4/5班次（排程日期当天夜/早/中），T+2区域取class6/7/8班次（排程日期+1天夜/早/中）
        log.info("构建排产小结导出数据, 排程日期(报告日期): {}, 分厂: {}",
                DateUtil.formatDate(scheduleDate), factoryCode);

        List<LhShiftConfig> shiftConfigs = this.loadShiftConfigs(factoryCode);
        Map<Integer, String> classShiftTypeMap = this.buildClassShiftTypeMap(shiftConfigs);

        // 排程数据只查一次（排程日期=T+1），T+1和T+2从同一份结果中按不同班次取数
        List<CxScheduleResult> cxResults = cxScheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, scheduleDate)
                        .eq(CxScheduleResult::getFactoryCode, factoryCode));
        log.info("成型排程结果查询完成, 排程日期: {}, 数量: {}", DateUtil.formatDate(scheduleDate), cxResults.size());

        List<LhScheduleResult> lhResults = cxLhScheduleResultMapper.selectList(
                new LambdaQueryWrapper<LhScheduleResult>()
                        .eq(LhScheduleResult::getScheduleDate, scheduleDate)
                        .eq(LhScheduleResult::getFactoryCode, factoryCode)
                        .eq(LhScheduleResult::getIsDelete, DeleteFlagEnum.NORMAL.getCode()));
        log.info("硫化排程结果查询完成, 排程日期: {}, 数量: {}", DateUtil.formatDate(scheduleDate), lhResults.size());

        // 原模板（左侧，无后缀）：T+1数据，取class3/4/5班次
        Date scheduleDateT2 = DateUtil.offsetDay(scheduleDate, 1);
        // 硫化机台单双模(模台数)映射：用于硫化开动机台数统计时合并K1501L/K1501R等单控机台
        Map<String, Integer> machineMaxMouldMap = this.loadMachineMaxMouldMap(factoryCode);
        Map<String, Object> tableMap = this.buildTableMapFromResults(
                scheduleDate, scheduleDate, scheduleDateT2, factoryCode,
                classShiftTypeMap, cxResults, lhResults, "", machineMaxMouldMap);

        // 新模板（右侧，后缀2）：T+2数据，从同一份排程结果中取class6/7/8班次
        Map<String, Object> tableMapT2 = this.buildTableMapFromResults(
                scheduleDateT2, scheduleDate, scheduleDateT2, factoryCode,
                classShiftTypeMap, cxResults, lhResults, "2", machineMaxMouldMap);
        tableMap.putAll(tableMapT2);

        // 模具交替/清洗信息：一次性查询该排程批次所有数据，按 planDate 分组到 T+1/T+2 栏位
        // 查询口径与硫化日计划导出"硫化换模计划"tab页一致：SCHEDULE_DATE = 排程目标日(T+1)
        Map<String, Object> mouldChangeAndCleanMap = this.buildMouldChangeAndCleanInfo(
                scheduleDate, scheduleDateT2, factoryCode);
        tableMap.putAll(mouldChangeAndCleanMap);

        // 小胶种列表：T+1 和 T+2 从同一份排程结果中按不同班次过滤，按胶种做 full outer join
        List<Map<String, Object>> smallRubberList = this.buildMergedSmallRubberList(
                scheduleDate, scheduleDateT2, factoryCode, cxResults);
        List<List<Map<String, Object>>> dataList = new ArrayList<>();
        dataList.add(smallRubberList);

        // 小胶种列表数据处理：
        // - 两边都无数据时隐藏第7行
        // - 有数据时按合并后行数同时合并 A 列（T+1）和 G 列（T+2）区域
        if (smallRubberList.isEmpty()) {
            List<Integer> hiddenRows = new ArrayList<>();
            hiddenRows.add(SMALL_RUBBER_TITLE_ROW_INDEX);
            tableMap.put(ExcelUtils.HIDDEN_ROWS, hiddenRows);
        } else {
            List<ExcelCellRangeAddress> rangeAddressList = new ArrayList<>();
            int endRowIndex = SMALL_RUBBER_TITLE_ROW_INDEX + smallRubberList.size() - 1;
            // T+1 区域（A 列）
            rangeAddressList.add(new ExcelCellRangeAddress(
                    SMALL_RUBBER_TITLE_ROW_INDEX,
                    endRowIndex,
                    SMALL_RUBBER_START_COL,
                    SMALL_RUBBER_END_COL));
            // T+2 区域（G 列）
            rangeAddressList.add(new ExcelCellRangeAddress(
                    SMALL_RUBBER_TITLE_ROW_INDEX,
                    endRowIndex,
                    SMALL_RUBBER_START_COL_T2,
                    SMALL_RUBBER_END_COL_T2));
            tableMap.put(ExcelUtils.RANGE_ADDRESS, rangeAddressList);
        }

        Map<String, Object> result = new HashMap<>(4);
        result.put("tableMap", tableMap);
        result.put("dataList", dataList);
        return result;
    }

    /**
     * 构建合并后的小胶种列表（T+1 与 T+2 按胶种 full outer join）。
     *
     * <p>同一行 Map 同时包含两套 key：</p>
     * <ul>
     *   <li>T+1: rubberTypeName、specPattern（填到模板 A 列起始区域）</li>
     *   <li>T+2: rubberTypeName2、specPattern2（填到模板 G 列起始区域）</li>
     * </ul>
     *
     * <p>按胶种编码做合并：T+1 和 T+2 出现的胶种取并集，T+1 缺失时仅填 T+2 的 key，
     * T+2 缺失时仅填 T+1 的 key，两边都有则同时填充。height 字段两个模板共用一行高度。</p>
     *
     * @param scheduleDateT1 T+1 排程日期
     * @param scheduleDateT2 T+2 排程日期（排程日期+1天）
     * @param factoryCode    分厂编码
     * @param cxResults      预查询的成型排程结果（排程日期=T+1）
     * @return 合并后的小胶种行数据列表（两边都无数据返回空列表）
     */
    private List<Map<String, Object>> buildMergedSmallRubberList(Date scheduleDateT1, Date scheduleDateT2,
                                                                  String factoryCode, List<CxScheduleResult> cxResults) {
        List<Map<String, Object>> listT1 = this.buildSmallRubberList(scheduleDateT1, factoryCode, cxResults, "");
        List<Map<String, Object>> listT2 = this.buildSmallRubberList(scheduleDateT1, factoryCode, cxResults, "2");

        if (listT1.isEmpty() && listT2.isEmpty()) {
            return new ArrayList<>();
        }

        // 按胶种编码做 full outer join，保留 T+1 的胶种顺序，T+2 独有的胶种追加在末尾
        Map<String, Map<String, Object>> mergedByRubber = new LinkedHashMap<>();

        // 先放入 T+1 的数据
        for (Map<String, Object> item : listT1) {
            String rubberType = String.valueOf(item.get("rubberTypeName"));
            Map<String, Object> row = new HashMap<>();
            row.put("rubberTypeName", item.get("rubberTypeName"));
            row.put("specPattern", item.get("specPattern"));
            row.put("height", 30);
            mergedByRubber.put(rubberType, row);
        }

        // 再合并 T+2 的数据，T+2 独有的胶种追加在末尾
        for (Map<String, Object> item : listT2) {
            String rubberType = String.valueOf(item.get("rubberTypeName"));
            Map<String, Object> row = mergedByRubber.computeIfAbsent(rubberType, k -> {
                Map<String, Object> r = new HashMap<>();
                r.put("height", 30);
                return r;
            });
            row.put("rubberTypeName2", item.get("rubberTypeName"));
            row.put("specPattern2", item.get("specPattern"));
        }

        return new ArrayList<>(mergedByRubber.values());
    }

    /**
     * 基于预查询的排程结果构建模板参数映射表。
     *
     * <p>T+1和T+2共用同一份排程数据（排程日期=T+1），通过 keySuffix 区分取哪组班次：</p>
     * <ul>
     *   <li>keySuffix="" → T+1参数（取class3/4/5班次），如 cxNightQty、lhMorningQty</li>
     *   <li>keySuffix="2" → T+2参数（取class6/7/8班次），如 cxNightQty2、lhMorningQty2</li>
     * </ul>
     *
     * @param reportDate         报告日期（标题日期、规格切换、模具等用此日期）
     * @param actualScheduleDate 排程数据查询日期（始终为T+1排程日期）
     * @param scheduleDateT2     T+2日期（排程日期+1天，用于精度备注筛选planDate）
     * @param factoryCode        分厂编码
     * @param classShiftTypeMap  班次类型映射
     * @param cxResults          预查询的成型排程结果
     * @param lhResults          预查询的硫化排程结果
     * @param keySuffix          key 后缀（"" 或 "2"）
     * @param machineMaxMouldMap 硫化机台单双模(模台数)映射，key=机台编号(大写), value=单双模值；
     *                           用于硫化开动机台数统计时合并K1501L/K1501R等单控机台
     * @return 模板参数映射
     */
    private Map<String, Object> buildTableMapFromResults(Date reportDate, Date actualScheduleDate, Date scheduleDateT2,
                                                         String factoryCode, Map<Integer, String> classShiftTypeMap,
                                                         List<CxScheduleResult> cxResults, List<LhScheduleResult> lhResults,
                                                         String keySuffix, Map<String, Integer> machineMaxMouldMap) {
        Map<String, Object> map = new HashMap<>(32);

        map.put("titleDate" + keySuffix, DateUtil.format(reportDate, "MM月dd日") + "计划排产\n"
                + "Ke hoach san xuat ngay " + DateUtil.format(reportDate, "dd/MM"));

        // 成型产量：根据 keySuffix 选择不同班次
        // keySuffix="" → class3/4/5（T+1的夜/早/中班），keySuffix="2" → class6/7/8（T+2的夜/早/中班）
        BigDecimal cxNightTotal = BigDecimal.ZERO;
        BigDecimal cxMorningTotal = BigDecimal.ZERO;
        BigDecimal cxMiddleTotal = BigDecimal.ZERO;

        if ("".equals(keySuffix)) {
            for (CxScheduleResult result : cxResults) {
                cxNightTotal = cxNightTotal.add(nvl(result.getClass3PlanQty()));
                cxMorningTotal = cxMorningTotal.add(nvl(result.getClass4PlanQty()));
                cxMiddleTotal = cxMiddleTotal.add(nvl(result.getClass5PlanQty()));
            }
        } else {
            for (CxScheduleResult result : cxResults) {
                cxNightTotal = cxNightTotal.add(nvl(result.getClass6PlanQty()));
                cxMorningTotal = cxMorningTotal.add(nvl(result.getClass7PlanQty()));
                cxMiddleTotal = cxMiddleTotal.add(nvl(result.getClass8PlanQty()));
            }
        }

        // 成型计划量为BigDecimal，直接toString()会保留小数点后00（如"100.00"），
        // 需转为整数字符串显示（如"100"），与硫化侧Integer类型保持一致
        map.put("cxNightQty" + keySuffix, cxNightTotal.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString());
        map.put("cxMorningQty" + keySuffix, cxMorningTotal.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString());
        map.put("cxMiddleQty" + keySuffix, cxMiddleTotal.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString());
        map.put("cxTotalQty" + keySuffix, cxNightTotal.add(cxMorningTotal).add(cxMiddleTotal).setScale(0, java.math.RoundingMode.HALF_UP).toPlainString());

        log.info("成型排程汇总[keySuffix={}] - 夜班: {}, 早班: {}, 中班: {}, 合计: {}",
                keySuffix, cxNightTotal, cxMorningTotal, cxMiddleTotal,
                cxNightTotal.add(cxMorningTotal).add(cxMiddleTotal));

        // 试制/量试信息：根据 keySuffix 选择班次的示方书类型归集（T=量试，X=试制）
        Set<String> trialSpecs = new LinkedHashSet<>();
        Set<String> setupSpecs = new LinkedHashSet<>();
        for (CxScheduleResult result : cxResults) {
            boolean hasPlanQty = "".equals(keySuffix)
                    ? hasAnyPlanQtyInShift345(result)
                    : hasAnyPlanQtyInShift678(result);
            if (!hasPlanQty) {
                continue;
            }
            String specDesc = StringUtils.defaultString(result.getMaterialDesc()).trim();
            if (StringUtils.isBlank(specDesc)) {
                continue;
            }
            // 根据班次收集有计划量的班次对应的示方书类型
            Set<String> recipeTypes;
            if ("".equals(keySuffix)) {
                recipeTypes = collectRecipeTypesFromShift345(result);
            } else {
                recipeTypes = collectRecipeTypesFromShift678(result);
            }
            recipeTypes.removeIf(StringUtils::isBlank);
            // 按示方书类型归集：T=量试，X=试制
            if (recipeTypes.contains("T")) {
                trialSpecs.add(specDesc);
            }
            if (recipeTypes.contains("X")) {
                setupSpecs.add(specDesc);
            }
        }
        map.put("cxSetupInfo" + keySuffix, setupSpecs.isEmpty() ? "无 Không" : String.join("，", setupSpecs));
        map.put("cxTrialInfo" + keySuffix, trialSpecs.isEmpty() ? "无 Không" : String.join("，", trialSpecs));

        // 成型规格切换：从T_MP_STRUCTURE_ALLOCATION取切换结构数据
        // 需同时传入reportDate和scheduleDate，支持非跨月和跨月两种场景
        map.put("cxSpecSwitch" + keySuffix, this.buildCxSpecSwitch(reportDate, actualScheduleDate, factoryCode));

        // 硫化产量和机台数：根据 keySuffix 限制班次序号范围
        // keySuffix="" → 班次1~5（class1~5映射的01/02/03），keySuffix="2" → 班次6~8（class6~8映射的01/02/03）
        int shiftIndexMin = "".equals(keySuffix) ? 1 : 6;
        int shiftIndexMax = "".equals(keySuffix) ? 5 : 8;

        BigDecimal lhNightTotal = BigDecimal.ZERO;
        BigDecimal lhMorningTotal = BigDecimal.ZERO;
        BigDecimal lhMiddleTotal = BigDecimal.ZERO;

        for (LhScheduleResult result : lhResults) {
            lhNightTotal = lhNightTotal.add(sumLhQtyByShiftTypeInRange(result, classShiftTypeMap, "01", shiftIndexMin, shiftIndexMax));
            lhMorningTotal = lhMorningTotal.add(sumLhQtyByShiftTypeInRange(result, classShiftTypeMap, "02", shiftIndexMin, shiftIndexMax));
            lhMiddleTotal = lhMiddleTotal.add(sumLhQtyByShiftTypeInRange(result, classShiftTypeMap, "03", shiftIndexMin, shiftIndexMax));
        }

        long nightMachines = countLhMachinesByShiftTypeInRange(lhResults, classShiftTypeMap, "01", shiftIndexMin, shiftIndexMax, machineMaxMouldMap);
        long morningMachines = countLhMachinesByShiftTypeInRange(lhResults, classShiftTypeMap, "02", shiftIndexMin, shiftIndexMax, machineMaxMouldMap);
        long middleMachines = countLhMachinesByShiftTypeInRange(lhResults, classShiftTypeMap, "03", shiftIndexMin, shiftIndexMax, machineMaxMouldMap);

        map.put("lhNightQty" + keySuffix, lhNightTotal.toString());
        map.put("lhMorningQty" + keySuffix, lhMorningTotal.toString());
        map.put("lhMiddleQty" + keySuffix, lhMiddleTotal.toString());
        map.put("lhTotalQty" + keySuffix, lhNightTotal.add(lhMorningTotal).add(lhMiddleTotal).toString());
        map.put("lhNightMachines" + keySuffix, String.valueOf(nightMachines));
        map.put("lhMorningMachines" + keySuffix, String.valueOf(morningMachines));
        map.put("lhMiddleMachines" + keySuffix, String.valueOf(middleMachines));
        // 硫化开动合计列取夜、早、中三个班次开动机台数的最大值(非求和)，与业务口径一致
        long maxMachines = Math.max(Math.max(nightMachines, morningMachines), middleMachines);
        map.put("lhTotalMachines" + keySuffix, String.valueOf(maxMachines));

        log.info("硫化排程汇总[keySuffix={}] - 夜班: {}, 早班: {}, 中班: {}, 合计: {}, 开动机台 - 夜: {}, 早: {}, 中: {}, 合计(取最大值): {}",
                keySuffix, lhNightTotal, lhMorningTotal, lhMiddleTotal,
                lhNightTotal.add(lhMorningTotal).add(lhMiddleTotal),
                nightMachines, morningMachines, middleMachines,
                maxMachines);

        // 模具交替/清洗信息已由外层 buildMouldChangeAndCleanInfo 统一查询并按 planDate 分组注入，
        // 不再在此处逐个日期查询，避免 T+2 栏位因 scheduleDate 过滤条件错误导致查不到数据

        // 精度备注：T+1按排程日期查精度计划，T+2按排程日期查精度计划后筛出planDate为T+2的数据
        if ("".equals(keySuffix)) {
            map.put("cxRemark" + keySuffix, this.buildCxRemark(reportDate, factoryCode));
            map.put("lhRemark" + keySuffix, this.buildLhRemark(reportDate, factoryCode));
        } else {
            map.put("cxRemark" + keySuffix, this.buildCxRemarkForT2(actualScheduleDate, scheduleDateT2, factoryCode));
            map.put("lhRemark" + keySuffix, this.buildLhRemarkForT2(actualScheduleDate, scheduleDateT2, factoryCode));
        }

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
    private String buildLhSetupOrTrialInfo(List<LhScheduleResult> lhResults, String keyword) {
        String targetCode;
        if ("试制".equals(keyword)) {
            targetCode = ConstructionStageEnum.MEASUREMENT.getStage();
        } else {
            targetCode = ConstructionStageEnum.TRIAL_PRODUCTION.getStage();
        }

        Set<String> matchedSpecs = new LinkedHashSet<>();
        for (LhScheduleResult result : lhResults) {
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
     * 一次性构建 T+1 和 T+2 的模具交替/清洗信息。
     *
     * <p>查询口径与硫化日计划导出"硫化换模计划"tab页一致：
     * SCHEDULE_DATE = 排程目标日(T+1)，不按 planDate 过滤，一次查出该排程批次下所有模具交替计划，
     * 再按 planDate 落地日期分组到 T+1/T+2 栏位。</p>
     *
     * <p>数据库字段语义（由 ResultValidationHandler.generateMouldChangePlan 生成）：</p>
     * <ul>
     *   <li>SCHEDULE_DATE：排程目标日(T+1)，同一次排程生成的所有计划该字段相同</li>
     *   <li>PLAN_DATE：真实换模执行时间，按排程窗口分布在 T+1、T+2 两天</li>
     * </ul>
     *
     * @param scheduleDateT1 排程目标日 T+1（如7月2日）
     * @param scheduleDateT2 T+2（如7月3日）
     * @param factoryCode    分厂编码
     * @return 包含 mouldChangeInfo/mouldChangeInfo2/mouldCleanDate/mouldCleanDate2/mouldCleanInfo/mouldCleanInfo2 的Map
     */
    private Map<String, Object> buildMouldChangeAndCleanInfo(Date scheduleDateT1, Date scheduleDateT2, String factoryCode) {
        Map<String, Object> map = new HashMap<>(8);

        // 一次查出该排程批次所有模具交替计划
        // 查询口径与硫化日计划导出"硫化换模计划"tab页完全一致：
        // 项目未配置 MyBatis-Plus 全局逻辑删除，tab 页 selectList 不自动过滤 is_delete，故此处也不加该条件；
        // changeMouldType 不在 SQL 层过滤，改为在内存中按 01/02→交替、03/04→清洗 分组，避免漏掉脏数据。
        List<LhMouldChangePlan> allPlans = lhMouldChangePlanEntityMapper.selectList(
                new LambdaQueryWrapper<LhMouldChangePlan>()
                        .eq(LhMouldChangePlan::getFactoryCode, factoryCode)
                        .eq(LhMouldChangePlan::getScheduleDate, scheduleDateT1));
        log.info("模具交替/清洗计划查询完成, 排程目标日(T+1): {}, 分厂: {}, 总数量: {}",
                DateUtil.formatDate(scheduleDateT1), factoryCode, allPlans.size());

        // 按 planDate 落地日期划分 T+1 / T+2 时间范围
        Date t1Start = LhScheduleTimeUtil.clearTime(scheduleDateT1);
        Date t1End = LhScheduleTimeUtil.getEndTime(scheduleDateT1);
        Date t2Start = LhScheduleTimeUtil.clearTime(scheduleDateT2);
        Date t2End = LhScheduleTimeUtil.getEndTime(scheduleDateT2);

        // T+1 模具交替机台（planDate 在 T+1 当天，更换类型 01/02）
        String mouldChangeInfoT1 = allPlans.stream()
                .filter(p -> this.isPlanDateInRange(p, t1Start, t1End))
                .filter(p -> MouldChangeTypeEnum.containsAnyCode(p.getChangeMouldType(),
                        MouldChangeTypeEnum.REGULAR.getCode(), MouldChangeTypeEnum.TYPE_BLOCK.getCode()))
                .map(LhMouldChangePlan::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .limit(15)
                .collect(Collectors.joining(";"));

        // T+2 模具交替机台（planDate 在 T+2 当天，更换类型 01/02）
        String mouldChangeInfoT2 = allPlans.stream()
                .filter(p -> this.isPlanDateInRange(p, t2Start, t2End))
                .filter(p -> MouldChangeTypeEnum.containsAnyCode(p.getChangeMouldType(),
                        MouldChangeTypeEnum.REGULAR.getCode(), MouldChangeTypeEnum.TYPE_BLOCK.getCode()))
                .map(LhMouldChangePlan::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .limit(15)
                .collect(Collectors.joining(";"));

        // T+1 模具清洗机台（planDate 在 T+1 当天，更换类型 03/04）
        String mouldCleanInfoT1 = allPlans.stream()
                .filter(p -> this.isPlanDateInRange(p, t1Start, t1End))
                .filter(p -> MouldChangeTypeEnum.containsAnyCode(p.getChangeMouldType(),
                        MouldChangeTypeEnum.SAND_BLAST.getCode(), MouldChangeTypeEnum.DRY_ICE.getCode()))
                .map(LhMouldChangePlan::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(";"));

        // T+2 模具清洗机台（planDate 在 T+2 当天，更换类型 03/04）
        String mouldCleanInfoT2 = allPlans.stream()
                .filter(p -> this.isPlanDateInRange(p, t2Start, t2End))
                .filter(p -> MouldChangeTypeEnum.containsAnyCode(p.getChangeMouldType(),
                        MouldChangeTypeEnum.SAND_BLAST.getCode(), MouldChangeTypeEnum.DRY_ICE.getCode()))
                .map(LhMouldChangePlan::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining(";"));

        log.info("模具交替/清洗分组结果 - T+1交替: [{}], T+2交替: [{}], T+1清洗: [{}], T+2清洗: [{}]",
                mouldChangeInfoT1, mouldChangeInfoT2, mouldCleanInfoT1, mouldCleanInfoT2);

        map.put("mouldChangeInfo", mouldChangeInfoT1);
        map.put("mouldChangeInfo2", mouldChangeInfoT2);
        map.put("mouldCleanDate", DateUtil.format(scheduleDateT1, "MM月dd日"));
        map.put("mouldCleanDate2", DateUtil.format(scheduleDateT2, "MM月dd日"));
        map.put("mouldCleanInfo", mouldCleanInfoT1);
        map.put("mouldCleanInfo2", mouldCleanInfoT2);
        return map;
    }

    /**
     * 判断模具交替计划的计划日期是否在指定时间范围内
     *
     * @param plan     模具交替计划
     * @param start    范围起始时间（00:00:00）
     * @param end      范围结束时间（23:59:59）
     * @return true=计划日期在范围内
     */
    private boolean isPlanDateInRange(LhMouldChangePlan plan, Date start, Date end) {
        if (plan == null || plan.getPlanDate() == null) {
            return false;
        }
        return !plan.getPlanDate().before(start) && !plan.getPlanDate().after(end);
    }



    /**
     * 构建小胶种列表数据（按胶种分组的行数据）。
     *
     * <p>取数逻辑：</p>
     * <ol>
     *   <li>从系统参数表读取TD胶种类型编码（PARAM_CODE=SYS04010002）</li>
     *   <li>从原材料消耗明细表按胶种类型查对应的胎胚（CHILD_MATERIAL_NAME='AQ'+胶种类型）</li>
     *   <li>匹配本次成型排程结果中的胎胚</li>
     *   <li>通过胎胚编号关联物料主数据取主胎胚描述(embryoDesc)，仅保留本次实际排产的物料</li>
     *   <li>按胶种分组，多个主胎胚描述用","隔开</li>
     *   <li>按示方类型（正规/量试/试制）在物料级别分组，每组主胎胚描述前加标题前缀，不同组用换行隔开</li>
     * </ol>
     *
     * <p>通过 keySuffix 区分 T+1/T+2 的班次过滤：
     * keySuffix="" 过滤 class3/4/5 有计划量的胎胚，keySuffix="2" 过滤 class6/7/8。</p>
     *
     * @param scheduleDate 排程日期（用于日志，实际数据来自预查询的cxResults）
     * @param factoryCode  分厂编码
     * @param cxResults    预查询的成型排程结果
     * @param keySuffix    key 后缀（"" 过滤class3/4/5，"2" 过滤class6/7/8）
     * @return 小胶种行数据列表（无数据返回空列表）
     */
    private List<Map<String, Object>> buildSmallRubberList(Date scheduleDate, String factoryCode,
                                                            List<CxScheduleResult> cxResults, String keySuffix) {
        List<Map<String, Object>> smallRubberList = new ArrayList<>();

        List<String> rubberTypeCodes = this.loadRubberTypeCodes(factoryCode);
        if (rubberTypeCodes.isEmpty()) {
            log.warn("未配置TD胶种类型编码（SYS04010002），小胶种列表为空");
            return smallRubberList;
        }

        // 根据 keySuffix 过滤有排计划量的胎胚
        // keySuffix="" → class3/4/5（T+1），keySuffix="2" → class6/7/8（T+2）
        log.info("构建小胶种列表[keySuffix={}], 排程日期: {}", keySuffix, scheduleDate);

        Set<String> scheduleEmbryoCodes = cxResults.stream()
                .filter("".equals(keySuffix) ? this::hasAnyPlanQtyInShift345 : this::hasAnyPlanQtyInShift678)
                .map(CxScheduleResult::getEmbryoCode)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());

        // 收集已排产的物料编码集合，用于过滤物料主数据中未排产的SKU
        // 注意：CxScheduleResult.materialCode 可能是逗号分隔的多个物料编码（排程生成时按胎胚+机台合并），
        // 需拆分后放入Set，否则 scheduledMaterialCodes.contains(单个物料) 会匹配失败
        Set<String> scheduledMaterialCodes = cxResults.stream()
                .filter("".equals(keySuffix) ? this::hasAnyPlanQtyInShift345 : this::hasAnyPlanQtyInShift678)
                .map(CxScheduleResult::getMaterialCode)
                .filter(StringUtils::isNotBlank)
                .flatMap(mc -> Arrays.stream(mc.split("[,，]")))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
        log.info("小胶种[keySuffix={}]已排产胎胚代码数量: {}, 已排产物料编码数量: {}",
                keySuffix, scheduleEmbryoCodes.size(), scheduledMaterialCodes.size());

        if (scheduleEmbryoCodes.isEmpty()) {
            log.warn("成型排程结果中无胎胚代码，小胶种列表为空[keySuffix={}]", keySuffix);
            return smallRubberList;
        }

        // 构建物料→示方类型集合映射（根据keySuffix选择班次范围）
        Map<String, Set<String>> materialRecipeTypeMap = this.buildMaterialRecipeTypeMap(cxResults, keySuffix);
        log.info("物料示方类型映射构建完成[keySuffix={}], 映射数量: {}", keySuffix, materialRecipeTypeMap.size());

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

            // 按示方类型顺序构建主胎胚描述字符串：正规 → 量试 → 试制
            // 取数口径：取物料主数据中的胎胚描述(embryoDesc)作为主胎胚描述，按胎胚描述去重
            List<String> recipeTypeOrder = Arrays.asList("S", "T", "X");
            List<String> groupParts = new ArrayList<>();

            for (String recipeType : recipeTypeOrder) {
                List<MdmMaterialInfo> materialsForType = recipeTypeMaterialGroupMap.get(recipeType);
                if (materialsForType == null || materialsForType.isEmpty()) {
                    continue;
                }

                // 按主胎胚描述去重（同一胎胚下多条物料记录的胎胚描述相同，去重后保留唯一值）
                Set<String> embryoDescSet = new LinkedHashSet<>();
                for (MdmMaterialInfo materialInfo : materialsForType) {
                    String embryoDesc = StringUtils.defaultString(materialInfo.getEmbryoDesc()).trim();
                    if (StringUtils.isBlank(embryoDesc)) {
                        continue;
                    }
                    embryoDescSet.add(embryoDesc);
                }

                if (embryoDescSet.isEmpty()) {
                    continue;
                }

                String title = this.getRecipeTypeTitle(recipeType);
                groupParts.add(title + String.join(",", embryoDescSet));
            }

            Map<String, Object> item = new HashMap<>();
            item.put("rubberTypeName", rubberType);
            item.put("specPattern", String.join("\n", groupParts));
            item.put("height", 30);
            smallRubberList.add(item);
        }

        return smallRubberList;
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
     * 判断成型排程结果的6、7、8班次中是否有任意一个班次排了计划量。
     * 6、7、8班次对应排程日期+1天（T+2）的夜、早、中班次，
     * 三个班次都没排计划量的胎胚需要过滤掉。
     *
     * @param result 成型排程结果
     * @return true=至少一个班次有计划量，false=三个班次都没计划量
     */
    private boolean hasAnyPlanQtyInShift678(CxScheduleResult result) {
        return nvl(result.getClass6PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || nvl(result.getClass7PlanQty()).compareTo(BigDecimal.ZERO) > 0
                || nvl(result.getClass8PlanQty()).compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 收集成型排程结果3/4/5班次中有计划量的班次对应的示方书类型
     *
     * @param result 成型排程结果
     * @return 示方书类型集合
     */
    private Set<String> collectRecipeTypesFromShift345(CxScheduleResult result) {
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
        return recipeTypes;
    }

    /**
     * 收集成型排程结果6/7/8班次中有计划量的班次对应的示方书类型
     *
     * @param result 成型排程结果
     * @return 示方书类型集合
     */
    private Set<String> collectRecipeTypesFromShift678(CxScheduleResult result) {
        Set<String> recipeTypes = new HashSet<>();
        if (nvl(result.getClass6PlanQty()).compareTo(BigDecimal.ZERO) > 0) {
            recipeTypes.add(StringUtils.defaultString(result.getClass6RecipeType()));
        }
        if (nvl(result.getClass7PlanQty()).compareTo(BigDecimal.ZERO) > 0) {
            recipeTypes.add(StringUtils.defaultString(result.getClass7RecipeType()));
        }
        if (nvl(result.getClass8PlanQty()).compareTo(BigDecimal.ZERO) > 0) {
            recipeTypes.add(StringUtils.defaultString(result.getClass8RecipeType()));
        }
        return recipeTypes;
    }

    /**
     * 构建物料→示方类型集合映射
     *
     * <p>根据成型排程结果的班次示方书类型确定每个物料的示方类型。
     * 通过 keySuffix 区分班次范围：keySuffix="" 取3/4/5班次，keySuffix="2" 取6/7/8班次。
     * 同一胎胚下不同物料可能有不同的示方类型（如正规和量试），因此按物料级别映射，
     * 确保每种物料都能正确归入对应的示方类型分组。</p>
     *
     * @param cxResults 成型排程结果列表
     * @param keySuffix key 后缀（"" 取class3/4/5，"2" 取class6/7/8）
     * @return 物料编码→示方类型编码集合映射（S-正规，T-量试，X-试制）
     */
    private Map<String, Set<String>> buildMaterialRecipeTypeMap(List<CxScheduleResult> cxResults, String keySuffix) {
        Map<String, Set<String>> materialRecipeTypeMap = new HashMap<>();
        for (CxScheduleResult result : cxResults) {
            boolean hasPlanQty = "".equals(keySuffix)
                    ? hasAnyPlanQtyInShift345(result)
                    : hasAnyPlanQtyInShift678(result);
            if (!hasPlanQty) {
                continue;
            }
            String materialCode = result.getMaterialCode();
            if (StringUtils.isBlank(materialCode)) {
                continue;
            }

            // 根据keySuffix选择班次范围收集示方书类型
            Set<String> recipeTypes = "".equals(keySuffix)
                    ? collectRecipeTypesFromShift345(result)
                    : collectRecipeTypesFromShift678(result);

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
    private BigDecimal sumLhQtyByShiftType(LhScheduleResult result,
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
    private long countLhMachinesByShiftType(List<LhScheduleResult> results,
                                            Map<Integer, String> classShiftTypeMap, String shiftType) {
        return results.stream()
                .filter(r -> hasNonZeroQtyForShiftType(r, classShiftTypeMap, shiftType))
                .count();
    }

    /**
     * 按班次类型和班次序号范围汇总硫化产量。
     * 用于区分T+1（班次1~5）和T+2（班次6~8）的硫化数据。
     *
     * @param result           硫化排程结果
     * @param classShiftTypeMap 班次类型映射
     * @param shiftType        班次类型（01-夜，02-早，03-中）
     * @param shiftIndexMin    班次序号下限（含）
     * @param shiftIndexMax    班次序号上限（含）
     * @return 指定范围内该班次类型的产量合计
     */
    private BigDecimal sumLhQtyByShiftTypeInRange(LhScheduleResult result,
                                                   Map<Integer, String> classShiftTypeMap,
                                                   String shiftType, int shiftIndexMin, int shiftIndexMax) {
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Integer, String> entry : classShiftTypeMap.entrySet()) {
            int shiftIndex = entry.getKey();
            // 限制班次序号范围：T+1取1~5，T+2取6~8
            if (shiftIndex < shiftIndexMin || shiftIndex > shiftIndexMax) {
                continue;
            }
            if (shiftType.equals(entry.getValue())) {
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
     * 按班次类型和班次序号范围统计硫化开动机台数。
     * 用于区分T+1（班次1~5）和T+2（班次6~8）的硫化数据。
     *
     * <p>统计规则：</p>
     * <ol>
     *   <li>收集该班次类型下"计划量>0"的机台号(去重，同一机台多条记录只算1条开动)；</li>
     *   <li>为每个机台号生成"计数标识"：
     *     <ul>
     *       <li>单控机台(编码以L/R结尾)且单双模(模台数)=1 → 用物理机台号(去L/R后缀)，
     *           使K1501L/K1501R归并为K1501，只算1条开动；</li>
     *       <li>其余 → 用原机台号。</li>
     *     </ul>
     *   </li>
     *   <li>去重后的计数标识数即为开动机台数。</li>
     * </ol>
     *
     * @param results            硫化排程结果列表
     * @param classShiftTypeMap  班次类型映射
     * @param shiftType          班次类型（01-夜，02-早，03-中）
     * @param shiftIndexMin      班次序号下限（含）
     * @param shiftIndexMax      班次序号上限（含）
     * @param machineMaxMouldMap 硫化机台单双模(模台数)映射，key=机台编号(大写), value=单双模值；
     *                           机台主数据缺失时按"不合并"保守计数
     * @return 在指定范围内该班次类型有计划产量的开动机台数(已按物理机台归并)
     */
    private long countLhMachinesByShiftTypeInRange(List<LhScheduleResult> results,
                                                    Map<Integer, String> classShiftTypeMap,
                                                    String shiftType, int shiftIndexMin, int shiftIndexMax,
                                                    Map<String, Integer> machineMaxMouldMap) {
        // 步骤1: 收集该班次类型下"计划量>0"的机台号(去重，同一机台多条记录只算1条开动)
        Set<String> activeMachineCodes = results.stream()
                .filter(r -> hasNonZeroQtyForShiftTypeInRange(r, classShiftTypeMap, shiftType, shiftIndexMin, shiftIndexMax))
                .map(LhScheduleResult::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .map(c -> c.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (activeMachineCodes.isEmpty()) {
            return 0;
        }

        // 步骤2: 为每个机台号生成"计数标识"
        //   - 单控(L/R结尾)且单双模=1 → 用物理机台号(去L/R后缀), 使K1501L/K1501R归并为K1501
        //   - 其余 → 用原机台号
        Set<String> countKeys = new HashSet<>(activeMachineCodes.size());
        for (String code : activeMachineCodes) {
            Integer maxMoldNum = machineMaxMouldMap.get(code);
            if (LhSingleControlMachineUtil.isSingleMouldMachine(code)
                    && maxMoldNum != null && maxMoldNum == 1) {
                String physicalCode = LhSingleControlMachineUtil.resolvePhysicalMachineCode(code);
                if (StringUtils.isNotBlank(physicalCode)) {
                    countKeys.add(physicalCode);
                }
            } else {
                countKeys.add(code);
            }
        }

        // 步骤3: 去重后的计数标识数即为开动机台数
        return countKeys.size();
    }

    /**
     * 判断某台机器在指定班次类型和班次序号范围内是否有计划产量
     */
    private boolean hasNonZeroQtyForShiftTypeInRange(LhScheduleResult result,
                                                      Map<Integer, String> classShiftTypeMap,
                                                      String shiftType, int shiftIndexMin, int shiftIndexMax) {
        for (Map.Entry<Integer, String> entry : classShiftTypeMap.entrySet()) {
            int shiftIndex = entry.getKey();
            if (shiftIndex < shiftIndexMin || shiftIndex > shiftIndexMax) {
                continue;
            }
            if (shiftType.equals(entry.getValue())) {
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

    /**
     * 判断某台机器在指定班次类型下是否有计划产量
     */
    private boolean hasNonZeroQtyForShiftType(LhScheduleResult result,
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
     * <p>取成型精度计划的排程日期在报告日期时间范围内要做的机台，
     * 成型精度做的时间固定在6:00~14:00</p>
     *
     * @param reportDate  报告日期（即排程日期）
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
     * @param reportDate  报告日期（即排程日期）
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
     * 构建成型备注信息（T+2版本）
     *
     * <p>按T+1排程日期查成型精度计划的scheduleDate，再筛出planDate为T+2的数据。
     * 成型精度做的时间固定在6:00~14:00</p>
     *
     * @param scheduleDateT1 T+1排程日期（查询精度计划的scheduleDate范围）
     * @param scheduleDateT2 T+2日期（筛选精度计划的planDate范围）
     * @param factoryCode    分厂编码
     * @return 成型备注字符串
     */
    private String buildCxRemarkForT2(Date scheduleDateT1, Date scheduleDateT2, String factoryCode) {
        Date t1Start = LhScheduleTimeUtil.clearTime(scheduleDateT1);
        Date t1End = LhScheduleTimeUtil.getEndTime(scheduleDateT1);
        Date t2Start = LhScheduleTimeUtil.clearTime(scheduleDateT2);
        Date t2End = LhScheduleTimeUtil.getEndTime(scheduleDateT2);

        // 按T+1排程日期查精度计划的scheduleDate
        List<CxPrecisionPlan> precisionPlans = cxPrecisionPlanMapper.selectList(
                new LambdaQueryWrapper<CxPrecisionPlan>()
                        .eq(CxPrecisionPlan::getFactoryCode, factoryCode)
                        .ge(CxPrecisionPlan::getScheduleDate, t1Start)
                        .le(CxPrecisionPlan::getScheduleDate, t1End));
        log.info("成型精度计划查询完成(T+2筛选), 排程日期: {}, 分厂: {}, 查询数量: {}",
                DateUtil.formatDate(scheduleDateT1), factoryCode, precisionPlans.size());

        // 从查到的数据中筛出planDate在T+2范围内的记录
        List<String> machineCodes = precisionPlans.stream()
                .filter(p -> p.getPlanDate() != null
                        && !p.getPlanDate().before(t2Start) && !p.getPlanDate().after(t2End))
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
     * 构建硫化备注信息（T+2版本）
     *
     * <p>按T+1排程日期查硫化精度计划的scheduleDate，再筛出planDate为T+2的数据。
     * 硫化精度做的时间及开产时间根据参数计算</p>
     *
     * @param scheduleDateT1 T+1排程日期（查询精度计划的scheduleDate范围）
     * @param scheduleDateT2 T+2日期（筛选精度计划的planDate范围）
     * @param factoryCode    分厂编码
     * @return 硫化备注字符串
     */
    private String buildLhRemarkForT2(Date scheduleDateT1, Date scheduleDateT2, String factoryCode) {
        Date t1Start = LhScheduleTimeUtil.clearTime(scheduleDateT1);
        Date t1End = LhScheduleTimeUtil.getEndTime(scheduleDateT1);
        Date t2Start = LhScheduleTimeUtil.clearTime(scheduleDateT2);
        Date t2End = LhScheduleTimeUtil.getEndTime(scheduleDateT2);

        // 按T+1排程日期查精度计划的scheduleDate
        List<LhPrecisionPlan> precisionPlans = lhPrecisionPlanMapper.selectList(
                new LambdaQueryWrapper<LhPrecisionPlan>()
                        .eq(LhPrecisionPlan::getFactoryCode, factoryCode)
                        .ge(LhPrecisionPlan::getScheduleDate, t1Start)
                        .le(LhPrecisionPlan::getScheduleDate, t1End));
        log.info("硫化精度计划查询完成(T+2筛选), 排程日期: {}, 分厂: {}, 查询数量: {}",
                DateUtil.formatDate(scheduleDateT1), factoryCode, precisionPlans.size());

        // 从查到的数据中筛出planDate在T+2范围内的记录
        List<String> machineCodes = precisionPlans.stream()
                .filter(p -> p.getPlanDate() != null
                        && !p.getPlanDate().before(t2Start) && !p.getPlanDate().after(t2End))
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

        log.info("硫化备注时间计算(T+2) - 保养开始: {}小时, 保养耗时: {}小时, 胶囊预热: {}小时, 保养时段: {}, 开产时间: {}",
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
     * 找到结束日等于排程日期日号的前结构，以及开始日等于排程日期日号或后一天日号的后结构，
     * 只展示第二天切换的数据，展示格式为"前结构 换 后结构"，多个切换用"；"隔开。
     *
     * <p>两种场景：</p>
     * <ul>
     *   <li>非跨月场景：排程日期12号，取当月转产数据，找endDay=12的前结构，
     *       后结构先查beginDay=12，12号没有再查beginDay=13</li>
     *   <li>跨月场景：排程日期6月1号，取6月转产数据，找endDay=1且后结构beginDay=1</li>
     * </ul>
     *
     * @param reportDate   报告日期（即排程日期）
     * @param scheduleDate 排程日期（与reportDate相同）
     * @param factoryCode  分厂编码
     * @return 规格切换信息字符串，如"结构A 换 结构B；结构C 换 结构D"
     */
    private String buildCxSpecSwitch(Date reportDate, Date scheduleDate, String factoryCode) {
        LocalDate localScheduleDate = DateUtil.toLocalDateTime(scheduleDate).toLocalDate();

        // 跨月场景：排程日期是1号时，直接取排程日期所在月的转产数据；否则取排程日期所在月的数据
        LocalDate queryMonthBase = localScheduleDate;

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
        int scheduleDayOfMonth = localScheduleDate.getDayOfMonth();

        for (Map.Entry<String, List<MpStructureAllocation>> entry : machineGroupMap.entrySet()) {
            List<MpStructureAllocation> structures = entry.getValue().stream()
                    .sorted(Comparator.comparing(MpStructureAllocation::getBeginDay, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            // 同一机台必须有2条及以上转产数据才说明有结构切换
            if (structures.size() < 2) {
                continue;
            }

            // 查找前结构：结束日等于排程日期日号
            int prevEndDay = scheduleDayOfMonth;
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

            // 查找后结构：先查开始日等于排程日期日号，没有再查开始日等于排程日期后一天日号
            MpStructureAllocation nextStructure = findNextStructure(structures, prevStructure,
                    scheduleDayOfMonth, scheduleDayOfMonth + 1);
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
     * 优先查找开始日等于排程日期日号的结构，若没有则查找开始日等于排程日期后一天日号的结构。
     *
     * @param structures            按beginDay排序的转产数据列表
     * @param prevStructure         前结构（已确定结束日的前结构）
     * @param scheduleDayOfMonth    排程日期的日号
     * @param nextDayOfMonth        排程日期后一天的日号
     * @return 下一个结构切换数据，未找到返回null
     */
    private MpStructureAllocation findNextStructure(List<MpStructureAllocation> structures,
                                                    MpStructureAllocation prevStructure,
                                                    int scheduleDayOfMonth,
                                                    int nextDayOfMonth) {
        // 优先查找开始日等于排程日期日号的结构
        for (MpStructureAllocation s : structures) {
            if (s == prevStructure) {
                continue;
            }
            if (s.getBeginDay() != null && s.getBeginDay() == scheduleDayOfMonth) {
                return s;
            }
        }
        // 排程日期日号没有匹配，再查找开始日等于排程日期后一天日号的结构
        for (MpStructureAllocation s : structures) {
            if (s == prevStructure) {
                continue;
            }
            if (s.getBeginDay() != null && s.getBeginDay() == nextDayOfMonth) {
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
        // 配置忽略未知属性，避免Feign返回数据含实体未定义字段（如groupKey）时反序列化失败
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

    /**
     * 加载指定分厂启用状态硫化机台的单双模(模台数)映射。
     *
     * <p>用于硫化开动机台数统计时识别单控机台(K1501L/K1501R)是否需合并：</p>
     * <ul>
     *   <li>key 统一为大写并去除首尾空格的机台编号，value 为单双模值；</li>
     *   <li>机台主数据缺失时，调用方按"不合并"保守计数。</li>
     * </ul>
     *
     * @param factoryCode 分厂编码
     * @return key=机台编号(大写去空格), value=单双模(模台数)；无数据返回空Map
     */
    private Map<String, Integer> loadMachineMaxMouldMap(String factoryCode) {
        List<LhMachineInfo> machineList = lhMachineInfoEntityMapper.selectList(
                new LambdaQueryWrapper<LhMachineInfo>()
                        .eq(LhMachineInfo::getFactoryCode, factoryCode)
                        .eq(LhMachineInfo::getStatus, MachineStatusUtil.STATUS_ENABLED));
        Map<String, Integer> map = new HashMap<>(machineList.size());
        for (LhMachineInfo machine : machineList) {
            String code = machine.getMachineCode();
            if (StringUtils.isBlank(code)) {
                continue;
            }
            map.put(code.trim().toUpperCase(Locale.ROOT), machine.getMaxMoldNum());
        }
        log.info("硫化机台单双模映射加载完成, 分厂: {}, 机台数: {}", factoryCode, map.size());
        return map;
    }
}
