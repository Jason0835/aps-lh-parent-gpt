package com.zlt.aps.factory.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.mapper.FactoryMonthPlanProductionDayResultMapper;
import com.zlt.aps.factory.service.IFactoryMonthPlanProductionDayResultService;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanMouldingDayResult;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionDayResult;
import com.zlt.aps.monthplan.api.domain.entity.ProductionMouldConfiguration;
import com.zlt.aps.monthplan.api.domain.entity.SaleMonthPlanRequire;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分厂月度计划日排产结果服务实现
 * 性能优化需要
 *
 * @author ZLT
 * 20250515
 */
@Slf4j
@Service
public class FactoryMonthPlanProductionDayResultServiceImpl extends ServiceImpl<FactoryMonthPlanProductionDayResultMapper, MonthPlanProductionDayResult> implements IFactoryMonthPlanProductionDayResultService {

    @Autowired
    private FactoryMonthPlanProductionDayResultMapper factoryMonthPlanProductionDayResultMapper;

    /**
     * 批量导入分厂月度计划日排产结果
     *
     * @return 结果
     */
    @Override
    public AjaxResult insertFormImportProductionDay(MonthPlanMouldingDayResult monthPlanMouldingDayResult) {
        // 根据需求版本和排产版本查询数据，写入表
        List<MonthPlanMouldingDayResult> monthPlanMouldingDayResultVoList = factoryMonthPlanProductionDayResultMapper.listInsertDataByVersion(monthPlanMouldingDayResult);
        if (CollectionUtils.isEmpty(monthPlanMouldingDayResultVoList)) {
            return AjaxResult.success();
        }

        SaleMonthPlanRequire requireParam = new SaleMonthPlanRequire();
        requireParam.setFactoryCode(monthPlanMouldingDayResult.getFactoryCode());
        requireParam.setYear(monthPlanMouldingDayResult.getYear());
        requireParam.setMonth(monthPlanMouldingDayResult.getMonth());
        requireParam.setMonthPlanVersion(monthPlanMouldingDayResult.getMonthPlanVersion());
        List<SaleMonthPlanRequire> saleMonthPlanRequireList = factoryMonthPlanProductionDayResultMapper.selectByVersionGroupProductCode(requireParam);
        Map<String, SaleMonthPlanRequire> requirePlanMap = new HashMap<>(16);
        Map<String, Long> prodReqPlanMap = new HashMap<>(16);
        Map<String, Long> netDemandQtyMap = new HashMap<>(16);
        Map<String, Long> stockUpDemandQtyMap = new HashMap<>(16);
        buildMap(saleMonthPlanRequireList, requirePlanMap, prodReqPlanMap, netDemandQtyMap, stockUpDemandQtyMap);

        ProductionMouldConfiguration confParam = new ProductionMouldConfiguration();
        confParam.setFactoryCode(monthPlanMouldingDayResult.getFactoryCode());
        confParam.setYear(monthPlanMouldingDayResult.getYear());
        confParam.setMonth(monthPlanMouldingDayResult.getMonth());
        List<ProductionMouldConfiguration> confList = factoryMonthPlanProductionDayResultMapper.selectIsContinueList(confParam);
        Map<String, ProductionMouldConfiguration> configurationMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(confList)) {
            configurationMap = confList.stream().collect(Collectors.toMap(ProductionMouldConfiguration::getProductCode, Function.identity(), (old, now) -> old));
        }

        List<MonthPlanProductionDayResult> resultList = genList(monthPlanMouldingDayResultVoList, requirePlanMap, configurationMap, prodReqPlanMap, netDemandQtyMap, stockUpDemandQtyMap);

        // 根据版本先批量删除原有数据
        factoryMonthPlanProductionDayResultMapper.deleteByVersion(monthPlanMouldingDayResult);
        // 批量保存
        factoryMonthPlanProductionDayResultMapper.insertBatch(resultList);
        return AjaxResult.success();
    }

    private static List<MonthPlanProductionDayResult> genList(List<MonthPlanMouldingDayResult> monthPlanMouldingDayResultVoList, Map<String, SaleMonthPlanRequire> requirePlanMap, Map<String, ProductionMouldConfiguration> configurationMap, Map<String, Long> prodReqPlanMap, Map<String, Long> netDemandQtyMap, Map<String, Long> stockUpDemandQtyMap) {
        List<MonthPlanProductionDayResult> list = new ArrayList<>();
        for (MonthPlanMouldingDayResult monthPlanMouldingDayResultVo : monthPlanMouldingDayResultVoList) {
            MonthPlanProductionDayResult monthPlanProductionDayResult = new MonthPlanProductionDayResult();

            BeanUtils.copyProperties(monthPlanMouldingDayResultVo, monthPlanProductionDayResult, "id");
            monthPlanProductionDayResult.setBaseVale(null);
            // 赋默认值
            monthPlanProductionDayResult.setIsDeliveryDate(YesOrNoEnum.NO.getValue());
            monthPlanProductionDayResult.setIsImportantCustom(YesOrNoEnum.NO.getValue());
            monthPlanProductionDayResult.setIsEnsurePlan(YesOrNoEnum.NO.getValue());
            monthPlanProductionDayResult.setIsEmergency(YesOrNoEnum.NO.getValue());
            monthPlanProductionDayResult.setIsDebitPlan(YesOrNoEnum.NO.getValue());
            monthPlanProductionDayResult.setIsStockUp(YesOrNoEnum.NO.getValue());
            monthPlanProductionDayResult.setIsContinue(YesOrNoEnum.NO.getValue());

            monthPlanProductionDayResult.setIsImport(YesOrNoEnum.YES.getValue());

            String productCode = monthPlanMouldingDayResultVo.getProductCode();
            if (requirePlanMap.containsKey(productCode)) {
                SaleMonthPlanRequire monthPlanRequire = requirePlanMap.get(productCode);
                Integer isDeliveryDateDue = monthPlanRequire.getIsDeliveryDateDue();
                if (isDeliveryDateDue != null) {
                    monthPlanProductionDayResult.setIsDeliveryDate(isDeliveryDateDue);
                }
                Integer isImportantCustom = monthPlanRequire.getIsImportantCustom();
                if (isImportantCustom != null) {
                    monthPlanProductionDayResult.setIsImportantCustom(isImportantCustom);
                }
                Integer isEnsurePlan = monthPlanRequire.getIsEnsurePlan();
                if (isEnsurePlan != null) {
                    monthPlanProductionDayResult.setIsEnsurePlan(isEnsurePlan);
                }
                Integer isEmergency = monthPlanRequire.getIsEmergency();
                if (isEmergency != null) {
                    monthPlanProductionDayResult.setIsEmergency(isEmergency);
                }
                Integer isDebitPlan = monthPlanRequire.getIsDebitPlan();
                if (isDebitPlan != null) {
                    monthPlanProductionDayResult.setIsDebitPlan(isDebitPlan);
                }
                Integer isStockUp = monthPlanRequire.getIsStockUp();
                if (isStockUp != null) {
                    monthPlanProductionDayResult.setIsStockUp(isStockUp);
                }
            }

            if (configurationMap.containsKey(productCode)) {
                monthPlanProductionDayResult.setIsContinue(YesOrNoEnum.YES.getValue());
            }

            if (prodReqPlanMap.containsKey(productCode)) {
                monthPlanProductionDayResult.setProdReqPlan(prodReqPlanMap.get(productCode));
            }
            if (netDemandQtyMap.containsKey(productCode)) {
                monthPlanProductionDayResult.setNetDemandQty(netDemandQtyMap.get(productCode));
            }
            if (stockUpDemandQtyMap.containsKey(productCode)) {
                monthPlanProductionDayResult.setStockUpDemandQty(stockUpDemandQtyMap.get(productCode));
            }

            list.add(monthPlanProductionDayResult);
        }
        return list;
    }

    private static void buildMap(List<SaleMonthPlanRequire> saleMonthPlanRequireList, Map<String, SaleMonthPlanRequire> requirePlanMap, Map<String, Long> prodReqPlanMap, Map<String, Long> netDemandQtyMap, Map<String, Long> stockUpDemandQtyMap) {
        if (CollectionUtils.isNotEmpty(saleMonthPlanRequireList)) {
            for (SaleMonthPlanRequire now : saleMonthPlanRequireList) {
                String productCode = now.getProductCode();
                if (requirePlanMap.containsKey(productCode)) {
                    SaleMonthPlanRequire old = requirePlanMap.get(productCode);
                    if (ObjectUtils.anyNotNull(old.getDeliveryDateDue(), now.getDeliveryDateDue())) {
                        old.setIsDeliveryDateDue(YesOrNoEnum.YES.getValue());
                    }
                    if (Objects.equals(YesOrNoEnum.YES.getValue(), old.getIsImportantCustom()) ||
                            Objects.equals(YesOrNoEnum.YES.getValue(), now.getIsImportantCustom())) {
                        old.setIsImportantCustom(YesOrNoEnum.YES.getValue());
                    }
                    if (Objects.equals(YesOrNoEnum.YES.getValue(), old.getIsEnsurePlan()) ||
                            Objects.equals(YesOrNoEnum.YES.getValue(), now.getIsEnsurePlan())) {
                        old.setIsEnsurePlan(YesOrNoEnum.YES.getValue());
                    }
                    if (Objects.equals(YesOrNoEnum.YES.getValue(), old.getIsEmergency()) ||
                            Objects.equals(YesOrNoEnum.YES.getValue(), now.getIsEmergency())) {
                        old.setIsEmergency(YesOrNoEnum.YES.getValue());
                    }
                    if (Objects.equals(YesOrNoEnum.YES.getValue(), old.getIsDebitPlan()) ||
                            Objects.equals(YesOrNoEnum.YES.getValue(), now.getIsDebitPlan())) {
                        old.setIsDebitPlan(YesOrNoEnum.YES.getValue());
                    }
                    if (Objects.equals(YesOrNoEnum.YES.getValue(), old.getIsStockUp()) ||
                            Objects.equals(YesOrNoEnum.YES.getValue(), now.getIsStockUp())) {
                        old.setIsStockUp(YesOrNoEnum.YES.getValue());
                    }
                    requirePlanMap.put(productCode, old);
                } else {
                    requirePlanMap.put(productCode, now);
                }

                if (prodReqPlanMap.containsKey(productCode)) {
                    prodReqPlanMap.put(productCode, prodReqPlanMap.get(productCode) + now.getPlanQty());
                } else {
                    prodReqPlanMap.put(productCode, now.getPlanQty());
                }
                if (YesOrNoEnum.NO.getValue().equals(now.getIsStockUp())) {
                    if (netDemandQtyMap.containsKey(productCode)) {
                        netDemandQtyMap.put(productCode, netDemandQtyMap.get(productCode) + now.getPlanQty());
                    } else {
                        netDemandQtyMap.put(productCode, now.getPlanQty());
                    }
                } else {
                    if (stockUpDemandQtyMap.containsKey(productCode)) {
                        stockUpDemandQtyMap.put(productCode, stockUpDemandQtyMap.get(productCode) + now.getPlanQty());
                    } else {
                        stockUpDemandQtyMap.put(productCode, now.getPlanQty());
                    }
                }
            }
        }
    }
}
