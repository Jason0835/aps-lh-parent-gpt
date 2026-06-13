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
import com.zlt.aps.cd90.engine.model.Cd90AutoScheduleInput;
import com.zlt.aps.cd90.engine.model.Cd90ConstructionMaterial;
import com.zlt.aps.cd90.engine.model.Cd90DemandShift;
import com.zlt.aps.cd90.engine.model.Cd90FormingScheduleSource;
import com.zlt.aps.cd90.engine.model.Cd90StockSource;
import com.zlt.aps.cd90.engine.model.Cd90StorageLaneState;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleInputService;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
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
        List<Cd90ConstructionMaterial> constructionMaterials = loadConstructionMaterials(
                factoryCode, embryoCodes);
        List<Cd90DemandShift> demandShifts = formingDemandExpander.expand(
                formingSchedules, constructionMaterials);

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
                .demandShifts(demandShifts)
                .storageLanesAtSix(storageLanesAtSix)
                .inboundRecords(Collections.emptyList())
                .build();
    }

    private List<CxScheduleResult> loadFormingSchedules(String factoryCode,
                                                         LocalDate startDate,
                                                         LocalDate endDate) {
        return cxScheduleMapper.selectList(Wrappers.<CxScheduleResult>lambdaQuery()
                .eq(CxScheduleResult::getFactoryCode, factoryCode)
                .between(CxScheduleResult::getScheduleDate,
                        Date.valueOf(startDate), Date.valueOf(endDate))
                .orderByAsc(CxScheduleResult::getScheduleDate)
                .orderByAsc(CxScheduleResult::getCxBatchNo));
    }

    private List<Cd90ConstructionMaterial> loadConstructionMaterials(String factoryCode,
                                                                      Set<String> embryoCodes) {
        if (embryoCodes.isEmpty()) {
            log.warn("[直裁自动排程] 成型计划未找到胎胚代码, factoryCode={}", factoryCode);
            return Collections.emptyList();
        }
        return constructionMapper.selectList(Wrappers.<MdmConstructionInfo>lambdaQuery()
                        .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                        .in(MdmConstructionInfo::getConstructionCode, embryoCodes)
                        .orderByAsc(MdmConstructionInfo::getConstructionCode))
                .stream()
                .flatMap(construction -> constructionMaterialMapper.map(construction).stream())
                .collect(Collectors.toList());
    }
}
