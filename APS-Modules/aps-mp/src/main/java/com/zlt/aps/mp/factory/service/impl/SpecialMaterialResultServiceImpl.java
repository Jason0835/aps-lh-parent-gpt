package com.zlt.aps.mp.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.FactoryParamMapper;
import com.zlt.aps.maindata.mapper.MdmBomInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmConstructionProcessEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialConsumeDetailMapper;
import com.zlt.aps.maindata.mapper.RawSpecialMaterialStockEntityMapper;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionProcess;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanMouldDayResult;
import com.zlt.aps.mp.api.domain.entity.FactoryParam;
import com.zlt.aps.mp.api.domain.entity.MdmBomInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialConsumeDetail;
import com.zlt.aps.mp.api.domain.entity.RawSpecialMaterialStock;
import com.zlt.aps.mp.api.domain.entity.SpecialMaterialResult;
import com.zlt.aps.mdm.api.enums.ProcessCodeEnum;
import com.zlt.aps.mp.engine.domain.vo.SpecialMaterialBomRelationVo;
import com.zlt.aps.mp.engine.domain.vo.SpecialMaterialInfoVo;
import com.zlt.aps.mp.engine.domain.vo.SpecialMaterialStockVo;
import com.zlt.aps.mp.engine.scheduling.cxcapacity.SpecialMaterialScheduleHandler;
import com.zlt.aps.mp.factory.mapper.SpecialMaterialResultEntityMapper;
import com.zlt.aps.mp.factory.service.ISpecialMaterialResultService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.core.dao.basedao.BaseDao;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 特殊材料排产结果服务接口
 *
 * @author zlt
 */
@Slf4j
@Service
public class SpecialMaterialResultServiceImpl extends AbstractDocService<SpecialMaterialResult>
        implements ISpecialMaterialResultService {
    @Autowired
    private SpecialMaterialScheduleHandler specialMaterialScheduleHandler;
    @Autowired
    private MdmMaterialConsumeDetailMapper mdmMaterialConsumeDetailMapper;
    @Autowired
    private RawSpecialMaterialStockEntityMapper rawSpecialMaterialStockEntityMapper;
    @Autowired
    private MdmConstructionProcessEntityMapper mdmConstructionProcessEntityMapper;
    @Autowired
    private MdmBomInfoEntityMapper mdmBomInfoEntityMapper;
    @Autowired
    private FactoryParamMapper factoryParamMapper;
    @Autowired
    private SpecialMaterialResultEntityMapper specialMaterialResultEntityMapper;
    @Autowired
    private BaseDao baseDao;

    /**
     * 构建特殊材料排产结果
     * <p>
     * 数据加载逻辑：
     * <ol>
     *   <li>consumeDetail：只查询 planList 中有的 embryoCode，且 childMaterialCode 在 SYS0209005 配置值中</li>
     *   <li>stockList：查询 planList 年月上一个月的库存，转 SpecialMaterialInfoVo</li>
     *   <li>processList：只查询 consumeDetail 中出现的 materialCode</li>
     * </ol>
     * </p>
     */
    @Override
    public void buildSecialMaterialResult(List<FactoryMonthPlanMouldDayResult> planList) {
        if (CollectionUtils.isEmpty(planList)) {
            return;
        }

        // 从 planList 获取工厂、年月信息
        String factoryCode = planList.get(0).getFactoryCode();
        Integer year = planList.get(0).getYear();
        Integer month = planList.get(0).getMonth();

        // ========== 1. 查询 SYS0209005 参数：参与排产的特殊原材料编码 ==========
        LambdaQueryWrapper<FactoryParam> paramsQueryWrapper = new LambdaQueryWrapper<>();
        paramsQueryWrapper.eq(FactoryParam::getParamCode, MonthPlanEnums.SPECIAL_MATERIAL_CODE.getCode());
        paramsQueryWrapper.eq(FactoryParam::getFactoryCode, factoryCode);
        List<FactoryParam> paramList = factoryParamMapper.selectList(paramsQueryWrapper);
        Set<String> specialMaterialCodes = new HashSet<>();
        if (!CollectionUtils.isEmpty(paramList)
                && StringUtils.isNotBlank(paramList.get(0).getParamValue())) {
            String[] codes = paramList.get(0).getParamValue().split(",");
            for (String code : codes) {
                String trimmed = code.trim();
                if (!trimmed.isEmpty()) {
                    specialMaterialCodes.add(trimmed);
                }
            }
        }
        if (specialMaterialCodes.isEmpty()) {
            log.warn("工厂 {} 未配置 SYS0209005 特殊原材料编码，跳过特殊材料排产", factoryCode);
            return;
        }

        // ========== 2. 收集 planList 的 embryoCode ==========
        Set<String> embryoCodes = planList.stream()
                .filter(p -> p.getEmbryoCode() != null)
                .map(FactoryMonthPlanMouldDayResult::getEmbryoCode)
                .collect(Collectors.toSet());
        if (embryoCodes.isEmpty()) {
            return;
        }

        // ========== 3. 查询 consumeDetail：按 embryoCode 过滤 + childMaterialCode 在配置中
        LambdaQueryWrapper<MdmMaterialConsumeDetail> consumeParams = new LambdaQueryWrapper<>();
        consumeParams.in(MdmMaterialConsumeDetail::getEmbryoCode, embryoCodes);
        consumeParams.in(MdmMaterialConsumeDetail::getChildMaterialCode, specialMaterialCodes);
        List<MdmMaterialConsumeDetail> consumeDetail = mdmMaterialConsumeDetailMapper.selectList(consumeParams);
        if (CollectionUtils.isEmpty(consumeDetail)) {
            return;
        }

        // ========== 4. 通过 BOM 两层展开构建关联 VO（含胎胚、半部件、原材料、工艺信息）==========
        // 4.1 收集原材料物料号
        Set<String> childMaterialCodes = consumeDetail.stream()
                .map(MdmMaterialConsumeDetail::getChildMaterialCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(childMaterialCodes)) {
            return;
        }
        // 4.2 第一层：PARENT_CODE in (embryoCodes) → 获取半部件 child_code
        List<MdmBomInfo> level1BomList = mdmBomInfoEntityMapper.selectList(
                new LambdaQueryWrapper<MdmBomInfo>()
                        .in(MdmBomInfo::getParentCode, embryoCodes));
        Set<String> level1ChildCodes = level1BomList.stream()
                .map(MdmBomInfo::getChildCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(level1ChildCodes)) {
            return;
        }
        // 4.3 第二层：PARENT_CODE in (level1ChildCodes) AND CHILD_MATERIAL_CODE in (specialMaterialCodes)
        // 只保留配置了特殊原材料的半部件代码
        List<MdmBomInfo> level2BomList = mdmBomInfoEntityMapper.selectList(
                new LambdaQueryWrapper<MdmBomInfo>()
                        .in(MdmBomInfo::getParentCode, level1ChildCodes)
                        .in(MdmBomInfo::getChildMaterialCode, specialMaterialCodes));
        Set<String> validSemiPartCodes = level2BomList.stream()
                .map(MdmBomInfo::getParentCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(validSemiPartCodes)) {
            return;
        }
        // 4.4 查询半部件的示方书工艺信息
        List<MdmConstructionProcess> processList = mdmConstructionProcessEntityMapper.selectList(
                new LambdaQueryWrapper<MdmConstructionProcess>()
                        .in(MdmConstructionProcess::getMaterialCode, validSemiPartCodes));

        // 4.5 构建半部件 → {长度, 宽度, 幅宽} 映射
        Map<String, String[]> processDimMap = buildProcessDimMap(processList);
        if (processDimMap.isEmpty()) {
            return;
        }

        // 4.6 构建 level2Bom 映射：semiPartCode → List<childMaterialCode>
        Map<String, List<MdmBomInfo>> level2BomMap = level2BomList.stream()
                .collect(Collectors.groupingBy(MdmBomInfo::getParentCode));

        // 4.7 前置校验：胎胚、半部件物料号、原材料物料号、示方书工艺信息全部不能为空
        boolean hasIncomplete = level1BomList.stream()
                .anyMatch(l1 -> l1.getParentCode() == null || l1.getChildCode() == null);
        if (hasIncomplete) {
            log.warn("BOM 第一层展开存在胎胚或半部件物料号为空，跳过");
            return;
        }
        hasIncomplete = level2BomList.stream()
                .anyMatch(l2 -> l2.getParentCode() == null || l2.getChildMaterialCode() == null);
        if (hasIncomplete) {
            log.warn("BOM 第二层展开存在半部件物料号或原材料物料号为空，跳过");
            return;
        }
        hasIncomplete = processDimMap.values().stream()
                .anyMatch(dims -> dims == null || dims.length < 3
                        || StringUtils.isAnyBlank(dims[0], dims[1], dims[2]));
        if (hasIncomplete) {
            log.warn("示方书工艺信息存在长度/宽度/幅宽为空，跳过");
            return;
        }

        // 4.8 构建 BOM 关联 VO 列表（按胎胚+半部件+原材料去重）
        List<SpecialMaterialBomRelationVo> bomRelationList = new ArrayList<>();
        Set<String> deduplicateSet = new HashSet<>();
        for (MdmBomInfo level1 : level1BomList) {
            String embryoCode = level1.getParentCode();
            String semiPartCode = level1.getChildCode();
            String[] dims = processDimMap.get(semiPartCode);
            if (dims == null) continue;
            List<MdmBomInfo> level2Entries = level2BomMap.get(semiPartCode);
            if (CollectionUtils.isEmpty(level2Entries)) continue;
            for (MdmBomInfo level2 : level2Entries) {
                String childMaterialCode = level2.getChildMaterialCode();
                String key = embryoCode + "|" + semiPartCode + "|" + childMaterialCode;
                if (!deduplicateSet.add(key)) {
                    continue; // 已存在，跳过
                }
                SpecialMaterialBomRelationVo vo = new SpecialMaterialBomRelationVo();
                vo.setEmbryoCode(embryoCode);
                vo.setSemiPartCode(semiPartCode);
                vo.setChildMaterialCode(childMaterialCode);
                vo.setProcessLength(dims[0]);
                vo.setProcessWidth(dims[1]);
                vo.setProcessFabricWidth(dims[2]);
                bomRelationList.add(vo);
            }
        }
        if (CollectionUtils.isEmpty(bomRelationList)) {
            log.warn("BOM 关联 VO 列表为空，跳过特殊材料排产");
            return;
        }

        // ========== 5. 查询 stockList：planList 年月上一个月的库存 ==========
        Integer prevMonth = month == 1 ? 12 : month - 1;
        Integer prevYear = month == 1 ? year - 1 : year;
        LambdaQueryWrapper<RawSpecialMaterialStock> stockQueryWrapper = new LambdaQueryWrapper<RawSpecialMaterialStock>();
        stockQueryWrapper.eq(RawSpecialMaterialStock::getFactoryCode, factoryCode);
        stockQueryWrapper.eq(RawSpecialMaterialStock::getYear, prevYear);
        stockQueryWrapper.eq(RawSpecialMaterialStock::getMonth, prevMonth);
        stockQueryWrapper.in(RawSpecialMaterialStock::getMaterialCode, childMaterialCodes);
        List<RawSpecialMaterialStock> stockEntities = rawSpecialMaterialStockEntityMapper.selectList(stockQueryWrapper);
        List<SpecialMaterialInfoVo> stockList = stockEntities.stream().map(this::convertToStockVo)
                .collect(Collectors.toList());

        // ========== 6. 调用引擎计算特殊材料排程结果 ==========
        // 构建原材料编码→名称映射（用于补充 materialDesc）
        Map<String, String> materialNameMap = consumeDetail.stream()
                .filter(d -> d.getChildMaterialCode() != null && d.getChildMaterialName() != null)
                .collect(Collectors.toMap(
                        MdmMaterialConsumeDetail::getChildMaterialCode,
                        MdmMaterialConsumeDetail::getChildMaterialName,
                        (a, b) -> a));
        List<SpecialMaterialResult> specialList = specialMaterialScheduleHandler.calSpecialMaterialResult(
                planList, bomRelationList, stockList);
        if (CollectionUtils.isEmpty(specialList)) {
            return;
        }
        // 补充工厂、版本信息以及物料描述
        String monthPlanVersion = planList.get(0).getMonthPlanVersion();
        String productionVersion = planList.get(0).getProductionVersion();
        for (SpecialMaterialResult result : specialList) {
            result.setFactoryCode(factoryCode);
            result.setMonthPlanVersion(monthPlanVersion);
            result.setProductionVersion(productionVersion);
            if (StringUtils.isBlank(result.getMaterialDesc())) {
                result.setMaterialDesc(materialNameMap.getOrDefault(result.getMaterialCode(), ""));
            }
        }
        // 保存前先删除同工厂+同 production_version 的旧记录
        LambdaQueryWrapper<SpecialMaterialResult> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SpecialMaterialResult::getFactoryCode, factoryCode);
        deleteWrapper.eq(SpecialMaterialResult::getProductionVersion, productionVersion);
        specialMaterialResultEntityMapper.delete(deleteWrapper);

        baseDao.saveBatch(specialList);
        log.info("特殊材料排产结果生成完成，共 {} 条", specialList.size());
    }

    /**
     * 构建半部件 → {长度, 宽度, 幅宽} 工艺维度映射
     *
     * @param processList 示方书工艺信息列表
     * @return 半部件代码 → [长度, 宽度, 幅宽]
     */
    private Map<String, String[]> buildProcessDimMap(List<MdmConstructionProcess> processList) {
        Map<String, List<MdmConstructionProcess>> processGroupMap = processList.stream()
                .filter(p -> p.getProcessValue() != null)
                .collect(Collectors.groupingBy(MdmConstructionProcess::getMaterialCode));
        Map<String, String[]> processDimMap = new HashMap<>();
        for (Map.Entry<String, List<MdmConstructionProcess>> entry : processGroupMap.entrySet()) {
            Map<String, String> valueMap = new HashMap<>(3);
            for (MdmConstructionProcess proc : entry.getValue()) {
                valueMap.put(proc.getProcessCode(), proc.getProcessValue());
            }
            if (!valueMap.containsKey(ProcessCodeEnum.LENGTH.getCode())
                    || !valueMap.containsKey(ProcessCodeEnum.WIDTH.getCode())
                    || !valueMap.containsKey(ProcessCodeEnum.FABRIC_WIDTH.getCode())) {
                continue;
            }
            processDimMap.put(entry.getKey(), new String[]{
                    valueMap.get(ProcessCodeEnum.LENGTH.getCode()),
                    valueMap.get(ProcessCodeEnum.WIDTH.getCode()),
                    valueMap.get(ProcessCodeEnum.FABRIC_WIDTH.getCode())
            });
        }
        return processDimMap;
    }

    /**
     * 将 RawSpecialMaterialStock 转换为 SpecialMaterialInfoVo
     */
    private SpecialMaterialInfoVo convertToStockVo(RawSpecialMaterialStock stock) {
        SpecialMaterialStockVo stockVo = new SpecialMaterialStockVo();
        stockVo.setMaterialCode(stock.getMaterialCode());
        stockVo.setMaterialDesc(stock.getMaterialDesc());
        stockVo.setStandardLength(stock.getStandardLength() != null ? stock.getStandardLength().longValue() : 0L);
        stockVo.setOriStandardLength(stock.getStandardLength() != null ? stock.getStandardLength().longValue() : 0L);
        stockVo.setStock(stock.getStock() != null ? stock.getStock().longValue() : 0L);
        return SpecialMaterialInfoVo.createInitInfo(stockVo);
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }

}
