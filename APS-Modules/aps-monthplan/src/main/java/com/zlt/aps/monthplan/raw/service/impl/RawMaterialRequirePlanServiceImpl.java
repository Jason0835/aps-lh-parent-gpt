package com.zlt.aps.monthplan.raw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.maindata.service.IRawMaterialRequirePlanService;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.factory.mapper.FactoryMonthPlanProdFinalMapper;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：RawMaterialRequirePlanServiceImpl.java
 * 描    述：RawMaterialRequirePlanServiceImpl原材料需求计划业务层处理
 *@author zlt
 *@date 2025-12-08
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：zlt
 *     修改内容：...
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class RawMaterialRequirePlanServiceImpl extends AbstractDocService<RawMaterialRequirePlan>  implements IRawMaterialRequirePlanService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private FactoryMonthPlanProdFinalMapper factoryMonthPlanProdFinalMapper;

    @Autowired
    private MpProductionPredictionEntityMapper mpOrderPredictionMapper;

    @Autowired
    private MdmMaterialConsumeDetailMapper mdmMaterialConsumeDetailMapper;

    @Autowired
    private RawMaterialRequirePlanEntityMapper rawMaterialRequirePlanMapper;

    @Autowired
    private RawSpecialMaterialRatioEntityMapper rawSpecialMaterialRatioMapper;

    @Autowired
    private RawSpecialMaterialRecordEntityMapper rawSpecialMaterialRecordMapper;

    @Autowired
    private RawMaterialMonthDiffMapper rawMaterialMonthDiffMapper;

    @Autowired
    private RawWeekUsageGenerateServiceImpl rawWeekUsageGenerateService;

    // 常量定义
    private static final String LOCK_PREFIX = "CREATE_RAW_MATERIAL_REQUIRE_";
    private static final int EUDR_WEEK_THRESHOLD = 3425;
    private static final long LOCK_TIMEOUT = 300; // 5分钟
    private static final int BATCH_SIZE = 1000; // 批量处理大小

    @Override
    protected String getDocTypeCode() {
        return "RAW9003";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("RAW9003");
        return sysDocType;
    }

    @Override
    public String checkUnique(RawMaterialRequirePlan docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.rawMaterialRequirePlan.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult generateRawMaterialRequirePlan(String factoryCode, Integer year, Integer month) {
        try {
            // 1. 检查生成状态
            AjaxResult checkResult = checkGeneratingStatus(year, month);
            if (isSuccess(checkResult)) {
                return checkResult;
            }

            // 2. 加锁
            String lockKey = LOCK_PREFIX + year + month;
            if (!tryLock(lockKey)) {
                String message = StringUtils.format(
                        I18nUtil.getMessage("raw.material.require.plan.generating.wait"),
                        year, String.format("%02d", month)
                );
                return AjaxResult.error(message);
            }

            try {
                // 3. 检查月度生产计划是否已定稿
                if (!checkMonthPlanFinalized(year, month)) {
                    String message = StringUtils.format(
                            I18nUtil.getMessage("raw.material.require.plan.month.plan.not.finalized"),
                            year, String.format("%02d", month)
                    );
                    return AjaxResult.error(message);
                }

                // 4. 检查订单预测生产计划
                AjaxResult predictionCheck = checkOrderPrediction(year, month);
                if (isSuccess(predictionCheck)) {
                    return predictionCheck;
                }

                // 查询特殊材料清单
                List<RawSpecialMaterialRecord> specialMaterialRecordsList = getSpecialMaterialRecords(factoryCode);
                Set<String> specialMaterialCodes = extractSpecialMaterialCodes(specialMaterialRecordsList);

                // 5. 获取月度生产计划
                List<FactoryMonthPlanProdFinal> monthPlans = getMonthProductionPlan(year, month);

                // 6. 计算当月原材料需求量
                Map<String, RawMaterialRequirePlan> currentMonthRequirements = calculateCurrentMonthRequirements(
                        monthPlans, specialMaterialCodes);

                // 7. 获取预测计划并计算需求
                Map<String, RawMaterialRequirePlan> t1Requirements = calculateT1MonthRequirements(
                        year, month, specialMaterialCodes);
                Map<String, RawMaterialRequirePlan> t2Requirements = calculateT2MonthRequirements(
                        year, month, specialMaterialCodes);

                // 8. 特殊材料批次计算
                calculateSpecialMaterialBatches(year, month, currentMonthRequirements);

                // 9. 汇总并保存需求计划
                saveRawMaterialRequirePlan(year, month, currentMonthRequirements,
                        t1Requirements, t2Requirements, factoryCode);

                // 10. 生成差异数据
                generateDifferenceData(year, month, factoryCode);

                // 11. 生成周维度原材料用量记录
                generateWeekUsageRecords(factoryCode, year, month);

                String message = StringUtils.format(
                        I18nUtil.getMessage("raw.material.require.plan.generate.success"),
                        year, String.format("%02d", month)
                );
                return AjaxResult.success(message);

            } finally {
                // 释放锁
                unlock(lockKey);
            }

        } catch (Exception e) {
            log.error(I18nUtil.getMessage("raw.material.require.plan.generate.error"), e);
            String message = StringUtils.format(
                    I18nUtil.getMessage("raw.material.require.plan.generate.exception"),
                    e.getMessage()
            );
            return AjaxResult.error(message);
        }
    }

    /**
     * 获取特殊材料记录
     */
    private List<RawSpecialMaterialRecord> getSpecialMaterialRecords(String factoryCode) {
        QueryWrapper<RawSpecialMaterialRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("factory_code", factoryCode);
        return rawSpecialMaterialRecordMapper.selectList(queryWrapper);
    }

    /**
     * 提取特殊材料编码集合
     */
    private Set<String> extractSpecialMaterialCodes(List<RawSpecialMaterialRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptySet();
        }
        return records.stream()
                .map(RawSpecialMaterialRecord::getMaterialCode)
                .collect(Collectors.toSet());
    }

    /**
     * 生成周维度原材料用量记录
     */
    private void generateWeekUsageRecords(String factoryCode, Integer year, Integer month) {
        try {
            AjaxResult result = rawWeekUsageGenerateService
                    .generateWeekUsageForMonth(factoryCode, year, month);

            if (isSuccess(result)) {
                String message = StringUtils.format(
                        I18nUtil.getMessage("raw.material.week.usage.generate.success"),
                        factoryCode, year, month
                );
                log.info(message);
            } else {
                String message = StringUtils.format(
                        I18nUtil.getMessage("raw.material.week.usage.generate.fail"),
                        factoryCode, year, month, result.get("msg")
                );
                log.warn(message);
            }
        } catch (Exception e) {
            String message = StringUtils.format(
                    I18nUtil.getMessage("raw.material.week.usage.generate.exception"),
                    factoryCode, year, month
            );
            log.error(message, e);
        }
    }

    /**
     * 检查是否正在生成
     */
    @Override
    public AjaxResult checkGeneratingStatus(Integer year, Integer month) {
        String lockKey = LOCK_PREFIX + year + month;
        Boolean isLocked = redisTemplate.hasKey(lockKey);
        if (Boolean.TRUE.equals(isLocked)) {
            String message = StringUtils.format(
                    I18nUtil.getMessage("raw.material.require.plan.generating.wait"),
                    year, String.format("%02d", month)
            );
            return AjaxResult.error(message);
        }
        return AjaxResult.success();
    }

    /**
     * 获取默认年月
     */
    @Override
    public Map<String, Integer> getDefaultYearMonth() {
        LocalDate now = LocalDate.now();
        LocalDate nextMonth = now.plusMonths(1);
        Map<String, Integer> result = new HashMap<>();
        result.put("year", nextMonth.getYear());
        result.put("month", nextMonth.getMonthValue());
        return result;
    }

    /**
     * 检查AjaxResult是否成功
     */
    private boolean isSuccess(AjaxResult result) {
        Object codeObj = result.get("code");
        if (codeObj instanceof Integer) {
            return (Integer) codeObj != 200 && (Integer) codeObj != 0;
        }
        return true;
    }

    /**
     * 检查月度生产计划是否已定稿
     */
    private boolean checkMonthPlanFinalized(Integer year, Integer month) {
        QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", year)
                .eq("MONTH", month);
        Long count = factoryMonthPlanProdFinalMapper.selectCount(queryWrapper);
        return count > 0;
    }

    /**
     * 检查订单预测生产计划
     */
    private AjaxResult checkOrderPrediction(Integer year, Integer month) {
        LocalDate date = LocalDate.of(year, month, 1);

        QueryWrapper<MpProductionPrediction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", date.getYear())
                .eq("MONTH", date.getMonthValue());
        Long currencyCount = mpOrderPredictionMapper.selectCount(queryWrapper);

        if (currencyCount == 0) {
            String message = StringUtils.format(
                    I18nUtil.getMessage("raw.material.require.plan.prediction.plan.not.generated"),
                    date.getYear(), String.format("%02d", date.getMonthValue())
            );
            return AjaxResult.error(message);
        }

        // 如果是春节，检查T+2月
        if (isSpringFestivalMonth(year, month)) {
            QueryWrapper<MpProductionPrediction> t2QueryWrapper = new QueryWrapper<>();
            LocalDate t2Date = date.plusMonths(2);
            t2QueryWrapper.eq("YEAR", t2Date.getYear())
                    .eq("MONTH", t2Date.getMonthValue());
            Long t2Count = mpOrderPredictionMapper.selectCount(t2QueryWrapper);

            if (t2Count == 0) {
                String message = StringUtils.format(
                        I18nUtil.getMessage("raw.material.require.plan.prediction.plan.not.generated"),
                        t2Date.getYear(), String.format("%02d", t2Date.getMonthValue())
                );
                return AjaxResult.error(message);
            }
        }
        return AjaxResult.success();
    }

    /**
     * 判断是否为春节月份
     */
    private boolean isSpringFestivalMonth(Integer year, Integer month) {
        // 这里实现春节判断逻辑，简化处理
        // 实际应该查询节假日表
        return (month == 1 || month == 2);
    }

    /**
     * 获取月度生产计划
     */
    private List<FactoryMonthPlanProdFinal> getMonthProductionPlan(Integer year, Integer month) {
        QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", year)
                .eq("MONTH", month);
        return factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
    }

    /**
     * 计算当月原材料需求量（优化版）
     */
    private Map<String, RawMaterialRequirePlan> calculateCurrentMonthRequirements(
            List<FactoryMonthPlanProdFinal> monthPlans, Set<String> specialMaterialCodes) {

        if (CollectionUtils.isEmpty(monthPlans)) {
            return Collections.emptyMap();
        }

        // 1. 收集所有不重复的胎胚代码
        List<String> embryoCodes = monthPlans.stream()
                .map(FactoryMonthPlanProdFinal::getMainMaterialDesc)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        if (embryoCodes.isEmpty()) {
            return Collections.emptyMap();
        }

        // 2. 批量查询所有BOM结构（避免循环内查数据库）
        Map<String, List<MdmMaterialConsumeDetail>> bomMap = getBomDetailsByEmbryoCodes(embryoCodes);

        // 3. 计算需求
        Map<String, RawMaterialRequirePlan> requirements = new HashMap<>();

        monthPlans.forEach(plan -> {
            Integer totalQty = plan.getTotalQty();
            if (totalQty == null || totalQty == 0) {
                return;
            }

            String embryoCode = plan.getMainMaterialDesc();
            List<MdmMaterialConsumeDetail> bomDetails = bomMap.get(embryoCode);
            if (CollectionUtils.isEmpty(bomDetails)) {
                return;
            }

            // 计算每种物料的需求
            calculateMaterialRequirements(requirements, bomDetails, totalQty, specialMaterialCodes, plan);
        });

        return requirements;
    }

    /**
     * 批量查询BOM结构（优化数据库查询）
     */
    private Map<String, List<MdmMaterialConsumeDetail>> getBomDetailsByEmbryoCodes(List<String> embryoCodes) {
        QueryWrapper<MdmMaterialConsumeDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("EMBRYO_CODE", embryoCodes);
        // queryWrapper.eq("EMBRYO_VERSION", "1");

        List<MdmMaterialConsumeDetail> allBomDetails = mdmMaterialConsumeDetailMapper.selectList(queryWrapper);

        return allBomDetails.stream()
                .collect(Collectors.groupingBy(MdmMaterialConsumeDetail::getEmbryoCode));
    }

    /**
     * 计算物料需求
     */
    private void calculateMaterialRequirements(Map<String, RawMaterialRequirePlan> requirements,
                                               List<MdmMaterialConsumeDetail> bomDetails,
                                               Integer totalQty,
                                               Set<String> specialMaterialCodes,
                                               FactoryMonthPlanProdFinal plan) {

        bomDetails.forEach(detail -> {
            String materialCode = detail.getChildMaterialCode();
            String materialDesc = detail.getChildMaterialName();
            BigDecimal dosage = detail.getDosage() != null ? detail.getDosage() : BigDecimal.ZERO;
            String materialType = specialMaterialCodes.contains(materialCode) ? "02" : "01";

            // 计算需求数量
            BigDecimal requiredQty = BigDecimal.valueOf(totalQty).multiply(dosage);

            RawMaterialRequirePlan requirement = requirements.computeIfAbsent(materialCode,
                    k -> new RawMaterialRequirePlan(materialCode, materialDesc, materialType));

            // 判断EUDR
            boolean isEudr = isEudrPlan(plan);
            if (isEudr) {
                requirement.setCurMonthRudrQty(requiredQty);
            } else {
                requirement.setCurMonthQty(requiredQty);
            }
        });
    }

    /**
     * 判断是否为EUDR计划
     */
    private boolean isEudrPlan(FactoryMonthPlanProdFinal plan) {
        // 根据年周号判断 todo 月计划字段还没加
        // 这里需要根据实际情况获取年周号
        // 简化处理，假设通过其他方式判断
        return false;
    }

    /**
     * 计算T+1月需求
     */
    private Map<String, RawMaterialRequirePlan> calculateT1MonthRequirements(Integer year, Integer month,
                                                                             Set<String> specialMaterialCodes) {
        LocalDate date = LocalDate.of(year, month, 1);
        LocalDate t1Date = date.plusMonths(1);
        return calculatePredictionRequirements(t1Date.getYear(), t1Date.getMonthValue(), "T1", specialMaterialCodes);
    }

    /**
     * 计算T+2月需求
     */
    private Map<String, RawMaterialRequirePlan> calculateT2MonthRequirements(Integer year, Integer month,
                                                                             Set<String> specialMaterialCodes) {
        if (!isSpringFestivalMonth(year, month)) {
            return Collections.emptyMap();
        }

        LocalDate date = LocalDate.of(year, month, 1);
        LocalDate t2Date = date.plusMonths(2);
        return calculatePredictionRequirements(t2Date.getYear(), t2Date.getMonthValue(), "T2", specialMaterialCodes);
    }

    /**
     * 计算预测需求（优化版）
     */
    private Map<String, RawMaterialRequirePlan> calculatePredictionRequirements(Integer year, Integer month,
                                                                                String type,
                                                                                Set<String> specialMaterialCodes) {
        // 获取预测计划
        QueryWrapper<MpProductionPrediction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", year)
                .eq("MONTH", month);
        List<MpProductionPrediction> predictions = mpOrderPredictionMapper.selectList(queryWrapper);

        if (CollectionUtils.isEmpty(predictions)) {
            return Collections.emptyMap();
        }

        // 1. 收集所有不重复的物料编码
        List<String> materialCodes = predictions.stream()
                .map(MpProductionPrediction::getMaterialCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 2. 批量查询所有BOM结构
        Map<String, List<MdmMaterialConsumeDetail>> bomMap = getBomDetailsByMaterialCodes(materialCodes);

        // 3. 计算需求
        Map<String, RawMaterialRequirePlan> requirements = new HashMap<>();

        predictions.forEach(prediction -> {
            Integer productionQty = prediction.getProductionQty();
            if (productionQty == null || productionQty == 0) {
                return;
            }

            String materialCode = prediction.getMaterialCode();
            List<MdmMaterialConsumeDetail> bomDetails = bomMap.get(materialCode);
            if (CollectionUtils.isEmpty(bomDetails)) {
                return;
            }

            // 计算每种物料的需求
            calculatePredictionMaterialRequirements(requirements, bomDetails, productionQty,
                    specialMaterialCodes, type);
        });

        return requirements;
    }

    /**
     * 批量根据物料编码获取BOM结构（优化数据库查询）
     */
    private Map<String, List<MdmMaterialConsumeDetail>> getBomDetailsByMaterialCodes(List<String> materialCodes) {
        if (CollectionUtils.isEmpty(materialCodes)) {
            return Collections.emptyMap();
        }

        QueryWrapper<MdmMaterialConsumeDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("EMBRYO_CODE", materialCodes)
                .eq("IS_DELETE", 0);

        List<MdmMaterialConsumeDetail> allBomDetails = mdmMaterialConsumeDetailMapper.selectList(queryWrapper);

        return allBomDetails.stream()
                .collect(Collectors.groupingBy(MdmMaterialConsumeDetail::getEmbryoCode));
    }

    /**
     * 计算预测物料需求
     */
    private void calculatePredictionMaterialRequirements(Map<String, RawMaterialRequirePlan> requirements,
                                                         List<MdmMaterialConsumeDetail> bomDetails,
                                                         Integer productionQty,
                                                         Set<String> specialMaterialCodes,
                                                         String type) {

        bomDetails.forEach(detail -> {
            String materialCode = detail.getChildMaterialCode();
            String materialDesc = detail.getChildMaterialName();
            String materialType = specialMaterialCodes.contains(materialCode) ? "02" : "01";
            BigDecimal dosage = detail.getDosage() != null ? detail.getDosage() : BigDecimal.ZERO;

            BigDecimal requiredQty = BigDecimal.valueOf(productionQty).multiply(dosage);

            RawMaterialRequirePlan requirement = requirements.computeIfAbsent(materialCode,
                    k -> new RawMaterialRequirePlan(materialCode, materialDesc, materialType));

            // 根据预测月份设置不同的字段
            //todo 预测表增加年周号区分
            if ("T1".equals(type)) {
                requirement.setT1MonthQty(requiredQty);
            } else if ("T2".equals(type)) {
                requirement.setT2MonthQty(requiredQty);
            }
        });
    }

    /**
     * 计算EUDR需求
     */
    private void calculateEudrRequirements(Map<String, RawMaterialRequirePlan>... requirementMaps) {
        // 合并所有需求并计算EUDR
        // 实现EUDR区分逻辑，这里简化处理
        for (Map<String, RawMaterialRequirePlan> requirementMap : requirementMaps) {
            requirementMap.values().forEach(requirement -> {
                BigDecimal totalQty = requirement.getCurMonthQty();
                if (totalQty != null && totalQty.compareTo(BigDecimal.ZERO) > 0) {
                    requirement.setCurMonthRudrQty(totalQty.multiply(new BigDecimal("0.5")));
                }
            });
        }
    }

    /**
     * 保存原材料需求计划（优化版，批量插入）
     */
    private void saveRawMaterialRequirePlan(Integer year, Integer month,
                                            Map<String, RawMaterialRequirePlan> currentMonthRequirements,
                                            Map<String, RawMaterialRequirePlan> t1Requirements,
                                            Map<String, RawMaterialRequirePlan> t2Requirements,
                                            String factoryCode) {

        // 删除旧的计划
        deleteOldRequirements(year, month);

        // 合并所有需求
        Map<String, RawMaterialRequirePlan> allRequirements = mergeAllRequirements(
                currentMonthRequirements, t1Requirements, t2Requirements);

        if (allRequirements.isEmpty()) {
            log.warn(I18nUtil.getMessage("raw.material.require.plan.no.requirements.to.save"));
            return;
        }

        // 批量保存新的计划
        batchSaveRequirements(year, month, allRequirements, factoryCode);
    }

    /**
     * 删除旧的需求计划
     */
    private void deleteOldRequirements(Integer year, Integer month) {
        QueryWrapper<RawMaterialRequirePlan> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("YEAR", year)
                .eq("MONTH", month);
        rawMaterialRequirePlanMapper.delete(deleteWrapper);
    }

    /**
     * 合并所有需求
     */
    private Map<String, RawMaterialRequirePlan> mergeAllRequirements(
            Map<String, RawMaterialRequirePlan> currentMonthRequirements,
            Map<String, RawMaterialRequirePlan> t1Requirements,
            Map<String, RawMaterialRequirePlan> t2Requirements) {

        Map<String, RawMaterialRequirePlan> allRequirements = new HashMap<>();

        // 使用Stream API合并所有需求
        java.util.stream.Stream.of(currentMonthRequirements, t1Requirements, t2Requirements)
                .filter(Objects::nonNull)
                .forEach(map -> mergeRequirements(allRequirements, map));

        return allRequirements;
    }

    /**
     * 合并需求
     */
    private void mergeRequirements(Map<String, RawMaterialRequirePlan> target,
                                   Map<String, RawMaterialRequirePlan> source) {
        source.forEach((key, sourceReq) -> {
            RawMaterialRequirePlan existing = target.get(key);
            if (existing != null) {
                existing.merge(sourceReq);
            } else {
                target.put(key, sourceReq);
            }
        });
    }

    /**
     * 批量保存需求计划
     */
    private void batchSaveRequirements(Integer year, Integer month,
                                       Map<String, RawMaterialRequirePlan> requirements,
                                       String factoryCode) {

        String username = SecurityUtils.getUsername();
        if (username == null) {
            username = "system";
        }
        final String finalUsername = username;

        // 将Map转换为实体列表
        List<RawMaterialRequirePlan> planList = requirements.values().stream()
                .map(req -> convertToEntity(year, month, req, factoryCode, finalUsername))
                .collect(Collectors.toList());

        // 分批保存
        List<List<RawMaterialRequirePlan>> batches = splitIntoBatches(planList, BATCH_SIZE);

        batches.forEach(batch -> {
            if (!batch.isEmpty()) {
                // 使用MyBatis Plus的批量插入（需要配置批量插入插件）
                // 如果没有配置批量插件，可以使用SQL批量插入
                batchInsertRawMaterialRequirePlans(batch);
            }
        });

        String message = StringUtils.format(
                I18nUtil.getMessage("raw.material.require.plan.batch.save.complete"),
                planList.size()
        );
        log.info(message);
    }

    /**
     * 将列表分成多个批次
     */
    private <T> List<List<T>> splitIntoBatches(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            batches.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return batches;
    }

    /**
     * 批量插入原材料需求计划
     */
    private void batchInsertRawMaterialRequirePlans(List<RawMaterialRequirePlan> plans) {
        rawMaterialRequirePlanMapper.batchInsert(plans);
    }

    /**
     * 转换为实体
     */
    private RawMaterialRequirePlan convertToEntity(Integer year, Integer month,
                                                   RawMaterialRequirePlan requirement,
                                                   String factoryCode, String username) {
        RawMaterialRequirePlan plan = new RawMaterialRequirePlan();
        plan.setYear(year);
        plan.setMonth(month);
        plan.setMaterialCode(requirement.getMaterialCode());
        plan.setMaterialDesc(requirement.getMaterialDesc());
        plan.setMaterialType(requirement.getMaterialType());
        plan.setFactoryCode(factoryCode);

        if (requirement.getRemark() != null) {
            String purchaseBatchMsg = StringUtils.format(
                    I18nUtil.getMessage("raw.material.purchase.batch.quantity"),
                    requirement.getRemark()
            );
            plan.setRemark(purchaseBatchMsg);
        }

        // 处理所有BigDecimal字段，确保2位小数，不超过10位整数
        plan.setCurMonthQty(formatAndValidateBigDecimal(requirement.getCurMonthQty(), "CUR_MONTH_QTY"));
        plan.setCurMonthRudrQty(formatAndValidateBigDecimal(requirement.getCurMonthRudrQty(), "CUR_MONTH_RUDR_QTY"));
        plan.setTMonthQty(formatAndValidateBigDecimal(requirement.getTMonthQty(), "T_MONTH_QTY"));
        plan.setTMonthEudrQty(formatAndValidateBigDecimal(requirement.getTMonthEudrQty(), "T_MONTH_EUDR_QTY"));
        plan.setT1MonthQty(formatAndValidateBigDecimal(requirement.getT1MonthQty(), "T1_MONTH_QTY"));
        plan.setT1MonthEudrQty(formatAndValidateBigDecimal(requirement.getT1MonthEudrQty(), "T1_MONTH_EUDR_QTY"));
        plan.setT2MonthQty(formatAndValidateBigDecimal(requirement.getT2MonthQty(), "T2_MONTH_QTY"));
        plan.setT2MonthEudrQty(formatAndValidateBigDecimal(requirement.getT2MonthEudrQty(), "T2_MONTH_EUDR_QTY"));

        // 设置创建信息
        plan.setCreateBy(username);
        plan.setCreateTime(new Date());

        return plan;
    }

    /**
     * 格式化并验证BigDecimal值
     * 要求：2位小数，整数部分不超过10位
     */
    private BigDecimal formatAndValidateBigDecimal(BigDecimal value, String fieldName) {
        if (value == null) {
            return null;
        }

        try {
            // 1. 四舍五入到2位小数
            BigDecimal roundedValue = value.setScale(2, RoundingMode.HALF_UP);

            // 2. 检查整数部分长度（8位整数 + 2位小数）
            // DECIMAL(10,2) 的最大值是 99999999.99，最小值是 -99999999.99
            BigDecimal maxValue = new BigDecimal("99999999.99");
            BigDecimal minValue = new BigDecimal("-99999999.99");

            // 3. 检查是否超出范围
            if (roundedValue.compareTo(maxValue) > 0) {
                String message = StringUtils.format(
                        I18nUtil.getMessage("raw.material.field.value.exceed.max"),
                        fieldName, roundedValue, maxValue
                );
                log.warn(message);
                return maxValue;
            }

            if (roundedValue.compareTo(minValue) < 0) {
                String message = StringUtils.format(
                        I18nUtil.getMessage("raw.material.field.value.exceed.min"),
                        fieldName, roundedValue, minValue
                );
                log.warn(message);
                return minValue;
            }

            // 4. 检查整数部分位数
            String strValue = roundedValue.toPlainString();
            int dotIndex = strValue.indexOf('.');
            if (dotIndex == -1) {
                // 没有小数点，整个都是整数部分
                if (strValue.length() > 8) {
                    // 取前8位
                    String limitedInt = strValue.substring(0, 8);
                    return new BigDecimal(limitedInt + ".00");
                }
            } else {
                String integerPart = strValue.substring(0, dotIndex);
                if (integerPart.length() > 8) {
                    // 取前8位
                    String limitedInt = integerPart.substring(0, 8);
                    return new BigDecimal(limitedInt + strValue.substring(dotIndex));
                }
            }

            return roundedValue;
        } catch (Exception e) {
            String message = StringUtils.format(
                    I18nUtil.getMessage("raw.material.field.format.error"),
                    fieldName, value, e.getMessage()
            );
            log.error(message, e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算特殊材料批次（优化版）
     */
    private void calculateSpecialMaterialBatches(Integer year, Integer month,
                                                 Map<String, RawMaterialRequirePlan> requirements) {

        if (requirements.isEmpty()) {
            return;
        }

        // 1. 批量查询特殊材料比例配置
        List<String> materialCodes = new ArrayList<>(requirements.keySet());
        Map<String, List<RawSpecialMaterialRatio>> ratioMap = getSpecialMaterialRatios(materialCodes);

        // 2. 计算采购批次
        requirements.forEach((materialCode, requirement) -> {
            if (requirement.getCurMonthQty() == null) {
                return;
            }

            BigDecimal totalRequirement = requirement.getCurMonthQty();
            List<RawSpecialMaterialRatio> ratios = ratioMap.get(materialCode);

            if (CollectionUtils.isEmpty(ratios)) {
                return;
            }

            // 为每种比例计算采购批次
            calculateBatchesForMaterial(requirement, totalRequirement, ratios);
        });
    }

    /**
     * 批量查询特殊材料比例配置
     */
    private Map<String, List<RawSpecialMaterialRatio>> getSpecialMaterialRatios(List<String> materialCodes) {
        QueryWrapper<RawSpecialMaterialRatio> ratioQuery = new QueryWrapper<>();
        ratioQuery.in("MATERIAL_CODE", materialCodes);

        List<RawSpecialMaterialRatio> allRatios = rawSpecialMaterialRatioMapper.selectList(ratioQuery);

        return allRatios.stream()
                .collect(Collectors.groupingBy(RawSpecialMaterialRatio::getMaterialCode));
    }

    /**
     * 为材料计算采购批次
     */
    private void calculateBatchesForMaterial(RawMaterialRequirePlan requirement,
                                             BigDecimal totalRequirement,
                                             List<RawSpecialMaterialRatio> ratios) {

        StringBuilder batchInfo = new StringBuilder();

        ratios.forEach(ratio -> {
            BigDecimal proportion = ratio.getRatio();
            BigDecimal standardLength = BigDecimal.valueOf(ratio.getStandardLength());

            // 计算该规格的需求量 = 总需求量 * 比例
            BigDecimal specRequirement = totalRequirement.multiply(proportion);

            // 计算采购批次 = 需求量 / 标准长度，向上取整
            BigDecimal purchaseBatch = specRequirement
                    .divide(standardLength, 2, RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.CEILING);

            // 记录批次信息
            if (batchInfo.length() > 0) {
                batchInfo.append(", ");
            }
            batchInfo.append(ratio.getStandardLength())
                    .append(":")
                    .append(purchaseBatch.intValue());

            // 使用多语言日志
            String message = StringUtils.format(
                    I18nUtil.getMessage("raw.material.special.material.batch.calculation"),
                    requirement.getMaterialCode(), standardLength, specRequirement, purchaseBatch
            );
            log.debug(message);
        });

        // 保存批次计算结果
        if (batchInfo.length() > 0) {
            requirement.setRemark(batchInfo.toString());
        }
    }

    /**
     * 生成差异数据（优化版）
     */
    private void generateDifferenceData(Integer year, Integer month, String factoryCode) {
        // 获取当前月和上个月的需求
        Map<String, RawMaterialRequirePlan> currentRequirements = getRequirementsByMonth(year, month);
        Map<String, RawMaterialRequirePlan> previousRequirements = getPreviousMonthRequirements(year, month);

        // 删除旧的差异数据
        deleteOldDifferenceData(year, month, factoryCode);

        // 计算差异并批量保存
        List<RawMaterialMonthDiff> diffRecords = calculateDifferences(
                year, month, currentRequirements, previousRequirements, factoryCode);

        if (!diffRecords.isEmpty()) {
            batchSaveDifferenceData(diffRecords);
        }
    }

    /**
     * 删除旧的差异数据
     */
    private void deleteOldDifferenceData(Integer year, Integer month, String factoryCode) {
        QueryWrapper<RawMaterialMonthDiff> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", year)
                .eq("MONTH", month)
                .eq("FACTORY_CODE", factoryCode);
        rawMaterialMonthDiffMapper.delete(queryWrapper);
    }

    /**
     * 计算差异
     */
    private List<RawMaterialMonthDiff> calculateDifferences(Integer year, Integer month,
                                                            Map<String, RawMaterialRequirePlan> current,
                                                            Map<String, RawMaterialRequirePlan> previous,
                                                            String factoryCode) {

        List<RawMaterialMonthDiff> diffRecords = new ArrayList<>();
        String username = SecurityUtils.getUsername();
        if (username == null) {
            username = "system";
        }
        final String finalUsername = username;

        // 多语言键值
        String diffTypeNew = I18nUtil.getMessage("raw.material.diff.type.new");
        String diffTypeIncrease = I18nUtil.getMessage("raw.material.diff.type.increase");
        String diffTypeDecrease = I18nUtil.getMessage("raw.material.diff.type.decrease");

        // 1. 处理当前月有的材料
        current.forEach((materialCode, currentReq) -> {
            RawMaterialRequirePlan prevReq = previous.get(materialCode);
            BigDecimal curQty = currentReq.getCurMonthQty() != null ? currentReq.getCurMonthQty() : BigDecimal.ZERO;
            BigDecimal prevQty = prevReq != null && prevReq.getCurMonthQty() != null
                    ? prevReq.getCurMonthQty()
                    : BigDecimal.ZERO;

            BigDecimal diffValue = curQty.subtract(prevQty);

            if (prevReq == null) {
                // 新增的原材料
                diffRecords.add(createDiffRecord(year, month, materialCode, diffTypeNew,
                        BigDecimal.ZERO, curQty, factoryCode, currentReq.getMaterialDesc(), finalUsername));
            } else if (diffValue.compareTo(BigDecimal.ZERO) != 0) {
                // 数量有变化
                String diffType = diffValue.compareTo(BigDecimal.ZERO) > 0 ? diffTypeIncrease : diffTypeDecrease;
                diffRecords.add(createDiffRecord(year, month, materialCode, diffType,
                        prevQty, curQty, factoryCode, currentReq.getMaterialDesc(), finalUsername));
            }
        });

        // 2. 处理上个月有但当前月没有的材料
        previous.forEach((materialCode, prevReq) -> {
            if (!current.containsKey(materialCode)) {
                BigDecimal prevQty = prevReq.getCurMonthQty() != null ? prevReq.getCurMonthQty() : BigDecimal.ZERO;
                // 减少的原材料
                diffRecords.add(createDiffRecord(year, month, materialCode, diffTypeDecrease,
                        prevQty, BigDecimal.ZERO, factoryCode, prevReq.getMaterialDesc(), finalUsername));
            }
        });

        return diffRecords;
    }

    /**
     * 创建差异记录
     */
    private RawMaterialMonthDiff createDiffRecord(Integer year, Integer month, String materialCode,
                                                  String diffType, BigDecimal prevQty, BigDecimal curQty,
                                                  String factoryCode, String materialDesc, String username) {

        RawMaterialMonthDiff diffRecord = new RawMaterialMonthDiff();
        diffRecord.setFactoryCode(factoryCode);
        diffRecord.setYear(year);
        diffRecord.setMonth(month);
        diffRecord.setMaterialCode(materialCode);
        diffRecord.setMaterialDesc(materialDesc);
        diffRecord.setDiffType(diffType);
        diffRecord.setPrevMonthQty(prevQty);
        diffRecord.setCurMonthQty(curQty);
        diffRecord.setDiffQty(curQty.subtract(prevQty).abs());
        diffRecord.setCreateBy(username);
        diffRecord.setCreateTime(new Date());

        return diffRecord;
    }

    /**
     * 批量保存差异数据
     */
    private void batchSaveDifferenceData(List<RawMaterialMonthDiff> diffRecords) {
        // 分批保存
        List<List<RawMaterialMonthDiff>> batches = splitIntoBatches(diffRecords, BATCH_SIZE);

        batches.forEach(batch -> {
            if (!batch.isEmpty()) {
                // 使用MyBatis Plus的批量插入
                rawMaterialMonthDiffMapper.batchInsert(batch);
            }
        });

        String message = StringUtils.format(
                I18nUtil.getMessage("raw.material.diff.data.batch.save.complete"),
                diffRecords.size()
        );
        log.info(message);
    }

    /**
     * 获取指定月份的需求
     */
    private Map<String, RawMaterialRequirePlan> getRequirementsByMonth(Integer year, Integer month) {
        QueryWrapper<RawMaterialRequirePlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", year)
                .eq("MONTH", month);
        List<RawMaterialRequirePlan> plans = rawMaterialRequirePlanMapper.selectList(queryWrapper);

        return plans.stream()
                .collect(Collectors.toMap(
                        RawMaterialRequirePlan::getMaterialCode,
                        this::convertToRequirement,
                        // 如果有重复，保留第一个
                        (oldValue, newValue) -> oldValue
                ));
    }

    /**
     * 转换为需求对象
     */
    private RawMaterialRequirePlan convertToRequirement(RawMaterialRequirePlan plan) {
        RawMaterialRequirePlan requirement = new RawMaterialRequirePlan();
        requirement.setMaterialCode(plan.getMaterialCode());
        requirement.setMaterialDesc(plan.getMaterialDesc());
        requirement.setFactoryCode(plan.getFactoryCode());
        requirement.setMaterialType(plan.getMaterialType());

        requirement.setCurMonthQty(plan.getCurMonthQty());
        requirement.setCurMonthRudrQty(plan.getCurMonthRudrQty());
        requirement.setTMonthQty(plan.getTMonthQty());
        requirement.setTMonthEudrQty(plan.getTMonthEudrQty());
        requirement.setT1MonthQty(plan.getT1MonthQty());
        requirement.setT1MonthEudrQty(plan.getT1MonthEudrQty());
        requirement.setT2MonthQty(plan.getT2MonthQty());
        requirement.setT2MonthEudrQty(plan.getT2MonthEudrQty());

        return requirement;
    }

    /**
     * 获取上个月的需求
     */
    private Map<String, RawMaterialRequirePlan> getPreviousMonthRequirements(Integer year, Integer month) {
        LocalDate date = LocalDate.of(year, month, 1);
        LocalDate prevDate = date.minusMonths(1);
        return getRequirementsByMonth(prevDate.getYear(), prevDate.getMonthValue());
    }

    /**
     * 尝试加锁
     */
    private boolean tryLock(String lockKey) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(LOCK_TIMEOUT)));
    }

    /**
     * 释放锁
     */
    private void unlock(String lockKey) {
        redisTemplate.delete(lockKey);
    }
}