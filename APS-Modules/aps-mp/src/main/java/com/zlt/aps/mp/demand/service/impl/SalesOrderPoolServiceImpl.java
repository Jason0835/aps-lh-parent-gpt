package com.zlt.aps.mp.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.baseVo.excelVo.CellStyle;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.AjaxResultUtils;
import com.zlt.aps.common.core.utils.BigDecimalUtils;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.scm.service.IScmItfService;
import com.zlt.aps.itf.scm.vo.SyncPlanedNotShipParamVo;
import com.zlt.aps.itf.scm.vo.SyncPlanedNotShipResultVo;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.DpAreaEntityMapper;
import com.zlt.aps.maindata.mapper.DpNationEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.mp.api.domain.entity.*;
import com.zlt.aps.mp.common.utils.PubUtil;
import com.zlt.aps.mp.demand.mapper.SalesOrderPoolEntityMapper;
import com.zlt.aps.mp.demand.mapper.SalesOrderPoolRecordEntityMapper;
import com.zlt.aps.mp.demand.service.ISalesOrderPoolService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.aps.utils.JsonI18nConvertUtils;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。 文件名称：SalesOrderPoolServiceImpl.java
 * 描 述：SalesOrderPoolServiceImpl销售订单池业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录： 修改时间：... 修 改 人：zlt 修改内容：...
 * @date 2025-12-04
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class SalesOrderPoolServiceImpl extends AbstractDocService<SalesOrderPool> implements ISalesOrderPoolService {
    @Autowired
    private IScmItfService iScmItfService;
    @Autowired
    private IFactoryParamService iFactoryParamService;
    @Autowired
    private IMesItfService iMesItfService;

    @Autowired
    private ISysDictDataCacheService sysDictDataCacheService;

    @Autowired
    private SalesOrderPoolEntityMapper salesOrderPoolEntityMapper;
    @Autowired
    private SalesOrderPoolRecordEntityMapper salesOrderPoolRecordEntityMapper;
    @Autowired
    private DpAreaEntityMapper dpAreaEntityMapper;
    @Autowired
    private MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;
    @Autowired
    private DpNationEntityMapper dpNationEntityMapper;

    @Override
    protected String getDocTypeCode() {
        return "DP0202";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("DP0202");
        return sysDocType;
    }

    @Override
    public String checkUnique(SalesOrderPool docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.salesOrderPool.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    /**
     * 锁定订单池
     */
    @Override
    public AjaxResult lockSalesOrderPool(SalesOrderPool salesOrderPool) {
        SyncPlanedNotShipParamVo planedNotShipParamVo = new SyncPlanedNotShipParamVo();
        planedNotShipParamVo.setYear(salesOrderPool.getYear());
        planedNotShipParamVo.setMonth(salesOrderPool.getMonth());
        planedNotShipParamVo.setFactory(salesOrderPool.getFactoryCode());
        return iScmItfService.lockSalesOrderPool(planedNotShipParamVo);
    }

    /**
     * 解锁订单池
     *
     * @param salesOrderPool 条件
     * @return 结果
     */
    @Override
    public AjaxResult unlockSalesOrderPool(SalesOrderPool salesOrderPool) {
        SyncPlanedNotShipParamVo planedNotShipParamVo = new SyncPlanedNotShipParamVo();
        planedNotShipParamVo.setYear(salesOrderPool.getYear());
        planedNotShipParamVo.setMonth(salesOrderPool.getMonth());
        planedNotShipParamVo.setFactory(salesOrderPool.getFactoryCode());
        return iScmItfService.unlockSalesOrderPool(planedNotShipParamVo);
    }

    @Override
    public List<SalesOrderPool> findCurrentSalesOrderPool(String factoryCode, Set<String> skus) {
        if (CollectionUtils.isEmpty(skus)) {
            return Collections.emptyList();
        }
        List<SalesOrderPool> result = Lists.newArrayList();
        final int batchSize = 1000;
        List<String> skuList = new ArrayList<>(skus);
        for (int i = 0; i < skus.size(); i += batchSize) {
            int end = Math.min(i + batchSize, skus.size());
            List<String> batchSkus = skuList.subList(i, end);
            LambdaQueryWrapper<SalesOrderPool> wrapper =
                    Wrappers.lambdaQuery(SalesOrderPool.class)
                            .eq(SalesOrderPool::getFactoryCode, factoryCode)
                            .in(SalesOrderPool::getOriMaterialCode, batchSkus)
                            .eq(SalesOrderPool::getIsDelete, ApsConstant.APS_YES_NO_0);
            result.addAll(salesOrderPoolEntityMapper.selectList(wrapper));
        }
        return result;
    }


    /**
     * 批量修改同PO号的销售优先级
     *
     * @param salesOrderPool
     * @return
     */
    @Override
    public AjaxResult editBySalCodePo(SalesOrderPool salesOrderPool) {
        LambdaQueryWrapper<SalesOrderPool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SalesOrderPool::getSalCodePo, salesOrderPool.getSalCodePo());
        if (!salesOrderPoolEntityMapper.exists(queryWrapper)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.SalesOrderPool.salCodePo.noexists"));
        }
        LambdaUpdateWrapper<SalesOrderPool> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SalesOrderPool::getSalCodePo, salesOrderPool.getSalCodePo());
        updateWrapper.set(SalesOrderPool::getScmPriority, salesOrderPool.getScmPriority());
        salesOrderPoolEntityMapper.update(null, updateWrapper);
        return AjaxResult.success();
    }

    /**
     * 校验SCM已计划未发货数据
     *
     * @param salesOrderPool
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public AjaxResult checkSCMData(SalesOrderPool salesOrderPool) {
        // 校验只能抓取当前月份和下个月的数据，不允许选择年月比已经同步销售订单年月小
        this.checkParamYearMonth(salesOrderPool);
        // 通过接口获取供应链数据
        AjaxResult result = this.syncItfData(salesOrderPool);
        if (!AppUtils.checkAjaxSuccess(result)) {
            return result;
        }
        List<SyncPlanedNotShipResultVo> syncResultList = (List<SyncPlanedNotShipResultVo>) result
                .get(AjaxResult.DATA_TAG);
        if (CollectionUtils.isEmpty(syncResultList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.SalesOrderPool.noScmData"));
        }

        // 加载区域
        LambdaQueryWrapper<DpArea> areaQueryWrapper = new LambdaQueryWrapper<>();
        areaQueryWrapper.eq(DpArea::getIsDelete, ApsConstant.APS_YES_NO_0);
        List<DpArea> dpAreaList = dpAreaEntityMapper.selectList(areaQueryWrapper);
        JsonI18nConvertUtils.conventJsonI18n(dpAreaList, DpArea.class);
        Map<String, String> areaMap = dpAreaList.stream()
                .collect(Collectors.toMap(DpArea::getAreaCode, DpArea::getAreaNameI18n));
        // 校验是否有没有录入优先级的数据
        List<String> notPriorityAreaList = syncResultList.stream()
                .filter(s -> StringUtils.isEmpty(s.getSalPriority()) && s.getEmployeeDept() != null)
                .map(s -> {
                    String areaCode = String.valueOf(s.getEmployeeDept());
                    return areaMap.getOrDefault(areaCode, areaCode);
                }) // 先从备注获取名称
                .distinct().collect(Collectors.toList());

        // 拼接警告信息
        StringBuilder warnMsg = new StringBuilder();
        String splitFlag = notPriorityAreaList.size() > 1 ? "\n" : ""; // 如果有多个区域没有维护优先级，则需要换行提示
        for (String area : notPriorityAreaList) {
            warnMsg.append(String.format(I18nUtil.getMessage("ui.data.alert.SalesOrderPool.notPriorityArea"), area))
                    .append(splitFlag);
        }
        if (warnMsg.length() > 0) {
            warnMsg.setLength(warnMsg.length() - 1);
            warnMsg.append(I18nUtil.getMessage("ui.data.alert.SalesOrderPool.isContinue"));
            return AjaxResult.success(warnMsg.toString(), ApsConstant.APS_YES_NO_1);
        }

        // 校验通过，直接做保存操作
        return this.saveItfData(salesOrderPool, syncResultList);
    }

    /**
     * 抓取SCM已计划未发货数据
     *
     * @param salesOrderPool
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public AjaxResult getSCMData(SalesOrderPool salesOrderPool) {
        // 通过接口获取供应链数据
        AjaxResult result = this.syncItfData(salesOrderPool);
        if (!AppUtils.checkAjaxSuccess(result)) {
            return result;
        }
        List<SyncPlanedNotShipResultVo> syncResultList = (List<SyncPlanedNotShipResultVo>) result
                .get(AjaxResult.DATA_TAG);
        if (CollectionUtils.isEmpty(syncResultList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.SalesOrderPool.noScmData"));
        }
        return this.saveItfData(salesOrderPool, syncResultList);
    }

    /**
     * 校验抓取年月是否是当前月份或下个月，且不允许选择年月比已经同步销售订单年月小
     *
     * @param salesOrderPool 抓取年月
     */
    private void checkParamYearMonth(SalesOrderPool salesOrderPool) {
        // 校验抓取年月是否是当前月份或下个月
        Integer year = salesOrderPool.getYear();
        Integer month = salesOrderPool.getMonth();
        int paramYearMonth = Integer.parseInt(String.format("%d%02d", year, month));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        int nowYear = calendar.get(Calendar.YEAR);
        int nowMonth = calendar.get(Calendar.MONTH) + 1;
        int nowYearMonth = Integer.parseInt(String.format("%d%02d", nowYear, nowMonth));

        calendar.add(Calendar.MONTH, 1);
        int nextYear = calendar.get(Calendar.YEAR);
        int nextMonth = calendar.get(Calendar.MONTH) + 1;
        int nextYearMonth = Integer.parseInt(String.format("%d%02d", nextYear, nextMonth));
        if (paramYearMonth < nowYearMonth || paramYearMonth > nextYearMonth) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.column.salesOrderPool.checkSCMData.nowYearMonth"));
        }

        // 不允许选择年月比已经同步销售订单年月小
        List<SalesOrderPoolRecord> existYearMonthList = salesOrderPoolRecordEntityMapper.selectExistYearMonth(salesOrderPool);
        Optional<Integer> optional = existYearMonthList.stream().map(item -> Integer.valueOf(String.format("%d%02d", item.getYear(), item.getMonth()))).max(Integer::compareTo);
        if (optional.isPresent()) {
            Integer existMaxYearMonth = optional.get();
            if (paramYearMonth < existMaxYearMonth) {
                throw new RuntimeException(I18nUtil.getMessage("ui.data.column.salesOrderPool.checkSCMData.existYearMonth"));
            }
        }
    }

    @Override
    public List<SalesOrderPool> findCurrentSalesOrderPool(String factoryCode) {
        LambdaQueryWrapper<SalesOrderPool> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.isNotBlank(factoryCode)) {
            wrapper.eq(SalesOrderPool::getFactoryCode, factoryCode);
        }
        wrapper.eq(SalesOrderPool::getIsDelete, YesOrNoEnum.NO.getValue());
        return salesOrderPoolEntityMapper.selectList(wrapper);
    }

    /**
     * 保存接口数据
     *
     * @param salesOrderPool 同步数据参数
     * @param syncResultList 同步数据返回结果
     * @return
     */
    public AjaxResult saveItfData(SalesOrderPool salesOrderPool, List<SyncPlanedNotShipResultVo> syncResultList) {
        // 根据年月取出年月现有数据
        Integer year = salesOrderPool.getYear();
        Integer month = salesOrderPool.getMonth();
        String factoryCode = salesOrderPool.getFactoryCode();
        boolean isFirstCatch = !syncResultList.stream()
                .anyMatch(r -> ApsConstant.SCM_PLANED_NOT_SHIP_SYNC_STATUS_LOCK.equals(r.getSyncStatus())); // 数据全部都不是锁定的，为首次抓取

        // 排产参数
        String scmOrderMatralCodePrefix = this.getFactoryParam(factoryCode, ProductTypeEnum.WHOLE_STEEL.getValue(),
                MonthPlanEnums.SCM_ORDER_MATRAL_CODE_PREFIX); // 从参数取出储备标记
        String salesOrderStockFlag = this.getFactoryParam(factoryCode, ProductTypeEnum.WHOLE_STEEL.getValue(),
                MonthPlanEnums.SALESORDER_STOCK_FLAG); // 从参数取出储备标记
        String hightPriorityOrderRateStr = this.getFactoryParam(factoryCode, ProductTypeEnum.WHOLE_STEEL.getValue(),
                MonthPlanEnums.HIGHT_PRIORITY_ORDER_RATE); // 高优先级订单占比
        BigDecimal hightPriorityOrderRate = BigDecimalUtils
                .percentages2Decimals(BigDecimalUtils.valueOf(hightPriorityOrderRateStr)); // 占比参数转换成小数
        String scmOrderMatralQualityState = this.getFactoryParam(factoryCode, ProductTypeEnum.WHOLE_STEEL.getValue(),
                MonthPlanEnums.SCM_ORDER_MATRAL_QUALITY_STATE); // 从参数取出质控标记

        String[] prefixArr = StringUtils.split(scmOrderMatralCodePrefix, ",");
        String[] qualityStateArr = StringUtils.split(scmOrderMatralQualityState, ",");


        // 加载物料明细，关联数据
        LambdaQueryWrapper<MdmMaterialInfo> materialQueryWrapper = new LambdaQueryWrapper<MdmMaterialInfo>();
        materialQueryWrapper.eq(MdmMaterialInfo::getFactoryCode, factoryCode);
        Map<String, MdmMaterialInfo> materialMap = mdmMaterialInfoEntityMapper.selectList(materialQueryWrapper).stream()
                .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, Function.identity(), (m1, m2) -> m1));

        // 把接口数据转换成aps订单池对象
        List<SalesOrderPool> newSalesOrderPoolList = syncResultList.stream().filter(s -> {
                            if (s.getId() == null) {
                                return false; // id为空的异常数据过滤掉
                            }
                            if (prefixArr == null || prefixArr.length == 0) {
                                return true;
                            }
                            return Arrays.stream(prefixArr).anyMatch(p -> StringUtils.startsWith(s.getOriMaterialCode(), p));
                        } // 只要32、33开头的物料
                ).filter(s -> {
                            if (qualityStateArr == null || qualityStateArr.length == 0) {
                                return true;
                            }
                            return Arrays.stream(qualityStateArr).anyMatch(p -> Objects.equals(p, s.getQualityStateCode()));
                        } // 质控状态只要投产的
                ).filter(vo -> materialMap.containsKey(vo.getOriMaterialCode()))
                .map(vo -> {
                    String salPriority = vo.getSalPriority(); // 销售优先级
                    String salCodePo = vo.getSalCodePo(); // po号
                    String oriMaterialCode = vo.getOriMaterialCode(); // 物料号
                    String shipType = null; // 发货模式，需要做字典映射
                    switch (vo.getShipType()) {
                        case ApsConstant.SCM_DELIVERY_MODE_ALL:
                            shipType = ApsConstant.DELIVERY_MODE_ALL;
                            break;
                        default:
                            shipType = ApsConstant.DELIVERY_MODE_SPLIT;
                    }
                    // 从物料信息关联的数据
                    MdmMaterialInfo materialInfo = materialMap.get(oriMaterialCode);
                    String materialDesc = Optional.ofNullable(materialInfo).map(MdmMaterialInfo::getMaterialDesc).orElse(vo.getMaterialDesc());
                    String productType = Optional.ofNullable(materialInfo).map(MdmMaterialInfo::getProductTypeCode).orElse(ProductTypeEnum.WHOLE_STEEL.getValue());

                    SalesOrderPool newVO = new SalesOrderPool();
                    newVO.setFactoryCode(factoryCode);
                    newVO.setYear(year);
                    newVO.setMonth(month);
                    newVO.setArea(String.valueOf(vo.getEmployeeDept()));
                    newVO.setBillDate(vo.getBillDate());
                    newVO.setBrand(materialInfo.getBrand()); // 品牌需要从物料信息表获取
                    newVO.setDeliverGoodsType(shipType);
                    newVO.setIsEudr(vo.getEudrFlag());
                    newVO.setMaterialDesc(materialDesc);
                    newVO.setNatCode(vo.getNatCode());
                    newVO.setOrderPriority(salPriority);
                    newVO.setOrdQty(vo.getPlanedNotShipQty()); // 数量为已计划未发货量
                    newVO.setOriMaterialCode(oriMaterialCode);
                    newVO.setProductType(productType);
                    newVO.setSalCode(vo.getSalCode());
                    newVO.setSalCodePo(salCodePo);
                    newVO.setLocationType(vo.getLocationType());
                    newVO.setSalNCode(vo.getSalNCode());
                    newVO.setWeekYear(vo.getWeekYearRequirement());
                    newVO.setScmDetailId(vo.getId());
//					newVO.setDynamicBalance(dynamicBalance);
//					newVO.setUniformity(uniformity);
                    String scmPriority = ApsConstant.SAL_PRIORITY_MID;
                    if (isFirstCatch) { // 年月首次抓取
                        // 订单类型 !=空，且PO号含有储备字样（20260204同时订单类型不为储备），则供应链优先级 = 中优先级;，否则供应链优先级 = 订单类型
                        if (StringUtils.isNotEmpty(salPriority) && StringUtils.isNotEmpty(salesOrderStockFlag)
                                && StringUtils.isNotEmpty(salCodePo) && !salCodePo.contains(salesOrderStockFlag)
                                && !ApsConstant.SAL_PRIORITY_STOCK.equals(salPriority)) {
                            scmPriority = salPriority;
                        }
                    }
                    newVO.setScmPriority(scmPriority);
                    newVO.setBaseVale(newVO.getId());

                    return newVO;
                }).collect(Collectors.toList());

        if (isFirstCatch) { // 年月首次抓取（20260320，改为锁定前）
            // 根据发货模式，当发货模式 = 整单发货的订单，对其下所有SKU获取供应链优先级最高的值(即供应链优先级的值最小)，
            List<String> salCodePoList = newSalesOrderPoolList.stream()
                    .filter(s -> ApsConstant.DELIVERY_MODE_ALL.equals(s.getDeliverGoodsType()))
                    .map(s -> s.getSalCodePo()).distinct().collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(salCodePoList)) {
                Map<String, String> greatestPriorityMap = newSalesOrderPoolList.stream()
                        .filter(s -> salCodePoList.contains(s.getSalCodePo()))
                        .collect(Collectors.groupingBy(SalesOrderPool::getSalCodePo,
                                Collectors.collectingAndThen(
                                        Collectors.minBy(Comparator.comparing(SalesOrderPool::getScmPriority)), // 取最高优先级（值最小）
                                        s -> s.get().getScmPriority())));
                for (Entry<String, String> entry : greatestPriorityMap.entrySet()) {
                    String salCodePo = entry.getKey();
                    String scmPriority = entry.getValue();
                    if (StringUtils.isEmpty(salCodePo)) {
                        continue;
                    }
                    // 将PO下的非暂缓SKU的供应链优先级 = 找到的最高优先级的值(即供应链优先级值最小的值)；
                    newSalesOrderPoolList.stream().filter(s -> salCodePo.equals(s.getSalCodePo()))
                            .filter(item -> !ApsConstant.SAL_PRIORITY_POSTPONE.equals(item.getOrderPriority()))
                            .forEach(s -> s.setScmPriority(scmPriority));
                }

                // 按供应链优先级汇总高优先级的订单总量【SUM(数量)，且供应链优先级 = 1】，与所有订单总量【SUM(数量)】。
                // 如果高优先级总量/所有总量> 85%，则对高优先级订单按提报日期升序排序
                // 将提报日期晚(最后)的订单的供应链优先级由高优先级修改为中优先级，直到新的高优先级总量/所有总量 <=85%（第一次）为止
                if (hightPriorityOrderRate.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal totalOrdQty = newSalesOrderPoolList.stream().map(SalesOrderPool::getOrdQty)
                            .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add); // 总订单量合计
                    List<SalesOrderPool> hightPriorityList = newSalesOrderPoolList.stream()
                            .filter(s ->
//									ApsConstant.SAL_PRIORITY_HIGHT.equals(s.getOrderPriority())
//									&&
                                    ApsConstant.SAL_PRIORITY_HIGHT.equals(s.getScmPriority()))
                            .sorted(Comparator.comparing(SalesOrderPool::getBillDate, Comparator.reverseOrder()))
                            .collect(Collectors.toList());
                    BigDecimal hightPriorityOrdQty = hightPriorityList.stream().map(SalesOrderPool::getOrdQty)
                            .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add); // 高优先级订单量合计
                    for (SalesOrderPool pool : hightPriorityList) {
                        if (BigDecimalUtils.div(hightPriorityOrdQty, totalOrdQty, 4, true, BigDecimal.ROUND_UP)
                                .compareTo(hightPriorityOrderRate) <= 0) { // 高优先级总量/所有总量<= 85%则结束，否则要把高优先级调成中优先级
                            break;
                        }
                        pool.setScmPriority(ApsConstant.SAL_PRIORITY_MID); // 优先级设置未中
                        hightPriorityOrdQty = BigDecimalUtils.sub(hightPriorityOrdQty, pool.getOrdQty()); // 高优先级订单量合计扣减掉该订单量
                    }
                }
            }
        } else { // 年月再次抓取数据（20260320，改为锁定后）
            LambdaQueryWrapper<SalesOrderPool> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SalesOrderPool::getFactoryCode, factoryCode);
            queryWrapper.eq(SalesOrderPool::getIsDelete, ApsConstant.APS_YES_NO_0);
            List<SalesOrderPool> salesOrderPoolList = salesOrderPoolEntityMapper.selectList(queryWrapper);
            Map<Long, List<SalesOrderPool>> scmDetailMap = salesOrderPoolList.stream()
                    .filter(s -> s.getScmDetailId() != null)
                    .collect(Collectors.groupingBy(SalesOrderPool::getScmDetailId));
            for (SalesOrderPool pool : newSalesOrderPoolList) {
                SalesOrderPool oldPool = CollectionUtils.firstElement(scmDetailMap.get(pool.getScmDetailId()));
                if (oldPool != null) {
                    // 如果(SCM行ID)存在，则订单类型 = 旧订单类型，供应链优先级 = 旧供应链优先级；同时，更新订单其他字段数据；
                    pool.setOrderPriority(oldPool.getOrderPriority());
                    pool.setScmPriority(oldPool.getScmPriority());
                } else {
                    // 如果(SCM行ID)不存在，则订单类型=抓取数据的订单类型；供应链优先级 = 中优先级；
                    pool.setScmPriority(ApsConstant.SAL_PRIORITY_MID);
                }
            }
        }
        // 删除旧数据
        LambdaUpdateWrapper<SalesOrderPool> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.exists("select 1 a"); // 加个条件用来绕过框架的防全表更新限制
        updateWrapper.set(SalesOrderPool::getIsDelete, ApsConstant.APS_YES_NO_1);
        salesOrderPoolEntityMapper.update(null, updateWrapper);
        salesOrderPoolEntityMapper.batchInsert(newSalesOrderPoolList); // 新增一批数据
        this.saveSyncRecord(syncResultList, year, month, factoryCode); // 记录同步操作日志
        try {
            // 同步区域表
            iScmItfService.syncArea();
        } catch (Exception e) { // 同步失败也不影响订单同步
            log.error(e.getMessage(), e);
        }
        try {
            // 触发调用itf接口同步成品库存
            MdmProductStock productStock = new MdmProductStock();
            productStock.setFactoryCode(factoryCode);
            productStock.setStockDate(DateUtils.parseDate(DateUtils.getDate(), DateUtils.YYYY_MM_DD));
            iMesItfService.syncProductStock(productStock);
            // 触发调用itf接口同步出库未扫描订单
            MdmOutbountOrdersNotScan outbountOrdersNotScan = new MdmOutbountOrdersNotScan();
            outbountOrdersNotScan.setFactoryCode(factoryCode);
            iMesItfService.syncOutbountOrdersNotScan(outbountOrdersNotScan);
        } catch (Exception e) { // 同步失败也不影响订单同步
            log.error(e.getMessage(), e);
        }
        return AjaxResult.success();
    }

    /**
     * 保存同步记录
     *
     * @param syncResultList
     * @param year
     * @param month
     * @param factoryCode
     */
    public void saveSyncRecord(List<SyncPlanedNotShipResultVo> syncResultList, Integer year, Integer month,
                               String factoryCode) {
        // 把接口数据转换成aps订单池对象
        List<SalesOrderPoolRecord> salesOrderPoolRecordList = syncResultList.stream().map(vo -> {
            String shipType = null; // 发货模式，需要做字典映射
            switch (vo.getShipType()) {
                case ApsConstant.SCM_DELIVERY_MODE_ALL:
                    shipType = ApsConstant.DELIVERY_MODE_ALL;
                    break;
                default:
                    shipType = ApsConstant.DELIVERY_MODE_SPLIT;
            }
            SalesOrderPoolRecord newVO = new SalesOrderPoolRecord();
            newVO.setFactoryCode(factoryCode);
            newVO.setYear(year);
            newVO.setMonth(month);
            newVO.setArea(String.valueOf(vo.getEmployeeDept()));
            newVO.setBillDate(vo.getBillDate());
            newVO.setBrand(vo.getBrandName());
            newVO.setDeliverGoodsType(shipType);
            newVO.setIsEudr(vo.getEudrFlag());
            newVO.setMaterialDesc(vo.getMaterialDesc());
            newVO.setNatCode(vo.getNatCode());
            newVO.setOrderPriority(vo.getSalPriority());
            newVO.setOrdQty(vo.getPlanedNotShipQty()); // 数量为已计划未发货量
            newVO.setOriMaterialCode(vo.getOriMaterialCode());
            newVO.setProductType(vo.getProductType());
            newVO.setSalCode(vo.getSalCode());
            newVO.setSalCodePo(vo.getSalCodePo());
            newVO.setSalNCode(vo.getSalNCode());
            newVO.setWeekYear(vo.getWeekYearRequirement());
            newVO.setScmDetailId(vo.getId());
            newVO.setBaseVale(newVO.getId());
            return newVO;
        }).collect(Collectors.toList());
        salesOrderPoolRecordEntityMapper.batchInsert(salesOrderPoolRecordList); // 新增操作日志
    }

    /**
     * 获取同步接口数据
     *
     * @param salesOrderPool
     * @return
     */
    private AjaxResult syncItfData(SalesOrderPool salesOrderPool) {
        // 调用接口获取供应链数据
        SyncPlanedNotShipParamVo planedNotShipParamVo = new SyncPlanedNotShipParamVo();
        planedNotShipParamVo.setYear(salesOrderPool.getYear());
        planedNotShipParamVo.setMonth(salesOrderPool.getMonth());
        planedNotShipParamVo.setFactory(salesOrderPool.getFactoryCode());
        AjaxResult result = iScmItfService.syncPlanedNotShipList(planedNotShipParamVo);
        if (!AppUtils.checkAjaxSuccess(result)) {
            log.error(String.valueOf(result.get(AjaxResult.MSG_TAG)));
            return result;
        }
        return AjaxResult.success(AjaxResultUtils.getList(result, SyncPlanedNotShipResultVo.class));
    }

    /**
     * 获取配置信息
     *
     * @param paramCode
     * @return
     */
    private String getFactoryParam(String factoryCode, String productTypeCode, MonthPlanEnums paramCode) {
        FactoryParam factoryParam = new FactoryParam();
        factoryParam.setFactoryCode(factoryCode);
        factoryParam.setParamCode(paramCode.getCode());
        factoryParam.setProductTypeCode(productTypeCode);
        FactoryParam param = iFactoryParamService.getFacParamSingle(factoryParam);
        String paramValue = null;
        if (param != null) {
            paramValue = StringUtils.isNotEmpty(param.getParamValue()) ? param.getParamValue()
                    : param.getDefauleValue();
        }
        return paramValue;
    }

    /**
     * 查询最新两个月的版本锁定情况
     *
     * @param salesOrderPool 查询
     * @return 结果
     */
    @Override
    public AjaxResult getMonthLock(SalesOrderPool salesOrderPool) {
        return iScmItfService.getMonthLock();
    }

    /**
     * 导出销售订单池数据
     *
     * @param queryVO 查询条件
     * @return 导出的字节数组
     * @throws IOException IO异常
     */
    @Override
    public byte[] exportSalesOrderPool(SalesOrderPool queryVO) throws IOException {
        // 获取模板
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("excelModel/salesOrderPool.xlsx");
        if (inputStream == null) {
            throw new IOException("模板文件不存在");
        }

        // 加载字典数据
        // 工厂名称字典
        List<SysDictData> factoryDatas = sysDictDataCacheService.getType("biz_factory_name");
        Map<String, String> factoryMap = factoryDatas != null ? factoryDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // 产品品类字典
        List<SysDictData> productTypeDatas = sysDictDataCacheService.getType("biz_product_type");
        Map<String, String> productTypeMap = productTypeDatas != null ? productTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // 订单优先级字典
        List<SysDictData> orderPriorityDatas = sysDictDataCacheService.getType("biz_order_type");
        Map<String, String> orderPriorityMap = orderPriorityDatas != null ? orderPriorityDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // 内外销字典
        List<SysDictData> locationTypeDatas = sysDictDataCacheService.getType("biz_stor_type");
        Map<String, String> locationTypeMap = locationTypeDatas != null ? locationTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // 品牌字典
        List<SysDictData> brandDatas = sysDictDataCacheService.getType("biz_brand_type");
        Map<String, String> brandMap = brandDatas != null ? brandDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // EUDR字典
        List<SysDictData> eudrDatas = sysDictDataCacheService.getType("biz_yes_no");
        Map<String, String> eudrMap = eudrDatas != null ? eudrDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // 发货模式字典
        List<SysDictData> deliverGoodsTypeDatas = sysDictDataCacheService.getType("biz_deliver_goods_type");
        Map<String, String> deliverGoodsTypeMap = deliverGoodsTypeDatas != null ? deliverGoodsTypeDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // 供应链优先级字典
        List<SysDictData> scmPriorityDatas = sysDictDataCacheService.getType("biz_scm_type");
        Map<String, String> scmPriorityMap = scmPriorityDatas != null ? scmPriorityDatas.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel)) : new HashMap<>();
        // 加载区域
        List<DpArea> dpAreaList = dpAreaEntityMapper.selectList(new LambdaQueryWrapper<>());
        JsonI18nConvertUtils.conventJsonI18n(dpAreaList, DpArea.class); // 翻译国际化栏位
        Map<String, String> areaMap = dpAreaList.stream().collect(Collectors.toMap(DpArea::getAreaCode, DpArea::getAreaNameI18n));
        // 加载国家地区
        List<DpNation> dpNationList = dpNationEntityMapper.selectList(new LambdaQueryWrapper<>());
        JsonI18nConvertUtils.conventJsonI18n(dpNationList, DpNation.class);
        Map<String, String> nationMap = dpNationList.stream()
                .collect(Collectors.toMap(DpNation::getNationCode, DpNation::getNationNameI18n));

        // 表头信息
        Map<String, Object> tableMap = new HashMap<>(16);
        // 列表数据
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();

        // 按当前年月取数
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH) + 1;

        // 设置年月标题
        String titleFormat = I18nUtil.getMessage("ui.data.column.SalesOrderPool.export.title");
        tableMap.put("title", String.format(titleFormat, currentYear, currentMonth));
        // 查询数据
        QueryWrapper<SalesOrderPool> wrapper = new QueryWrapper<>();
        // 构建查询条件
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productType")), "PRODUCT_TYPE", queryVO.getFieldValueByFieldName("productType"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderPriority")), "ORDER_PRIORITY", queryVO.getFieldValueByFieldName("orderPriority"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("area")), "AREA", queryVO.getFieldValueByFieldName("area"));
        wrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("salCode")), "SAL_CODE", queryVO.getFieldValueByFieldName("salCode"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("salNCode")), "SAL_N_CODE", queryVO.getFieldValueByFieldName("salNCode"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("natCode")), "NAT_CODE", queryVO.getFieldValueByFieldName("natCode"));
        wrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        wrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("salCodePo")), "SAL_CODE_PO", queryVO.getFieldValueByFieldName("salCodePo"));
        wrapper.ge(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("billDateStartTime")), "BILL_DATE", queryVO.getBillDateStartTime());
        wrapper.le(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("billDateEndTime")), "BILL_DATE", queryVO.getBillDateEndTime());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("oriMaterialCode")), "ORI_MATERIAL_CODE", queryVO.getFieldValueByFieldName("oriMaterialCode"));
        wrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("ordQty")), "ORD_QTY", queryVO.getFieldValueByFieldName("ordQty"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("weekYear")), "WEEK_YEAR", queryVO.getFieldValueByFieldName("weekYear"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isDynamicBalance")), "IS_DYNAMIC_BALANCE", queryVO.getFieldValueByFieldName("isDynamicBalance"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isUniformity")), "IS_UNIFORMITY", queryVO.getFieldValueByFieldName("isUniformity"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isEudr")), "IS_EUDR", queryVO.getFieldValueByFieldName("isEudr"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deliverGoodsType")), "DELIVER_GOODS_TYPE", queryVO.getFieldValueByFieldName("deliverGoodsType"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scmPriority")), "SCM_PRIORITY", queryVO.getFieldValueByFieldName("scmPriority"));
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scmDetailId")), "SCM_DETAIL_ID", queryVO.getFieldValueByFieldName("scmDetailId"));

        List<SalesOrderPool> dataList = salesOrderPoolEntityMapper.selectList(wrapper);

        // 设置表头
        tableMap.put("factoryCode", I18nUtil.getMessage("ui.data.column.SalesOrderPool.factoryCode"));
        tableMap.put("productType", I18nUtil.getMessage("ui.data.column.SalesOrderPool.productType"));
        tableMap.put("orderPriority", I18nUtil.getMessage("ui.data.column.SalesOrderPool.orderPriority"));
        tableMap.put("area", I18nUtil.getMessage("ui.data.column.SalesOrderPool.area"));
        tableMap.put("salCode", I18nUtil.getMessage("ui.data.column.SalesOrderPool.salCode"));
        tableMap.put("locationType", I18nUtil.getMessage("ui.data.column.SalesOrderPool.locationType"));
        tableMap.put("salNCode", I18nUtil.getMessage("ui.data.column.SalesOrderPool.salNCode"));
        tableMap.put("natCode", I18nUtil.getMessage("ui.data.column.SalesOrderPool.natCode"));
        tableMap.put("brand", I18nUtil.getMessage("ui.data.column.SalesOrderPool.brand"));
        tableMap.put("salCodePo", I18nUtil.getMessage("ui.data.column.SalesOrderPool.salCodePo"));
        tableMap.put("billDate", I18nUtil.getMessage("ui.data.column.SalesOrderPool.billDate"));
        tableMap.put("oriMaterialCode", I18nUtil.getMessage("ui.data.column.SalesOrderPool.oriMaterialCode"));
        tableMap.put("materialDesc", I18nUtil.getMessage("ui.data.column.SalesOrderPool.materialDesc"));
        tableMap.put("ordQty", I18nUtil.getMessage("ui.data.column.SalesOrderPool.ordQty"));
        tableMap.put("weekYear", I18nUtil.getMessage("ui.data.column.SalesOrderPool.weekYear"));
        tableMap.put("isUniformity", I18nUtil.getMessage("ui.data.column.SalesOrderPool.isUniformity"));
        tableMap.put("isDynamicBalance", I18nUtil.getMessage("ui.data.column.SalesOrderPool.isDynamicBalance"));
        tableMap.put("isEudr", I18nUtil.getMessage("ui.data.column.SalesOrderPool.isEudr"));
        tableMap.put("deliverGoodsType", I18nUtil.getMessage("ui.data.column.SalesOrderPool.deliverGoodsType"));
        tableMap.put("scmPriority", I18nUtil.getMessage("ui.data.column.SalesOrderPool.scmPriority"));
        tableMap.put("updateTimeExport", I18nUtil.getMessage("ui.data.column.SalesOrderPool.updateTime"));
        tableMap.put("total", I18nUtil.getMessage("ui.data.column.SalesOrderPool.export.total"));

        if (dataList != null && !dataList.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < dataList.size(); i++) {
                Map<String, Object> listDataMap = new HashMap<>(16);
                SalesOrderPool item = dataList.get(i);

                // 转义字典值
                listDataMap.put("factoryCode", factoryMap.getOrDefault(item.getFactoryCode(), item.getFactoryCode()));
                listDataMap.put("productType", productTypeMap.getOrDefault(item.getProductType(), item.getProductType()));
                listDataMap.put("orderPriority", orderPriorityMap.getOrDefault(item.getOrderPriority(), item.getOrderPriority()));
                listDataMap.put("area", areaMap.getOrDefault(item.getArea(), item.getArea()));
                listDataMap.put("salCode", item.getSalCode() != null ? item.getSalCode() : "");
                listDataMap.put("locationType", locationTypeMap.getOrDefault(item.getLocationType(), item.getLocationType()));
                listDataMap.put("salNCode", nationMap.getOrDefault(item.getSalNCode(), item.getSalNCode()));
                listDataMap.put("natCode", nationMap.getOrDefault(item.getNatCode(), item.getNatCode()));
                listDataMap.put("brand", brandMap.getOrDefault(item.getBrand(), item.getBrand()));
                listDataMap.put("salCodePo", item.getSalCodePo() != null ? item.getSalCodePo() : "");
                listDataMap.put("billDate", item.getBillDate() != null ? DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, item.getBillDate()) : "");
                listDataMap.put("oriMaterialCode", item.getOriMaterialCode() != null ? item.getOriMaterialCode() : "");
                listDataMap.put("materialDesc", item.getMaterialDesc() != null ? item.getMaterialDesc() : "");
                listDataMap.put("ordQty", item.getOrdQty() != null ? item.getOrdQty().doubleValue() : 0);
                listDataMap.put("weekYear", item.getWeekYear() != null ? item.getWeekYear() : "");
                listDataMap.put("isUniformity", item.getIsUniformity() != null ? item.getIsUniformity() : "");
                listDataMap.put("isDynamicBalance", item.getIsDynamicBalance() != null ? item.getIsDynamicBalance() : "");
                listDataMap.put("isEudr", eudrMap.getOrDefault(item.getIsEudr(), item.getIsEudr()));
                listDataMap.put("deliverGoodsType", deliverGoodsTypeMap.getOrDefault(item.getDeliverGoodsType(), item.getDeliverGoodsType()));
                listDataMap.put("scmPriority", scmPriorityMap.getOrDefault(item.getScmPriority(), item.getScmPriority()));
                listDataMap.put("scmDetailId", item.getScmDetailId() != null ? item.getScmDetailId().toString() : "");
                listDataMap.put("updateTimeExport", item.getUpdateTime() != null ? DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, item.getUpdateTime()) : "");
                listDataMap.put("orderStatus", item.getOrderStatus() != null ? item.getOrderStatus() : "");

                list.add(listDataMap);
            }
            excelDataList.add(list);
        }
        log.debug("开始生成excel");
        // 写到文件
        return ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
    }
}
