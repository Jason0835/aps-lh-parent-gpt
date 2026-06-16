package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.aps.cd90.engine.mapper.Cd90AutoScheduleSourceMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90ConstructionMaterialMapper;
import com.zlt.aps.cd90.engine.algorithm.Cd90FormingDemandExpander;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineConstructionMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineCxScheduleMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineStockMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineStorageLaneMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineMonthSurplusMapper;
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90FormingScheduleSource;
import com.zlt.aps.cd90.engine.model.Cd90EmbryoPlanSurplus;
import com.zlt.aps.cd90.engine.model.Cd90StockSource;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleInputService;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmMonthSurplus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 直裁自动排程输入数据加载实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Cd90AutoScheduleInputServiceImpl implements Cd90AutoScheduleInputService {

    private final Cd90EngineCxScheduleMapper cxScheduleMapper;
    private final Cd90EngineConstructionMapper constructionMapper;
    private final Cd90EngineStockMapper stockMapper;
    private final Cd90EngineStorageLaneMapper storageLaneMapper;
    private final Cd90EngineMonthSurplusMapper monthSurplusMapper;
    private final Cd90ConstructionMaterialMapper constructionMaterialMapper;
    private final Cd90AutoScheduleSourceMapper sourceMapper;
    private final Cd90FormingDemandExpander formingDemandExpander;

    /**
     * 加载第1至5步所需的成型计划、施工、6点库存和当前班次库排快照。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param classField 直裁结果班次字段
     * @param shiftCode 业务班次编码，用于匹配库排班次
     * @return 标准化输入数据
     */
    @Override
    public Cd90AutoScheduleInput load(String factoryCode, LocalDate scheduleDate,
                                      String classField, String shiftCode) {
        Assert.hasText(factoryCode, "工厂编码不能为空");
        Assert.notNull(scheduleDate, "排程日期不能为空");
        Assert.hasText(classField, "班次字段不能为空");
        Assert.hasText(shiftCode, "班次编码不能为空");

        LocalDate formingStartDate = scheduleDate.minusDays(1);
        LocalDate formingEndDate = scheduleDate.plusDays(3);
        List<CxScheduleResult> formingEntities = loadFormingSchedules(
                factoryCode, formingStartDate, formingEndDate);
        List<Cd90FormingScheduleSource> formingSchedules = formingEntities.stream()
                .map(sourceMapper::mapFormingSchedule)
                .collect(Collectors.toList());

        Set<String> embryoCodes = formingEntities.stream()
                .map(CxScheduleResult::getEmbryoCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> constructionVersions = formingSchedules.stream()
                .flatMap(schedule -> safe(schedule.getClassRecipeNos()).stream())
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        List<Cd90ConstructionMaterial> constructionMaterials = loadConstructionMaterials(
                factoryCode, embryoCodes, constructionVersions);
        List<Cd90DemandShift> demandShifts = formingDemandExpander.expand(
                formingSchedules, constructionMaterials);
        List<Cd90EmbryoPlanSurplus> embryoPlanSurpluses = loadEmbryoPlanSurpluses(
                factoryCode, scheduleDate, embryoCodes);

        List<Cd90StockSource> stocksAtSix = stockMapper.selectList(Wrappers.<Cd90Stock>lambdaQuery()
                        .eq(Cd90Stock::getFactoryCode, factoryCode)
                        .eq(Cd90Stock::getStockDate, Date.valueOf(scheduleDate))
                        .orderByAsc(Cd90Stock::getMaterialCode))
                .stream()
                .map(sourceMapper::mapStock)
                .collect(Collectors.toList());

        List<Cd90StorageLaneState> storageLanesAtSix = storageLaneMapper.selectList(
                        Wrappers.<Cd90StorageLaneLimit>lambdaQuery()
                                .eq(Cd90StorageLaneLimit::getFactoryCode, factoryCode)
                                .eq(Cd90StorageLaneLimit::getLaneDate, Date.valueOf(scheduleDate))
                                .eq(Cd90StorageLaneLimit::getShiftCode, shiftCode)
                                .orderByAsc(Cd90StorageLaneLimit::getStorageLaneCode))
                .stream()
                .map(sourceMapper::mapStorageLane)
                .collect(Collectors.toList());

        log.info("[直裁自动排程] 输入数据加载完成, factoryCode={}, scheduleDate={}, classField={}, shiftCode={}, "
                        + "formingRange={}~{}, formingCount={}, constructionMaterialCount={}, "
                        + "demandShiftCount={}, stockCount={}, storageLaneCount={}",
                factoryCode, scheduleDate, classField, shiftCode, formingStartDate, formingEndDate,
                formingSchedules.size(), constructionMaterials.size(), demandShifts.size(), stocksAtSix.size(),
                storageLanesAtSix.size());

        return Cd90AutoScheduleInput.builder()
                .formingSchedules(formingSchedules)
                .constructionMaterials(constructionMaterials)
                .stocksAtSix(stocksAtSix)
                .embryoPlanSurpluses(embryoPlanSurpluses)
                .demandShifts(demandShifts)
                .storageLanesAtSix(storageLanesAtSix)
                .inboundRecords(Collections.emptyList())
                .build();
    }

    /** 按胎胚代码加载当前排程月份的月计划剩余量。 */
    private List<Cd90EmbryoPlanSurplus> loadEmbryoPlanSurpluses(String factoryCode,
                                                                LocalDate scheduleDate,
                                                                Set<String> embryoCodes) {
        if (embryoCodes.isEmpty()) {
            return Collections.emptyList();
        }
        return monthSurplusMapper.selectList(Wrappers.<MdmMonthSurplus>lambdaQuery()
                        .select(MdmMonthSurplus::getMaterialCode, MdmMonthSurplus::getPlanSurplusQty)
                        .eq(MdmMonthSurplus::getFactoryCode, factoryCode)
                        .eq(MdmMonthSurplus::getYear, scheduleDate.getYear())
                        .eq(MdmMonthSurplus::getMonth, scheduleDate.getMonthValue())
                        .in(MdmMonthSurplus::getMaterialCode, embryoCodes)
                        .orderByAsc(MdmMonthSurplus::getMaterialCode))
                .stream().map(item -> Cd90EmbryoPlanSurplus.builder()
                        .embryoCode(item.getMaterialCode())
                        .planSurplusQuantity(item.getPlanSurplusQty()).build())
                .collect(Collectors.toList());
    }

    private List<CxScheduleResult> loadFormingSchedules(String factoryCode,
                                                         LocalDate startDate,
                                                         LocalDate endDate) {
        // 直裁按胎胚代码分解施工层位，仅查询需求计算所需字段，避免共享实体的展示字段影响排程。
        return cxScheduleMapper.selectList(Wrappers.<CxScheduleResult>lambdaQuery()
                .select(CxScheduleResult::getCxBatchNo,
                        CxScheduleResult::getScheduleDate,
                        CxScheduleResult::getEmbryoCode,
                        CxScheduleResult::getClass1PlanQty,
                        CxScheduleResult::getClass1RecipeNo,
                        CxScheduleResult::getClass2PlanQty,
                        CxScheduleResult::getClass2RecipeNo,
                        CxScheduleResult::getClass3PlanQty,
                        CxScheduleResult::getClass3RecipeNo,
                        CxScheduleResult::getClass4PlanQty,
                        CxScheduleResult::getClass4RecipeNo,
                        CxScheduleResult::getClass5PlanQty,
                        CxScheduleResult::getClass5RecipeNo,
                        CxScheduleResult::getClass6PlanQty,
                        CxScheduleResult::getClass6RecipeNo,
                        CxScheduleResult::getClass7PlanQty,
                        CxScheduleResult::getClass7RecipeNo,
                        CxScheduleResult::getClass8PlanQty,
                        CxScheduleResult::getClass8RecipeNo)
                .eq(CxScheduleResult::getFactoryCode, factoryCode)
                .between(CxScheduleResult::getScheduleDate,
                        Date.valueOf(startDate), Date.valueOf(endDate))
                .orderByAsc(CxScheduleResult::getScheduleDate)
                .orderByAsc(CxScheduleResult::getCxBatchNo));
    }

    private List<Cd90ConstructionMaterial> loadConstructionMaterials(String factoryCode,
                                                                      Set<String> embryoCodes,
                                                                      Set<String> constructionVersions) {
        if (embryoCodes.isEmpty()) {
            log.warn("[直裁自动排程] 成型计划未找到胎胚代码, factoryCode={}", factoryCode);
            return Collections.emptyList();
        }
        if (constructionVersions.isEmpty()) {
            log.warn("[直裁自动排程] 成型计划未找到施工版本CLASSn_RECIPE_NO, factoryCode={}", factoryCode);
            return Collections.emptyList();
        }
        return constructionMapper.selectList(Wrappers.<MdmConstructionInfo>lambdaQuery()
                        .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                        .in(MdmConstructionInfo::getConstructionCode, embryoCodes)
                        .in(MdmConstructionInfo::getConstructionVersion, constructionVersions)
                        .orderByAsc(MdmConstructionInfo::getConstructionCode)
                        .orderByAsc(MdmConstructionInfo::getConstructionVersion))
                .stream()
                .flatMap(construction -> constructionMaterialMapper.map(construction).stream())
                .collect(Collectors.toList());
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }
}
