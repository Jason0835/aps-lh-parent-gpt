package com.zlt.aps.maindata.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.service.ISysConfigService;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.redis.service.RedisService;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.enums.OperationBusinessEnums;
import com.zlt.aps.common.core.utils.AjaxResultUtils;
import com.zlt.aps.itf.scm.service.IScmItfService;
import com.zlt.aps.itf.scm.vo.SyncOutShipDmdOrdResultVo;
import com.zlt.aps.itf.scm.vo.SyncPlanedNotShipParamVo;
import com.zlt.aps.maindata.enums.BizScheduleTypeEnum;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MpHistorySaleRecordEntityMapper;
import com.zlt.aps.maindata.mapper.MpMonthlySaleQtyEntityMapper;
import com.zlt.aps.maindata.service.IMpMonthlySaleQtyService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.sysdef.domain.SysDocType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthlySaleQtyServiceImpl.java
 * 描    述：MpMonthlySaleQtyServiceImpl月均销量业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-11
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class MpMonthlySaleQtyServiceImpl extends AbstractDocService<MpMonthlySaleQty> implements IMpMonthlySaleQtyService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private MpMonthlySaleQtyEntityMapper entityMapper;

    @Autowired
    private MpHistorySaleRecordEntityMapper mpHistorySaleRecordEntityMapper;

    @Autowired
    private MdmMaterialInfoEntityMapper materialInfoEntityMapper;

    @Autowired
    private IScmItfService iScmItfService;

    @Autowired
    private ISysConfigService sysConfigService;

    @Override
    protected String getDocTypeCode() {
        return "MP1209";
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("MP1209");
        return sysDocType;
    }

    @Override
    public String checkUnique(MpMonthlySaleQty docEntityVO) {
        String unique = super.checkUnique(docEntityVO);
        if (UserConstants.NOT_UNIQUE.equals(unique)) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.mpMonthlySaleQty.notUnique"));
        }
        return unique;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一校验字段
        return Collections.emptyList();
    }

    /**
     * 生成月均销量
     *
     * @param mpMonthlySaleQty 参数
     * @return 结果
     */
    @Override
    public AjaxResult genMonthlySaleQty(MpMonthlySaleQty mpMonthlySaleQty) {
        String redisValue = redisService.getCacheObject(OperationBusinessEnums.CREATE_MONTH_AVERAGE_SALE.getCode());
        if (StringUtils.isNotBlank(redisValue)) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.alert.mpMonthlySaleQty.generating"));
        }
        String factoryCode = mpMonthlySaleQty.getFactoryCode();
        Calendar instance = Calendar.getInstance();
        Date nowDate = new Date();
        instance.setTime(nowDate);
        int year = instance.get(Calendar.YEAR);
        String month = String.format("%02d", instance.get(Calendar.MONTH) + 1);
        String maxYearMonth = year + month;
        // 上个月
        instance.add(Calendar.MONTH, -1);
        int lastYear = instance.get(Calendar.YEAR);
        String lastMonth = String.format("%02d", instance.get(Calendar.MONTH) + 1);
        String lastYearMonth = lastYear + lastMonth;
        Date lastMonthDate = instance.getTime();
        // 查询SCM发货明细，根据SKU+区域汇总发货量，写入历史销售记录表
        genMpHistorySaleRecord(factoryCode, nowDate, lastMonthDate, lastYear, lastMonth);

        // 获取当前年月及之前6个月的历史销售记录
        int passThreeMonth = 3;
        int passSixMonth = 6;
        int passTwelveMonth = 12;
        instance.setTime(new Date());
        instance.add(Calendar.MONTH, -passSixMonth);
        int last6Year = instance.get(Calendar.YEAR);
        String last6Month = String.format("%02d", instance.get(Calendar.MONTH) + 1);
        String last6YearMonth = last6Year + last6Month;

        instance.setTime(new Date());
        instance.add(Calendar.MONTH, -passTwelveMonth);
        int last12Year = instance.get(Calendar.YEAR);
        String last12Month = String.format("%02d", instance.get(Calendar.MONTH) + 1);
        String last12YearMonth = last12Year + last12Month;

        // 生成月均销量
        genMonthSaleQty(factoryCode, last12YearMonth, maxYearMonth, last6YearMonth, passThreeMonth, passSixMonth);
        // 生成SKU排产分类
        genSkuClassify(factoryCode, last12YearMonth, maxYearMonth);
        return AjaxResult.success();
    }

    /**
     * 生成月均销量
     *
     * @param nowDate       当前时间
     * @param lastMonthDate 上个月对应时间
     * @param lastYear      上个月对应年份
     * @param lastMonth     上月
     */
    private void genMpHistorySaleRecord(String factoryCode, Date nowDate, Date lastMonthDate, int lastYear, String lastMonth) {
        String nowDateStr = DateUtils.parseDateToStr("yyyy-MM-dd", nowDate);
        String lastDateStr = DateUtils.parseDateToStr("yyyy-MM-dd", lastMonthDate);
        SalesOrderPool param = new SalesOrderPool();
        param.setBillDateStartTime(lastDateStr);
        param.setBillDateEndTime(nowDateStr);
        SyncPlanedNotShipParamVo paramVo = new SyncPlanedNotShipParamVo();
        paramVo.setFactory(factoryCode);
        paramVo.setYear(lastYear);
        paramVo.setMonth(Integer.parseInt(lastMonth));
        try {
            String config = sysConfigService.selectConfigByKey("mp.avgSaleQty.gen.date");
            String[] split = config.split("-");
            lastYear = Integer.parseInt(split[0]);
            paramVo.setMonth(Integer.parseInt(split[1]));
        } catch (NumberFormatException e) {
            log.error("获取配置失败", e);
        }
        AjaxResult ajaxResult = iScmItfService.syncOutShipDmdOrdList(paramVo);
        AjaxResultUtils.getList(ajaxResult, SyncOutShipDmdOrdResultVo.class);
        List<SyncOutShipDmdOrdResultVo> outShipDmdOrdResultVos = JSON.parseArray(JSON.toJSONString(ajaxResult.get(AjaxResult.DATA_TAG)), SyncOutShipDmdOrdResultVo.class);

        List<MpHistorySaleRecord> saveList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(outShipDmdOrdResultVos)) {
            for (SyncOutShipDmdOrdResultVo outShipDmdOrdResultVo : outShipDmdOrdResultVos) {
                MpHistorySaleRecord mpHistorySaleRecord = new MpHistorySaleRecord();
                mpHistorySaleRecord.setBaseVale(null);
                mpHistorySaleRecord.setFactoryCode(outShipDmdOrdResultVo.getFactory());
                mpHistorySaleRecord.setYear(lastYear);
                mpHistorySaleRecord.setMonth(Integer.parseInt(lastMonth));
                mpHistorySaleRecord.setYearMonth(Integer.parseInt(lastYear + lastMonth));
                mpHistorySaleRecord.setAreaCode(outShipDmdOrdResultVo.getEmployeeDept().toString());
                mpHistorySaleRecord.setMaterialCode(outShipDmdOrdResultVo.getOriMaterialCode());
                mpHistorySaleRecord.setSaleQty(outShipDmdOrdResultVo.getDnNum().intValue());
                mpHistorySaleRecord.setGenrateDate(nowDate);
                saveList.add(mpHistorySaleRecord);
            }
            baseDao.saveBatch(saveList);
        }
    }

    @Override
    public List<MpMonthlySaleQty> findCurrentMonthlySaleQty() {
        LambdaQueryWrapper<MpMonthlySaleQty> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MpMonthlySaleQty::getIsDelete, YesOrNoEnum.NO.getValue());
        return entityMapper.selectList(wrapper);
    }

    @Override
    public MpMonthlySaleQty getMpMonthlySaleQtyByMaterialCode(String materialCode) {
        LambdaQueryWrapper<MpMonthlySaleQty> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MpMonthlySaleQty::getMaterialCode, materialCode);
        wrapper.eq(MpMonthlySaleQty::getIsDelete, YesOrNoEnum.NO.getValue());
        List<MpMonthlySaleQty> monthlySaleQties = entityMapper.selectList(wrapper);
        return CollectionUtils.isEmpty(monthlySaleQties) ? null : monthlySaleQties.get(0);
    }

    @Override
    public Map<String, Long> findMonthlySaleQtyGroupByMaterialCode() {
        List<MpMonthlySaleQty> list = this.findCurrentMonthlySaleQty();
        if(CollectionUtils.isEmpty(list)){
            return Collections.emptyMap();
        }
        return list.stream()
            .filter(Objects::nonNull)
            .filter(monthlySaleQty -> StringUtils.isNotBlank(monthlySaleQty.getMaterialCode()) && monthlySaleQty.getAverageSaleQty() != null)
            .collect(Collectors.groupingBy(MpMonthlySaleQty::getMaterialCode, Collectors.summingLong(MpMonthlySaleQty::getAverageSaleQty)));
    }

    /**
     * 生成月均销量
     *
     * @param factoryCode     工厂
     * @param last12YearMonth 减12月年月
     * @param maxYearMonth    当前年月
     * @param last6YearMonth  减6月年月
     * @param passThreeMonth  减3月
     * @param passSixMonth    减6月
     */
    private void genMonthSaleQty(String factoryCode, String last12YearMonth, String maxYearMonth, String last6YearMonth, int passThreeMonth, int passSixMonth) {
        List<MpHistorySaleRecord> rollMonthSaleQtyList = mpHistorySaleRecordEntityMapper.selectRollMonthSaleQty(factoryCode, last12YearMonth, maxYearMonth);
        Map<String, Integer> rollMonthSaleQtyMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(rollMonthSaleQtyList)) {
            rollMonthSaleQtyMap = rollMonthSaleQtyList.stream().collect(Collectors.toMap(MpHistorySaleRecord::getMaterialCode, MpHistorySaleRecord::getSaleQty));
        }

        List<MpMonthlySaleQty> monthlySaleQtyList = new ArrayList<>();
        LambdaQueryWrapper<MpHistorySaleRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpHistorySaleRecord::getFactoryCode, factoryCode)
                .ge(MpHistorySaleRecord::getYearMonth, last6YearMonth)
                .le(MpHistorySaleRecord::getYearMonth, maxYearMonth);
        List<MpHistorySaleRecord> last6MonthHistorySaleList = mpHistorySaleRecordEntityMapper.selectList(wrapper);

        List<MdmMaterialInfo> materialInfoList = new ArrayList<>();
        List<String> materialCodeList = last6MonthHistorySaleList.stream().map(MpHistorySaleRecord::getMaterialCode).collect(Collectors.toList());
        List<List<String>> materialCodeSplitList = ScmListUtils.getSplitList(materialCodeList, 1000);
        for (List<String> codeList : materialCodeSplitList) {
            LambdaQueryWrapper<MdmMaterialInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(MdmMaterialInfo::getMaterialCode, codeList);
            List<MdmMaterialInfo> selectList = materialInfoEntityMapper.selectList(queryWrapper);
            materialInfoList.addAll(selectList);
        }

        Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(materialInfoList)) {
            materialInfoMap = materialInfoList.stream().collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, Function.identity(), (v1, v2) -> v1));
        }

        Map<String, List<MpHistorySaleRecord>> groupMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(last6MonthHistorySaleList)) {
            groupMap = last6MonthHistorySaleList.stream().collect(Collectors.groupingBy(MpHistorySaleRecord::getMaterialCode));
            Set<Map.Entry<String, List<MpHistorySaleRecord>>> entrySet = groupMap.entrySet();
            for (Map.Entry<String, List<MpHistorySaleRecord>> entry : entrySet) {
                String materialCode = entry.getKey();
                List<MpHistorySaleRecord> value = entry.getValue();

                MpMonthlySaleQty monthlySaleQty = new MpMonthlySaleQty();
                monthlySaleQty.setBaseVale(null);
                monthlySaleQty.setFactoryCode(factoryCode);
                monthlySaleQty.setMaterialCode(materialCode);

                // 按SKU分组，销量降序，适销区域用逗号分隔
                String area = value.stream().sorted(Comparator.comparing(MpHistorySaleRecord::getSaleQty).reversed())
                        .map(MpHistorySaleRecord::getAreaCodeName).distinct().filter(StringUtils::isNotBlank).collect(Collectors.joining(","));
                monthlySaleQty.setSaleArea(area);

                // 月均销量=销量汇总/6，近3个月销量=取月份最大三个月汇总/3，向上取整
                List<MpHistorySaleRecord> sortedList = value.stream().sorted(Comparator.comparing(MpHistorySaleRecord::getYear).reversed()
                        .thenComparing(MpHistorySaleRecord::getMonth).reversed()).collect(Collectors.toList());

                Integer totalSaleQty = 0;
                for (int i = 0; i < sortedList.size(); i++) {
                    MpHistorySaleRecord historySaleRecord = sortedList.get(i);
                    Integer saleQty = historySaleRecord.getSaleQty();
                    totalSaleQty += saleQty;
                    if (i == passThreeMonth - 1) {
                        // 近3个月
                        BigDecimal result = BigDecimal.valueOf(totalSaleQty).divide(BigDecimal.valueOf(passThreeMonth), 0, RoundingMode.UP);
                        monthlySaleQty.setPassThreeMonthSaleQty(result.longValue());
                    }
                }
                // 月均销量
                BigDecimal result = BigDecimal.valueOf(totalSaleQty).divide(BigDecimal.valueOf(passSixMonth), 0, RoundingMode.UP);
                monthlySaleQty.setAverageSaleQty(result.longValue());

                // 滚动月销量
                if (rollMonthSaleQtyMap.containsKey(materialCode)) {
                    Integer saleQty = rollMonthSaleQtyMap.get(materialCode);
                    monthlySaleQty.setRollTwelveMonthSaleQty(saleQty.longValue());
                }

                // 根据物料信息回写物料信息
                if (materialInfoMap.containsKey(materialCode)) {
                    MdmMaterialInfo materialInfo = materialInfoMap.get(materialCode);
                    monthlySaleQty.setProductTypeCode(materialInfo.getProductTypeCode());
//                    monthlySaleQty.setLocationType(materialInfo.getLocationType());
                    monthlySaleQty.setBrand(materialInfo.getBrand());
                    monthlySaleQty.setMaterialDesc(materialInfo.getMaterialDesc());
                }

                monthlySaleQtyList.add(monthlySaleQty);
            }
        }

        LambdaUpdateWrapper<MpMonthlySaleQty> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MpMonthlySaleQty::getFactoryCode, factoryCode)
                .set(BaseEntity::getIsDelete, ApsConstant.DEL_FLAG_DEL);
        entityMapper.update(null, updateWrapper);
        if (CollectionUtils.isNotEmpty(monthlySaleQtyList)) {
            baseDao.saveBatch(monthlySaleQtyList);
        }
    }

    /**
     * 生成sku排产分类
     *
     * @param factoryCode     分厂
     * @param last12YearMonth 减12月年月
     * @param maxYearMonth    当前年月
     */
    private void genSkuClassify(String factoryCode, String last12YearMonth, String maxYearMonth) {
        LambdaQueryWrapper<MpHistorySaleRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MpHistorySaleRecord::getFactoryCode, factoryCode)
                .ge(MpHistorySaleRecord::getYearMonth, last12YearMonth)
                .le(MpHistorySaleRecord::getYearMonth, maxYearMonth);
        List<MpHistorySaleRecord> last12MonthHistorySaleList = mpHistorySaleRecordEntityMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(last12MonthHistorySaleList)) {
            List<MdmSkuScheduleCategory> skuScheduleCategoryList = new ArrayList<>();
            // 物料分组
            Map<String, List<MpHistorySaleRecord>> groupMap = last12MonthHistorySaleList.stream().collect(Collectors.groupingBy(MpHistorySaleRecord::getMaterialCode));
            Set<Map.Entry<String, List<MpHistorySaleRecord>>> entrySet = groupMap.entrySet();
            for (Map.Entry<String, List<MpHistorySaleRecord>> entry : entrySet) {
                String materialCode = entry.getKey();
                List<MpHistorySaleRecord> value = entry.getValue();

                MdmSkuScheduleCategory skuScheduleCategory = new MdmSkuScheduleCategory();
                skuScheduleCategory.setFactoryCode(factoryCode);
                skuScheduleCategory.setMaterialCode(materialCode);
                skuScheduleCategory.setGenrateDate(new Date());
                // 12月平均销量
                long sumSaleQty = value.stream().mapToLong(MpHistorySaleRecord::getSaleQty).sum();
                BigDecimal averageSaleQty = BigDecimal.valueOf(sumSaleQty).divide(BigDecimal.valueOf(12), 0, RoundingMode.UP);

                // 年月汇总，把不同区域的销量汇总
                Map<String, MpHistorySaleRecord> monthSaleQtyMap = value.stream().collect(Collectors
                        .toMap(item -> GenerageMapKeyUtils.createMapKey(item.getYear(), item.getMonth()), Function.identity(),
                                (oldValue, newValue) -> {
                                    oldValue.setSaleQty(oldValue.getSaleQty() + newValue.getSaleQty());
                                    return oldValue;
                                }));
                int moreThanZeroMonthCount = 0;
                int moreThanFiftyMonthCount = 0;
                Set<Map.Entry<String, MpHistorySaleRecord>> monthSaleQtyEntry = monthSaleQtyMap.entrySet();
                for (Map.Entry<String, MpHistorySaleRecord> recordEntry : monthSaleQtyEntry) {
                    MpHistorySaleRecord monthSaleRecord = recordEntry.getValue();
                    // 月销量>0
                    if (monthSaleRecord.getSaleQty() > 0) {
                        moreThanZeroMonthCount++;
                    }
                    // 月销量>50
                    if (monthSaleRecord.getSaleQty() > 50) {
                        moreThanFiftyMonthCount++;
                    }
                }
                String scheduleType = "";
                if (averageSaleQty.compareTo(BigDecimal.ZERO) > 0 && averageSaleQty.compareTo(BigDecimal.valueOf(150)) < 0) {
                    if (moreThanZeroMonthCount < 11) {
                        // 按单排产产品
                        scheduleType = BizScheduleTypeEnum.ORDINARY_ORDER_PRODUCT.getCode();
                    } else {
                        // 常规周期产品
                        scheduleType = BizScheduleTypeEnum.ORDINARY_CYCLE_PRODUCT.getCode();
                    }
                } else if (averageSaleQty.compareTo(BigDecimal.valueOf(150)) >= 0) {
                    if (moreThanZeroMonthCount < 8) {
                        // 波动产品
                        scheduleType = BizScheduleTypeEnum.WAVE_PRODUCT.getCode();
                    }
                    if (averageSaleQty.compareTo(BigDecimal.valueOf(500)) < 0) {
                        if (moreThanFiftyMonthCount >= 8) {
                            // 常规产品
                            scheduleType = BizScheduleTypeEnum.ORDINARY_PRODUCT.getCode();
                        }
                    } else {
                        if (moreThanFiftyMonthCount >= 8) {
                            // 主销产品
                            scheduleType = BizScheduleTypeEnum.MAIN_SALE_PRODUCT.getCode();
                        }
                    }
                }
                skuScheduleCategory.setScheduleType(scheduleType);
                skuScheduleCategoryList.add(skuScheduleCategory);
            }
            if (CollectionUtils.isNotEmpty(skuScheduleCategoryList)) {
                baseDao.saveBatch(skuScheduleCategoryList);
            }
        }
    }
}
