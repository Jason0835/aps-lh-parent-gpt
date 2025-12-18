package com.zlt.aps.monthplan.factory.handler.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.tlt.aps.constant.FactoryConstant;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProdDetailFinal;
import com.zlt.aps.monthplan.api.domain.entity.MouldingProductionResultHelper;
import com.zlt.aps.monthplan.api.domain.vo.DayLeftOverCuringTimeVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanFinalVersionInfoVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanProdFinalVo;
import com.zlt.aps.monthplan.api.domain.vo.ProductProductionInfoVo;
import com.zlt.aps.monthplan.factory.handler.AdjustInfoHelperVo;
import com.zlt.aps.monthplan.factory.handler.FactoryMonthPlanAdjustHandler;
import com.zlt.aps.monthplan.factory.handler.MonthPlanAdjustHelper;
import com.zlt.aps.monthplan.factory.handler.ProductSubtractVo;
import com.zlt.aps.monthplan.factory.mapper.FactoryMouldingProductionResultMapper;
import com.zlt.aps.monthplan.factory.mapper.MonthPlanProdDetailFinalMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProdFinalService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 新增-规格的计划调整处理实现
 *
 * @author ZLT
 * @date 20250322
 */
@Slf4j
@Service("addProductAdjustService")
@RequiredArgsConstructor
public class AddProductAdjustService implements FactoryMonthPlanAdjustHandler {

    private final BaseDao baseDao;

    private final IFactoryMonthPlanProdFinalService prodFinalService;

    private final MonthPlanProdDetailFinalMapper monthPlanProdDetailFinalMapper;

    private final FactoryMouldingProductionResultMapper mouldingProductionResultMapper;

    /**
     * 新增规格-调整
     * 根据规格编码，获取规格基础信息，确定是正式规格还是试制、量试规格，取得硫化时间
     * 根据模具，获取定稿版本中该模具的排产计划，按每日进行相应的数量减少
     *
     * @param monthPlanAdjustHelper
     * @return
     */
    @Override
    public AjaxResult calculateMonthPlanAdjust(MonthPlanAdjustHelper monthPlanAdjustHelper) {
        FactoryMonthPlanProdFinalVo addProductPlan = monthPlanAdjustHelper.getAdjustPlan();
        FactoryMonthPlanFinalVersionInfoVo finalVersion = monthPlanAdjustHelper.getFinalVersion();
        //获取模具大类
        String mouldNo = addProductPlan.getMouldNo();
        String productCode = addProductPlan.getProductCode();
        Set<Integer> adjustDateSet = monthPlanAdjustHelper.getAdjustDateList();
        //获取mouldNo相同的排产计划
        QueryWrapper<FactoryMonthPlanProdFinal> productionPlanQuery = buildQueryCondition(finalVersion, mouldNo, productCode, false);
        List<FactoryMonthPlanProdFinal> productionPlanList = prodFinalService.getList(productionPlanQuery);
        //纯新增量
        if (isNoProductionAdd(productionPlanList, adjustDateSet, addProductPlan)) {
            return addAdjustPlan(addProductPlan, finalVersion, adjustDateSet);
        }
        return addAdjustPlanAndSubtractOther(addProductPlan, finalVersion, adjustDateSet, productionPlanList);
    }

    /**
     * 获取对应模具号的排产计划查询条件
     *
     * @param finalVersion     定稿版本信息
     * @param mouldNo          模具
     * @param productCode      物料编码
     * @param isExcludeProduct 是否要排产物料
     * @return
     */
    private <T> QueryWrapper<T> buildQueryCondition(FactoryMonthPlanFinalVersionInfoVo finalVersion, String mouldNo, String productCode, boolean isExcludeProduct) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", finalVersion.getFactoryCode());
        queryWrapper.eq("YEAR", finalVersion.getYear());
        queryWrapper.eq("MONTH", finalVersion.getMonth());
        queryWrapper.eq("MONTH_PLAN_VERSION", finalVersion.getMonthPlanVersion());
        queryWrapper.eq("PRODUCTION_VERSION", finalVersion.getProductionVersion());
        if (isExcludeProduct) {
            queryWrapper.ne("PRODUCT_CODE", productCode);
        }
        queryWrapper.eq("MOULD_NO", mouldNo);
        queryWrapper.gt("TOTAL_QTY", BigDecimal.ZERO);
        return queryWrapper;
    }

    /**
     * 判断新增规格在排产日列表上是否完全为新增
     * 即在各个调整日上的都没有排产
     *
     * @param productionPlanList
     * @param adjustDateSet
     * @return
     */
    private boolean isNoProductionAdd(List<FactoryMonthPlanProdFinal> productionPlanList, Set<Integer> adjustDateSet, FactoryMonthPlanProdFinal addProductPlan) {
        if (CollectionUtils.isEmpty(productionPlanList)) {
            return true;
        }
        String fieldName;
        for (Integer adjustDate : adjustDateSet) {
            fieldName = String.format("day%d", adjustDate);
            Object adjustValue = addProductPlan.getFieldValueByFieldName(fieldName);
            if (!(null != adjustValue && (Long) adjustValue > 0)) {
                continue;
            }
            for (FactoryMonthPlanProdFinal existPlan : productionPlanList) {
                Object existValue = existPlan.getFieldValueByFieldName(fieldName);
                if (null != existValue && (Long) existValue > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 纯新增规格量，没有共用模具的量
     *
     * @param addProductPlan
     * @param finalVersion
     * @return
     * @
     */
    private AjaxResult addAdjustPlan(FactoryMonthPlanProdFinalVo addProductPlan, FactoryMonthPlanFinalVersionInfoVo finalVersion, Set<Integer> adjustDateSet) {
        fillFinalVersionInfo(addProductPlan, finalVersion);
        fillTotalInfo(addProductPlan);
        baseDao.insert(addProductPlan);
        //排产明细及模具日剩余硫化时间更新
        return AjaxResult.success();
    }

    /**
     * 确认新增规格，并根据新增量，相应减少其它规格计划
     *
     * @param addProductPlan     需要新增的规格量
     * @param finalVersion       定稿版本信息
     * @param adjustDateSet      可调整日
     * @param productionPlanList 其它有排产的计划
     * @return
     */
    private AjaxResult addAdjustPlanAndSubtractOther(FactoryMonthPlanProdFinalVo addProductPlan, FactoryMonthPlanFinalVersionInfoVo finalVersion, Set<Integer> adjustDateSet, List<FactoryMonthPlanProdFinal> productionPlanList) {
        //需要返回给前端的调整计划集合
        Map<String, FactoryMonthPlanProdFinal> needAdjustPlanMap = new HashMap<>(productionPlanList.size());
        String mouldNo = addProductPlan.getMouldNo();
        String productCode = addProductPlan.getProductCode();
        Integer mouldQty = addProductPlan.getMouldQty();
        Set<String> maxMouldSet = addProductPlan.getMaxMouldSet();
        //查询排产明细，用来确认挑选减量的规格及是否换规格时间
        QueryWrapper<MonthPlanProdDetailFinal> queryWrapper = buildQueryCondition(finalVersion, mouldNo, productCode, false);
        List<MonthPlanProdDetailFinal> detailList = monthPlanProdDetailFinalMapper.selectList(queryWrapper);
        String filedName;
        for (Integer adjustDate : adjustDateSet) {
            filedName = String.format("day%s", adjustDate);
            Long adjustQty = (Long) addProductPlan.getFieldValueByFieldName(filedName);
            if (null == adjustQty || adjustQty <= 0) {
                continue;
            }
            AdjustInfoHelperVo adjustInfoHelper = new AdjustInfoHelperVo(productCode, adjustDate, adjustQty, mouldNo, mouldQty, maxMouldSet);
            adjustInfoHelper.setCuringTime(BigDecimal.valueOf(addProductPlan.getCuringTime()));
            adjustInfoHelper.setDayMaxCuringTime(addProductPlan.getDayMaxCuringTime());
            adjustInfoHelper.setChangeProductConsumeTime(addProductPlan.getChangeProductConsumeTime());
            buildSubtractPlan(addProductPlan, detailList, adjustInfoHelper, productionPlanList, needAdjustPlanMap);
        }
        fillFinalVersionInfo(addProductPlan, finalVersion);
        fillTotalInfo(addProductPlan);
        needAdjustPlanMap.put(addProductPlan.getProductionNo(), addProductPlan);
        if (CollectionUtils.isEmpty(needAdjustPlanMap)) {
            return AjaxResult.success();
        }
        List<FactoryMonthPlanProdFinal> confirmList = needAdjustPlanMap.entrySet().stream().map(entry -> entry.getValue()).collect(Collectors.toList());
        return AjaxResult.success(confirmList);
    }

    /**
     * 根据调整日以及调增量，构建对应计划的调减量结果
     *
     * @param addProductPlan     调整新增规格计划
     * @param detailList         排产计划明细
     * @param adjustInfoHelper   日调整信息
     * @param productionPlanList 排产计划汇总
     * @param needAdjustPlanMap  需要调整的计划
     */
    private void buildSubtractPlan(FactoryMonthPlanProdFinalVo addProductPlan, List<MonthPlanProdDetailFinal> detailList, AdjustInfoHelperVo adjustInfoHelper, List<FactoryMonthPlanProdFinal> productionPlanList, Map<String, FactoryMonthPlanProdFinal> needAdjustPlanMap) {
        Integer adjustDate = adjustInfoHelper.getAdjustDate();
        Set<String> maxMouldSet = adjustInfoHelper.getMaxMouldSet();
        List<MouldingProductionResultHelper> mouldProductionInfoList = getMouldResultList(addProductPlan, maxMouldSet);
        if (CollectionUtils.isEmpty(mouldProductionInfoList)) {
            return;
        }
        Set<String> usedMouldSet = getProductionMouldQty(detailList, adjustDate);
        Integer usedMouldQty = usedMouldSet.size();
        Integer maxMouldQty = maxMouldSet.size();
        Integer addNewMouldQty = maxMouldQty - usedMouldQty;
        Long needAdjustQty = adjustInfoHelper.getAdjustQty();
        Map<String, Integer> addMouldAddQtyMap = new HashMap<>();
        //获取已排模具剩余可排产量
        for (MouldingProductionResultHelper singleMouldProduction : mouldProductionInfoList) {
            Integer singleMouldQty = getCuringQty(singleMouldProduction, adjustInfoHelper);
            Integer realQty = Math.min(needAdjustQty.intValue(), singleMouldQty);
            //todo 构建排产明细，更新模具剩余时间
            if (singleMouldQty > 0) {
            }
            needAdjustQty = needAdjustQty - realQty;
            if (needAdjustQty <= 0) {
                break;
            }
        }
        //已排模具可满足
        if (needAdjustQty <= 0) {
            return;
        }
        //新模具排产
        for (String mouldCode : maxMouldSet) {
            if (usedMouldSet.contains(mouldCode)) {
                continue;
            }
            Integer dayMaxQty = adjustInfoHelper.getMaxCuringQty();
            Integer realQty = Math.min(needAdjustQty.intValue(), dayMaxQty);
            //todo 构建排产明细，更新模具剩余时间
            needAdjustQty = needAdjustQty - realQty;
            addMouldAddQtyMap.put(mouldCode, realQty);
            if (needAdjustQty <= 0) {
                break;
            }
        }
        //模具可满足
        if (needAdjustQty <= 0) {

            return;
        }
        //新模具可排产量
        Integer addMouldAddQty = BigDecimal.ZERO.intValue();
        if(!CollectionUtils.isEmpty(addMouldAddQtyMap)){
            for (Map.Entry<String, Integer> entry : addMouldAddQtyMap.entrySet()) {
                addMouldAddQty = addNewMouldQty + entry.getValue();
            }
        }
        //已排模具还需排产量
        Long leftOverProductionQty = adjustInfoHelper.getAdjustQty() - addMouldAddQty;
        //模具满足不了，则其它规格要减量-按排产顺序倒序
        List<MonthPlanProdDetailFinal> dayProductionList = new ArrayList<>();
        String filedName = String.format("day%d", adjustDate);
        detailList.forEach(dayProductionDetail -> {
            Long dayProductionQty = (Long) dayProductionDetail.getFieldValueByFieldName(filedName);
            if (null != dayProductionQty && dayProductionQty > 0) {
                dayProductionList.add(dayProductionDetail);
            }
        });
        dayProductionList.sort(Comparator.comparing(MonthPlanProdDetailFinal::getProductionSequence, Comparator.reverseOrder()));




        //需要扣减的硫化时间
        BigDecimal subtractCuringTime = adjustInfoHelper.getCuringTime().multiply(BigDecimal.valueOf(needAdjustQty));
        Map<String, ProductSubtractVo> subQtyMap = new HashMap<>();
        int index = 0;
        for (MonthPlanProdDetailFinal dayProductionSort : dayProductionList) {
            String sumKey = dayProductionSort.getSummaryValue();
            Long totalQty = dayProductionSort.getTotalQty();
            BigDecimal totalVulcanizationMinutes = dayProductionSort.getTotalVulcanizationMinutes();
            totalVulcanizationMinutes = totalVulcanizationMinutes.multiply(BigDecimal.valueOf(FactoryConstant.MINUTE_SECOND));
            BigDecimal singleTime = BigDecimal.valueOf(dayProductionSort.getCuringTime());
            //原有排产量
            Integer dayProductionQty = (Integer) dayProductionSort.getFieldValueByFieldName(filedName);
            //向上取整
            Integer subtractQty = subtractCuringTime.divide(singleTime, 0, RoundingMode.UP).intValue();
            //实际减量
            Integer realQty = Math.min(dayProductionQty, subtractQty);
            //新的排产量
            Integer newProductionQty = dayProductionQty - realQty;
            BigDecimal realSubtractCuringTime = singleTime.multiply(BigDecimal.valueOf(realQty));
            dayProductionSort.setFieldValueByFieldName(filedName, newProductionQty);
            //迭代的扣减时间减少
            subtractCuringTime = subtractCuringTime.subtract(realSubtractCuringTime);

            String mouldCode = dayProductionSort.getMouldCode();

            dayProductionSort.setTotalQty(totalQty - realQty);
            totalVulcanizationMinutes = totalVulcanizationMinutes.subtract(realSubtractCuringTime);
            dayProductionSort.setTotalVulcanizationMinutes(totalVulcanizationMinutes.divide(BigDecimal.valueOf(FactoryConstant.MINUTE_SECOND), 2, RoundingMode.HALF_UP));
            ProductSubtractVo sumSubQty = subQtyMap.get(sumKey);
            if (null == sumSubQty) {
                sumSubQty = new ProductSubtractVo();
                sumSubQty.setSumSubtractQty(BigDecimal.ZERO.longValue());
                sumSubQty.setSumSubtractCuringTime(BigDecimal.ZERO);
            }
            sumSubQty.setSumSubtractQty(sumSubQty.getSumSubtractQty() + realQty);
            sumSubQty.setSumSubtractCuringTime(sumSubQty.getSumSubtractCuringTime().add(realSubtractCuringTime));
            subQtyMap.put(sumKey, sumSubQty);
            if (subtractCuringTime.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            index = index + 1;
        }
        //需扣减的汇总规格
        productionPlanList.forEach(needAdjustFinal -> {
            String subKey = needAdjustFinal.getUpdateImportValue();
            if (!subQtyMap.containsKey(subKey)) {
                return;
            }
            ProductSubtractVo productSub = subQtyMap.get(subKey);
            Long productionQty = (Long) needAdjustFinal.getFieldValueByFieldName(filedName);
            Long newProductionQty = productionQty - productSub.getSumSubtractQty();
            needAdjustFinal.setFieldValueByFieldName(filedName, newProductionQty);
            BigDecimal totalCuringTime = needAdjustFinal.getTotalVulcanizationMinutes();
            totalCuringTime = totalCuringTime.multiply(BigDecimal.valueOf(FactoryConstant.MINUTE_SECOND)).subtract(productSub.getSumSubtractCuringTime());
            totalCuringTime = totalCuringTime.divide(BigDecimal.valueOf(FactoryConstant.MINUTE_SECOND), 2, RoundingMode.HALF_UP);
            needAdjustFinal.setTotalVulcanizationMinutes(totalCuringTime);
            needAdjustPlanMap.put(needAdjustFinal.getProductionNo(), needAdjustFinal);
        });
    }

    /**
     * 获取排产日使用的模具数
     *
     * @param detailList
     * @param productionDate
     * @return
     */
    private Set<String> getProductionMouldQty(List<MonthPlanProdDetailFinal> detailList, Integer productionDate) {
        if (CollectionUtils.isEmpty(detailList)) {
            return Collections.emptySet();
        }
        Set<String> mouldSet = new HashSet<>();
        detailList.forEach(productionFinal -> {
            String fieldName = String.format("day%d", productionDate);
            Long productionQty = (Long) productionFinal.getFieldValueByFieldName(fieldName);
            if (null == productionQty || productionQty < 1) {
                return;
            }
            mouldSet.add(productionFinal.getMouldCode());
        });
        return mouldSet;
    }

    /**
     * 补充定稿版本相关信息
     * 分厂编码、年份、月份、制造需求版本、分厂排产版本，制造单号，年月
     *
     * @param addProductPlan
     * @param finalVersion
     */
    private void fillFinalVersionInfo(FactoryMonthPlanProdFinal addProductPlan, FactoryMonthPlanFinalVersionInfoVo finalVersion) {
        Integer year = finalVersion.getYear();
        Integer month = finalVersion.getMonth();
        String yearAndMonth = String.format("%s%s", year, String.format("%02d", month));
        String productionNoFormat = "SO%s%s";
        //制造单号
        String productionNo = String.format(productionNoFormat, DateUtils.dateTimeNow(), String.format("%06d", 1));
        addProductPlan.setProductionNo(productionNo);
        addProductPlan.setId(null);
        addProductPlan.setFactoryCode(finalVersion.getFactoryCode());
        addProductPlan.setYear(year);
        addProductPlan.setMonth(month);
        addProductPlan.setMonthPlanVersion(finalVersion.getMonthPlanVersion());
        addProductPlan.setProductionVersion(finalVersion.getProductionVersion());
        addProductPlan.setYearMonth(Integer.valueOf(yearAndMonth));
    }

    /**
     * 补充统计信息数据
     *
     * @param addProductPlan
     */
    private void fillTotalInfo(FactoryMonthPlanProdFinalVo addProductPlan) {
        Long totalValue = BigDecimal.ZERO.longValue();
        Integer beginDate = addProductPlan.getMaxDays();
        Integer endDay = BigDecimal.ZERO.intValue();
        String fieldName;
        for (Integer index : FactoryConstant.PRODUCTION_CYCLE) {
            if (index > 0) {
                fieldName = "day";
            } else {
                fieldName = "preDay";
            }
            fieldName = fieldName + Math.abs(index);
            Long dayValue;
            Object value = addProductPlan.getFieldValueByFieldName(fieldName);
            if (null == value) {
                dayValue = BigDecimal.ZERO.longValue();
            } else {
                if (beginDate > index) {
                    beginDate = index;
                }
                if (index > endDay) {
                    endDay = index;
                }
                dayValue = (Long) value;
            }
            totalValue = totalValue + dayValue;
        }
        addProductPlan.setBeginDate(beginDate);
        addProductPlan.setEndDay(endDay);
//        addProductPlan.setFactProdReqQty(totalValue);
//        addProductPlan.setProdReqPlan(totalValue);
//        addProductPlan.setTotalQty(totalValue);
//        addProductPlan.setDifferenceQty(BigDecimal.ZERO.longValue());
//        addProductPlan.setTotalVulcanizationMinutes(addProductPlan.getCuringTime().multiply(BigDecimal.valueOf(totalValue)));
    }

    /**
     * 获取对应模具排产信息
     *
     * @param addProductPlan
     * @param mouldSet
     * @return
     */
    private List<MouldingProductionResultHelper> getMouldResultList(FactoryMonthPlanProdFinalVo addProductPlan, Set<String> mouldSet) {
        QueryWrapper<MouldingProductionResultHelper> mouldingResultQuery = new QueryWrapper<>();
        mouldingResultQuery.eq("FACTORY_CODE", addProductPlan.getFactoryCode());
        mouldingResultQuery.eq("YEAR", addProductPlan.getYear());
        mouldingResultQuery.eq("MONTH", addProductPlan.getMonth());
        mouldingResultQuery.eq("MONTH_PLAN_VERSION", addProductPlan.getMonthPlanVersion());
        mouldingResultQuery.eq("PRODUCTION_VERSION", addProductPlan.getProductionVersion());
        mouldingResultQuery.eq("MOULD_NO", addProductPlan.getMouldNo());
        mouldingResultQuery.in("MOULD_CODE", new ArrayList<>(mouldSet));
        return mouldingProductionResultMapper.selectList(mouldingResultQuery);
    }

    /**
     * 计算模具在productionDate还能排产productCode的硫化量
     *
     * @param mouldingProductionInfo 模具排产信息
     * @param adjustInfoHelper       调整信息，包含调整的规格、日期、等
     * @return
     */
    private Integer getCuringQty(MouldingProductionResultHelper mouldingProductionInfo, AdjustInfoHelperVo adjustInfoHelper) {
        Integer initQty = BigDecimal.ZERO.intValue();
        Map<Integer, DayLeftOverCuringTimeVo> dayLeftOverTimeMap = mouldingProductionInfo.getDayLeftOverCuringTime();
        if (CollectionUtils.isEmpty(dayLeftOverTimeMap)) {
            return initQty;
        }
        Integer productionDate = adjustInfoHelper.getAdjustDate();
        DayLeftOverCuringTimeVo dayLeftOverTime = dayLeftOverTimeMap.get(productionDate);
        if (null == dayLeftOverTime || null == dayLeftOverTime.getLeftOverCuringTime()) {
            return initQty;
        }
        BigDecimal singleCuringTime = adjustInfoHelper.getCuringTime();
        //不足以硫化一条
        BigDecimal leftOverTime = dayLeftOverTime.getLeftOverCuringTime();
        if (leftOverTime.compareTo(singleCuringTime) < 0) {
            return initQty;
        }
        Integer maxQty = leftOverTime.divide(singleCuringTime, 0, RoundingMode.DOWN).intValue();
        Map<Integer, List<ProductProductionInfoVo>> dayProductionMap = mouldingProductionInfo.getDayProductionInfo();
        if (CollectionUtils.isEmpty(dayProductionMap)) {
            return maxQty;
        }
        List<ProductProductionInfoVo> dayProductionInfo = dayProductionMap.get(productionDate);
        if (CollectionUtils.isEmpty(dayProductionInfo)) {
            return maxQty;
        }
        Set<String> productCodeSet = dayProductionInfo.stream().map(ProductProductionInfoVo::getProductCode).collect(Collectors.toSet());
        if (productCodeSet.contains(adjustInfoHelper.getProductCode())) {
            return maxQty;
        }
        leftOverTime = leftOverTime.subtract(adjustInfoHelper.getChangeProductConsumeTime());
        if (leftOverTime.compareTo(singleCuringTime) < 0) {
            return initQty;
        }
        return leftOverTime.divide(singleCuringTime, 0, RoundingMode.DOWN).intValue();
    }
}
