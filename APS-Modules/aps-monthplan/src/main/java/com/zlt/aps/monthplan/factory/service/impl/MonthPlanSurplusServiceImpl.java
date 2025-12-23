package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.LocationTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.maindata.mapper.LhMonthPlanSurplusDetailMapper;
import com.zlt.aps.maindata.mapper.LhMonthPlanSurplusEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMonthSurplusEntityMapper;
import com.zlt.aps.maindata.service.ICxEmbryoMonthPlanSurplusService;
import com.zlt.aps.maindata.service.ILhMonthPlanSurplusService;
import com.zlt.aps.maindata.service.IMdmProductConstructionService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.LhMonthPlanSurplus;
import com.zlt.aps.monthplan.api.domain.entity.LhMonthPlanSurplusDetail;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.monthplan.api.domain.vo.MdmProductConstructionVO;
import com.zlt.aps.monthplan.factory.service.IMonthPlanSurplusService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 月度剩余量业务实现接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthPlanSurplusServiceImpl implements IMonthPlanSurplusService {
    private final BaseDao baseDao;
    private final LhMonthPlanSurplusDetailMapper lhMonthPlanSurplusDetailMapper;

    private final LhMonthPlanSurplusEntityMapper lhMonthPlanSurplusEntityMapper;

    private final ILhMonthPlanSurplusService iLhMonthPlanSurplusService;

    private final IMdmProductConstructionService mdmProductConstructionService;

    private final ICxEmbryoMonthPlanSurplusService iCxEmbryoMonthPlanSurplusService;

    private final MdmMonthSurplusEntityMapper mdmMonthSurplusEntityMapper;


    @Override
    public void savePlanSurplusList(List<FactoryMonthPlanProdFinal> finalList) {
        if (CollectionUtils.isEmpty(finalList)) {
            return;
        }
        finalList.forEach(item -> {
            if (StringUtils.isBlank(item.getLocationType())) {
                item.setLocationType(LocationTypeEnum.DOMESTIC_LOCATION.getValue());
            }
        });
        // 按照年月、分厂、物料、规格汇总月度外胎汇总和外胎汇总明细
        List<LhMonthPlanSurplus> surplusList = new ArrayList<>();
        List<LhMonthPlanSurplusDetail> surplusDetailList = new ArrayList<>();
        this.buildSurplusAndDetail(finalList, surplusDetailList, surplusList);

        // 查询历史年月、分厂、物料、规格、库位的月度计划外胎汇总明细，有则更新，无则插入
        this.updatePlanSurplusDetailList(surplusDetailList);

        // 查询历史年月、分厂、物料、规格的月度计划外胎汇总，有则重算计划量和明细完成量，无则插入
        this.updatePlanSurplusList(surplusList);

        // 更新成型胎胚汇总表数据，有则更新，无则插入
        this.updateMouldingPlanDetailList(surplusList);
    }

    @Override
    public void batchInsertPlanSurplusList(List<MdmMonthSurplus> mdmMonthSurpluses) {
        this.baseDao.insertBatch(mdmMonthSurpluses);
    }

    @Override
    public List<MdmMonthSurplus> findCurrentMonthPlanSurplus() {
        LambdaQueryWrapper<MdmMonthSurplus> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MdmMonthSurplus::getIsDelete, YesOrNoEnum.NO.getValue());
        // 获取当前月的第一天和最后一天
        LocalDate now = LocalDate.now();
        LocalDate firstDayOfMonth = now.withDayOfMonth(1);
        LocalDate lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        // 转换为Date类型（如果createTime是Date类型）
        Date startDate = DateUtils.toDate(firstDayOfMonth);
        Date endDate = DateUtils.toDate(lastDayOfMonth);
        wrapper.between(MdmMonthSurplus::getCreateTime, startDate, endDate);
        return mdmMonthSurplusEntityMapper.selectList(wrapper);
    }

    /**
     * 按照年月、分厂、物料、规格汇总月度外胎汇总和外胎汇总明细
     *
     * @param finalList         定稿的月计划记录
     * @param surplusDetailList 月度外胎汇总明细列表
     * @param surplusList       月度外胎汇总列表
     */
    private void buildSurplusAndDetail(List<FactoryMonthPlanProdFinal> finalList, List<LhMonthPlanSurplusDetail> surplusDetailList, List<LhMonthPlanSurplus> surplusList) {
        if (CollectionUtils.isEmpty(finalList)) {
            return;
        }

        // 按照年月、分厂、物料、规格汇总
        Function<FactoryMonthPlanProdFinal, String> finalKey = v -> GenerageMapKeyUtils.createMapKey(v.getYear(), v.getMonth(), v.getFactoryCode(), v.getProductCode(), v.getSpecCode());
        Map<String, List<FactoryMonthPlanProdFinal>> groupMap = finalList.stream().collect(Collectors.groupingBy(finalKey));
        groupMap.forEach((key, itemList) -> {
            if (CollectionUtils.isEmpty(itemList)) {
                return;
            }

            FactoryMonthPlanProdFinal itemFinal = itemList.get(0);
            LhMonthPlanSurplus planSurplus = new LhMonthPlanSurplus();
            planSurplus.setMonthPlanVersion(itemFinal.getMonthPlanVersion());
            planSurplus.setProductionVersion(itemFinal.getProductionVersion());
            planSurplus.setYear(itemFinal.getYear());
            planSurplus.setMonth(itemFinal.getMonth());
            planSurplus.setFactoryCode(itemFinal.getFactoryCode());
            planSurplus.setProductCode(itemFinal.getProductCode());
            planSurplus.setSpecCode(itemFinal.getSpecCode());
            // 月计划总量
            long monthPlanQty = itemList.stream().filter(v -> v.getTotalQty() != null).mapToLong(FactoryMonthPlanProdFinal::getTotalQty).sum();
            planSurplus.setMonthPlanQty((int) monthPlanQty);
            planSurplus.setLastMonthStock(0);
            planSurplus.setSpecBadQty(0);
            planSurplus.setMonthFinishQty(0);
            // 初始剩余量=月计划
            planSurplus.setMonthRemainQty(planSurplus.getMonthPlanQty());
            planSurplus.setDataSource(StringConstant.ZERO);

            // 按照库位分组
            Map<String, List<FactoryMonthPlanProdFinal>> locationTypeMap = itemList.stream().collect(Collectors.groupingBy(v -> String.valueOf(v.getLocationType())));
            locationTypeMap.forEach((locationType, locationTypeList) -> {
                if (CollectionUtils.isEmpty(locationTypeList)) {
                    return;
                }

                LhMonthPlanSurplusDetail surplusDetail = new LhMonthPlanSurplusDetail();
                BeanUtils.copyProperties(planSurplus, surplusDetail);
                surplusDetail.setLocationType(locationType);
                // 起始日取月度计划最小的起始日
                int minStart = Integer.MAX_VALUE;
                // 结束日取月度计划最大的结束日
                int maxEnd = Integer.MIN_VALUE;
                // 计算总量
                long sumProductionQty = 0;
                for (FactoryMonthPlanProdFinal locationFinal : locationTypeList) {
                    // 如果总量为0，对应起始日和结束日不纳入计算
                    if (locationFinal.getTotalQty() == null || locationFinal.getTotalQty() == 0) {
                        continue;
                    }
                    sumProductionQty += locationFinal.getTotalQty();

                    if (locationFinal.getBeginDate() != null && locationFinal.getBeginDate() < minStart) {
                        minStart = locationFinal.getBeginDate();
                    }
                    if (locationFinal.getEndDay() != null && locationFinal.getEndDay() > maxEnd) {
                        maxEnd = locationFinal.getEndDay();
                    }
                }
                surplusDetail.setStartDate(minStart < Integer.MAX_VALUE ? minStart : null);
                surplusDetail.setEndDate(maxEnd > Integer.MIN_VALUE ? maxEnd : null);
                surplusDetail.setProductionQty(sumProductionQty);
                surplusDetail.setCompleteQty(0L);

                surplusDetailList.add(surplusDetail);
            });

            surplusList.add(planSurplus);
        });
    }

    /**
     * 查询历史年月、分厂、物料、规格、库位的月度计划外胎汇总明细，有则更新，无则插入
     *
     * @param surplusDetailList 月度计划外胎汇总明细
     */
    private void updatePlanSurplusDetailList(List<LhMonthPlanSurplusDetail> surplusDetailList) {
        if (CollectionUtils.isEmpty(surplusDetailList)) {
            return;
        }

        List<LhMonthPlanSurplusDetail> insertDetailList = new ArrayList<>();
        List<LhMonthPlanSurplusDetail> updateDetailList = new ArrayList<>();
        // 查询对应年月、分厂、物料、规格、库位的外胎汇总记录
        Map<String, LhMonthPlanSurplusDetail> oldDetailMap = new HashMap<>();
        Function<LhMonthPlanSurplusDetail, String> detailKey =
                v -> GenerageMapKeyUtils.createMapKey(v.getYear(), v.getMonth(), v.getFactoryCode(), v.getProductCode(), v.getSpecCode(), v.getLocationType());
        ListUtils.partition(surplusDetailList, 900).forEach(itemList -> {
            List<LhMonthPlanSurplusDetail> oldDetailList = lhMonthPlanSurplusDetailMapper.selectParamDetailList(itemList);
            oldDetailMap.putAll(oldDetailList.stream().collect(Collectors.toMap(detailKey, Function.identity(), (v1, v2) -> v1)));
        });

        for (LhMonthPlanSurplusDetail itemDetail : surplusDetailList) {
            String key = detailKey.apply(itemDetail);
            LhMonthPlanSurplusDetail oldDetail = oldDetailMap.get(key);
            if (oldDetail == null) {
                insertDetailList.add(itemDetail);
            } else {
                // 如果已经存在，历史记录更新开始时间、结束时间、总生产量，主表更新后会重新计算对应完成量
                oldDetail.setStartDate(itemDetail.getStartDate());
                oldDetail.setEndDate(itemDetail.getEndDate());
                oldDetail.setProductionQty(itemDetail.getProductionQty());
                updateDetailList.add(oldDetail);
            }
        }

        this.baseDao.updateBatch(updateDetailList);
        this.baseDao.insertBatch(insertDetailList);
    }

    /**
     * 查询历史年月、分厂、物料、规格的月度计划外胎汇总，有则重算计划量和明细完成量，无则插入
     *
     * @param surplusList 月度计划外胎汇总
     */
    private void updatePlanSurplusList(List<LhMonthPlanSurplus> surplusList) {
        if (CollectionUtils.isEmpty(surplusList)) {
            return;
        }

        List<LhMonthPlanSurplus> insertList = new ArrayList<>();
        Map<String, LhMonthPlanSurplus> updateMap = new HashMap<>();
        // 查询对应年月、分厂、物料、规格的外胎汇总记录
        Map<String, LhMonthPlanSurplus> oldDetailMap = new HashMap<>();
        Function<LhMonthPlanSurplus, String> detailKey = v -> GenerageMapKeyUtils.createMapKey(v.getYear(), v.getMonth(), v.getFactoryCode(), v.getProductCode(), v.getSpecCode());
        ListUtils.partition(surplusList, 900).forEach(itemList -> {
            List<LhMonthPlanSurplus> oldDetailList = lhMonthPlanSurplusEntityMapper.selectParamList(itemList);
            oldDetailMap.putAll(oldDetailList.stream().collect(Collectors.toMap(detailKey, Function.identity(), (v1, v2) -> v1)));
        });

        for (LhMonthPlanSurplus itemDetail : surplusList) {
            String key = detailKey.apply(itemDetail);
            LhMonthPlanSurplus oldDetail = oldDetailMap.get(key);
            if (oldDetail == null) {
                insertList.add(itemDetail);
            } else {
                // 如果已经存在，重算计划量和明细完成量
                updateMap.put(key, oldDetail);
            }
        }

        this.baseDao.insertBatch(insertList);

        Collection<LhMonthPlanSurplus> updateCollection = updateMap.values();
        if (CollectionUtils.isEmpty(updateCollection)) {
            return;
        }

        // 重新汇总月计划量、月剩余量
        lhMonthPlanSurplusEntityMapper.updateMonthPlanQty(updateCollection);
        // 重算分配明细完成量
        iLhMonthPlanSurplusService.reAssignmentFinishQty(new ArrayList<>(updateCollection));

    }

    /**
     * 更新成型胎胚汇总表数据，有则更新，无则插入
     *
     * @param finalList 定稿的月计划记录
     */
    private void updateMouldingPlanDetailList(List<LhMonthPlanSurplus> finalList) {
        // 依据分厂,物料, 规格匹配
        if (finalList.isEmpty()) {
            return;
        }

        // 1.依据物料施工代码汇总一个列表
        List<String> specCodeList = finalList.stream().filter(item -> StringUtils.isNotEmpty(item.getProductCode()) && StringUtils.isNotEmpty(item.getSpecCode()))
                .map(item -> item.getProductCode() + "_" + item.getSpecCode())
                .collect(Collectors.toList());

        //2. 获取第一条数据的分厂
        String factoryCode = finalList.get(0).getFactoryCode();

        // 3. 获取物料代码+规格代码的-胎胚分组
        Map<String, List<MdmProductConstructionVO>> constructionMap = new HashMap<>();
        List<MdmProductConstructionVO> cxMdmProductConstructionList =
                mdmProductConstructionService.queryByFactoryCodeAndSpecCodes(factoryCode, new HashSet<>(specCodeList));

        if (CollectionUtils.isEmpty(cxMdmProductConstructionList)) {
            return;
        }

        constructionMap = cxMdmProductConstructionList.stream()
                .collect(Collectors.groupingBy(item -> item.getProductCode() + item.getSpecCode()));


        // 4.finalList匹配胎胚Map
        for (LhMonthPlanSurplus itemFinal : finalList) {
            String key = itemFinal.getProductCode() + itemFinal.getSpecCode();
            if (constructionMap.containsKey(key)) {
                MdmProductConstructionVO cxMdmProductConstruction = constructionMap.get(key).get(0);
                //填充胎胚
                itemFinal.setEmbryoCode(cxMdmProductConstruction.getEmbryoCode());
                //填充BOM
                itemFinal.setBomDataVersion(cxMdmProductConstruction.getBomVersion());
            }
        }

        //5.finalList依据相同胎胚,Bom版本进行合并,总计划量、总剩余量
        Map<String, LhMonthPlanSurplus> mergedMap = new HashMap<>();

        for (LhMonthPlanSurplus item : finalList) {
            // 跳过没有胎胚编码的记录
            if (StringUtils.isEmpty(item.getEmbryoCode()) || StringUtils.isEmpty(item.getBomDataVersion())) {
                continue;
            }

            String mergeKey = item.getEmbryoCode() + "_" + item.getBomDataVersion();
            if (mergedMap.containsKey(mergeKey)) {
                // 合并记录
                LhMonthPlanSurplus mergedItem = mergedMap.get(mergeKey);
                // 计划量累加
                mergedItem.setMonthPlanQty(mergedItem.getMonthPlanQty() + (item.getMonthPlanQty()));
            } else {
                // 新建合并记录
                LhMonthPlanSurplus newItem = new LhMonthPlanSurplus();
                BeanUtils.copyProperties(item, newItem);
                mergedMap.put(mergeKey, newItem);
            }
        }

        // 6. 将合并后的数据保存到数据库
        if (!mergedMap.isEmpty()) {
            // 这里假设有对应的service方法来批量保存或更新
            iCxEmbryoMonthPlanSurplusService.batchSaveOrUpdate(new ArrayList<>(mergedMap.values()));
        }
    }
}
