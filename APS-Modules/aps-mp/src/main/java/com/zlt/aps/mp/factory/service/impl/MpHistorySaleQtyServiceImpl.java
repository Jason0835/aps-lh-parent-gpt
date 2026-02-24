package com.zlt.aps.mp.factory.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.utils.AppUtils;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.utils.ImportExcelValidatedUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.maindata.mapper.LocationChannelConfigurationMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.utils.LambdaWrapperBuilder;
import com.zlt.aps.maindata.utils.RemoteImportExcelUtils;
import com.zlt.aps.monthplan.api.domain.entity.LocationChannelConfiguration;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MpDayHistorySaleQty;
import com.zlt.aps.monthplan.api.domain.entity.MpHistorySaleQty;
import com.zlt.aps.monthplan.api.domain.itf.InDataListVo;
import com.zlt.aps.monthplan.api.domain.itf.InSaleOrderDto;
import com.zlt.aps.monthplan.api.domain.vo.CalcStockingResultVo;
import com.zlt.aps.monthplan.api.domain.vo.MpHistorySaleQtyExcel4MonthVo;
import com.zlt.aps.monthplan.api.domain.vo.MpHistorySaleQtyExcelVo;
import com.zlt.aps.monthplan.api.domain.vo.QueryCalcStockingParamVo;
import com.zlt.aps.monthplan.api.service.IRemoteImportErrorLogService;
import com.zlt.aps.monthplan.api.service.IRemoteImportLogService;
import com.zlt.aps.mp.factory.mapper.MpDayHistorySaleQtyEntityMapper;
import com.zlt.aps.mp.factory.mapper.MpHistorySaleQtyMapper;
import com.zlt.aps.mp.factory.service.IMpHistorySaleQtyService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpHistorySaleQtyServiceImpl.java
 * 描    述：MpHistorySaleQtyServiceImpl历史销售记录业务层处理
 *
 * @author hsc
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：hsc
 * 修改内容：...
 * @date 2025-02-13
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class MpHistorySaleQtyServiceImpl extends ServiceImpl<MpHistorySaleQtyMapper, MpHistorySaleQty> implements IMpHistorySaleQtyService {

    private final BaseDao baseDao;

    private final IRemoteImportLogService iRemoteImportLogService;

    private final IRemoteImportErrorLogService iRemoteImportErrorLogService;

    private final MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;

    private static final int BATCH_SIZE = 100;

    @Override
    public List<MpHistorySaleQty> selectMpHistorySaleQtyList(MpHistorySaleQty mpHistorySaleQty) {
        List<MpHistorySaleQty> list = getBaseMapper().selectMpHistorySaleQtyList(mpHistorySaleQty);
        // 回显创建人
        AppUtils.formatData(list, new String[]{
                "createByName->getcolvaluewithcondition(sys_user, nick_name, user_name, createBy, del_flag='0')",
        });
        return list;
    }

    @Override
    public List<CalcStockingResultVo> selectCalcStocking(QueryCalcStockingParamVo queryCalcStockingParamVo, Integer lastMonth) {
        // 获取当前日期
        LocalDate currentDate = LocalDate.now();

        // 计算包含当前月的前x月的最起始的年月
        LocalDate startYearMonthDate = currentDate.minusMonths(queryCalcStockingParamVo.getMonthRange() - 1).with(TemporalAdjusters.firstDayOfMonth());
        //20250521 ZLT 会出现可能需要跨月提前值 ，因为近一个月月数据没有或是没有意义
        if (null != lastMonth && lastMonth > BigDecimal.ZERO.intValue()) {
            currentDate = currentDate.minusMonths(lastMonth).with(TemporalAdjusters.firstDayOfMonth());
            startYearMonthDate = startYearMonthDate.minusMonths(lastMonth).with(TemporalAdjusters.firstDayOfMonth());
        }
        // 定义日期格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");

        // 格式化日期为"YYYYMM"
        String startYearMonthStr = startYearMonthDate.format(formatter);
        String endYearMonthStr = currentDate.format(formatter);
        log.info(String.format("备货取历史销售记录月份:%s-%s", startYearMonthStr, endYearMonthStr));
        List<CalcStockingResultVo> resultVoList = getBaseMapper().selectCalcStocking(startYearMonthStr, endYearMonthStr, queryCalcStockingParamVo.getTireType(), queryCalcStockingParamVo.getMonthRange());
        // 年月默认为当前时间的下一个月
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(DateUtils.getNowDate());
        calendar.add(Calendar.MONTH, 1);
        int nextYear = calendar.get(Calendar.YEAR);
        int nextMonth = calendar.get(Calendar.MONTH) + 1;
        resultVoList.forEach(v -> {
            v.setYear(nextYear);
            v.setMonth(nextMonth);
        });
        return resultVoList;
    }

    @Override
    public AjaxResult importData(List<MpHistorySaleQtyExcelVo> list, boolean updateSupport, Long importLogId) {
        // 初始化
        int successNum = 0;
        AtomicInteger failureNum = new AtomicInteger();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        // 公共校验（非空校验、长度校验等）
        List<MpHistorySaleQtyExcelVo> importList = list.stream()
                .map(vo -> {
                    int errorNum = list.indexOf(vo) + 2;
                    List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, vo);
                    if (CollectionUtils.isNotEmpty(validated)) {
                        failureNum.getAndIncrement();
                        importErrorLogs.addAll(validated);
                        return null;
                    } else {
                        return vo;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                // 处理导入数据
                processImportList(importList, importErrorLogs, successNum);
            } else {
//                //唯一则新增
//                for (int i = 0; i < list.size(); i++) {
//                    MpHistorySaleQtyExcelVo mpHistorySaleQtyExcelVo = list.get(i);
//                    // 错误记录跳过
//                    if (mpHistorySaleQtyExcelVo.getId() != null && mpHistorySaleQtyExcelVo.getId().equals(-999L)) {
//                        continue;
//                    }
//                    String unique = this.checkMixMasterRubberStockUnique(mixMasterRubberStock);
//                    if (UserConstants.UNIQUE.equals(unique)) {
//                        successNum++;
//                        this.insertMixMasterRubberStock(mixMasterRubberStock);
//                    } else {
//                        failureNum++;
//                        addImportErrorLog(importLogId, i + 2,
//                                I18nUtil.getMessage("此处需手动填写唯一校验失败国际化信息"), importErrorLogs);
//                    }
//                }
            }
        } catch (Exception e) {
            handleException(e, list.size(), importErrorLogs, importLogId, successNum, failureNum.get());
        }
        return buildAjaxResult(successNum, failureNum.get(), importErrorLogs);
    }

    @Async
    @Override
    public void importDataAsync(List<MpHistorySaleQtyExcelVo> list, boolean updateSupport, long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes) {
        try {
            RequestContextHolder.setRequestAttributes(attributes, true);

            AjaxResult result = this.importData(list, updateSupport, importLogId);
            Date endTime = DateUtils.getNowDate();
            importLog.setRowCount(list.size());
            importLog.setBeginTime(beginTime);
            importLog.setEndTime(endTime);
            importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
            RemoteImportExcelUtils.updateImportLogAndFormatMsg(importLog, result, iRemoteImportLogService);
            RemoteImportExcelUtils.saveImportErrorLogs(result, iRemoteImportErrorLogService);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    /**
     * 处理导入数据
     *
     * @param importList      导入数据
     * @param importErrorLogs 导入错误日志数据
     * @param successNum      成功数
     */
    private void processImportList(List<MpHistorySaleQtyExcelVo> importList, List<ImportErrorLog> importErrorLogs, int successNum) {
        processBatch(importList);
    }
    @Autowired
    private MdmMaterialInfoEntityMapper productInfoEntityMapper;

    /**
     * 创建历史销售记录参数
     *
     * @param vo
     * @return
     */
    private MpHistorySaleQty createMpHistorySaleQtyParams(MpHistorySaleQtyExcelVo vo) {
        MpHistorySaleQty mpHistorySaleQty = new MpHistorySaleQty();
        mpHistorySaleQty.setYear(vo.getYear());
        mpHistorySaleQty.setProductCode(vo.getSapCode());
        return mpHistorySaleQty;
    }

    /**
     * 新增所有月份的历史销售记录
     *
     * @param vo
     * @param insertList
     */
    private void addRecordsForAllMonths(MpHistorySaleQtyExcelVo vo, List<MpHistorySaleQty> insertList) {
        for (int month = 1; month <= 12; month++) {
            for (int locationType = 1; locationType <= 3; locationType++) {
                // 创建历史销售记录实例
                MpHistorySaleQty record = createMpHistorySaleQty(vo, month, locationType);
                // 根据月份、库位类型映射赋值订单数、销售数
                setSalesDataUsingMap(vo, record, month, locationType);
                insertList.add(record);
            }
        }
    }

    /**
     * 更新已存在的历史销售记录
     *
     * @param vo
     * @param existingRecords
     * @param updateList
     */
    private void updateExistingRecords(MpHistorySaleQtyExcelVo vo, List<MpHistorySaleQty> existingRecords, List<MpHistorySaleQty> updateList) {
        for (MpHistorySaleQty record : existingRecords) {
            // 根据月份、库位类型映射赋值订单数、销售数
            setSalesDataUsingMap(vo, record, record.getMonth(), Integer.valueOf(record.getLocationType()));
            updateList.add(record);
        }
    }

    /**
     * 创建历史销售记录实例
     *
     * @param vo
     * @param month
     * @param locationType
     * @return
     */
    private MpHistorySaleQty createMpHistorySaleQty(MpHistorySaleQtyExcelVo vo, int month, int locationType) {
        MpHistorySaleQty mpHistorySaleQty = new MpHistorySaleQty();
        mpHistorySaleQty.setYear(vo.getYear());
        mpHistorySaleQty.setMonth(month);
        mpHistorySaleQty.setFactoryCode("116");
        mpHistorySaleQty.setProductCode(vo.getSapCode());
        mpHistorySaleQty.setProductDesc(vo.getSpecDesc());
        mpHistorySaleQty.setLocationType(String.valueOf(locationType));
        mpHistorySaleQty.setRemark(vo.getRemark());
        mpHistorySaleQty.setCreateBy(vo.getCreateBy());
        mpHistorySaleQty.setCreateTime(vo.getCreateTime());
        return mpHistorySaleQty;
    }


    /**
     * 根据月份、库位类型映射赋值订单数、销售数
     *
     * @param vo
     * @param mpHistorySaleQty
     * @param month
     * @param locationType
     */
    private void setSalesDataUsingMap(MpHistorySaleQtyExcelVo vo, MpHistorySaleQty mpHistorySaleQty, int month, int locationType) {
        String suffix = ApsConstant.MONTH_SUFFIX_MAP.get(month);
        String fieldPrefix = ApsConstant.LOCATION_TYPE_FIELD_MAP.get(locationType);
        if (fieldPrefix != null && suffix != null) {
            String orderQtyFieldName = fieldPrefix + "SalesOrderCount" + suffix;
            String saleQtyFieldName = fieldPrefix + "SalesCount" + suffix;

            // 假设你有一个方法来获取字段值
            Long orderQty = (Long) getField(vo, orderQtyFieldName);
            Long saleQty = (Long) getField(vo, saleQtyFieldName);

            if (orderQty != null) {
                mpHistorySaleQty.setOrderQty(orderQty.longValue());
            } else {
                mpHistorySaleQty.setOrderQty(0L);
            }
            if (saleQty != null) {
                mpHistorySaleQty.setSaleQty(saleQty.longValue());
            } else {
                mpHistorySaleQty.setSaleQty(0L);
            }
        }
    }

    // 辅助方法来获取字段值
    private Object getField(Object object, String fieldName) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Handle exception, e.g., log it or throw a custom exception
            return null;
        }
    }

    private void handleException(Exception e, int listSize, List<ImportErrorLog> importErrorLogs, Long importLogId, int successNum, int failureNum) {
        // Log exception details
        log.error("Import data failed", e);
        successNum = 0;
        failureNum = listSize;
        importErrorLogs.clear();
        addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
    }

    private AjaxResult buildAjaxResult(int successNum, int failureNum, List<ImportErrorLog> importErrorLogs) {
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 将接口的列表数据转成内销历史订单数据
     *
     * @param inDataListVoList                接口返回的数据
     * @param productInfoMap                  物料信息
     * @param locationChannelConfigurationMap 库位类别渠道数据
     * @return 内销历史订单数据
     */
    private static List<MpDayHistorySaleQty> transFormSyncListToHisOrderList(List<InDataListVo> inDataListVoList, Map<String, MdmMaterialInfo> productInfoMap, Map<String, LocationChannelConfiguration> locationChannelConfigurationMap) {
        List<MpDayHistorySaleQty> saveList = new ArrayList<>();
        for (InDataListVo inDataListVo : inDataListVoList) {
            MpDayHistorySaleQty mpDayHistorySaleQty = new MpDayHistorySaleQty();
            mpDayHistorySaleQty.setLocationType("下架");

            mpDayHistorySaleQty.setOrderDate(DateUtils.parseDate(inDataListVo.getDate()));
            mpDayHistorySaleQty.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
            mpDayHistorySaleQty.setProductCode(inDataListVo.getGoodsNum());

            String goodsNum = inDataListVo.getGoodsNum();
            mpDayHistorySaleQty.setProductCode(goodsNum);

            if (productInfoMap.containsKey(goodsNum)) {
                MdmMaterialInfo productInfo = productInfoMap.get(goodsNum);
                mpDayHistorySaleQty.setBrand(productInfo.getBrand());
                mpDayHistorySaleQty.setProductDesc(productInfo.getMaterialDesc());
                if (ApsConstant.APS_STRING_2.equals(productInfo.getCommonType())) {
                    mpDayHistorySaleQty.setLocationType(ApsConstant.APS_STRING_2);
                }
            }

            String mapKey = String.join("|", mpDayHistorySaleQty.getFactoryCode(), inDataListVo.getClientExtendName(), mpDayHistorySaleQty.getBrand());
            if (locationChannelConfigurationMap.containsKey(mapKey)) {
                LocationChannelConfiguration locationChannelConfiguration = locationChannelConfigurationMap.get(mapKey);
                mpDayHistorySaleQty.setLocationType(locationChannelConfiguration.getLocationType().toString());
            }

            mpDayHistorySaleQty.setSaleQty(inDataListVo.getSellNum());
            mpDayHistorySaleQty.setOrderQty(inDataListVo.getOrderNum());
            mpDayHistorySaleQty.setRemark(inDataListVo.getRemark());

            mpDayHistorySaleQty.setBaseVale(null);
            saveList.add(mpDayHistorySaleQty);
        }
        return saveList;
    }

    @Autowired
    private LocationChannelConfigurationMapper locationChannelConfigurationMapper;

    @Autowired
    private MpDayHistorySaleQtyEntityMapper mpDayHistorySaleQtyEntityMapper;

    private void processBatch(List<MpHistorySaleQtyExcelVo> batchList) {
        List<MpHistorySaleQty> insertList = new ArrayList<>();

        for (MpHistorySaleQtyExcelVo vo : batchList) {
            addRecordsForAllMonths(vo, insertList);
        }

        // 先删除历史年月、分厂的数据
        LambdaQueryWrapper<MpHistorySaleQty> wrapper = LambdaWrapperBuilder.buildWrapperByFunction(insertList, MpHistorySaleQty::getYear,
                MpHistorySaleQty::getMonth, MpHistorySaleQty::getFactoryCode);
        getBaseMapper().delete(wrapper);

        // 执行批量插入
        if (CollectionUtils.isNotEmpty(insertList)) {
            // 分厂+物料号 关联 物料信息表
            Map<String, List<MpHistorySaleQty>> groupMap = insertList.stream()
                    .filter(v -> StringUtils.isNotBlank(v.getFactoryCode()) && StringUtils.isNotBlank(v.getProductCode()))
                    .collect(Collectors.groupingBy(MpHistorySaleQty::getFactoryCode));
            groupMap.forEach((factoryCode, itemList) -> {
                List<String> productCodeList = itemList.stream().map(MpHistorySaleQty::getProductCode).distinct().collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(productCodeList)) {
                    LambdaQueryWrapper<MdmMaterialInfo> itemWrapper = Wrappers.lambdaQuery();
                    itemWrapper.eq(MdmMaterialInfo::getFactoryCode, factoryCode);
                    itemWrapper.in(MdmMaterialInfo::getMaterialCode, productCodeList);
                    List<MdmMaterialInfo> productInfoList = mdmMaterialInfoEntityMapper.selectList(itemWrapper);
                    Map<String, String> productInfoMap = productInfoList.stream()
                            .filter(v -> StringUtils.isNotBlank(v.getMaterialDesc()))
                            .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, MdmMaterialInfo::getMaterialDesc, (v1, v2) -> v1));
                    for (MpHistorySaleQty item : itemList) {
                        if (productInfoMap.containsKey(item.getProductCode())) {
                            item.setProductDesc(productInfoMap.get(item.getProductCode()));
                        }
                    }
                }
            });

            for (List<MpHistorySaleQty> itemList : ListUtils.partition(insertList, BATCH_SIZE)) {
                getBaseMapper().batchInsertHistorySaleQty(itemList);
            }
        }
    }

    /**
     * 处理内销销售订单同步结果数据
     *
     * @param inSaleOrderDto   查询参数
     * @param inDataListVoList 内销历史销售订单同步结果数据
     */
    @Override
    public void handleInHisSaleOrderSyncResultData(InSaleOrderDto inSaleOrderDto, List<InDataListVo> inDataListVoList) {
        if (CollectionUtils.isEmpty(inDataListVoList)) {
            return;
        }
        long start = System.currentTimeMillis();
        log.info("开始处理内销订单返回数据");
        // 查询物料信息
        List<String> productCodeList = inDataListVoList.stream().map(InDataListVo::getGoodsNum).distinct().collect(Collectors.toList());
        LambdaQueryWrapper<MdmMaterialInfo> productQueryWrapper = new LambdaQueryWrapper<>();
        productQueryWrapper.in(MdmMaterialInfo::getMaterialCode, productCodeList);
        List<MdmMaterialInfo> mdmMaterialInfoList = productInfoEntityMapper.selectList(productQueryWrapper);
        Map<String, MdmMaterialInfo> productInfoMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(mdmMaterialInfoList)) {
            productInfoMap = mdmMaterialInfoList.stream().collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, Function.identity(), (v1, v2) -> v1));
        }

        // 查询库位类别渠道数据，分厂编号、市场类别、品牌分组
        LambdaQueryWrapper<LocationChannelConfiguration> locationChannelQueryWrapper = new LambdaQueryWrapper<>();
        List<LocationChannelConfiguration> locationChannelConfigurationList = locationChannelConfigurationMapper.selectList(locationChannelQueryWrapper);
        Map<String, LocationChannelConfiguration> locationChannelConfigurationMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(locationChannelConfigurationList)) {
            locationChannelConfigurationMap = locationChannelConfigurationList.stream().collect(Collectors.toMap(item -> String.join("|", item.getFactoryCode(), item.getMarketCategory(), item.getBrand()), Function.identity(), (v1, v2) -> v1));
        }

        List<MpDayHistorySaleQty> saveList = transFormSyncListToHisOrderList(inDataListVoList, productInfoMap, locationChannelConfigurationMap);

        // 先查询订单，如果存在，赋值ID更新数据，否则新增
        if (CollectionUtils.isNotEmpty(saveList)) {
            LambdaQueryWrapper<MpDayHistorySaleQty> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.between(MpDayHistorySaleQty::getOrderDate, inSaleOrderDto.getDates1(), inSaleOrderDto.getDates2());
            List<MpDayHistorySaleQty> monthPlanSaleOrderList = mpDayHistorySaleQtyEntityMapper.selectList(queryWrapper);
            Map<String, MpDayHistorySaleQty> monthPlanSaleOrderMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(monthPlanSaleOrderList)) {
                monthPlanSaleOrderMap = monthPlanSaleOrderList.stream().collect(Collectors.toMap(MpDayHistorySaleQty::getImportUpdateKey, Function.identity(), (v1, v2) -> v1));
            }
            for (MpDayHistorySaleQty mpDayHistorySaleQty : saveList) {
                String importUpdateKey = mpDayHistorySaleQty.getImportUpdateKey();
                if (monthPlanSaleOrderMap.containsKey(importUpdateKey)) {
                    MpDayHistorySaleQty sourceSaleOrder = monthPlanSaleOrderMap.get(importUpdateKey);
                    mpDayHistorySaleQty.setBaseVale(sourceSaleOrder.getId());
                    mpDayHistorySaleQty.setId(sourceSaleOrder.getId());
                }
            }
        }
        long start1 = System.currentTimeMillis();
        log.info("开始保存内销订单数据,处理数据时间耗时:{}", start1 - start);

        if (CollectionUtils.isNotEmpty(saveList)) {
            baseDao.saveBatch(saveList);
            // 更新历史订单汇总表
            getBaseMapper().updateByDayHisSale(inSaleOrderDto.getDates1(), inSaleOrderDto.getDates2());
            // 如果不存在的，则新增
            getBaseMapper().insertByDayHisSaleNotExist();
        }
        long end = System.currentTimeMillis();
        log.info("保存完成,时间耗时:{}", end - start1);
    }

    /**
     * 导入历史销售计划-月
     *
     * @param list          要导入的数据
     * @param updateSupport 是否更新
     * @param importLogId   导入日志id
     * @param importLog     导入日志
     * @param beginTime     开始时间
     * @param attributes    请求属性
     */
    @Async
    @Override
    public void importMonthDataAsync(List<MpHistorySaleQtyExcel4MonthVo> list, boolean updateSupport, Long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes) {
        try {
            RequestContextHolder.setRequestAttributes(attributes, true);

            AjaxResult result = this.importMonthData(list, updateSupport, importLogId);
            Date endTime = DateUtils.getNowDate();
            importLog.setRowCount(list.size());
            importLog.setBeginTime(beginTime);
            importLog.setEndTime(endTime);
            importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
            RemoteImportExcelUtils.updateImportLogAndFormatMsg(importLog, result, iRemoteImportLogService);
            RemoteImportExcelUtils.saveImportErrorLogs(result, iRemoteImportErrorLogService);
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    /**
     * 导入历史销售计划-月
     *
     * @param list          要导入的数据
     * @param updateSupport 是否更新
     * @param importLogId   导入日志id
     * @return 结果
     */
    @Override
    public AjaxResult importMonthData(List<MpHistorySaleQtyExcel4MonthVo> list, boolean updateSupport, Long importLogId) {
        // 初始化
        int successNum = 0;
        AtomicInteger failureNum = new AtomicInteger();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();


        // 公共校验（非空校验、长度校验等）
        List<MpHistorySaleQtyExcel4MonthVo> importList = list.stream()
                .map(vo -> {
                    int errorNum = list.indexOf(vo) + 2;
                    List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, vo);
                    if (CollectionUtils.isNotEmpty(validated)) {
                        failureNum.getAndIncrement();
                        importErrorLogs.addAll(validated);
                        return null;
                    } else {
                        return vo;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, Long> groupMap = importList.stream().collect(Collectors.groupingBy(item -> GenerageMapKeyUtils.createMapKey(item.getYear(), item.getMonth(), item.getSapCode()), Collectors.counting()));
        Set<Map.Entry<String, Long>> entrySet = groupMap.entrySet();
        for (Map.Entry<String, Long> entry : entrySet) {
            if (entry.getValue() > 1) {
                ImportErrorLog importErrorLog = new ImportErrorLog(importLogId, entry.getValue().intValue(), "数据重复，存在相同年月、物料号的数据");
                importErrorLogs.add(importErrorLog);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                // 处理导入数据
                processImportList4Month(importList, importErrorLogs, successNum);
            }
        } catch (Exception e) {
            handleException(e, list.size(), importErrorLogs, importLogId, successNum, failureNum.get());
        }
        return buildAjaxResult(successNum, failureNum.get(), importErrorLogs);
    }

    /**
     * 处理导入数据
     *
     * @param importList      导入数据
     * @param importErrorLogs 导入错误日志数据
     * @param successNum      成功数
     */
    private void processImportList4Month(List<MpHistorySaleQtyExcel4MonthVo> importList, List<ImportErrorLog> importErrorLogs, int successNum) {
        processBatch4Month(importList);
    }

    private void processBatch4Month(List<MpHistorySaleQtyExcel4MonthVo> batchList) {
        List<MpHistorySaleQty> insertList = new ArrayList<>();

        for (MpHistorySaleQtyExcel4MonthVo vo : batchList) {
            addRecordsForMonth(vo, insertList);
        }

        // 先删除历史年月、分厂的数据
        LambdaQueryWrapper<MpHistorySaleQty> wrapper = LambdaWrapperBuilder.buildWrapperByFunction(insertList, MpHistorySaleQty::getYear,
                MpHistorySaleQty::getMonth, MpHistorySaleQty::getFactoryCode);
        getBaseMapper().delete(wrapper);

        // 执行批量插入
        if (CollectionUtils.isNotEmpty(insertList)) {
            // 分厂+物料号 关联 物料信息表
            Map<String, List<MpHistorySaleQty>> groupMap = insertList.stream()
                    .filter(v -> StringUtils.isNotBlank(v.getFactoryCode()) && StringUtils.isNotBlank(v.getProductCode()))
                    .collect(Collectors.groupingBy(MpHistorySaleQty::getFactoryCode));
            groupMap.forEach((factoryCode, itemList) -> {
                List<String> productCodeList = itemList.stream().map(MpHistorySaleQty::getProductCode).distinct().collect(Collectors.toList());
                if (CollectionUtils.isNotEmpty(productCodeList)) {
                    LambdaQueryWrapper<MdmMaterialInfo> itemWrapper = Wrappers.lambdaQuery();
                    itemWrapper.eq(MdmMaterialInfo::getFactoryCode, factoryCode);
                    itemWrapper.in(MdmMaterialInfo::getMaterialCode, productCodeList);
                    List<MdmMaterialInfo> productInfoList = mdmMaterialInfoEntityMapper.selectList(itemWrapper);
                    Map<String, String> productInfoMap = productInfoList.stream()
                            .filter(v -> StringUtils.isNotBlank(v.getMaterialDesc()))
                            .collect(Collectors.toMap(MdmMaterialInfo::getMaterialCode, MdmMaterialInfo::getMaterialDesc, (v1, v2) -> v1));
                    for (MpHistorySaleQty item : itemList) {
                        if (productInfoMap.containsKey(item.getProductCode())) {
                            item.setProductDesc(productInfoMap.get(item.getProductCode()));
                        }
                    }
                }
            });

            for (List<MpHistorySaleQty> itemList : ListUtils.partition(insertList, BATCH_SIZE)) {
                getBaseMapper().batchInsertHistorySaleQty(itemList);
            }
        }
    }

    /**
     * 查询导出列表-年
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    @Override
    public List<MpHistorySaleQtyExcelVo> selectMpHistorySaleQtyList4ExportData(MpHistorySaleQty queryVO) {
        List<MpHistorySaleQtyExcelVo> resultList = new ArrayList<>();
        List<MpHistorySaleQty> mpHistorySaleQtyList = this.selectMpHistorySaleQtyList(queryVO);
        if (CollectionUtils.isNotEmpty(mpHistorySaleQtyList)) {
            Map<String, List<MpHistorySaleQty>> groupMap = mpHistorySaleQtyList.stream().collect(Collectors.groupingBy(item -> GenerageMapKeyUtils.createMapKey(item.getYear(), item.getProductCode())));
            Set<Map.Entry<String, List<MpHistorySaleQty>>> entrySet = groupMap.entrySet();
            for (Map.Entry<String, List<MpHistorySaleQty>> entry : entrySet) {
                MpHistorySaleQtyExcelVo excelVo = new MpHistorySaleQtyExcelVo();
                List<MpHistorySaleQty> value = entry.getValue();
                for (MpHistorySaleQty mpHistorySaleQty : value) {
                    setSalesData2ExcelVo(excelVo, mpHistorySaleQty, mpHistorySaleQty.getMonth(), mpHistorySaleQty.getLocationType());
                }
                resultList.add(excelVo);
            }
        }
        return resultList;
    }

    /**
     * 将历史销售记录的数据映射到对应的导出Vo上
     *
     * @param vo 导出数据的Vo
     * @param mpHistorySaleQty 历史销售记录
     * @param month 月份
     * @param locationType 库位
     */
    private void setSalesData2ExcelVo(MpHistorySaleQtyExcelVo vo, MpHistorySaleQty mpHistorySaleQty, int month, String locationType) {
        String suffix = ApsConstant.MONTH_SUFFIX_MAP.get(month);
        String fieldPrefix = ApsConstant.LOCATION_TYPE_FIELD_MAP.get(Integer.parseInt(locationType));
        if (fieldPrefix != null && suffix != null) {
            String orderQtyFieldName = fieldPrefix + "SalesOrderCount" + suffix;
            String saleQtyFieldName = fieldPrefix + "SalesCount" + suffix;

            Long orderQty = mpHistorySaleQty.getOrderQty();
            Long saleQty = mpHistorySaleQty.getSaleQty();

            ReflectUtils.setFieldValue(vo, orderQtyFieldName, orderQty);
            ReflectUtils.setFieldValue(vo, saleQtyFieldName, saleQty);
        }
        vo.setYear(mpHistorySaleQty.getYear());
        vo.setSapCode(mpHistorySaleQty.getProductCode());
        vo.setSpecDesc(mpHistorySaleQty.getProductDesc());
        String remark = mpHistorySaleQty.getRemark();
        if (StringUtils.isNotBlank(remark)) {
            vo.setRemark(StringUtils.defaultIfBlank(vo.getRemark(), "") + " " + remark);
        }
        vo.setCreateBy(mpHistorySaleQty.getCreateBy());
        vo.setCreateTime(mpHistorySaleQty.getCreateTime());
    }

    /**
     * 新增所有月份的历史销售记录
     *
     * @param vo 源数据
     * @param insertList 新增列表
     */
    private void addRecordsForMonth(MpHistorySaleQtyExcel4MonthVo vo, List<MpHistorySaleQty> insertList) {
        for (int locationType = 1; locationType <= 3; locationType++) {
            // 创建历史销售记录实例
            MpHistorySaleQty record = new MpHistorySaleQty();
            record.setYear(vo.getYear());
            record.setMonth(vo.getMonth());
            record.setFactoryCode("116");
            record.setProductCode(vo.getSapCode());
            record.setProductDesc(vo.getSpecDesc());
            record.setLocationType(String.valueOf(locationType));
            record.setRemark(vo.getRemark());
            record.setCreateBy(vo.getCreateBy());
            record.setCreateTime(vo.getCreateTime());
            // 根据月份、库位类型映射赋值订单数、销售数
            setSalesDataUsingMap4Month(vo, record, vo.getMonth(), locationType);
            insertList.add(record);
        }
    }

    /**
     * 根据月份、库位类型映射赋值订单数、销售数
     *
     * @param vo
     * @param mpHistorySaleQty
     * @param month
     * @param locationType
     */
    private void setSalesDataUsingMap4Month(MpHistorySaleQtyExcel4MonthVo vo, MpHistorySaleQty mpHistorySaleQty, int month, int locationType) {
        String suffix = ApsConstant.MONTH_SUFFIX_MAP.get(month);
        String fieldPrefix = ApsConstant.LOCATION_TYPE_FIELD_MAP.get(locationType);
        if (fieldPrefix != null && suffix != null) {
            String orderQtyFieldName = fieldPrefix + "SalesOrderCount";
            String saleQtyFieldName = fieldPrefix + "SalesCount";

            // 假设你有一个方法来获取字段值
            Long orderQty = (Long) getField(vo, orderQtyFieldName);
            Long saleQty = (Long) getField(vo, saleQtyFieldName);

            if (orderQty != null) {
                mpHistorySaleQty.setOrderQty(orderQty);
            } else {
                mpHistorySaleQty.setOrderQty(0L);
            }
            if (saleQty != null) {
                mpHistorySaleQty.setSaleQty(saleQty);
            } else {
                mpHistorySaleQty.setSaleQty(0L);
            }
        }
    }
}


