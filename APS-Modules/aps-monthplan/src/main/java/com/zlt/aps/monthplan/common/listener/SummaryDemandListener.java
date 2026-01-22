package com.zlt.aps.monthplan.common.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlan;
import com.zlt.aps.monthplan.api.domain.entity.DpDemandPlanSum;
import com.zlt.aps.monthplan.common.event.SummaryDemandEvent;
import com.zlt.aps.monthplan.demand.mapper.DpDemandPlanEntityMapper;
import com.zlt.aps.monthplan.demand.service.IDpStockVersionService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.SmartApplicationListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 汇总需求计划
 * @author Yelq
 */
@Component
@RequiredArgsConstructor
public class SummaryDemandListener implements SmartApplicationListener {

  private final DpDemandPlanEntityMapper demandPlanEntityMapper;
  private final BaseDao baseDao;
  // 版本库存
  private final IDpStockVersionService dpStockVersionService;

  @Override
  public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
    return eventType == SummaryDemandEvent.class;
  }

  @Override
  public void onApplicationEvent(ApplicationEvent event) {
    DpDemandPlan demandPlan = (DpDemandPlan) event.getSource();
    Map<String, Map<String, Integer>> stockQtyMap =  dpStockVersionService.calculateStockQty(demandPlan.getMonthPlanVersion());
    List<DpDemandPlan> list = this.findDemandPlan(demandPlan);
    Map<String,List<DpDemandPlan>> map = list.stream().collect(Collectors.groupingBy(DpDemandPlan::getMonthPlanVersionKey));
    List<DpDemandPlanSum> datas = Lists.newArrayList();
    map.forEach((key, value) -> {
        Map<String,Integer> stockMap = stockQtyMap.getOrDefault(key, Collections.emptyMap());
        DpDemandPlanSum entity = new DpDemandPlanSum();
        BeanUtils.copyProperties(value.get(0), entity);
        entity.setId(null);
        entity.setBaseVale(null);
        entity.setStockQty(stockMap.getOrDefault(StringConstant.ZERO, BigDecimal.ZERO.intValue()));
        entity.setCurrentYearStockQty(stockMap.getOrDefault(StringConstant.ONE,BigDecimal.ZERO.intValue()));
        entity.setSub1YearStockQty(stockMap.getOrDefault(StringConstant.TWO,BigDecimal.ZERO.intValue()));
        entity.setSub2YearStockQty(stockMap.getOrDefault(StringConstant.THREE,BigDecimal.ZERO.intValue()));
        entity.setNetQty(calculateNetQty(value));
        entity.setPostponeNetQty(calculatePostponeNetQty(value));
        entity.setUnPostponeNetQty(calculateUnPostponeNetQty(value));
        entity.setHeightQty(calculateHeightQty(value));
        entity.setMidQty(calculateMidQty(value));
        entity.setPostponeQty(calculatePostponeQty(value));
        entity.setCycleReserveQty(calculateCycleReserveQty(value));
        entity.setConventionReserveQty(calculateConventionReserveQty(value));
        entity.setIsReachMinProductionQty(entity.getNetQty() >= entity.getMinProductionQty()?YesOrNoEnum.YES.getCode() : YesOrNoEnum.NO.getCode());
        datas.add(entity);
    });
    baseDao.insertBatch(datas);
  }

  private List<DpDemandPlan> findDemandPlan(DpDemandPlan param) {
    LambdaQueryWrapper<DpDemandPlan> wrapper = Wrappers.lambdaQuery();
    wrapper.eq(DpDemandPlan::getFactoryCode, param.getFactoryCode());
    wrapper.eq(DpDemandPlan::getYear, param.getYear());
    wrapper.eq(DpDemandPlan::getMonth, param.getMonth());
    wrapper.eq(DpDemandPlan::getMonthPlanVersion, param.getMonthPlanVersion());
    wrapper.eq(DpDemandPlan::getIsDelete, YesOrNoEnum.NO.getValue());
    return this.demandPlanEntityMapper.selectList(wrapper);
  }

  private int calculateConventionReserveQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getConventionReserveQty() != null)
        .mapToInt(DpDemandPlan::getConventionReserveQty).sum();
  }

  private int calculateCycleReserveQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getCycleReserveQty() != null)
        .mapToInt(DpDemandPlan::getCycleReserveQty).sum();
  }

  private int calculatePostponeQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getPostponeQty() != null)
        .mapToInt(DpDemandPlan::getPostponeQty).sum();
  }

  private int calculateMidQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getMidQty() != null)
        .mapToInt(DpDemandPlan::getMidQty).sum();
  }

  private int calculateHeightQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getHeightQty() != null)
        .mapToInt(DpDemandPlan::getHeightQty).sum();
  }

  private int calculateUnPostponeNetQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getUnPostponeNetQty() != null)
        .mapToInt(DpDemandPlan::getUnPostponeNetQty).sum();
  }

  private int calculateNetQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getNetQty() != null)
        .mapToInt(DpDemandPlan::getNetQty).sum();
  }

  private int calculatePostponeNetQty(List<DpDemandPlan> dataList) {
    return dataList.stream()
        .filter(Objects::nonNull)
        .filter(demandPlan ->  demandPlan.getPostponeNetQty() != null)
        .mapToInt(DpDemandPlan::getPostponeNetQty).sum();
  }


}
