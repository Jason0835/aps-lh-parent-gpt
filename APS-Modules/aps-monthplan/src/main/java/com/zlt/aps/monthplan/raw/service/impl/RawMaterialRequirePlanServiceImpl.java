package com.zlt.aps.monthplan.raw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

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
                return AjaxResult.error(String.format("正在生成%d年%02d月原材料需求计划，请稍候！", year, month));
            }

            try {
                // 3. 检查月度生产计划是否已定稿
                if (!checkMonthPlanFinalized(year, month)) {
                    return AjaxResult.error(String.format("%d年%02d月的月度计划还没有定稿，请先生成并定稿", year, month));
                }

                // 4. 检查订单预测生产计划
                AjaxResult predictionCheck = checkOrderPrediction(year, month);
                if (isSuccess(predictionCheck)) {
                    return predictionCheck;
                }

                // 5. 获取月度生产计划
                List<FactoryMonthPlanProdFinal> monthPlans = getMonthProductionPlan(year, month);

                // 6. 计算当月原材料需求量
                Map<String, RawMaterialRequirePlan> currentMonthRequirements = calculateCurrentMonthRequirements(monthPlans);

                // 7. 获取预测计划并计算需求
                Map<String, RawMaterialRequirePlan> t1Requirements = calculateT1MonthRequirements(year, month);
                Map<String, RawMaterialRequirePlan> t2Requirements = calculateT2MonthRequirements(year, month);

                // 8. 计算EUDR和非EUDR
                //calculateEudrRequirements(currentMonthRequirements, t1Requirements, t2Requirements);

                // 9. 特殊材料批次计算
                calculateSpecialMaterialBatches(year, month, currentMonthRequirements);

                // 10. 汇总并保存需求计划
                saveRawMaterialRequirePlan(year, month, currentMonthRequirements, t1Requirements, t2Requirements, factoryCode);

                // 11. 生成差异数据
                generateDifferenceData(year, month,factoryCode);

                // 12. 生成周维度原材料用量记录
                generateWeekUsageRecords(factoryCode, year, month);

                return AjaxResult.success(String.format("%d年%02d月原材料需求计划生成完成", year, month));

            } finally {
                // 释放锁
                unlock(lockKey);
            }

        } catch (Exception e) {
            log.error("生成原材料需求计划失败", e);
            return AjaxResult.error("生成原材料需求计划失败：" + e.getMessage());
        }
    }


    /**
     * 生成周维度原材料用量记录
     */
    private void generateWeekUsageRecords(String factoryCode, Integer year, Integer month) {
        try {
                try {
                    AjaxResult result = rawWeekUsageGenerateService
                            .generateWeekUsageForMonth(factoryCode, year, month);

                    if (isSuccess(result)) {
                        log.info("生成周维度用量记录成功，工厂：{}，年月：{}-{}",
                                factoryCode, year, month);
                    } else {
                        log.warn("生成周维度用量记录失败，工厂：{}，年月：{}-{}，错误：{}",
                                factoryCode, year, month, result.get("msg"));
                    }
                } catch (Exception e) {
                    log.error("生成周维度用量记录异常，工厂：{}，年月：{}-{}",
                            factoryCode, year, month, e);
                }
        } catch (Exception e) {
            log.error("生成周维度用量记录总体失败", e);
            // 不抛出异常，避免影响主流程
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
            return AjaxResult.error(String.format("正在生成%d年%02d月原材料需求计划，请稍候！", year, month));
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
        // 计算T+1月
        LocalDate date = LocalDate.of(year, month, 1);

        QueryWrapper<MpProductionPrediction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", date.getYear())
                .eq("MONTH", date.getMonthValue());
        Long currencyCount = mpOrderPredictionMapper.selectCount(queryWrapper);

        if (currencyCount == 0) {
            return AjaxResult.error(String.format("%d年%02d月的预测月生产计划还没有生成，请先生成",
                    date.getYear(), date.getMonthValue()));
        }

        // 如果是春节，检查T+2月
        if (isSpringFestivalMonth(year, month)) {
            List<MpProductionPrediction> mpProductionPredictionList = mpOrderPredictionMapper.selectList(queryWrapper);

            for (MpProductionPrediction mpProductionPrediction : mpProductionPredictionList){
                if (mpProductionPrediction == null || mpProductionPrediction.getMonth3() == null || mpProductionPrediction.getMonth3() == 0) {
                    return AjaxResult.error(String.format("%d年%02d月的预测月生产计划还没有生成，请先生成",
                            date.getYear(), date.getMonthValue()));
                }
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
     * 计算当月原材料需求量
     */
    private Map<String, RawMaterialRequirePlan> calculateCurrentMonthRequirements(
            List<FactoryMonthPlanProdFinal> monthPlans) {

        Map<String, RawMaterialRequirePlan> requirements = new HashMap<>();

        for (FactoryMonthPlanProdFinal plan : monthPlans) {
            // 获取总产量
            Integer totalQty = plan.getTotalQty();
            if (totalQty == null || totalQty == 0) {
                continue;
            }

            // 根据胎胚代码获取BOM结构
            //todo 需要用SKU与施工关系拿胎胚版本
            List<MdmMaterialConsumeDetail> bomDetails = getBomDetails(plan.getMainMaterialDesc());

            // 计算原材料需求
            for (MdmMaterialConsumeDetail detail : bomDetails) {
                String materialCode = detail.getChildMaterialCode();
                BigDecimal dosage = detail.getDosage();

                if (dosage == null) {
                    log.warn("物料 {} 的用量为空，设为0", materialCode);
                    dosage = BigDecimal.ZERO;
                }

                // 计算需求数量 = 产量 * 单胎消耗量
                BigDecimal requiredQty = BigDecimal.valueOf(totalQty).multiply(dosage);

                RawMaterialRequirePlan requirement = requirements.getOrDefault(materialCode,
                        new RawMaterialRequirePlan(materialCode));

                // 判断EUDR
                boolean isEudr = isEudrPlan(plan);
                if (isEudr) {
                    requirement.setCurMonthRudrQty(requiredQty);
                }else {
                    requirement.setCurMonthQty(requiredQty);
                }

                requirements.put(materialCode, requirement);
            }
        }

        return requirements;
    }

    /**
     * 获取BOM结构详情
     */
    private List<MdmMaterialConsumeDetail> getBomDetails(String embryoCode) {
        QueryWrapper<MdmMaterialConsumeDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("EMBRYO_CODE", embryoCode);
        //queryWrapper.eq("EMBRYO_VERSION", "1");
        return mdmMaterialConsumeDetailMapper.selectList(queryWrapper);
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
    private Map<String, RawMaterialRequirePlan> calculateT1MonthRequirements(Integer year, Integer month) {
        LocalDate date = LocalDate.of(year, month, 1);
        LocalDate t1Date = date.plusMonths(0);

        return calculatePredictionRequirements(t1Date.getYear(), t1Date.getMonthValue(), "T1");
    }

    /**
     * 计算T+2月需求
     */
    private Map<String, RawMaterialRequirePlan> calculateT2MonthRequirements(Integer year, Integer month) {
        if (!isSpringFestivalMonth(year, month)) {
            return new HashMap<>();
        }

        LocalDate date = LocalDate.of(year, month, 1);
        LocalDate t2Date = date.plusMonths(0);

        return calculatePredictionRequirements(t2Date.getYear(), t2Date.getMonthValue(), "T2");
    }

    /**
     * 计算预测需求
     */
    private Map<String, RawMaterialRequirePlan> calculatePredictionRequirements(Integer year, Integer month, String type) {
        Map<String, RawMaterialRequirePlan> requirements = new HashMap<>();

        // 获取预测计划
        QueryWrapper<MpProductionPrediction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", year)
                .eq("MONTH", month);
        List<MpProductionPrediction> predictions = mpOrderPredictionMapper.selectList(queryWrapper);

        for (MpProductionPrediction prediction : predictions) {
            Integer productionQty = prediction.getProductionQty();
            if (productionQty == null || productionQty == 0) {
                continue;
            }

            // 这里需要根据物料编码获取BOM结构
            // 简化处理，假设可以通过物料编码获取胎胚代码
            List<MdmMaterialConsumeDetail> bomDetails = getBomDetailsByMaterialCode(prediction.getMaterialCode());

            for (MdmMaterialConsumeDetail detail : bomDetails) {
                String materialCode = detail.getChildMaterialCode();
                BigDecimal dosage = detail.getDosage();

                BigDecimal requiredQty = BigDecimal.valueOf(productionQty).multiply(dosage);

                RawMaterialRequirePlan requirement = requirements.getOrDefault(materialCode,
                        new RawMaterialRequirePlan(materialCode));

                // 根据预测月份设置不同的字段
                //todo 预测表增加年周号区分
                if ("T1".equals(type)) {
                    requirement.setT1MonthQty(requiredQty);
                } else if ("T2".equals(type)) {
                    requirement.setT2MonthQty(requiredQty);
                }
                requirements.put(materialCode, requirement);
            }
        }
        return requirements;
    }

    /**
     * 根据物料编码获取BOM结构
     */
    private List<MdmMaterialConsumeDetail> getBomDetailsByMaterialCode(String materialCode) {
        // 这里需要实现根据成品物料编码查找对应的胎胚代码和BOM
        // 简化处理，查询所有相关的BOM
        QueryWrapper<MdmMaterialConsumeDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("PARENT_MATERIAL_CODE", materialCode)
                .eq("IS_DELETE", 0);
        return mdmMaterialConsumeDetailMapper.selectList(queryWrapper);
    }

    /**
     * 计算EUDR需求
     */
    private void calculateEudrRequirements(Map<String, RawMaterialRequirePlan>... requirementMaps) {
        // 合并所有需求并计算EUDR
        // 实现EUDR区分逻辑，这里简化处理
        for (Map<String, RawMaterialRequirePlan> requirementMap : requirementMaps) {
            for (RawMaterialRequirePlan requirement : requirementMap.values()) {
                // 实际中需要根据计划类型判断EUDR
                // 这里假设EUDR占总需求的50%
                BigDecimal totalQty = requirement.getCurMonthQty();
                if (totalQty.compareTo(BigDecimal.ZERO) > 0) {
                    requirement.setCurMonthRudrQty(totalQty.multiply(new BigDecimal("0.5")));
                }
            }
        }
    }

    /**
     * 保存原材料需求计划
     */
    private void saveRawMaterialRequirePlan(Integer year, Integer month,
                                            Map<String, RawMaterialRequirePlan> currentMonthRequirements,
                                            Map<String, RawMaterialRequirePlan> t1Requirements,
                                            Map<String, RawMaterialRequirePlan> t2Requirements, String factoryCode) {

        // 删除旧的计划
        QueryWrapper<RawMaterialRequirePlan> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("YEAR", year)
                .eq("MONTH", month);
        rawMaterialRequirePlanMapper.delete(deleteWrapper);

        // 合并所有需求
        Map<String, RawMaterialRequirePlan> allRequirements = new HashMap<>();
        mergeRequirements(allRequirements, currentMonthRequirements);
        mergeRequirements(allRequirements, t1Requirements);
        mergeRequirements(allRequirements, t2Requirements);

        // 保存新的计划
        for (RawMaterialRequirePlan requirement : allRequirements.values()) {
            RawMaterialRequirePlan plan = convertToEntity(year, month, requirement, factoryCode);
            rawMaterialRequirePlanMapper.insert(plan);
        }
    }

    /**
     * 合并需求
     */
    private void mergeRequirements(Map<String, RawMaterialRequirePlan> target,
                                   Map<String, RawMaterialRequirePlan> source) {
        for (Map.Entry<String, RawMaterialRequirePlan> entry : source.entrySet()) {
            String materialCode = entry.getKey();
            RawMaterialRequirePlan sourceReq = entry.getValue();

            if (target.containsKey(materialCode)) {
                target.get(materialCode).merge(sourceReq);
            } else {
                target.put(materialCode, sourceReq);
            }
        }
    }

    /**
     * 转换为实体
     */
    private RawMaterialRequirePlan convertToEntity(Integer year, Integer month,
                                                   RawMaterialRequirePlan requirement, String factoryCode) {
        RawMaterialRequirePlan plan = new RawMaterialRequirePlan();
        plan.setYear(year);
        plan.setMonth(month);
        plan.setMaterialCode(requirement.getMaterialCode());
        plan.setFactoryCode(factoryCode);

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
        String username = SecurityUtils.getUsername();
        if (username == null) {
            username = "system";
        }
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
                log.warn("字段 {} 的值 {} 超出最大范围，将被限制为 {}",
                        fieldName, roundedValue, maxValue);
                return maxValue;
            }

            if (roundedValue.compareTo(minValue) < 0) {
                log.warn("字段 {} 的值 {} 超出最小范围，将被限制为 {}",
                        fieldName, roundedValue, minValue);
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
            log.error("格式化字段 {} 的值 {} 时发生错误: {}",
                    fieldName, value, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }


    /**
     * 计算特殊材料批次
     */
    private void calculateSpecialMaterialBatches(Integer year, Integer month,
                                                 Map<String, RawMaterialRequirePlan> requirements) {

        // 获取特殊材料列表
        QueryWrapper<RawMaterialRequirePlan> specialMaterialQuery = new QueryWrapper<>();
        specialMaterialQuery.eq("YEAR", year)
                .eq("MONTH", month);
        List<RawMaterialRequirePlan> specialMaterials = rawMaterialRequirePlanMapper.selectList(specialMaterialQuery);

        for (RawMaterialRequirePlan specialMaterial : specialMaterials) {
            String materialCode = specialMaterial.getMaterialCode();
            RawMaterialRequirePlan requirement = requirements.get(materialCode);

            if (requirement != null && requirement.getCurMonthQty() != null) {
                BigDecimal totalRequirement = requirement.getCurMonthQty();

                // 获取特殊材料比例配置
                QueryWrapper<RawSpecialMaterialRatio> ratioQuery = new QueryWrapper<>();
                ratioQuery.eq("MATERIAL_CODE", materialCode);
                List<RawSpecialMaterialRatio> ratios = rawSpecialMaterialRatioMapper.selectList(ratioQuery);

                // 计算采购批次
                for (RawSpecialMaterialRatio ratio : ratios) {
                    BigDecimal proportion = ratio.getRatio();
                    BigDecimal standardLength = BigDecimal.valueOf(ratio.getStandardLength());

                    // 计算该规格的需求量 = 总需求量 * 比例
                    BigDecimal specRequirement = totalRequirement.multiply(proportion);

                    // 计算采购批次 = 需求量 / 标准长度，向上取整
                    BigDecimal purchaseBatch = specRequirement.divide(standardLength,
                            RoundingMode.CEILING);

                    // 保存批次计算结果
                    saveBatchCalculation(materialCode, ratio.getStandardLength(),
                            specRequirement, purchaseBatch.intValue(),requirement);
                }
            }
        }
    }

    /**
     * 保存批次计算结果
     */
    private void saveBatchCalculation(String materialCode, Integer standardLength,
                                      BigDecimal requirement, Integer purchaseBatch, RawMaterialRequirePlan rawMaterialRequirePlan) {
        rawMaterialRequirePlan.setRemark(purchaseBatch.toString());
        log.info("特殊材料批次计算 - 物料编码: {}, 标准长度: {}, 需求量: {}, 采购批次: {}",
                materialCode, standardLength, requirement, purchaseBatch);
    }

    /**
     * 生成差异数据
     */
    private void generateDifferenceData(Integer year, Integer month, String factoryCode) {
        // 计算当前月与上个月的差异
        Map<String, RawMaterialRequirePlan> currentRequirements = getRequirementsByMonth(year, month);
        Map<String, RawMaterialRequirePlan> previousRequirements = getPreviousMonthRequirements(year, month);

        // 计算差异并保存
        calculateAndSaveDifferences(year, month, currentRequirements, previousRequirements, factoryCode);
    }

    /**
     * 获取指定月份的需求
     */
    private Map<String, RawMaterialRequirePlan> getRequirementsByMonth(Integer year, Integer month) {
        QueryWrapper<RawMaterialRequirePlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", year)
                .eq("MONTH", month);
        List<RawMaterialRequirePlan> plans = rawMaterialRequirePlanMapper.selectList(queryWrapper);

        Map<String, RawMaterialRequirePlan> requirements = new HashMap<>();
        for (RawMaterialRequirePlan plan : plans) {
            RawMaterialRequirePlan requirement = new RawMaterialRequirePlan(plan.getMaterialCode());
            requirement.setCurMonthQty(plan.getCurMonthQty());
            requirement.setCurMonthRudrQty(plan.getCurMonthRudrQty());
            requirement.setTMonthQty(plan.getTMonthQty());
            requirement.setTMonthEudrQty(plan.getTMonthEudrQty());
            requirement.setT1MonthQty(plan.getT1MonthQty());
            requirement.setT1MonthEudrQty(plan.getT1MonthEudrQty());
            requirement.setT2MonthQty(plan.getT2MonthQty());
            requirement.setT2MonthEudrQty(plan.getT2MonthEudrQty());

            requirements.put(plan.getMaterialCode(), requirement);
        }
        return requirements;
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
     * 计算并保存差异
     */
    private void calculateAndSaveDifferences(Integer year, Integer month,
                                             Map<String, RawMaterialRequirePlan> current,
                                             Map<String, RawMaterialRequirePlan> previous, String factoryCode) {

        // 计算新增的原材料
        for (String materialCode : current.keySet()) {
            RawMaterialRequirePlan currentReq = current.get(materialCode);
            RawMaterialRequirePlan prevReq = previous.get(materialCode);

            if (prevReq == null) {
                // 新增的原材料
                saveDifferenceRecord(year, month, materialCode, "新增",
                        BigDecimal.ZERO, currentReq.getCurMonthQty(), factoryCode);
            } else {
                // 计算差异
                BigDecimal diff = currentReq.getCurMonthQty().subtract(prevReq.getCurMonthQty());
                if (diff.compareTo(BigDecimal.ZERO) != 0) {
                    String diffType = diff.compareTo(BigDecimal.ZERO) > 0 ? "增加" : "减少";
                    saveDifferenceRecord(year, month, materialCode, diffType,
                            prevReq.getCurMonthQty(), currentReq.getCurMonthQty(), factoryCode);
                }
            }
        }

        // 计算减少的原材料
        for (String materialCode : previous.keySet()) {
            if (!current.containsKey(materialCode)) {
                RawMaterialRequirePlan prevReq = previous.get(materialCode);
                // 减少的原材料
                saveDifferenceRecord(year, month, materialCode, "减少",
                        prevReq.getCurMonthQty(), BigDecimal.ZERO, factoryCode);
            }
        }
    }

    /**
     * 保存差异记录
     */
    private void saveDifferenceRecord(Integer year, Integer month, String materialCode,
                                      String diffType, BigDecimal prevQty, BigDecimal curQty, String factoryCode) {
        RawMaterialMonthDiff diffRecord = new RawMaterialMonthDiff();
        diffRecord.setFactoryCode(factoryCode);
        diffRecord.setYear(year);
        diffRecord.setMonth(month);
        diffRecord.setMaterialCode(materialCode);

        diffRecord.setDiffType(diffType);
        diffRecord.setPrevMonthQty(prevQty);
        diffRecord.setCurMonthQty(curQty);
        diffRecord.setDiffQty(curQty.subtract(prevQty).abs());

        // 设置创建信息
        String username = SecurityUtils.getUsername();
        if (username == null) {
            username = "system";
        }
        diffRecord.setCreateBy(username);
        diffRecord.setCreateTime(new Date());

        rawMaterialMonthDiffMapper.insert(diffRecord);
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



