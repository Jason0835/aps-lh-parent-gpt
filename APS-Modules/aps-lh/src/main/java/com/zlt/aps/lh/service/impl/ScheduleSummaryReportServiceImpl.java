package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanPlan;
import com.zlt.aps.lh.api.domain.entity.LhShiftConfig;
import com.zlt.aps.lh.api.domain.vo.ScheduleSummaryReportVO;
import com.zlt.aps.lh.mapper.CxLhScheduleResultMapper;
import com.zlt.aps.lh.mapper.CxScheduleResultMapper;
import com.zlt.aps.lh.mapper.LhMouldCleanPlanMapper;
import com.zlt.aps.lh.mapper.LhShiftConfigMapper;
import com.zlt.aps.lh.service.IScheduleSummaryReportService;
import com.zlt.aps.constant.FactoryConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
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
 * @author APS Team
 */
@Slf4j
@Service
public class ScheduleSummaryReportServiceImpl implements IScheduleSummaryReportService {

    @Resource
    private CxLhScheduleResultMapper cxLhScheduleResultMapper;

    @Resource
    private CxScheduleResultMapper cxScheduleResultMapper;

    @Resource
    private LhShiftConfigMapper lhShiftConfigMapper;

    @Resource
    private LhMouldCleanPlanMapper lhMouldCleanPlanMapper;

    @Override
    public byte[] exportScheduleSummaryReport(ScheduleSummaryReportVO queryVO) {
        if (queryVO == null || StringUtils.isBlank(queryVO.getScheduleDate())) {
            throw new ServiceException("排程日期不能为空");
        }

        Date scheduleDate = DateUtil.parse(queryVO.getScheduleDate(), "yyyy-MM-dd");
        String factoryCode = StringUtils.defaultString(queryVO.getFactoryCode(), FactoryConstant.DEFAULT_FACTORY_CODE);

        List<LhShiftConfig> shiftConfigs = loadShiftConfigs(factoryCode);
        Map<Integer, String> classShiftTypeMap = buildClassShiftTypeMap(shiftConfigs);

        Map<String, Object> tableMap = buildTableMap(scheduleDate, factoryCode, classShiftTypeMap);
        List<List<Map<String, Object>>> dataList = buildDataList(scheduleDate, factoryCode, classShiftTypeMap);

        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/scheduleSummaryReport.xlsx");

        if (inputStream == null) {
            throw new ServiceException("排产小结模板文件不存在");
        }

        return ExcelUtils.writeMultiList(inputStream, 0, tableMap, dataList);
    }

    /**
     * 构建模板参数映射表（普通占位符）
     */
    private Map<String, Object> buildTableMap(Date scheduleDate, String factoryCode,
                                               Map<Integer, String> classShiftTypeMap) {
        Map<String, Object> map = new HashMap<>(32);

        map.put("titleDate", DateUtil.format(scheduleDate, "MM月dd日") + " 计划排产 "
                + DateUtil.format(scheduleDate, "dd/MM"));

        List<CxScheduleResult> cxResults = cxScheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, scheduleDate)
                        .eq(CxScheduleResult::getFactoryCode, factoryCode));

        BigDecimal cxNightTotal = BigDecimal.ZERO;
        BigDecimal cxMorningTotal = BigDecimal.ZERO;
        BigDecimal cxMiddleTotal = BigDecimal.ZERO;

        Set<String> structureChanges = new LinkedHashSet<>();
        String prevStructure = null;
        for (CxScheduleResult result : cxResults) {
            cxNightTotal = cxNightTotal.add(sumCxQtyByShiftType(result, classShiftTypeMap, "夜班"));
            cxMorningTotal = cxMorningTotal.add(sumCxQtyByShiftType(result, classShiftTypeMap, "早班"));
            cxMiddleTotal = cxMiddleTotal.add(sumCxQtyByShiftType(result, classShiftTypeMap, "中班"));
            String currentStructure = StringUtils.defaultString(result.getStructureName()).trim();
            if (prevStructure != null && !prevStructure.equals(currentStructure)) {
                structureChanges.add(prevStructure + "→" + currentStructure);
            }
            prevStructure = currentStructure;
        }

        map.put("cxNightQty", cxNightTotal.toString());
        map.put("cxMorningQty", cxMorningTotal.toString());
        map.put("cxMiddleQty", cxMiddleTotal.toString());
        map.put("cxTotalQty", cxNightTotal.add(cxMorningTotal).add(cxMiddleTotal).toString());
        map.put("cxSetupInfo", "");
        map.put("cxTrialInfo", "");
        map.put("cxSpecSwitch", String.join("；", structureChanges));

        List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> lhResults = cxLhScheduleResultMapper.selectList(
                new LambdaQueryWrapper<com.zlt.aps.cx.entity.schedule.LhScheduleResult>()
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getScheduleDate, scheduleDate)
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getFactoryCode, factoryCode));

        BigDecimal lhNightTotal = BigDecimal.ZERO;
        BigDecimal lhMorningTotal = BigDecimal.ZERO;
        BigDecimal lhMiddleTotal = BigDecimal.ZERO;

        for (com.zlt.aps.cx.entity.schedule.LhScheduleResult result : lhResults) {
            lhNightTotal = lhNightTotal.add(sumLhQtyByShiftType(result, classShiftTypeMap, "夜班"));
            lhMorningTotal = lhMorningTotal.add(sumLhQtyByShiftType(result, classShiftTypeMap, "早班"));
            lhMiddleTotal = lhMiddleTotal.add(sumLhQtyByShiftType(result, classShiftTypeMap, "中班"));
        }

        long nightMachines = countLhMachinesByShiftType(lhResults, classShiftTypeMap, "夜班");
        long morningMachines = countLhMachinesByShiftType(lhResults, classShiftTypeMap, "早班");
        long middleMachines = countLhMachinesByShiftType(lhResults, classShiftTypeMap, "中班");

        map.put("lhNightQty", lhNightTotal.toString());
        map.put("lhMorningQty", lhMorningTotal.toString());
        map.put("lhMiddleQty", lhMiddleTotal.toString());
        map.put("lhTotalQty", lhNightTotal.add(lhMorningTotal).add(lhMiddleTotal).toString());
        map.put("lhNightMachines", String.valueOf(nightMachines));
        map.put("lhMorningMachines", String.valueOf(morningMachines));
        map.put("lhMiddleMachines", String.valueOf(middleMachines));
        map.put("lhTotalMachines", String.valueOf(nightMachines + morningMachines + middleMachines));
        map.put("mouldChangeInfo", "");

        LocalDate localDate = scheduleDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        Date startDate = Date.from(localDate.minusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(localDate.plusDays(1).atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

        List<LhMouldCleanPlan> cleanPlans = lhMouldCleanPlanMapper.selectList(
                new LambdaQueryWrapper<LhMouldCleanPlan>()
                        .eq(LhMouldCleanPlan::getFactoryCode, factoryCode)
                        .ge(LhMouldCleanPlan::getCleanTime, startDate)
                        .le(LhMouldCleanPlan::getCleanTime, endDate));

        if (!cleanPlans.isEmpty()) {
            map.put("mouldCleanDate", DateUtil.format(cleanPlans.get(0).getCleanTime(), "MM月dd日"));
            map.put("mouldCleanInfo", cleanPlans.stream()
                    .map(LhMouldCleanPlan::getLhCode)
                    .filter(StringUtils::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining("、")));
        } else {
            map.put("mouldCleanDate", "");
            map.put("mouldCleanInfo", "");
        }

        map.put("cxRemark", buildCxRemark(scheduleDate, factoryCode));
        map.put("lhRemark", buildLhRemark(scheduleDate, factoryCode));

        return map;
    }

    /**
     * 构建列表数据（小胶种列表，使用 {.xxx} 占位符）
     */
    private List<List<Map<String, Object>>> buildDataList(Date scheduleDate, String factoryCode,
                                                           Map<Integer, String> classShiftTypeMap) {
        List<Map<String, Object>> smallRubberList = new ArrayList<>();

        List<CxScheduleResult> cxResults = cxScheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, scheduleDate)
                        .eq(CxScheduleResult::getFactoryCode, factoryCode)
                        .orderByAsc(CxScheduleResult::getCxMachineCode));

        Map<String, Map<String, Object>> materialGroupMap = new LinkedHashMap<>();
        for (CxScheduleResult result : cxResults) {
            String materialCode = StringUtils.defaultString(result.getMaterialCode());
            if (!materialGroupMap.containsKey(materialCode)) {
                Map<String, Object> item = new HashMap<>();
                item.put("smallRubberCode", materialCode);
                item.put("smallRubberDetail", StringUtils.defaultString(result.getMaterialName())
                        + " | 计划:" + sumCxQtyByShiftType(result, classShiftTypeMap, "夜班")
                        + "/" + sumCxQtyByShiftType(result, classShiftTypeMap, "早班")
                        + "/" + sumCxQtyByShiftType(result, classShiftTypeMap, "中班"));
                materialGroupMap.put(materialCode, item);
            }
        }
        smallRubberList.addAll(materialGroupMap.values());

        List<List<Map<String, Object>>> dataList = new ArrayList<>();
        dataList.add(smallRubberList);
        return dataList;
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
                    case 1: qty = nvl(result.getClass1PlanQty()); break;
                    case 2: qty = nvl(result.getClass2PlanQty()); break;
                    case 3: qty = nvl(result.getClass3PlanQty()); break;
                    case 4: qty = nvl(result.getClass4PlanQty()); break;
                    case 5: qty = nvl(result.getClass5PlanQty()); break;
                    case 6: qty = nvl(result.getClass6PlanQty()); break;
                    case 7: qty = nvl(result.getClass7PlanQty()); break;
                    case 8: qty = nvl(result.getClass8PlanQty()); break;
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

    /**
     * 构建成型备注信息
     */
    private String buildCxRemark(Date scheduleDate, String factoryCode) {
        List<CxScheduleResult> cxResults = cxScheduleResultMapper.selectList(
                new LambdaQueryWrapper<CxScheduleResult>()
                        .eq(CxScheduleResult::getScheduleDate, scheduleDate)
                        .eq(CxScheduleResult::getFactoryCode, factoryCode));
        return cxResults.stream()
                .map(CxScheduleResult::getSpecialRequirements)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.joining("；"));
    }

    /**
     * 构建硫化备注信息
     */
    private String buildLhRemark(Date scheduleDate, String factoryCode) {
        List<com.zlt.aps.cx.entity.schedule.LhScheduleResult> lhResults = cxLhScheduleResultMapper.selectList(
                new LambdaQueryWrapper<com.zlt.aps.cx.entity.schedule.LhScheduleResult>()
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getScheduleDate, scheduleDate)
                        .eq(com.zlt.aps.cx.entity.schedule.LhScheduleResult::getFactoryCode, factoryCode));
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
        return map;
    }
}
