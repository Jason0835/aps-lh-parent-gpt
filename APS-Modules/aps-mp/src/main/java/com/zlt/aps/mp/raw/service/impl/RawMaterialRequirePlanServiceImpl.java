package com.zlt.aps.mp.raw.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.constant.Constant;
import com.zlt.aps.constant.StringConstant;
import com.zlt.aps.maindata.mapper.*;
import com.zlt.aps.maindata.service.IRawMaterialRequirePlanService;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.api.domain.vo.PredictionVersionInfoVo;
import com.zlt.aps.mp.common.utils.DistributedVersionGenerator;
import com.zlt.aps.mp.demand.mapper.DpOrderOffsetDetailEntityMapper;
import com.zlt.aps.mp.demand.mapper.MpPredictionDetailEntityMapper;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanMouldDayResultEntityMapper;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanProdFinalMapper;
import com.zlt.aps.mp.factory.mapper.MpFactoryProductionVersionMapper;
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
 *@author nick
 *@version 1.0
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
    private MpFactoryProductionVersionMapper mpFactoryProductionVersionMapper;

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

    @Autowired
    private MpProductionPredictionEntityMapper mpProductionPredictionMapper;

    @Autowired
    private MpPredictionDetailEntityMapper mpPredictionDetailMapper;

    @Autowired
    private FactoryMonthPlanMouldDayResultEntityMapper factoryMonthPlanMouldDayResultMapper;

    @Autowired
    private DpOrderOffsetDetailEntityMapper orderOffsetDetailMapper;

    /**
     * redis 锁前缀
     */
    private static final String LOCK_PREFIX = "CREATE_RAW_MATERIAL_REQUIRE_";

    /**
     * redis 锁超时时间
     */
    private static final long LOCK_TIMEOUT = 300;

    /**
     * 批量插入条数
     */
    private static final int BATCH_SIZE = 1000;

    /**
     * 版本号前缀
     */
    private static final String VERSION_PREFIX = "RAW";


    @Autowired
    protected DistributedVersionGenerator versionGenerator;


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
    public AjaxResult generateRawMaterialRequirePlan(String factoryCode, Integer year, Integer month, boolean isSpringFestivalMonth, String version) {
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
                if (!checkMonthPlanFinalized(factoryCode, year, month)) {
                    String message = StringUtils.format(
                            I18nUtil.getMessage("raw.material.require.plan.month.plan.not.finalized"),
                            year, String.format("%02d", month)
                    );
                    return AjaxResult.error(message);
                }

                // 3.1补充版本重复校验
                if (checkVersionExist(factoryCode, year, month, version)) {
                    String message = StringUtils.format(
                            I18nUtil.getMessage("raw.material.require.plan.version.exist"),
                            year, String.format("%02d", month), version
                    );
                    return AjaxResult.error(message);
                }

                // 4. 检查订单预测生产计划
                AjaxResult predictionCheck = checkOrderPrediction(year, month, isSpringFestivalMonth);
                if (isSuccess(predictionCheck)) {
                    return predictionCheck;
                }

                // 5. 查询特殊材料清单
                List<RawSpecialMaterialRecord> specialMaterialRecordsList = getSpecialMaterialRecords(factoryCode);
                // 5.1. 对特殊材料进行材料编码SET去重
                Map<String, List<RawSpecialMaterialRecord>> specialMaterialCodes = extractSpecialMaterialCodes(specialMaterialRecordsList);

                // 6. 获取月度生产计划
                List<FactoryMonthPlanProdFinal> monthPlans = getMonthProductionPlan(year, month, factoryCode);
                // 6.1. 月计划依据订单冲减表分解EudR数量
                splitAndSetEudrQuantities(monthPlans);

                // 7. 计算当月原材料需求量
                Map<String, RawMaterialRequirePlan> currentMonthRequirements = calculateCurrentMonthRequirements(
                        monthPlans, specialMaterialCodes);
                // 7.1.1 计算当月原材料采购批次
                calculateSpecialMaterialBatches(year, month, currentMonthRequirements);

                // 8. 获取预测计划并计算需求
                Map<String, RawMaterialRequirePlan> t1Requirements = calculateT1MonthRequirements(
                        factoryCode, year, month, specialMaterialCodes);
                Map<String, RawMaterialRequirePlan> t2Requirements = new HashMap<>();
                if (isSpringFestivalMonth) {
                    t2Requirements = calculateT2MonthRequirements(
                            factoryCode, year, month, specialMaterialCodes);
                }

                // 9. 汇总并保存需求计划
                saveRawMaterialRequirePlan(year, month, currentMonthRequirements,
                        t1Requirements, t2Requirements, factoryCode,  version);

                // 10. 生成差异数据
                generateDifferenceData(year, month, factoryCode, version);

                // 11. 生成周维度原材料用量记录
                generateWeekUsageRecords(factoryCode, year, month, version);

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
    private Map<String, List<RawSpecialMaterialRecord>> extractSpecialMaterialCodes(List<RawSpecialMaterialRecord> records) {
        if (CollectionUtils.isEmpty(records)) {
            return Collections.emptyMap();
        }
        return records.stream()
                .map(RawSpecialMaterialRecord::getMaterialCode)
                .collect(Collectors.toMap(
                        materialCode -> materialCode,
                        materialCode -> records.stream()
                                .filter(record -> materialCode.equals(record.getMaterialCode()))
                                .collect(Collectors.toList())
                ));
    }

    /**
     * 生成周维度原材料用量记录
     */
    private void generateWeekUsageRecords(String factoryCode, Integer year, Integer month, String version) {
        try {
            AjaxResult result = rawWeekUsageGenerateService
                    .generateWeekUsageForMonth(factoryCode, year, month, version);

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
     * 生成原材料需求计划版本
     *
     * @param billVO
     */
    @Override
    public AjaxResult generateVersion(RawMaterialRequirePlan billVO) {
        try {
            String version = versionGenerator.generateVersion(VERSION_PREFIX);
            billVO.setVersion(version);
            return AjaxResult.success(version);
        } catch (Exception e) {
            log.error("版本号生成失败", e);
            return AjaxResult.error(I18nUtil.getMessage("raw.material.require.plan.version.generate.error") );
        }
    }

    /**
     * 获取原材料需求计划版本列表
     *
     * @param queryVO
     * @return
     */
    @Override
    public AjaxResult getVersionList(RawMaterialRequirePlan queryVO) {
        QueryWrapper<RawMaterialRequirePlan> queryWrapper = new QueryWrapper<>();
        // 假设 queryVO 中包含 factoryCode, year, month 等查询条件，这里需要根据实际业务补充
        if (queryVO.getFactoryCode() != null) {
            queryWrapper.eq("FACTORY_CODE", queryVO.getFactoryCode());
        }
        if (queryVO.getYear() != null) {
            queryWrapper.eq("YEAR", queryVO.getYear());
        }
        if (queryVO.getMonth() != null) {
            queryWrapper.eq("MONTH", queryVO.getMonth());
        }

        queryWrapper.select("version", "MAX(CREATE_TIME) as createTime")
                .groupBy("version")
                .orderByDesc("create_time");
        
        List<Map<String, Object>> versionList = rawMaterialRequirePlanMapper.selectMaps(queryWrapper);
        
        return AjaxResult.success(versionList);
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
    private boolean checkMonthPlanFinalized(String factoryCode, Integer year, Integer month) {
        QueryWrapper<MpFactoryProductionVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", year)
                .eq("MONTH", month)
                .eq("IS_FINAL", Constant.TRUE)
                .eq("FACTORY_CODE", factoryCode);
        Long count = mpFactoryProductionVersionMapper.selectCount(queryWrapper);
        return count > 0;
    }

    /**
     * 检查版本是否存在重复
     */
    private boolean checkVersionExist(String factoryCode, Integer year, Integer month, String version) {
        QueryWrapper<RawMaterialRequirePlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode)
                .eq("YEAR", year)
                .eq("MONTH", month)
                .eq("VERSION", version);
        Long count = rawMaterialRequirePlanMapper.selectCount(queryWrapper);
        return count > 0;
    }


    /**
     * 检查订单预测生产计划
     */
    private AjaxResult checkOrderPrediction(Integer year, Integer month, boolean isSpringFestivalMonth) {
        LocalDate date = LocalDate.of(year, month, 1);

        QueryWrapper<MpProductionPrediction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", date.getYear())
                .eq("MONTH", date.getMonthValue());
        Long currencyCount = mpOrderPredictionMapper.selectCount(queryWrapper);

        if (currencyCount == 0) {
            //获取当前年月+1的月份
            LocalDate nextMonth = date.plusMonths(1);
            String message = StringUtils.format(
                    I18nUtil.getMessage("raw.material.require.plan.prediction.plan.not.generated"),
                    date.getYear(), String.format("%02d", nextMonth.getMonthValue())
            );
            return AjaxResult.error(message);
        }
        return AjaxResult.success();
    }

    /**
     * 获取月度生产计划
     */
    private List<FactoryMonthPlanProdFinal> getMonthProductionPlan(Integer year, Integer month, String factoryCode) {
        QueryWrapper<FactoryMonthPlanProdFinal> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("YEAR", year)
                .eq("MONTH", month)
                .eq("FACTORY_CODE", factoryCode);
        return factoryMonthPlanProdFinalMapper.selectList(queryWrapper);
    }

    /**
     * 分解 EudR 数量并设置到计划对象中
     */
    private void splitAndSetEudrQuantities(List<FactoryMonthPlanProdFinal> monthPlans) {
        log.info("开始分解EudR数量，共{}个月度计划", monthPlans.size());

        // 获取第一个计划的工厂、年份、月份信息
        FactoryMonthPlanProdFinal firstPlan = monthPlans.get(0);
        String factoryCode = firstPlan.getFactoryCode();
        Integer year = firstPlan.getYear();
        Integer month = firstPlan.getMonth();
        String monthPlanVersion = firstPlan.getMonthPlanVersion();

        // 查询订单冲减详情
        QueryWrapper<DpOrderOffsetDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode)
                .eq("YEAR", year)
                .eq("MONTH", month)
                .eq("MONTH_PLAN_VERSION", monthPlanVersion);

        List<DpOrderOffsetDetail> offsetDetails = orderOffsetDetailMapper.selectList(queryWrapper);

        if (CollectionUtils.isEmpty(offsetDetails)) {
            // 如果没有订单冲减数据，则将所有数量视为非EudR
            log.warn("工厂[{}] {}年{}月未找到订单冲减数据，所有物料按非EudR处理", factoryCode, year, month);
            setDefaultEudrQuantities(monthPlans);
            return;
        }

        // 计算EudR数量并设置到计划对象中
        setEudrQuantitiesToPlans(monthPlans, offsetDetails);

        log.info("EudR数量分解完成");
    }

    /**
     * 计算EudR数量并设置到计划对象中
     */
    private void setEudrQuantitiesToPlans(List<FactoryMonthPlanProdFinal> monthPlans,
                                          List<DpOrderOffsetDetail> offsetDetails) {

        // 按物料编码分组，汇总EudR和非EudR数量
        Map<String, Integer[]> materialEudrMap = new HashMap<>();

        for (DpOrderOffsetDetail detail : offsetDetails) {
            String materialCode = detail.getMaterialCode();
            if (materialCode == null) {
                continue;
            }

            // 初始化物料编码的EudR和非EudR数量
            Integer[] quantities = materialEudrMap.computeIfAbsent(materialCode, k -> new Integer[]{0, 0});

            // 获取冲减分配数量
            int productionQty = detail.getProductionQty() != null ? detail.getProductionQty() : 0;

            if (StringConstant.ONE.equals(detail.getIsEudr())) {
                // EudR数量
                quantities[1] += productionQty;
            } else {
                // 非EudR数量
                quantities[0] += productionQty;
            }
        }

        // 为每个计划设置EudR分解结果
        for (FactoryMonthPlanProdFinal plan : monthPlans) {
            if (plan.getMaterialCode() == null) {
                continue;
            }

            Integer[] quantities = materialEudrMap.get(plan.getMaterialCode());
            if (quantities != null) {
                // 设置非EudR和EudR数量
                plan.setNonEudrQty(plan.getTotalQty() != null ? plan.getTotalQty() : 0);
                plan.setEudrQty(quantities[1]);

                log.debug("物料[{}] EudR分解：非EudR={}, EudR={}, 总计={}",
                        plan.getMaterialCode(), quantities[0],
                        quantities[1],  quantities[0] + quantities[1]);
            } else {
                // 对于计划中有但订单冲减中没有的物料，设置为非EudR
                plan.setEudrQty(0);
                plan.setNonEudrQty(plan.getTotalQty() != null ? plan.getTotalQty() : 0);

                log.info("物料[{}]在订单冲减数据中未找到，按非EudR处理", plan.getMaterialCode());
            }
        }
    }

    /**
     * 设置默认EudR数量（全部为非EudR）
     */
    private void setDefaultEudrQuantities(List<FactoryMonthPlanProdFinal> monthPlans) {
        for (FactoryMonthPlanProdFinal plan : monthPlans) {
            plan.setEudrQty(0);
            plan.setNonEudrQty(plan.getTotalQty() != null ? plan.getTotalQty() : 0);
        }
    }

    /**
     * 计算当月原材料需求量
     */
    private Map<String, RawMaterialRequirePlan> calculateCurrentMonthRequirements(
            List<FactoryMonthPlanProdFinal> monthPlans, Map<String, List<RawSpecialMaterialRecord>> specialMaterialCodes) {

        // 1. 收集所有不重复的胎胚代码，批量查询所有BOM结构
        List<String> embryoCodes = monthPlans.stream()
                .map(FactoryMonthPlanProdFinal::getEmbryoCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, List<MdmMaterialConsumeDetail>> bomMap = getBomDetailsByEmbryoCodes(embryoCodes);

        // 2. 计算原材料需求
        Map<String, RawMaterialRequirePlan> requirements = new HashMap<>();
        monthPlans.forEach(plan -> {
            Integer totalQty = plan.getTotalQty();
            if (totalQty == null || totalQty == 0) {
                return;
            }

            String embryoCode = plan.getEmbryoCode();
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

        if (CollectionUtils.isEmpty(embryoCodes)){
            return new HashMap<>();
        }
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
                                               Map<String, List<RawSpecialMaterialRecord>> specialMaterialCodes,
                                               FactoryMonthPlanProdFinal plan) {
        // 1.获取计划中已分解的EudR和非EudR数量
        int nonEudrQty = plan.getNonEudrQty() != null ? plan.getNonEudrQty() : 0;
        int eudrQty = plan.getEudrQty() != null ? plan.getEudrQty() : 0;

        bomDetails.forEach(detail -> {
            String materialCode = detail.getChildMaterialCode();
            String materialDesc = detail.getChildMaterialName();
            BigDecimal dosage = detail.getDosage() != null ? detail.getDosage() : BigDecimal.ZERO;
            String materialType;
            List<RawSpecialMaterialRecord> specialMaterialRecords = specialMaterialCodes.get(materialCode);
            if (!CollectionUtils.isEmpty(specialMaterialRecords)) {
                materialType =  specialMaterialRecords.get(0).getMaterialType();
            } else {
                materialType = "01";
            }

            RawMaterialRequirePlan requirement = requirements.computeIfAbsent(materialCode,
                    k -> new RawMaterialRequirePlan(materialCode, materialDesc, materialType));

            // 计算非EudR需求数量
            if (nonEudrQty > 0) {
                BigDecimal nonEudrRequiredQty = BigDecimal.valueOf(nonEudrQty).multiply(dosage);
                requirement.addCurMonthQty(nonEudrRequiredQty);
            }

            // 计算EudR需求数量
            if (eudrQty > 0) {
                BigDecimal eudrRequiredQty = BigDecimal.valueOf(eudrQty).multiply(dosage);
                requirement.addCurMonthRudrQty(eudrRequiredQty);
            }
        });
    }


    /**
     * 计算T+1月需求
     */
    private Map<String, RawMaterialRequirePlan> calculateT1MonthRequirements(
            String factoryCode, Integer year, Integer month,
            Map<String, List<RawSpecialMaterialRecord>> specialMaterialCodes) {

        // 1. 获取最大预测版本的数据
        List<MpProductionPrediction> predictionList = getMaxPredictionVersionData(
                factoryCode, year, month);

        if (CollectionUtils.isEmpty(predictionList)) {
            log.warn("未找到预测数据，工厂: {}, 年月: {}-{}", factoryCode, year, month);
            return Collections.emptyMap();
        }

        LocalDate t1Date = LocalDate.of(year, month, 1).plusMonths(1);

        return calculatePredictionRequirements(
                factoryCode, t1Date.getYear(), t1Date.getMonthValue(),
                specialMaterialCodes, "t1MonthQty", "t1MonthEudrQty", predictionList);
    }

    /**
     * 计算T+2月需求
     */
    private Map<String, RawMaterialRequirePlan> calculateT2MonthRequirements(
            String factoryCode, Integer year, Integer month,
            Map<String, List<RawSpecialMaterialRecord>> specialMaterialCodes) {
        // 1. 获取最大预测版本的数据
        List<MpProductionPrediction> predictionList = getMaxPredictionVersionData(
                factoryCode, year, month);

        if (CollectionUtils.isEmpty(predictionList)) {
            return Collections.emptyMap();
        }

        LocalDate t2Date = LocalDate.of(year, month, 1).plusMonths(2);

        return calculatePredictionRequirements(
                factoryCode, t2Date.getYear(), t2Date.getMonthValue(),
                specialMaterialCodes, "t2MonthQty", "t2MonthEudrQty", predictionList);
    }


    /**
     * 计算预测需求（通用方法）
     */
    private Map<String, RawMaterialRequirePlan> calculatePredictionRequirements(
            String factoryCode, Integer year, Integer month,
            Map<String, List<RawSpecialMaterialRecord>> specialMaterialCodes, String qtyField, String eudrQtyField, List<MpProductionPrediction> predictionList) {

        try {
            log.info("开始计算预测需求，工厂: {}, 年月: {}-{}", factoryCode, year, month);

            // 2. 获取预测版本（所有记录的预测版本相同）
            String predictionVersion = predictionList.get(0).getPredictionVersion();

            // 3. 获取统一的预测版本信息
            Map<String, PredictionVersionInfoVo> versionMap = getPredictionVersionMap(
                    factoryCode, year, month, predictionList, predictionVersion);

            if (versionMap.isEmpty()) {
                log.warn("未找到版本信息，工厂: {}, 年月: {}-{}", factoryCode, year, month);
                return Collections.emptyMap();
            }

            // 4. 获取统一的版本信息
            PredictionVersionInfoVo commonVersionInfo = versionMap.values().iterator().next();
            String productionVersion = commonVersionInfo.getProductionVersion();
            String monthPlanVersion = commonVersionInfo.getMonthPlanVersion();

            if (StringUtils.isEmpty(productionVersion)) {
                log.warn("生产版本为空，工厂: {}, 年月: {}-{}", factoryCode, year, month);
                return Collections.emptyMap();
            }

            // 查询排产结果
            QueryWrapper<FactoryMonthPlanMouldDayResult> resultQuery = new QueryWrapper<>();
            resultQuery.eq("FACTORY_CODE", factoryCode)
                    .eq("YEAR", year)
                    .eq("MONTH", month)
                    .eq("PRODUCTION_VERSION", productionVersion)
                    .eq("MONTH_PLAN_VERSION", monthPlanVersion);

            List<FactoryMonthPlanMouldDayResult> resultList =
                    factoryMonthPlanMouldDayResultMapper.selectList(resultQuery);

            if (CollectionUtils.isEmpty(resultList)) {
                log.warn("未找到排产结果，工厂: {}, 年月: {}-{}, 生产版本: {}",
                        factoryCode, year, month, productionVersion);
                return Collections.emptyMap();
            }

            // 5. 获取排产结果数据
            Map<String, Integer[]> eudrMap = getProductionResultEudrQuantities(
                    factoryCode, year, month, productionVersion, monthPlanVersion, resultList);

            // 6. 计算原材料需求
            Map<String, RawMaterialRequirePlan> requirements = calculateMaterialRequirementsFromPredictions(
                    resultList, specialMaterialCodes, qtyField, eudrQtyField);

            log.info("预测需求计算完成，工厂: {}, 年月: {}-{}, 物料数量: {}",
                    factoryCode, year, month, requirements.size());
            return requirements;

        } catch (Exception e) {
            log.error("计算预测需求失败，工厂: {}, 年月: {}-{}", factoryCode, year, month, e);
            return Collections.emptyMap();
        }
    }


    /**
     * 获取最大预测版本的数据
     */
    private List<MpProductionPrediction> getMaxPredictionVersionData(
            String factoryCode, Integer year, Integer month) {

        // 先查询最大版本
        QueryWrapper<MpProductionPrediction> maxVersionQuery = new QueryWrapper<>();
        maxVersionQuery.select("MAX(PREDICTION_VERSION) as maxVersion")
                .eq("FACTORY_CODE", factoryCode)
                .eq("YEAR", year)
                .eq("MONTH", month);

        List<Map<String, Object>> maxVersionResult = mpProductionPredictionMapper.selectMaps(maxVersionQuery);
        if (CollectionUtils.isEmpty(maxVersionResult) ||
                maxVersionResult.get(0).get("maxVersion") == null) {
            return Collections.emptyList();
        }

        String maxVersion = maxVersionResult.get(0).get("maxVersion").toString();

        // 查询最大版本的数据
        QueryWrapper<MpProductionPrediction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode)
                .eq("YEAR", year)
                .eq("MONTH", month)
                .eq("PREDICTION_VERSION", maxVersion);

        return mpProductionPredictionMapper.selectList(queryWrapper);
    }


    /**
     * 获取预测版本信息
     */
    private Map<String, PredictionVersionInfoVo> getPredictionVersionMap(
            String factoryCode, Integer year, Integer month,
            List<MpProductionPrediction> predictionList,
            String predictionVersion) {

        if (CollectionUtils.isEmpty(predictionList)) {
            return Collections.emptyMap();
        }

        // 由于同一预测版本下所有物料的MonthPlanVersion和ProductionVersion一致，
        // 只需查询一条记录获取版本信息即可
        QueryWrapper<MpPredictionDetail> detailQuery = new QueryWrapper<>();
        detailQuery.eq("FACTORY_CODE", factoryCode)
                .eq("YEAR", year)
                .eq("MONTH", month)
                .eq("BATCH_NUMBER", predictionVersion)
                .last("LIMIT 1");  // 只需要一条记录

        MpPredictionDetail detail = mpPredictionDetailMapper.selectOne(detailQuery);

        if (detail == null) {
            log.warn("未找到预测明细数据，工厂: {}, 年月: {}-{}, 版本: {}",
                    factoryCode, year, month, predictionVersion);
            return Collections.emptyMap();
        }

        // 构建物料到版本信息的映射（所有物料使用相同的版本信息）
        Map<String, PredictionVersionInfoVo> versionMap = new HashMap<>();
        PredictionVersionInfoVo commonVersionInfo = new PredictionVersionInfoVo();
        commonVersionInfo.setMonthPlanVersion(detail.getMonthPlanVersion());
        commonVersionInfo.setProductionVersion(detail.getProductionVersion());

        // 为每个物料分配相同的版本信息
        for (MpProductionPrediction prediction : predictionList) {
            String materialCode = prediction.getMaterialCode();
            if (materialCode != null) {
                versionMap.put(materialCode, commonVersionInfo);
            }
        }

        log.info("获取预测版本信息完成，工厂: {}, 年月: {}-{}, 版本: {}, 物料数量: {}",
                factoryCode, year, month, predictionVersion, versionMap.size());
        return versionMap;
    }


    /**
     * 获取排产结果的EudR数量
     */
    private Map<String, Integer[]> getProductionResultEudrQuantities(
            String factoryCode, Integer year, Integer month, String productionVersion, String monthPlanVersion, List<FactoryMonthPlanMouldDayResult> resultList) {

        Map<String, Integer[]> eudrMap = new HashMap<>();

        // 查询订单冲减详情
        QueryWrapper<DpOrderOffsetDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode)
                .eq("YEAR", year)
                .eq("MONTH", month)
                .eq("MONTH_PLAN_VERSION", monthPlanVersion);

        List<DpOrderOffsetDetail> offsetDetails = orderOffsetDetailMapper.selectList(queryWrapper);

        if (!CollectionUtils.isEmpty(offsetDetails)) {
            for (DpOrderOffsetDetail detail : offsetDetails) {
                String materialCode = detail.getMaterialCode();
                if (materialCode == null) {
                    continue;
                }

                // 初始化物料编码的EudR和非EudR数量
                Integer[] quantities = eudrMap.computeIfAbsent(materialCode, k -> new Integer[]{0, 0});

                // 获取冲减分配数量
                int productionQty = detail.getProductionQty() != null ? detail.getProductionQty() : 0;

                if (StringConstant.ONE.equals(detail.getIsEudr())) {
                    // EudR数量
                    quantities[1] += productionQty;
                }else {
                    quantities[0] += productionQty;
                }
            }
        }


        for (FactoryMonthPlanMouldDayResult plan : resultList) {
            String materialCode = plan.getMaterialCode();
            if (materialCode == null) {
                continue;
            }

            Integer[] quantities = eudrMap.get(plan.getMaterialCode());
            if (quantities != null) {
                // 设置非EudR和EudR数量
                plan.setNonEudrQty(plan.getTotalQty() != null ? plan.getTotalQty() : 0);
                plan.setEudrQty(quantities[1]);

                log.debug("物料[{}] EudR分解：非EudR={}, EudR={}, 总计={}",
                        plan.getMaterialCode(), quantities[0],
                        quantities[1],  quantities[0] + quantities[1]);
            } else {
                // 对于计划中有但订单冲减中没有的物料，设置为非EudR
                plan.setEudrQty(0);
                plan.setNonEudrQty(plan.getTotalQty() != null ? plan.getTotalQty() : 0);

                log.info("物料[{}]在订单冲减数据中未找到，按非EudR处理", plan.getMaterialCode());
            }
        }

        log.info("获取排产结果EudR数量完成，工厂: {}, 年月: {}-{}, 版本: {}, 物料数量: {}",
                factoryCode, year, month, productionVersion, eudrMap.size());
        return eudrMap;
    }


    /**
     * 从预测数据计算物料需求
     */
    private Map<String, RawMaterialRequirePlan> calculateMaterialRequirementsFromPredictions(
            List<FactoryMonthPlanMouldDayResult> resultList,
            Map<String, List<RawSpecialMaterialRecord>> specialMaterialCodes,
            String qtyField, String eudrQtyField) {

        Map<String, RawMaterialRequirePlan> requirements = new HashMap<>();

        // 1. 收集所有不重复的胎胚代码
        List<String> embryoCodes = resultList.stream()
                .map(FactoryMonthPlanMouldDayResult::getEmbryoCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 2. 批量查询所有BOM结构
        Map<String, List<MdmMaterialConsumeDetail>> bomMap = getBomDetailsByEmbryoCodes(embryoCodes);

        // 3. 计算每个物料的需求
        for (FactoryMonthPlanMouldDayResult prediction : resultList) {
            String embryoCode = prediction.getEmbryoCode();
            if (embryoCode == null) {
                continue;
            }

            if (prediction.getTotalQty() == null || prediction.getTotalQty() == 0) {
                continue;
            }


            int nonEudrQty = prediction.getTotalQty();
            int eudrQty = prediction.getEudrQty();

            // 获取BOM详情
            List<MdmMaterialConsumeDetail> bomDetails = bomMap.get(embryoCode);
            if (CollectionUtils.isEmpty(bomDetails)) {
                continue;
            }

            // 计算原材料需求
            calculatePredictionMaterialRequirements(requirements, bomDetails,
                    nonEudrQty, eudrQty, specialMaterialCodes, qtyField, eudrQtyField);
        }

        return requirements;
    }


    /**
     * 计算预测物料需求
     */
    private void calculatePredictionMaterialRequirements(
            Map<String, RawMaterialRequirePlan> requirements,
            List<MdmMaterialConsumeDetail> bomDetails,
            int nonEudrQty, int eudrQty,
            Map<String, List<RawSpecialMaterialRecord>> specialMaterialCodes,
            String qtyField, String eudrQtyField) {

        for (MdmMaterialConsumeDetail detail : bomDetails) {
            String materialCode = detail.getChildMaterialCode();
            String materialDesc = detail.getChildMaterialName();
            BigDecimal dosage = detail.getDosage() != null ? detail.getDosage() : BigDecimal.ZERO;
            String materialType;
            List<RawSpecialMaterialRecord> specialMaterialRecords = specialMaterialCodes.get(materialCode);
            if (!CollectionUtils.isEmpty(specialMaterialRecords)) {
                materialType =  specialMaterialRecords.get(0).getMaterialType();
            } else {
                materialType = "01";
            }

            RawMaterialRequirePlan requirement = requirements.computeIfAbsent(materialCode,
                    k -> new RawMaterialRequirePlan(materialCode, materialDesc, materialType));

            // 计算需求数量
            if (nonEudrQty > 0) {
                BigDecimal nonEudrRequiredQty = BigDecimal.valueOf(nonEudrQty).multiply(dosage);
                setPredictionQty(requirement, qtyField, nonEudrRequiredQty);
            }

            if (eudrQty > 0) {
                BigDecimal eudrRequiredQty = BigDecimal.valueOf(eudrQty).multiply(dosage);
                setPredictionQty(requirement, eudrQtyField, eudrRequiredQty);
            }
        }
    }

    /**
     * 设置预测需求数量
     */
    private void setPredictionQty(RawMaterialRequirePlan requirement,
                                  String fieldName, BigDecimal qty) {
        switch (fieldName) {
            case "t1MonthQty":
                requirement.setT1MonthQty(qty);
                break;
            case "t1MonthEudrQty":
                requirement.setT1MonthEudrQty(qty);
                break;
            case "t2MonthQty":
                requirement.setT2MonthQty(qty);
                break;
            case "t2MonthEudrQty":
                requirement.setT2MonthEudrQty(qty);
                break;
        }
    }


    /**
     * 保存原材料需求计划（优化版，批量插入）
     */
    private void saveRawMaterialRequirePlan(Integer year, Integer month,
                                            Map<String, RawMaterialRequirePlan> currentMonthRequirements,
                                            Map<String, RawMaterialRequirePlan> t1Requirements,
                                            Map<String, RawMaterialRequirePlan> t2Requirements,
                                            String factoryCode, String version) {

        // 删除旧的计划
        // deleteOldRequirements(year, month);

        // 合并所有需求
        Map<String, RawMaterialRequirePlan> allRequirements = mergeAllRequirements(
                currentMonthRequirements, t1Requirements, t2Requirements);

        if (allRequirements.isEmpty()) {
            log.warn(I18nUtil.getMessage("raw.material.require.plan.no.requirements.to.save"));
            return;
        }

        // 批量保存新的计划
        batchSaveRequirements(year, month, allRequirements, factoryCode, version);
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
                                       String factoryCode, String version) {

        String username = SecurityUtils.getUsername();
        if (username == null) {
            username = "system";
        }
        final String finalUsername = username;

        // 将Map转换为实体列表
        List<RawMaterialRequirePlan> planList = requirements.values().stream()
                .map(req -> convertToEntity(year, month, req, factoryCode, finalUsername, version))
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
                                                   String factoryCode, String username, String version) {
        RawMaterialRequirePlan plan = new RawMaterialRequirePlan();
        plan.setYear(year);
        plan.setMonth(month);
        plan.setVersion(version);
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
        plan.setTMonthEudrQty(formatAndValidateBigDecimal(requirement.getTMonthEudrQty(), "T_MONTH_EudR_QTY"));
        plan.setT1MonthQty(formatAndValidateBigDecimal(requirement.getT1MonthQty(), "T1_MONTH_QTY"));
        plan.setT1MonthEudrQty(formatAndValidateBigDecimal(requirement.getT1MonthEudrQty(), "T1_MONTH_EudR_QTY"));
        plan.setT2MonthQty(formatAndValidateBigDecimal(requirement.getT2MonthQty(), "T2_MONTH_QTY"));
        plan.setT2MonthEudrQty(formatAndValidateBigDecimal(requirement.getT2MonthEudrQty(), "T2_MONTH_EudR_QTY"));

        // 设置创建信息
        plan.setCreateBy(username);
        plan.setCreateTime(new Date());
        plan.setUpdateTime(new Date());

        return plan;
    }

    /**
     * 格式化并验证BigDecimal值
     * 要求：2位小数，整数部分不超过10位
     */
    private BigDecimal formatAndValidateBigDecimal(BigDecimal value, String fieldName) {
        if (value == null) {
            return BigDecimal.ZERO;
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
            BigDecimal proportion = ratio.getRatio().divide(new BigDecimal(100));
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
    private void generateDifferenceData(Integer year, Integer month, String factoryCode, String version) {
        // 获取当前月和上个月的需求
        Map<String, RawMaterialRequirePlan> currentRequirements = getRequirementsByMonth(year, month);
        Map<String, RawMaterialRequirePlan> previousRequirements = getPreviousMonthRequirements(year, month);

        // 删除旧的差异数据
        //deleteOldDifferenceData(year, month, factoryCode);

        // 计算差异并批量保存
        List<RawMaterialMonthDiff> diffRecords = calculateDifferences(
                year, month, currentRequirements, previousRequirements, factoryCode, version);

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
                                                            String factoryCode, String version) {

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
                diffRecords.add(createDiffRecord(year, month, materialCode, diffTypeNew, version,
                        BigDecimal.ZERO, curQty, factoryCode, currentReq.getMaterialDesc(), finalUsername));
            } else if (diffValue.compareTo(BigDecimal.ZERO) != 0) {
                // 数量有变化
                String diffType = diffValue.compareTo(BigDecimal.ZERO) > 0 ? diffTypeIncrease : diffTypeDecrease;
                diffRecords.add(createDiffRecord(year, month, materialCode, diffType,
                        version, prevQty, curQty, factoryCode, currentReq.getMaterialDesc(), finalUsername));
            }
        });

        // 2. 处理上个月有但当前月没有的材料
        previous.forEach((materialCode, prevReq) -> {
            if (!current.containsKey(materialCode)) {
                BigDecimal prevQty = prevReq.getCurMonthQty() != null ? prevReq.getCurMonthQty() : BigDecimal.ZERO;
                // 减少的原材料
                diffRecords.add(createDiffRecord(year, month, materialCode, diffTypeDecrease,
                        version, prevQty, BigDecimal.ZERO, factoryCode, prevReq.getMaterialDesc(), finalUsername));
            }
        });

        return diffRecords;
    }

    /**
     * 创建差异记录
     */
    private RawMaterialMonthDiff createDiffRecord(Integer year, Integer month, String materialCode,
                                                  String diffType, String version, BigDecimal prevQty, BigDecimal curQty,
                                                  String factoryCode, String materialDesc, String username) {

        RawMaterialMonthDiff diffRecord = new RawMaterialMonthDiff();
        diffRecord.setFactoryCode(factoryCode);
        diffRecord.setYear(year);
        diffRecord.setMonth(month);
        diffRecord.setVersion(version);
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