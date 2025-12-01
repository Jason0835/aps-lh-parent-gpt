package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.tlt.aps.constant.Constant;
import com.tlt.aps.enums.LocationTypeEnum;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.maindata.mapper.MdmProductVulcanizingEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductionCalendarEntityMapper;
import com.zlt.aps.maindata.mapper.MdmProductionMoldingEntityMapper;
import com.zlt.aps.monthplan.api.domain.entity.EstimateExceedShort;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductVulcanizing;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductionCalendar;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductionMolding;
import com.zlt.aps.monthplan.factory.service.IMdmProductionGenerateService;
import com.zlt.aps.monthplan.mdm.mapper.EstimateExceedShortMapper;
import com.zlt.aps.monthplan.mdm.service.IEstimateExceedShortService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 生成成型/硫化机正在生成品种
 */
@Service
@RequiredArgsConstructor
public class MdmProductionGenerateServiceImpl implements IMdmProductionGenerateService {

    private final MdmProductionMoldingEntityMapper mdmProductionMoldingEntityMapper;
    private final MdmProductVulcanizingEntityMapper mdmProductVulcanizingEntityMapper;
    private final MdmProductionCalendarEntityMapper mdmProductionCalendarEntityMapper;
    private final EstimateExceedShortMapper estimateExceedShortMapper;

    private final BaseDao baseDao;
    private final IEstimateExceedShortService iEstimateExceedShortService;


    /**
     * 根据指定年月上个月的数据，生成分厂/硫化成型正在生成产品
     *
     * @param generateYear  生成年
     * @param generateMonth 生成月
     * @param factoryCode   生成分厂
     * @return 结果数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateProductData(Integer generateYear, Integer generateMonth, String factoryCode) {
        Calendar calendar = Calendar.getInstance();
        String username = SecurityUtils.getUsername();

        // 获取抓取的月份的上个月数据
        Date date = DateUtils.dateTime("yyyy-MM-dd", generateYear + "-" + generateMonth + "-01");
        Date lastDate = DateUtils.addDays(date, -1);
        calendar.setTime(lastDate);
        int fromYear = calendar.get(Calendar.YEAR);
        int fromMonth = calendar.get(Calendar.MONTH) + 1;
        int lastDay = calendar.get(Calendar.DATE);

        // 删除对应月份的成型机/硫化机正在生成的品种
        LambdaQueryWrapper<MdmProductionMolding> deleteMoldingWrapper = Wrappers.lambdaQuery();
        deleteMoldingWrapper.eq(MdmProductionMolding::getYear, generateYear);
        deleteMoldingWrapper.eq(MdmProductionMolding::getMonth, generateMonth);
        deleteMoldingWrapper.eq(MdmProductionMolding::getFactoryCode, factoryCode);
        mdmProductionMoldingEntityMapper.delete(deleteMoldingWrapper);
        LambdaQueryWrapper<MdmProductVulcanizing> deleteVulcanizingWrapper = Wrappers.lambdaQuery();
        deleteVulcanizingWrapper.eq(MdmProductVulcanizing::getYear, generateYear);
        deleteVulcanizingWrapper.eq(MdmProductVulcanizing::getMonth, generateMonth);
        deleteVulcanizingWrapper.eq(MdmProductVulcanizing::getFactoryCode, factoryCode);
        mdmProductVulcanizingEntityMapper.delete(deleteVulcanizingWrapper);

        // 将日期排除到停工日期之外的之后一天
        LambdaQueryWrapper<MdmProductionCalendar> calendarWrapper = Wrappers.lambdaQuery();
        calendarWrapper.eq(MdmProductionCalendar::getYear, fromYear);
        calendarWrapper.eq(MdmProductionCalendar::getMonth, fromMonth);
        calendarWrapper.eq(MdmProductionCalendar::getFactoryCode, factoryCode);
        List<MdmProductionCalendar> calendarList = mdmProductionCalendarEntityMapper.selectList(calendarWrapper)
                .stream().filter(v -> v.getBeginDate() != null && v.getEndDate() != null).collect(Collectors.toList());
        // 依据分厂取出可生产的最后一天，生成对应分厂的数据
        boolean[] productionDateArray = new boolean[lastDay];
        for (MdmProductionCalendar item : calendarList) {
            calendar.setTime(item.getBeginDate());
            int minDay = calendar.get(Calendar.DATE) - 1;
            calendar.setTime(item.getEndDate());
            int maxDay = calendar.get(Calendar.DATE);
            Arrays.fill(productionDateArray, minDay, maxDay, true);
        }
        int productionDate = productionDateArray.length - 1;
        while (productionDate >= 0 && productionDateArray[productionDate]) {
            productionDate--;
        }
        // 没有一天可以进行生产
        if (productionDate < 0) {
            return 0;
        }
        productionDate += 1;

        // 生成-成型正在生产品种
        int count = mdmProductionMoldingEntityMapper.generateByMonthPlan(username, factoryCode, fromYear, fromMonth, generateYear, generateMonth, "DAY_" + productionDate);
        // 生成-硫化正在生产品种
        count += mdmProductVulcanizingEntityMapper.generateByMonthPlan(username, factoryCode, fromYear, fromMonth, generateYear, generateMonth, "DAY_" + productionDate);

        return count;
    }

    /**
     * 根据指定年月和当前所在日，生成预计超欠产
     *
     * @param generateYear  生成年
     * @param generateMonth 生成月
     * @param factoryCode   分厂
     * @return 结果数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateEstimateExceedShort(Integer generateYear, Integer generateMonth, String factoryCode) {
        //如果年月、分厂的超欠产记录，存在导入的记录，不重新生成
        LambdaQueryWrapper<EstimateExceedShort> importWrapper = Wrappers.lambdaQuery();
        importWrapper.eq(EstimateExceedShort::getYear, generateYear);
        importWrapper.eq(EstimateExceedShort::getMonth, generateMonth);
        importWrapper.eq(EstimateExceedShort::getFactoryCode, factoryCode);
        importWrapper.eq(EstimateExceedShort::getIsImport, Constant.TRUE);
        if (estimateExceedShortMapper.selectCount(importWrapper) > 0) {
            return 0;
        }
        //删除自动计算的预计超欠产数据
        LambdaQueryWrapper<EstimateExceedShort> deleteWrapper = Wrappers.lambdaQuery();
        deleteWrapper.eq(EstimateExceedShort::getYear, generateYear);
        deleteWrapper.eq(EstimateExceedShort::getMonth, generateMonth);
        deleteWrapper.eq(EstimateExceedShort::getFactoryCode, factoryCode);
        estimateExceedShortMapper.delete(deleteWrapper);
        //todo 需要根据年月对应的定稿版本周期来计算
        Calendar calendar = Calendar.getInstance();
        // 获取当前所在的日-1
        calendar.setTime(DateUtils.getNowDate());
        int nowDay = calendar.get(Calendar.DATE) - 1;
        // 没有可生产的数据
        if (nowDay <= 0) {
            return 0;
        }
        // 需要汇总的字段名
        List<String> sumFieldList = new ArrayList<>();
        while (nowDay > 0) {
            sumFieldList.add("DAY_" + nowDay);
            nowDay--;
        }
        // 查询月度计划排产量
        List<EstimateExceedShort> planList = estimateExceedShortMapper.selectMonthPlanList(factoryCode, generateYear, generateMonth, sumFieldList);
        Function<EstimateExceedShort, String> planKeyFunc = item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getProductCode(), item.getLocationType());
        // 查询实际生产库存量
        List<EstimateExceedShort> surplusList = estimateExceedShortMapper.selectSurplusPlanList(factoryCode, generateYear, generateMonth);
        Function<EstimateExceedShort, String> surplusKeyFunc = item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getProductCode());

        // 记录欠产量、生产库存量
        Map<String, EstimateExceedShort> planMap = new HashMap<>();
        Map<String, EstimateExceedShort> surplusMap = surplusList.stream().collect(Collectors.toMap(surplusKeyFunc, Function.identity()));

        // 区分外销、内销和OE计划
        List<EstimateExceedShort> outPlanList = new ArrayList<>();
        List<EstimateExceedShort> inPlanList = new ArrayList<>();
        List<EstimateExceedShort> oePlanList = new ArrayList<>();
        for (EstimateExceedShort item : planList) {
            if (LocationTypeEnum.FOREIGN_LOCATION.getValue().equals(item.getLocationType())) {
                outPlanList.add(item);
            } else if (LocationTypeEnum.OE_LOCATION.getValue().equals(item.getLocationType())) {
                oePlanList.add(item);
            } else {
                inPlanList.add(item);
            }
        }

        // 超欠产-优先给OE，再给外贸，最后是内销
        reduceSurplus(oePlanList, surplusKeyFunc, surplusMap, planKeyFunc, planMap);
        reduceSurplus(outPlanList, surplusKeyFunc, surplusMap, planKeyFunc, planMap);
        reduceSurplus(inPlanList, surplusKeyFunc, surplusMap, planKeyFunc, planMap);

        // 超产的部分，记录到外销库位
        for (EstimateExceedShort item : surplusMap.values()) {
            if (item.getExceedShortQty() > 0) {
                item.setLocationType(LocationTypeEnum.FOREIGN_LOCATION.getValue());
                String planKey = planKeyFunc.apply(item);
                EstimateExceedShort oldPlan = planMap.get(planKey);
                if (oldPlan != null) {
                    oldPlan.setExceedShortQty(oldPlan.getExceedShortQty() + item.getExceedShortQty());
                } else {
                    planMap.put(planKey, item);
                }
            }
        }

        // 过滤掉可能【超欠产=0】的记录
        List<EstimateExceedShort> resultList = planMap.values().stream().filter(v -> v.getExceedShortQty() != null && v.getExceedShortQty() != 0)
                .peek(v -> {
                    v.setYear(generateYear);
                    v.setMonth(generateMonth);
                })
                .collect(Collectors.toList());
        iEstimateExceedShortService.setProductInfo(resultList);
        return baseDao.insertBatch(resultList);
    }

    /**
     * 扣减剩余量
     *
     * @param exceedShortList 月度计划排产量
     * @param surplusKeyFunc  生产量key
     * @param surplusMap      生产量Map
     * @param planKeyFunc     计划量key
     * @param planMap         计划量Map
     */
    private void reduceSurplus(List<EstimateExceedShort> exceedShortList,
                               Function<EstimateExceedShort, String> surplusKeyFunc,
                               Map<String, EstimateExceedShort> surplusMap,
                               Function<EstimateExceedShort, String> planKeyFunc,
                               Map<String, EstimateExceedShort> planMap) {
        if (CollectionUtils.isEmpty(exceedShortList)) {
            return;
        }

        for (EstimateExceedShort item : exceedShortList) {
            Integer plan = item.getExceedShortQty();
            if (plan == 0) {
                continue;
            }
            String surplusKey = surplusKeyFunc.apply(item);
            EstimateExceedShort surplusItem = surplusMap.get(surplusKey);
            if (surplusItem != null) {
                Integer surplus = surplusItem.getExceedShortQty();
                if (surplus != null && surplus > 0) {
                    // 存在生产量，优先扣减生产量的部分
                    plan = surplus + plan;
                    if (plan >= 0) {
                        surplusItem.setExceedShortQty(plan);
                    } else {
                        surplusItem.setExceedShortQty(0);
                    }
                }
            }

            if (plan < 0) {
                // 生产量不足，记录到欠产列表
                String planKey = planKeyFunc.apply(item);
                EstimateExceedShort oldPlan = planMap.get(planKey);
                if (oldPlan != null) {
                    oldPlan.setExceedShortQty(oldPlan.getExceedShortQty() + plan);
                } else {
                    item.setExceedShortQty(plan);
                    planMap.put(planKey, item);
                }
            }
        }
    }
}

