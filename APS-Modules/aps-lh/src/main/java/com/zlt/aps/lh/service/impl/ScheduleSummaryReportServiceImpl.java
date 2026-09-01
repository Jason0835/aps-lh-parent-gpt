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
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.lh.api.domain.entity.LhMouldChangePlan;
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
import com.zlt.aps.mdm.api.domain.entity.MdmSkuConstructionRef;
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
 *   <li>{mouldCleanDate} - 模具清洗日期（固定显示排程目标日T+1，如"08月23日"）</li>
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

    /**
     * 硫化精度固定原因项：硫化侧班次分析中真正表示精度保养的固定写法为独立原因项"精度计划"
     * （来自 ResultDowntimeSummaryUtil 的固定常量）。
     * 判定时先按分隔符拆分为独立原因项，再对每项做<b>精确等于</b>匹配：
     * <ul>
     *   <li>"喷砂清洗+精度"属于模具清洗与精度重叠的组合原因，应归入模具清洗栏位展示，精确不等于"精度计划"被排除</li>
     *   <li>"换模+精度计划"属于换模组合原因（历史残留写法），应归入模具交替栏位展示，被排除</li>
     *   <li>"成型精度影响: ..."拆分后无任何独立项等于"精度计划"，天然被排除</li>
     * </ul>
     */
    private static final String LH_PRECISION_ANALYSIS = "精度计划";

    /**
     * 成型精度固定原因项：成型侧精度扣减班次由 buildTaskAnalysis 写入独立原因"精度"
     * （可能与其他原因组合，如"试制,精度"）。判定时先按分隔符拆分为独立原因项，
     * 再对每项做精确等于匹配，硫化侧写法（"精度计划"等）与"成型精度影响"说明文本均不会误命中。
     */
    private static final String CX_PRECISION_ANALYSIS = "精度";

    /**
     * 班次分析文本的分隔符：硫化侧 ShiftFieldUtil 用英文逗号拼接，
     * ProductionCalculator.appendClassAnalysisByIndex 同样用英文逗号，
     * 成型侧结构切换备注用中文分号拼接，统一按两种分隔符拆分为独立原因项
     */
    private static final String ANALYSIS_ITEM_SEPARATOR = "[,；]";

    @Resource
    private CxLhScheduleResultMapper cxLhScheduleResultMapper;

    @Resource
    private CxScheduleResultMapper cxScheduleResultMapper;

    @Resource
    private LhShiftConfigMapper lhShiftConfigMapper;

    @Resource
    private LhMouldChangePlanEntityMapper lhMouldChangePlanEntityMapper;

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

    @Resource
    private MdmSkuConstructionRefMapper mdmSkuConstructionRefMapper;

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
        // SKU与示方书关系映射（物料级类型兜底数据源，T+1/T+2 共用一次查询）
        Map<String, Set<String>> skuEmbryoTypeMap = this.loadSkuEmbryoTypeMap();
        Map<String, Object> tableMap = this.buildTableMapFromResults(
                scheduleDate, scheduleDate, scheduleDateT2, factoryCode,
                classShiftTypeMap, cxResults, lhResults, "", machineMaxMouldMap, skuEmbryoTypeMap);

        // 新模板（右侧，后缀2）：T+2数据，从同一份排程结果中取class6/7/8班次
        Map<String, Object> tableMapT2 = this.buildTableMapFromResults(
                scheduleDateT2, scheduleDate, scheduleDateT2, factoryCode,
                classShiftTypeMap, cxResults, lhResults, "2", machineMaxMouldMap, skuEmbryoTypeMap);
        tableMap.putAll(tableMapT2);

        // 模具交替/清洗信息：一次性查询排程窗口（T+1 ~ T+2）数据，按 planDate 分组到 T+1/T+2 栏位，不含T日
        Map<String, Object> mouldChangeAndCleanMap = this.buildMouldChangeAndCleanInfo(
                scheduleDate, scheduleDateT2, factoryCode);
        tableMap.putAll(mouldChangeAndCleanMap);

        // 精度计划备注：从排程结果表取数，按班次分析原因含"精度"标记分组
        // class3/4/5（排程日期当天，T+1）→ T+1报表栏位
        // class6/7/8（排程日期+1天，T+2）→ T+2报表栏位
        // class1/2（排程日期前一天，T）不在报表展示范围内，忽略
        Map<String, Object> precisionRemarkMap = this.buildPrecisionRemarkInfo(
                cxResults, lhResults, factoryCode);
        tableMap.putAll(precisionRemarkMap);

        // 小胶种列表：T+1 和 T+2 从同一份排程结果中按不同班次过滤，按胶种做 full outer join
        List<Map<String, Object>> smallRubberList = this.buildMergedSmallRubberList(
                scheduleDate, scheduleDateT2, factoryCode, cxResults, skuEmbryoTypeMap);
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
     * @param scheduleDateT1  T+1 排程日期
     * @param scheduleDateT2  T+2 排程日期（排程日期+1天）
     * @param factoryCode     分厂编码
     * @param cxResults       预查询的成型排程结果（排程日期=T+1）
     * @param skuEmbryoTypeMap SKU与示方书关系映射（物料编码→示方类型集合，物料级类型兜底数据源）
     * @return 合并后的小胶种行数据列表（两边都无数据返回空列表）
     */
    private List<Map<String, Object>> buildMergedSmallRubberList(Date scheduleDateT1, Date scheduleDateT2,
                                                                  String factoryCode, List<CxScheduleResult> cxResults,
                                                                  Map<String, Set<String>> skuEmbryoTypeMap) {
        List<Map<String, Object>> listT1 = this.buildSmallRubberList(scheduleDateT1, factoryCode, cxResults, "", skuEmbryoTypeMap);
        List<Map<String, Object>> listT2 = this.buildSmallRubberList(scheduleDateT1, factoryCode, cxResults, "2", skuEmbryoTypeMap);

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
     * @param skuEmbryoTypeMap   SKU与示方书关系映射（物料编码→示方类型集合，物料级类型兜底数据源）
     * @return 模板参数映射
     */
    private Map<String, Object> buildTableMapFromResults(Date reportDate, Date actualScheduleDate, Date scheduleDateT2,
                                                         String factoryCode, Map<Integer, String> classShiftTypeMap,
                                                         List<CxScheduleResult> cxResults, List<LhScheduleResult> lhResults,
                                                         String keySuffix, Map<String, Integer> machineMaxMouldMap,
                                                         Map<String, Set<String>> skuEmbryoTypeMap) {
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
        // 取数口径：优先使用主物料描述(胎胚描述mainMaterialDesc)，若为空则回退使用物料描述(materialDesc)
        Set<String> trialSpecs = new LinkedHashSet<>();
        Set<String> setupSpecs = new LinkedHashSet<>();
        for (CxScheduleResult result : cxResults) {
            boolean hasPlanQty = "".equals(keySuffix)
                    ? hasAnyPlanQtyInShift345(result)
                    : hasAnyPlanQtyInShift678(result);
            if (!hasPlanQty) {
                continue;
            }
            String specDesc = StringUtils.defaultString(result.getMainMaterialDesc()).trim();
            if (StringUtils.isBlank(specDesc)) {
                specDesc = StringUtils.defaultString(result.getMaterialDesc()).trim();
            }
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
            if (recipeTypes.isEmpty()) {
                recipeTypes.add("S");
            }
            // 物料级类型判定：materialCode 可能是逗号分隔的多个物料编码（排程生成时按胎胚+机台合并），
            // 记录级 recipeType 仅代表第一个物料（主物料），其余共用胎胚物料需用SKU关系表的
            // 物料全部类型兜底，否则共用胎胚下的量试/试制物料会被漏统计
            Map<String, Set<String>> materialTypes = this.resolveMaterialTypes(result, recipeTypes, skuEmbryoTypeMap);
            Set<String> allMaterialTypes = materialTypes.values().stream()
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
            // 按示方书类型归集：T=量试，X=试制
            if (allMaterialTypes.contains("T")) {
                trialSpecs.add(specDesc);
            }
            if (allMaterialTypes.contains("X")) {
                setupSpecs.add(specDesc);
            }
        }
        map.put("cxSetupInfo" + keySuffix, setupSpecs.isEmpty() ? "无 Không" : String.join("，", setupSpecs));
        map.put("cxTrialInfo" + keySuffix, trialSpecs.isEmpty() ? "无 Không" : String.join("，", trialSpecs));

        // 成型规格切换：从T_MP_STRUCTURE_ALLOCATION取切换结构数据
        // 用 reportDate 作为查询日期，实现 T+1/T+2 日期隔离：
        //   keySuffix=""  → reportDate=T+1，查T+1的结构切换
        //   keySuffix="2" → reportDate=T+2，查T+2的结构切换
        map.put("cxSpecSwitch" + keySuffix, this.buildCxSpecSwitch(
                reportDate, reportDate, factoryCode, cxResults, classShiftTypeMap, keySuffix));

        // 硫化产量和机台数：根据 keySuffix 限制班次序号范围
        // keySuffix="" → 班次3~5（class3~5映射的01/02/03，与成型T+1班次范围一致），keySuffix="2" → 班次6~8（class6~8映射的01/02/03）
        int shiftIndexMin = "".equals(keySuffix) ? 3 : 6;
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

        // 精度计划备注已由外层 buildPrecisionRemarkInfo 统一查询并按 planDate 分组注入，
        // 不再在此处逐个日期查询，避免 T+1 报表误含 planDate=T+2 的数据

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
     * <p>按真实执行时间 PLAN_DATE 范围（T+1 ~ T+2）查询模具交替/清洗计划，
     * T+1/T+2 栏位各自只取栏位对应当天的数据，不并入T日。</p>
     *
     * <p>查询必须做批次隔离（SCHEDULE_DATE = 排程目标日 T+1）并显式过滤 IS_DELETE=0：
     * 模具交替计划表按排程批次（LH_RESULT_BATCH_NO）管理，同一排程日重跑会生成多个批次、
     * 旧批次置 IS_DELETE=1；且上一排程日批次的6班排程窗口会延伸到次日（如9/1批次仍
     * 存在 PLAN_DATE 落在9/2 的有效记录）。不加这两个条件会把旧批次/已删除批次的机台
     * 混入本报表（2026-08-31 生产库核对：9/2报表T+1交替曾多出 K1204/K1406/K1908）。</p>
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

        // 一次查出本次排程批次窗口内所有模具交替/清洗计划（按真实执行时间 PLAN_DATE 过滤，范围 T+1 ~ T+2）
        // 模具交替/清洗口径（2026-08-31 业务确认）：T+1/T+2 栏位均只取栏位对应当天 PLAN_DATE 的数据，
        // 不并入T日（T日白天的换模/清洗属于前一批次T日生产自己的数据，与T+1报表无关；
        // 此前曾把T日并入T+1栏位，导致T+1报表混入T日白天的机台，已于2026-08-31回滚为仅取当天）。
        // 批次隔离（2026-08-31 生产库逐条核对确认，两个条件缺一不可）：
        //   ① SCHEDULE_DATE = 排程目标日(T+1)：模具交替计划表按排程批次管理，上一排程日批次的6班窗口
        //      会延伸到次日（如9/1批次 LHPC20260901014 仍为有效数据，但其 PLAN_DATE 落在9/2 06:00/14:00
        //      的换模计划属于9/1批次，不应进入9/2报表——9/2报表多出的 K1204/K1406/K1908 即来源于此）；
        //      T+2栏位同样取本批次对次日的预测，与硫化日计划tab页 eq(SCHEDULE_DATE) 的口径一致；
        //   ② IS_DELETE = 0：同一排程日重跑会生成多个批次（如 LHPC20260902001/002 被003取代后已置为
        //      逻辑删除）；项目未配置 MyBatis-Plus 全局逻辑删除，selectList 不会自动过滤 is_delete，
        //      必须显式加该条件，否则已删除批次的机台也会混入；
        // changeMouldType 不在 SQL 层过滤，改为在内存中按 01/02→交替、03/04→清洗 分组，避免漏掉脏数据。
        Date t1Start = LhScheduleTimeUtil.clearTime(scheduleDateT1);
        Date t1End = LhScheduleTimeUtil.getEndTime(scheduleDateT1);
        Date t2Start = LhScheduleTimeUtil.clearTime(scheduleDateT2);
        Date t2End = LhScheduleTimeUtil.getEndTime(scheduleDateT2);
        List<LhMouldChangePlan> allPlans = lhMouldChangePlanEntityMapper.selectList(
                new LambdaQueryWrapper<LhMouldChangePlan>()
                        .eq(LhMouldChangePlan::getFactoryCode, factoryCode)
                        // 批次隔离：只取本次排程（SCHEDULE_DATE=T+1）生成的计划，排除上一排程日批次延伸到次日的残留记录
                        .eq(LhMouldChangePlan::getScheduleDate, t1Start)
                        // 显式过滤逻辑删除：同排程日重跑后旧批次 IS_DELETE=1，框架无全局逻辑删除不会自动过滤
                        .eq(LhMouldChangePlan::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                        .ge(LhMouldChangePlan::getPlanDate, t1Start)
                        .le(LhMouldChangePlan::getPlanDate, t2End));
        log.info("模具交替/清洗计划查询完成, 排程目标日(T+1): {}, 分厂: {}, 计划日期范围: {} ~ {}, 总数量: {}",
                DateUtil.formatDate(scheduleDateT1), factoryCode,
                DateUtil.formatDate(t1Start), DateUtil.formatDate(t2End), allPlans.size());

        // T+1 模具交替机台（planDate 仅取 T+1 当天，更换类型 01/02；不并入T日数据）
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

        // T+1 模具清洗机台（planDate 仅取 T+1 当天，更换类型 03/04；业务确认清洗不并入T日前一日的准备数据）
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

        // 模具清洗日期：清洗机台仅取栏位对应当天（T+1/T+2）的数据，日期固定显示栏位日期，不再拼接多日
        String mouldCleanDateT1 = DateUtil.format(scheduleDateT1, "MM月dd日");
        String mouldCleanDateT2 = DateUtil.format(scheduleDateT2, "MM月dd日");

        map.put("mouldChangeInfo", mouldChangeInfoT1);
        map.put("mouldChangeInfo2", mouldChangeInfoT2);
        map.put("mouldCleanDate", mouldCleanDateT1);
        map.put("mouldCleanDate2", mouldCleanDateT2);
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
     * @param skuEmbryoTypeMap SKU与示方书关系映射（物料编码→示方类型集合，物料级类型兜底数据源）
     * @return 小胶种行数据列表（无数据返回空列表）
     */
    private List<Map<String, Object>> buildSmallRubberList(Date scheduleDate, String factoryCode,
                                                            List<CxScheduleResult> cxResults, String keySuffix,
                                                            Map<String, Set<String>> skuEmbryoTypeMap) {
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

        // 构建物料→示方类型集合映射（根据keySuffix选择班次范围，SKU关系表兜底共用胎胚物料类型）
        Map<String, Set<String>> materialRecipeTypeMap = this.buildMaterialRecipeTypeMap(cxResults, keySuffix, skuEmbryoTypeMap);
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

                // 从胎胚描述(embryoDesc)解析规格和花纹，按规格分组收集花纹列表
                // 输出格式：规格1 花纹1/花纹2，规格2 花纹3（同规格多花纹用"/"隔开，不同规格用"，"隔开）
                Map<String, LinkedHashSet<String>> specPatternsMap = new LinkedHashMap<>();
                for (MdmMaterialInfo materialInfo : materialsForType) {
                    String embryoDesc = StringUtils.defaultString(materialInfo.getEmbryoDesc()).trim();
                    String[] specPattern = this.parseSpecAndPatternFromEmbryoDesc(embryoDesc);
                    if (specPattern == null) {
                        continue;
                    }
                    String specifications = specPattern[0];
                    String pattern = specPattern[1];
                    specPatternsMap.computeIfAbsent(specifications, k -> new LinkedHashSet<>()).add(pattern);
                }

                if (specPatternsMap.isEmpty()) {
                    continue;
                }

                // 格式化：规格 花纹1/花纹2，不同规格用"，"隔开
                List<String> specPatternParts = new ArrayList<>();
                for (Map.Entry<String, LinkedHashSet<String>> entry : specPatternsMap.entrySet()) {
                    String spec = entry.getKey();
                    String patterns = String.join("/", entry.getValue());
                    specPatternParts.add(spec + " " + patterns);
                }

                String title = this.getRecipeTypeTitle(recipeType);
                groupParts.add(title + String.join("，", specPatternParts));
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
     * <p><b>多物料合并修复：</b>CxScheduleResult.materialCode 可能是逗号分隔的多个物料编码
     * （排程生成时按胎胚+机台合并），必须拆分后逐个物料作为key放入映射，否则用整条组合串作key
     * 会导致后续按单个物料编码查询时匹配失败、回退默认"正规"，使量试/试制物料被错误归入正规分组。</p>
     *
     * @param cxResults 成型排程结果列表
     * @param keySuffix key 后缀（"" 取class3/4/5，"2" 取class6/7/8）
     * @param skuEmbryoTypeMap SKU与示方书关系映射（物料编码→示方类型集合，物料级类型兜底数据源）
     * @return 物料编码→示方类型编码集合映射（S-正规，T-量试，X-试制）
     */
    private Map<String, Set<String>> buildMaterialRecipeTypeMap(List<CxScheduleResult> cxResults, String keySuffix,
                                                                 Map<String, Set<String>> skuEmbryoTypeMap) {

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

            // 拆分逗号分隔的多物料编码后逐个物料归集类型
            //（主物料沿用排程写入的班次示方类型，共用胎胚物料用SKU关系表类型兜底），
            // 同一物料可能有多条排程结果记录（不同机台），合并所有示方类型
            Map<String, Set<String>> materialTypes = this.resolveMaterialTypes(result, recipeTypes, skuEmbryoTypeMap);
            for (Map.Entry<String, Set<String>> typeEntry : materialTypes.entrySet()) {
                materialRecipeTypeMap.computeIfAbsent(typeEntry.getKey(), k -> new HashSet<>())
                        .addAll(typeEntry.getValue());
            }
        }
        return materialRecipeTypeMap;
    }

    /**
     * 加载SKU与示方书关系映射（物料编码→示方类型集合）。
     *
     * <p>数据来源：T_MDM_SKU_CONSTRUCTION_REF（物料+产品状态→制造示方书类型），
     * 与排程引擎 CoreScheduleAlgorithmServiceImpl 构建的 materialCode|trialStatus→embryoType
     * 映射同源。一个物料可能存在多种产品状态（正式/量试/试制），因此收集其全部有效示方类型，
     * 用于共用胎胚合并记录中非主物料的类型兜底判定。</p>
     *
     * @return 物料编码→示方类型编码集合映射（S-正规，T-量试，X-试制）
     */
    private Map<String, Set<String>> loadSkuEmbryoTypeMap() {
        List<MdmSkuConstructionRef> skuRefList = mdmSkuConstructionRefMapper.selectList(
                new LambdaQueryWrapper<MdmSkuConstructionRef>()
                        .select(MdmSkuConstructionRef::getMaterialCode, MdmSkuConstructionRef::getEmbryoType)
                        .eq(MdmSkuConstructionRef::getIsDelete, 0));
        Map<String, Set<String>> skuEmbryoTypeMap = new HashMap<>();
        for (MdmSkuConstructionRef ref : skuRefList) {
            if (ref == null || StringUtils.isBlank(ref.getMaterialCode())
                    || StringUtils.isBlank(ref.getEmbryoType())) {
                continue;
            }
            // 仅保留有效示方类型（S-正规，T-量试，X-试制），过滤脏数据
            String embryoType = ref.getEmbryoType().trim();
            if (!"S".equals(embryoType) && !"T".equals(embryoType) && !"X".equals(embryoType)) {
                continue;
            }
            skuEmbryoTypeMap.computeIfAbsent(ref.getMaterialCode().trim(), k -> new HashSet<>()).add(embryoType);
        }
        log.info("SKU与示方书关系映射加载完成, 物料数量: {}", skuEmbryoTypeMap.size());
        return skuEmbryoTypeMap;
    }

    /**
     * 解析单条排程记录的物料级示方类型映射。
     *
     * <p>将逗号分隔的多物料编码拆分后逐个归集类型：</p>
     * <ul>
     *   <li>第一个物料（主物料）：沿用排程写入的班次示方类型 recipeTypes
     *       （与排程引擎 resolveRecipeType 多物料合并时仅取第一个物料的行为一致）</li>
     *   <li>其余共用胎胚物料：记录级 recipeType 仅代表主物料，无法感知物料级差异，
     *       用 SKU 与示方书关系表的物料全部类型兜底；SKU 关系查不到时回退 recipeTypes</li>
     * </ul>
     *
     * @param result           成型排程结果（materialCode 可能是逗号分隔的多物料编码）
     * @param recipeTypes      排程写入的班次示方类型集合（记录级，代表主物料）
     * @param skuEmbryoTypeMap SKU与示方书关系映射（物料编码→示方类型集合）
     * @return 单个物料编码→示方类型集合映射
     */
    private Map<String, Set<String>> resolveMaterialTypes(CxScheduleResult result, Set<String> recipeTypes,
                                                           Map<String, Set<String>> skuEmbryoTypeMap) {
        Map<String, Set<String>> materialTypes = new LinkedHashMap<>();
        String materialCode = StringUtils.defaultString(result.getMaterialCode());
        String[] codes = materialCode.split("[,，]");
        boolean isFirst = true;
        for (String code : codes) {
            String trimmedCode = code.trim();
            if (StringUtils.isBlank(trimmedCode)) {
                continue;
            }
            Set<String> types = isFirst
                    ? recipeTypes
                    : skuEmbryoTypeMap.getOrDefault(trimmedCode, recipeTypes);
            materialTypes.computeIfAbsent(trimmedCode, k -> new HashSet<>()).addAll(types);
            isFirst = false;
        }
        // materialCode 无法拆分出有效编码时，整体作为key兜底，保持与旧逻辑兼容
        if (materialTypes.isEmpty() && StringUtils.isNotBlank(materialCode)) {
            materialTypes.put(materialCode.trim(), new HashSet<>(recipeTypes));
        }
        return materialTypes;
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
     * 从胎胚描述中解析规格和花纹。
     *
     * <p>胎胚描述格式样例：{@code 295/80R22.5 152/149L 18PR BA267 BL4HBL}</p>
     * <ul>
     *   <li>规格：按空格分割后的第1段（如 295/80R22.5）</li>
     *   <li>花纹：按空格分割后的第4段（如 BA267）</li>
     * </ul>
     *
     * @param embryoDesc 胎胚描述
     * @return 包含规格和花纹的数组，[0]=规格，[1]=花纹；解析失败返回null
     */
    private String[] parseSpecAndPatternFromEmbryoDesc(String embryoDesc) {
        if (StringUtils.isBlank(embryoDesc)) {
            return null;
        }
        // 按空格分割胎胚描述
        String[] parts = embryoDesc.trim().split("\\s+");
        // 胎胚描述至少需要4段才能解析出规格（第1段）和花纹（第4段）
        if (parts.length < 4) {
            return null;
        }
        String specifications = parts[0];
        String pattern = parts[3];
        if (StringUtils.isBlank(specifications) || StringUtils.isBlank(pattern)) {
            return null;
        }
        return new String[]{specifications, pattern};
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
     * 判断某台机器在指定班次类型和班次序号范围内是否有排程记录（含计划量为0的机台）。
     *
     * <p>统计口径：只要机台在该班次序号范围内有排程记录（classNPlanQty 不为 null，即使为0），
     * 即视为该班次开动，计入开动机台数。</p>
     *
     * @param result           硫化排程结果
     * @param classShiftTypeMap 班次类型映射
     * @param shiftType        班次类型（01-夜，02-早，03-中）
     * @param shiftIndexMin    班次序号下限（含）
     * @param shiftIndexMax    班次序号上限（含）
     * @return true=该机台在该班次类型和范围内有排程记录，false=无排程记录
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
                Integer qty = null;
                switch (shiftIndex) {
                    case 1:
                        qty = result.getClass1PlanQty();
                        break;
                    case 2:
                        qty = result.getClass2PlanQty();
                        break;
                    case 3:
                        qty = result.getClass3PlanQty();
                        break;
                    case 4:
                        qty = result.getClass4PlanQty();
                        break;
                    case 5:
                        qty = result.getClass5PlanQty();
                        break;
                    case 6:
                        qty = result.getClass6PlanQty();
                        break;
                    case 7:
                        qty = result.getClass7PlanQty();
                        break;
                    case 8:
                        qty = result.getClass8PlanQty();
                        break;
                    default:
                        break;
                }
                // 计划量不为null（即使为0）即表示该机台在该班次有排程记录，计入开动机台数
                if (qty != null) {
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
     * 一次性构建 T+1 和 T+2 的精度计划备注信息。
     *
     * <p>取数口径：直接从排程结果表（T_CX_SCHEDULE_RESULT / T_LH_SCHEDULE_RESULT）取数，
     * 不再查询精度计划表。一条排程结果记录包含8个班次数据，覆盖3天（T、T+1、T+2）。</p>
     *
     * <p>班次与报表栏位映射（以排程日期=8月7日为例，覆盖6/7/8日3天）：</p>
     * <ul>
     *   <li>class1/2 → 8月6日（T），不在报表展示范围内，忽略</li>
     *   <li>class3/4/5 → 8月7日（T+1），对应 T+1 报表栏位</li>
     *   <li>class6/7/8 → 8月8日（T+2），对应 T+2 报表栏位</li>
     * </ul>
     *
     * <p>精度标记判定规则（硫化精度与成型精度互斥区分，均先按分隔符拆分为独立原因项再匹配）：</p>
     * <ul>
     *   <li>硫化侧：精度保养结束班次由 {@code ResultDowntimeSummaryUtil} 写入固定原因"精度计划"，
     *       独立项包含匹配"精度计划"/"喷砂清洗+精度"（"换模+精度计划"含"精度计划"自动覆盖）；
     *       成型排程联动写入硫化结果的"成型精度影响: ..."拆分后无独立项命中白名单，天然被排除，
     *       避免仅受成型精度影响的硫化机台被误计入硫化备注</li>
     *   <li>成型侧：精度扣减班次由 {@code buildTaskAnalysis} 写入独立原因"精度"
     *       （可能与其他原因组合，如"试制,精度"），独立项精确等于"精度"才命中；
     *       硫化侧写法（"精度计划"等）与"成型精度影响"说明文本均不会误命中，只统计成型精度校验</li>
     * </ul>
     *
     * @param cxResults  成型排程结果列表（已按 scheduleDate + factoryCode 过滤）
     * @param lhResults  硫化排程结果列表（已按 scheduleDate + factoryCode 过滤）
     * @param factoryCode 分厂编码
     * @return 包含 cxRemark/cxRemark2/lhRemark/lhRemark2 的Map
     */
    private Map<String, Object> buildPrecisionRemarkInfo(List<CxScheduleResult> cxResults,
                                                         List<LhScheduleResult> lhResults,
                                                         String factoryCode) {
        Map<String, Object> map = new HashMap<>(8);

        // 成型精度机台：按班次分析原因含成型精度标记分组（排除硫化侧写法及"成型精度影响"文本）
        // T+1（class3/4/5）和 T+2（class6/7/8）从同一份排程结果中取数
        List<String> cxMachineCodesT1 = cxResults.stream()
                .filter(r -> this.containsCxPrecisionKeyword(
                        r.getClass3Analysis(), r.getClass4Analysis(), r.getClass5Analysis()))
                .map(CxScheduleResult::getCxMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        List<String> cxMachineCodesT2 = cxResults.stream()
                .filter(r -> this.containsCxPrecisionKeyword(
                        r.getClass6Analysis(), r.getClass7Analysis(), r.getClass8Analysis()))
                .map(CxScheduleResult::getCxMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        // 成型备注格式："机台A、机台B 6:00-14:00精度校验"
        String cxRemarkT1 = cxMachineCodesT1.isEmpty() ? ""
                : String.join("、", cxMachineCodesT1) + " 6:00-14:00精度校验";
        String cxRemarkT2 = cxMachineCodesT2.isEmpty() ? ""
                : String.join("、", cxMachineCodesT2) + " 6:00-14:00精度校验";

        // 硫化精度机台：按班次分析原因含硫化精度固定写法分组（白名单匹配，排除"成型精度影响"文本）
        List<String> lhMachineCodesT1 = lhResults.stream()
                .filter(r -> this.containsLhPrecisionKeyword(
                        r.getClass3Analysis(), r.getClass4Analysis(), r.getClass5Analysis()))
                .map(LhScheduleResult::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        List<String> lhMachineCodesT2 = lhResults.stream()
                .filter(r -> this.containsLhPrecisionKeyword(
                        r.getClass6Analysis(), r.getClass7Analysis(), r.getClass8Analysis()))
                .map(LhScheduleResult::getLhMachineCode)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        // 硫化备注需要计算保养时段和开产时间
        String lhRemarkT1 = this.buildLhRemarkText(lhMachineCodesT1, factoryCode, "");
        String lhRemarkT2 = this.buildLhRemarkText(lhMachineCodesT2, factoryCode, "(T+2)");

        log.info("精度计划备注分组结果 - T+1成型: [{}], T+2成型: [{}], T+1硫化: [{}], T+2硫化: [{}]",
                cxRemarkT1, cxRemarkT2, lhRemarkT1, lhRemarkT2);

        map.put("cxRemark", cxRemarkT1);
        map.put("cxRemark2", cxRemarkT2);
        map.put("lhRemark", lhRemarkT1);
        map.put("lhRemark2", lhRemarkT2);
        return map;
    }

    /**
     * 判断硫化侧班次分析原因中是否包含硫化精度保养的固定写法。
     *
     * <p>硫化侧含"精度"的文本来自 {@code ResultDowntimeSummaryUtil} 的固定常量，属于封闭集合。
     * 判定时先按分隔符（英文逗号/中文分号）拆分为独立原因项，再对每项与"精度计划"做
     * <b>精确等于</b>匹配（仅真正做精度保养的机台计入硫化精度备注）：</p>
     * <ul>
     *   <li>"精度计划" → 命中，计入硫化精度备注</li>
     *   <li>"喷砂清洗+精度"（模具清洗与精度重叠的组合原因）→ 不等于独立项，归入模具清洗栏位展示</li>
     *   <li>"换模+精度计划"（历史残留写法）→ 不等于独立项，归入模具交替栏位展示</li>
     *   <li>"成型精度影响: 库存X+产量Y=Z&lt;硫化计划W, 缺口V条" → 拆分后无独立项等于"精度计划"，天然被排除</li>
     * </ul>
     *
     * <p>避免仅做模具清洗（如喷砂清洗叠加精度时段）的机台被误计入硫化精度维保备注；
     * 若同一班次同时含"精度计划"与"成型精度影响"，仍按"精度计划"正确计入。</p>
     *
     * @param analyses 待检查的班次分析原因文本（可变参数，任一班次的独立原因项精确等于"精度计划"即返回true）
     * @return true-任一班次分析含独立原因项"精度计划"；false-全部不含
     */
    private boolean containsLhPrecisionKeyword(String... analyses) {
        if (analyses == null || analyses.length == 0) {
            return false;
        }
        for (String analysis : analyses) {
            if (StringUtils.isBlank(analysis)) {
                continue;
            }
            // 按分隔符拆分为独立原因项后，与"精度计划"做精确等于匹配，
            // 排除"喷砂清洗+精度"/"换模+精度计划"等组合原因
            for (String item : this.splitAnalysisItems(analysis)) {
                if (LH_PRECISION_ANALYSIS.equals(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 判断成型侧班次分析原因中是否包含成型精度标记。
     *
     * <p>成型侧精度扣减班次由 {@code buildTaskAnalysis} 写入独立原因"精度"
     * （可能与其他原因组合，如"试制,精度"；也可能与结构切换备注用中文分号拼接）。
     * 判定时先按分隔符拆分为独立原因项，再对每项做<b>精确等于</b>"精度"匹配：</p>
     * <ul>
     *   <li>"试制,精度" 拆分后含独立项"精度" → 命中</li>
     *   <li>"精度；本成型机计划23号切换..." 拆分后含独立项"精度" → 命中</li>
     *   <li>"精度计划"（硫化侧写法）拆分后独立项为"精度计划" ≠ "精度" → 不命中，防御性排除硫化精度</li>
     *   <li>"成型精度影响: ..."拆分后无独立项等于"精度" → 不命中，防御性排除</li>
     * </ul>
     *
     * @param analyses 待检查的班次分析原因文本（可变参数）
     * @return true-任一班次分析含独立原因项"精度"；false-全部不含
     */
    private boolean containsCxPrecisionKeyword(String... analyses) {
        if (analyses == null || analyses.length == 0) {
            return false;
        }
        for (String analysis : analyses) {
            if (StringUtils.isBlank(analysis)) {
                continue;
            }
            // 按分隔符拆分为独立原因项后，逐项与"精度"做精确等于匹配
            for (String item : this.splitAnalysisItems(analysis)) {
                if (CX_PRECISION_ANALYSIS.equals(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 将班次分析文本按分隔符拆分为独立原因项。
     *
     * <p>硫化侧 {@code ShiftFieldUtil.appendShiftAnalysis} 与成型联动
     * {@code ProductionCalculator.appendClassAnalysisByIndex} 均用英文逗号拼接原因项，
     * 成型侧结构切换备注（{@code markMachineSwitchInMainTable}）用中文分号拼接，
     * 故统一按英文逗号和中文分号两种分隔符拆分，并对每项做 trim 去除首尾空白。</p>
     *
     * @param analysis 班次分析原因文本（可能为多原因拼接串）
     * @return 拆分并 trim 后的独立原因项数组；入参为空时返回空数组
     */
    private String[] splitAnalysisItems(String analysis) {
        if (StringUtils.isBlank(analysis)) {
            return new String[0];
        }
        return Arrays.stream(analysis.split(ANALYSIS_ITEM_SEPARATOR))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
    }

    /**
     * 构建硫化备注文本。
     *
     * <p>硫化精度做的时间及开产时间根据以下三个参数来定：</p>
     * <ul>
     *   <li>胶囊预热时间（小时）SYS0307009，如：2.5</li>
     *   <li>保养开始小时 SYS0307002，如：8</li>
     *   <li>保养耗时（小时）SYS0307001，如：7</li>
     * </ul>
     * <p>开产为保养完后胶囊预热完后开产。
     * 例如：保养8:00开始，保养7小时到15:00结束，胶囊预热2.5小时，开产时间17:30</p>
     *
     * @param machineCodes 机台编号列表
     * @param factoryCode  分厂编码
     * @param logSuffix    日志后缀（用于区分T+1/T+2）
     * @return 硫化备注字符串，格式如："机台A、机台B 8:00-15:00 维保,17:30开产"；无机台返回空字符串
     */
    private String buildLhRemarkText(List<String> machineCodes, String factoryCode, String logSuffix) {
        if (machineCodes == null || machineCodes.isEmpty()) {
            return "";
        }

        String maintenanceStartHourStr = this.loadFactoryParamValue(factoryCode, null, "SYS0307002");
        String maintenanceDurationStr = this.loadFactoryParamValue(factoryCode, null, "SYS0307001");
        String capsulePreheatStr = this.loadFactoryParamValue(factoryCode, null, "SYS0307009");

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

        log.info("硫化备注时间计算{} - 保养开始: {}小时, 保养耗时: {}小时, 胶囊预热: {}小时, 保养时段: {}, 开产时间: {}",
                logSuffix, maintenanceStartHour, maintenanceDuration, capsulePreheatHours,
                maintenanceTimeRange, productionStartTime);

        String machineStr = String.join("、", machineCodes);
        return machineStr + " " + maintenanceTimeRange + " 维保," + productionStartTime + "开产";
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
     * 只展示第二天切换的数据，展示格式为"机台号：前结构 换 后结构（班次）"，
     * 多个切换用"；"隔开（班次为夜班/早班/中班，从排程结果切换备注推断，推断不到默认夜班）。
     *
     * <p>两种场景：</p>
     * <ul>
     *   <li>非跨月场景：排程日期12号，取当月转产数据，找endDay=12的前结构，
     *       后结构先查beginDay=12，12号没有再查beginDay=13</li>
     *   <li>跨月场景：排程日期6月1号，取6月转产数据，找endDay=1且后结构beginDay=1</li>
     * </ul>
     *
     * <p>T+1和T+2共用相同的结构切换数据（以actualScheduleDate为准），确保两个日期显示一致。</p>
     *
     * @param reportDate        报告日期（未使用，保留为兼容调用）
     * @param scheduleDate      排程日期（用于确定查询年月和日号）
     * @param factoryCode       分厂编码
     * @param cxResults         预查询的成型排程结果（用于推断切换班次）
     * @param classShiftTypeMap 班次类型映射（班次序号→班次类型编码 01夜/02早/03中）
     * @param keySuffix         key 后缀（"" 查class3/4/5班次备注，"2" 查class6/7/8班次备注）
     * @return 规格切换信息字符串，如"H1401：结构A 换 结构B（夜班）"
     */
    private String buildCxSpecSwitch(Date reportDate, Date scheduleDate, String factoryCode,
                                      List<CxScheduleResult> cxResults, Map<Integer, String> classShiftTypeMap,
                                      String keySuffix) {
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
            String machineCode = entry.getKey();
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
                // 推断切换班次：从成型排程结果该机台+前结构的班次分析备注（"本成型机计划XX号切换..."）定位切换班次，
                // 推断不到时默认夜班（结构切换通常在生产日切换的夜班完成）
                String shiftName = this.resolveSwitchShiftName(machineCode, prevStructureName,
                        cxResults, classShiftTypeMap, keySuffix);
                if (StringUtils.isBlank(shiftName)) {
                    shiftName = "夜班";
                }
                switchList.add(machineCode + "：" + prevStructureName + " 换 " + nextStructureName
                        + "（" + shiftName + "）");
            }
        }

        String result = String.join("；", switchList);
        log.info("成型规格切换: {}", result);
        return result;
    }

    /**
     * 推断机台结构切换的班次名称。
     *
     * <p>排程引擎 {@code CoreScheduleAlgorithmServiceImpl.markMachineSwitchInMainTable} 会在
     * 该机台前结构记录的切换班次 ANALYSIS 中写入"本成型机计划XX号切换XX结构..."备注，
     * 据此定位切换发生的班次索引，再经班次类型映射转换为中文名称（夜班/早班/中班）。</p>
     *
     * @param machineCode       成型机台编号（已trim）
     * @param prevStructureName 前结构名称（已trim）
     * @param cxResults         预查询的成型排程结果
     * @param classShiftTypeMap 班次类型映射（班次序号→班次类型编码 01夜/02早/03中）
     * @param keySuffix         key 后缀（"" 查class3/4/5班次，"2" 查class6/7/8班次）
     * @return 班次中文名称（夜班/早班/中班），推断不到返回空字符串
     */
    private String resolveSwitchShiftName(String machineCode, String prevStructureName,
                                           List<CxScheduleResult> cxResults,
                                           Map<Integer, String> classShiftTypeMap, String keySuffix) {
        if (cxResults == null || cxResults.isEmpty()) {
            return "";
        }
        // keySuffix="" 查T+1班次范围（class3/4/5），keySuffix="2" 查T+2班次范围（class6/7/8）
        int[] shiftIndexes = "".equals(keySuffix) ? new int[]{3, 4, 5} : new int[]{6, 7, 8};
        for (CxScheduleResult result : cxResults) {
            if (!machineCode.equals(StringUtils.trimToEmpty(result.getCxMachineCode()))) {
                continue;
            }
            if (!prevStructureName.equals(StringUtils.trimToEmpty(result.getStructureName()))) {
                continue;
            }
            // 逐班次检查分析备注，命中机台切换备注即认定该班次为切换班次
            for (int shiftIndex : shiftIndexes) {
                String analysis = this.getCxShiftAnalysis(result, shiftIndex);
                if (analysis != null && analysis.contains("本成型机计划")) {
                    return this.resolveShiftName(classShiftTypeMap.get(shiftIndex));
                }
            }
        }
        return "";
    }

    /**
     * 读取成型排程结果指定班次索引的原因分析文本。
     *
     * @param result     成型排程结果
     * @param shiftIndex 班次索引（3~8）
     * @return 班次分析文本，索引超范围返回null
     */
    private String getCxShiftAnalysis(CxScheduleResult result, int shiftIndex) {
        switch (shiftIndex) {
            case 3: return result.getClass3Analysis();
            case 4: return result.getClass4Analysis();
            case 5: return result.getClass5Analysis();
            case 6: return result.getClass6Analysis();
            case 7: return result.getClass7Analysis();
            case 8: return result.getClass8Analysis();
            default: return null;
        }
    }

    /**
     * 班次类型编码转换为中文名称。
     *
     * @param shiftType 班次类型编码（T_LH_SHIFT_CONFIG.SHIFT_TYPE：01-夜班，02-早班，03-中班）
     * @return 班次中文名称，无法识别返回空字符串
     */
    private String resolveShiftName(String shiftType) {
        if (StringUtils.isBlank(shiftType)) {
            return "";
        }
        switch (shiftType.trim()) {
            case "01": return "夜班";
            case "02": return "早班";
            case "03": return "中班";
            default: return "";
        }
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
