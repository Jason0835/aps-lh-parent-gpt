package com.zlt.aps.mp.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.LocationChannelConfigurationMapper;
import com.zlt.aps.maindata.mapper.MdmCustomerInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.mp.api.domain.entity.LocationChannelConfiguration;
import com.zlt.aps.mp.api.domain.entity.MdmCustomerInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.domain.entity.MonthPlanSaleOrder;
import com.zlt.aps.mp.api.domain.itf.InDataListVo;
import com.zlt.aps.mp.api.domain.itf.InSaleOrderDto;
import com.zlt.aps.mp.api.domain.vo.MonthPlanSaleRequirePlanVo;
import com.zlt.aps.mp.api.enums.SaleOrderSourceTypeEnum;
import com.zlt.aps.mp.demand.mapper.MonthPlanSaleOrderMapper;
import com.zlt.aps.mp.demand.service.IMonthPlanSaleOrderService;
import com.zlt.aps.mp.demand.service.MonthPlanSaleOrderHelper;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.core.dao.basedao.BaseDao;
import io.jsonwebtoken.lang.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MonthPlanSaleOrderServiceImpl.java
 * 描    述：MonthPlanSaleOrderServiceImpl月度销售计划订单业务层处理
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonthPlanSaleOrderServiceImpl implements IMonthPlanSaleOrderService {

    private final BaseDao baseDao;

    private final MdmMaterialInfoEntityMapper productInfoEntityMapper;

    private final MdmCustomerInfoEntityMapper customerInfoEntityMapper;

    private final MonthPlanSaleOrderMapper monthPlanSaleOrderMapper;
    /**
     * 最大长度不能大于8
     */
    private final String MAX_LENGTH_ERROR_INFO = "最大长度不能大于8";

    @Override
    public List<MonthPlanSaleOrder> getList(Wrapper<MonthPlanSaleOrder> queryWrapper) {
        return monthPlanSaleOrderMapper.selectList(queryWrapper);
    }

    /**
     * 新增月度销售计划订单
     *
     * @param monthPlanSaleOrder 月度销售计划订单
     * @return 结果
     */
    @Override
    public int insertMonthPlanSaleOrder(MonthPlanSaleOrder monthPlanSaleOrder) {
        monthPlanSaleOrder.setBaseVale(null);
        return baseDao.insert(monthPlanSaleOrder);
    }

    /**
     * 修改月度销售计划订单
     *
     * @param monthPlanSaleOrder 月度销售计划订单
     * @return 结果
     */
    @Override
    public int updateMonthPlanSaleOrder(MonthPlanSaleOrder monthPlanSaleOrder) {
        monthPlanSaleOrder.setBaseVale(monthPlanSaleOrder.getId());
        return baseDao.update(monthPlanSaleOrder);
    }

    /**
     * 校验月度销售计划订单唯一性
     */
    @Override
    public String checkMonthPlanSaleOrderUnique(MonthPlanSaleOrder monthPlanSaleOrder) {
        if (monthPlanSaleOrder == null) {
            return UserConstants.NOT_UNIQUE;
        }
        // 年月、分厂、物料号、订单号
        LambdaQueryWrapper<MonthPlanSaleOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MonthPlanSaleOrder::getFactoryCode, monthPlanSaleOrder.getFactoryCode())
                .eq(MonthPlanSaleOrder::getYear, monthPlanSaleOrder.getYear())
                .eq(MonthPlanSaleOrder::getMonth, monthPlanSaleOrder.getMonth())
                .eq(MonthPlanSaleOrder::getProductCode, monthPlanSaleOrder.getProductCode())
                .eq(MonthPlanSaleOrder::getOrderNo, monthPlanSaleOrder.getOrderNo());
        List<MonthPlanSaleOrder> list = monthPlanSaleOrderMapper.selectList(queryWrapper);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x -> !x.getId().equals(monthPlanSaleOrder.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public int removeByIds(List<Long> ids) {
        return monthPlanSaleOrderMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public AjaxResult importData(List<MonthPlanSaleOrder> excelDataList, boolean updateSupport, Long importLogId) {
        if (CollectionUtils.isEmpty(excelDataList)) {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + 0);
        }
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        //excel数据验证
        int rowIndex = 2;
        int successNum = 0;
        int failureNum = 0;
        List<MonthPlanSaleOrder> importDataList = new ArrayList<>();
        MonthPlanSaleOrderHelper helper = new MonthPlanSaleOrderHelper();
        helper.setCustomCodeSet(new HashSet<>());
        helper.setProductCodeSet(new HashSet<>());
        helper.setExistCustomCodeSet(new HashSet<>());
        helper.setExistProductCodeSet(new HashSet<>());
        helper.setNoExistProductInfo(I18nUtil.getMessage("ui.data.column.saleOrder.check.noExistCheck.productInfo"));
        helper.setNoExistCustomInfo(I18nUtil.getMessage("ui.data.column.saleOrder.check.noExistCheck.customInfo"));
        helper.setImportLogId(importLogId);
        for (MonthPlanSaleOrder monthPlanSaleOrder : excelDataList) {
            monthPlanSaleOrder.setSourceType(SaleOrderSourceTypeEnum.IMPORT.getSourceType());
            if (null == monthPlanSaleOrder.getIsEmergency()) {
                monthPlanSaleOrder.setIsEmergency(YesOrNoEnum.NO.getValue());
            }
            if (null == monthPlanSaleOrder.getIsEnsurePlan()) {
                monthPlanSaleOrder.setIsEnsurePlan(YesOrNoEnum.NO.getValue());
            }
            String productCode = monthPlanSaleOrder.getProductCode() == null ? null : monthPlanSaleOrder.getProductCode().replaceAll("\\s+", "");
            String customCode = monthPlanSaleOrder.getCustomCode() == null ? null : monthPlanSaleOrder.getCustomCode().replaceAll("\\s+", "");
            //物料编码及客户编码去除所有空格
            monthPlanSaleOrder.setProductCode(productCode);
            monthPlanSaleOrder.setCustomCode(customCode);
            helper.setRowIndex(rowIndex);
            boolean checkResult = checkInfo(helper, monthPlanSaleOrder, importErrorLogs);
            rowIndex = rowIndex + 1;
            if (!checkResult) {
                failureNum = failureNum + 1;
                continue;
            }
            importDataList.add(monthPlanSaleOrder);
        }
        //没有数据能导入，则表示校验没有通过
        if (CollectionUtils.isEmpty(importDataList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        MonthPlanSaleOrder firstRow = importDataList.get(0);
        Integer year = firstRow.getYear();
        Integer month = firstRow.getMonth();
        String factoryCode = firstRow.getFactoryCode();
        //更新处理
        if (updateSupport) {
            updateImport(importDataList);
        } else {
            monthPlanSaleOrderMapper.deletedByYearAndMonth(factoryCode, year, month);
            baseDao.insertBatch(importDataList);
        }
        //补充信息
        supplyInfo(factoryCode, year, month);
        successNum = importDataList.size();
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /**
     * 信息校验
     *
     * @param helper             校验辅助类
     * @param monthPlanSaleOrder 销售订单
     * @param importErrorLogs    错误信息集合
     * @return
     */
    private boolean checkInfo(MonthPlanSaleOrderHelper helper, MonthPlanSaleOrder monthPlanSaleOrder, List<ImportErrorLog> importErrorLogs) {
        Long importLogId = helper.getImportLogId();
        int rowIndex = helper.getRowIndex();
        List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, rowIndex, monthPlanSaleOrder);
        if (!CollectionUtils.isEmpty(validated)) {
            convertError(validated);
            importErrorLogs.addAll(validated);
            return false;
        }
        String productCode = monthPlanSaleOrder.getProductCode();
        String customCode = monthPlanSaleOrder.getCustomCode();
        String noExistProductInfo = helper.getNoExistProductInfo();
        String noExistCustomInfo = helper.getNoExistCustomInfo();
        String factoryCode = monthPlanSaleOrder.getFactoryCode();
        //不存在
        if (helper.getProductCodeSet().contains(productCode)) {
            String errorInfo = String.format(noExistProductInfo, rowIndex, productCode);
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, rowIndex, errorInfo, importErrorLogs);
            return false;
        }
        if (helper.getCustomCodeSet().contains(customCode)) {
            String errorInfo = String.format(noExistCustomInfo, rowIndex, customCode);
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, rowIndex, errorInfo, importErrorLogs);
            return false;
        }
        //存在
        if (helper.getExistCustomCodeSet().contains(customCode) && helper.getExistProductCodeSet().contains(productCode)) {
            return true;
        }
        boolean hasExistCustom = hasExistCustomCode(factoryCode, customCode);
        if (hasExistCustom) {
            helper.getExistCustomCodeSet().add(customCode);
        } else {
            helper.getCustomCodeSet().add(customCode);
            String errorInfo = String.format(noExistCustomInfo, rowIndex, customCode);
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, rowIndex, errorInfo, importErrorLogs);
            return false;
        }
        boolean hasExistProduct = hasExistProductCode(factoryCode, productCode);
        if (hasExistProduct) {
            helper.getExistProductCodeSet().add(productCode);
        } else {
            helper.getProductCodeSet().add(productCode);
            String errorInfo = String.format(noExistProductInfo, rowIndex, productCode);
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, rowIndex, errorInfo, importErrorLogs);
            return false;
        }
        return true;
    }

    /**
     * 更新的方式导入
     *
     * @param importDataList
     * @return
     */
    private void updateImport(List<MonthPlanSaleOrder> importDataList) {
        MonthPlanSaleOrder firstRow = importDataList.get(0);
        Integer year = firstRow.getYear();
        Integer month = firstRow.getMonth();
        String factoryCode = firstRow.getFactoryCode();
        QueryWrapper<MonthPlanSaleOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(true, "FACTORY_CODE", factoryCode);
        queryWrapper.eq(true, "YEAR", year);
        queryWrapper.eq(true, "MONTH", month);
        List<MonthPlanSaleOrder> oldList = getList(queryWrapper);
        if (CollectionUtils.isEmpty(oldList)) {
            baseDao.insertBatch(importDataList);
            return;
        }
        Map<String, Long> oldDataMap = new HashMap<>();
        oldList.stream().forEach(oldData -> {
            String importUpdateKey = oldData.getImportUpdateKey();
            if (StringUtils.isBlank(importUpdateKey)) {
                return;
            }
            oldDataMap.put(importUpdateKey, oldData.getId());
        });
        List<MonthPlanSaleOrder> insertList = new ArrayList<>();
        List<MonthPlanSaleOrder> updateList = new ArrayList<>();
        importDataList.stream().forEach(importData -> {
            String updateKey = importData.getImportUpdateKey();
            //更新
            if (oldDataMap.containsKey(updateKey)) {
                importData.setId(oldDataMap.get(updateKey));
                updateList.add(importData);
                return;
            }
            //插入
            insertList.add(importData);
        });
        if (!CollectionUtils.isEmpty(insertList)) {
            baseDao.insertBatch(insertList);
        }
        if (!CollectionUtils.isEmpty(updateList)) {
            baseDao.updateBatch(updateList);
        }
    }

    /**
     * 根据物料编码，填充物料的品牌，规格，规格描述、花纹、寸口、层级
     * 根据重要客户信息，设置是否为重要客户
     * 根据年、月、客户及物料设置是否必保计划
     *
     * @param factoryCode 分厂编号
     * @param year        年份
     * @param month       月份
     */
    private void supplyInfo(String factoryCode, Integer year, Integer month) {
        MonthPlanSaleRequirePlanVo updateCondition = new MonthPlanSaleRequirePlanVo();
        updateCondition.setFactoryCode(factoryCode);
        updateCondition.setYear(year);
        updateCondition.setMonth(month);
        //更新补充物料基础数据、重要客户
        monthPlanSaleOrderMapper.updateProductInfo(updateCondition);
        monthPlanSaleOrderMapper.updateImportantCustomFlag(updateCondition);
        //20250911 ZLT 必保采用订单导入直接使用
    }

    /**
     * 将接口的列表数据转成内销订单数据
     *
     * @param inSaleOrderDto                  查询参数
     * @param inDataListVoList                接口返回的数据
     * @param customerInfoMap                 客户信息
     * @param productInfoMap                  物料信息
     * @param locationChannelConfigurationMap 库位类别渠道数据
     * @return 内销订单数据
     */
    private static List<MonthPlanSaleOrder> transFormSyncListToOrderList(InSaleOrderDto inSaleOrderDto, List<InDataListVo> inDataListVoList, Map<String, MdmCustomerInfo> customerInfoMap, Map<String, MdmMaterialInfo> productInfoMap, Map<String, LocationChannelConfiguration> locationChannelConfigurationMap) {
        List<MonthPlanSaleOrder> saveList = new ArrayList<>();
        for (InDataListVo inDataListVo : inDataListVoList) {
            MonthPlanSaleOrder monthPlanSaleOrder = new MonthPlanSaleOrder();
            monthPlanSaleOrder.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
            monthPlanSaleOrder.setOrderNo(inDataListVo.getNumbers());
            monthPlanSaleOrder.setSourceType(SaleOrderSourceTypeEnum.DOMESTIC_SYSTEM.getSourceType());
            String clientnum = removeLeadingZeros(inDataListVo.getClientnum());
            monthPlanSaleOrder.setCustomCode(clientnum);
            if (customerInfoMap.containsKey(clientnum)) {
                MdmCustomerInfo customerInfo = customerInfoMap.get(clientnum);
                monthPlanSaleOrder.setCustomName(customerInfo.getCustomName());
            }
            monthPlanSaleOrder.setYear(inSaleOrderDto.getYears());
            monthPlanSaleOrder.setMonth(inSaleOrderDto.getMonths());

            String goodsNum = inDataListVo.getGoodsNum();
            monthPlanSaleOrder.setProductCode(goodsNum);

            if (productInfoMap.containsKey(goodsNum)) {
                MdmMaterialInfo productInfo = productInfoMap.get(goodsNum);
                monthPlanSaleOrder.setBrand(productInfo.getBrand());
            }

            String mapKey = String.join("|", monthPlanSaleOrder.getFactoryCode(), inDataListVo.getClientExtendName(), monthPlanSaleOrder.getBrand());
            if (locationChannelConfigurationMap.containsKey(mapKey)) {
                LocationChannelConfiguration locationChannelConfiguration = locationChannelConfigurationMap.get(mapKey);
                monthPlanSaleOrder.setLocationType(locationChannelConfiguration.getLocationType().toString());
                monthPlanSaleOrder.setChannel(locationChannelConfiguration.getChannel());
                monthPlanSaleOrder.setLocationType(locationChannelConfiguration.getLocationType().toString());
            }

            monthPlanSaleOrder.setPlanQty(inDataListVo.getNum());
            monthPlanSaleOrder.setSubmissionDate(inDataListVo.getInnerDate());

            monthPlanSaleOrder.setIsImportantCustom(YesOrNoEnum.NO.getValue());
            monthPlanSaleOrder.setIsEnsurePlan(YesOrNoEnum.NO.getValue());
            monthPlanSaleOrder.setIsEmergency(YesOrNoEnum.NO.getValue());

            monthPlanSaleOrder.setBaseVale(null);
            saveList.add(monthPlanSaleOrder);
        }
        return saveList;
    }

    /**
     * 是否存在
     *
     * @param factoryCode
     * @param customCode
     * @return
     */
    private boolean hasExistCustomCode(String factoryCode, String customCode) {
        QueryWrapper<MdmCustomerInfo> customerInfoQueryWrapper = new QueryWrapper<>();
        customerInfoQueryWrapper.eq("FACTORY_CODE", factoryCode);
        customerInfoQueryWrapper.eq("CUSTOM_CODE", customCode);
        customerInfoQueryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return customerInfoEntityMapper.selectCount(customerInfoQueryWrapper) > 0;
    }

    /**
     * 错误提示信息转化处理
     *
     * @param validated
     */
    private void convertError(List<ImportErrorLog> validated) {
        validated.stream().forEach(importErrorLog -> {
            String errorDetail = importErrorLog.getErrorDetail();
            String[] temp = errorDetail.split("：");
            if (temp.length > 1 && MAX_LENGTH_ERROR_INFO.equals(temp[1])) {
                temp[1] = "：只能输入8位以内的整数";
                importErrorLog.setErrorDetail(temp[0] + temp[1]);
            }
        });
    }

    @Autowired
    private MdmCustomerInfoEntityMapper mdmCustomerInfoEntityMapper;

    @Autowired
    private LocationChannelConfigurationMapper locationChannelConfigurationMapper;

    /**
     * 是否存在
     *
     * @param factoryCode 分厂编码
     * @param productCode 物料编号
     * @return
     */
    private boolean hasExistProductCode(String factoryCode, String productCode) {
        QueryWrapper<MdmMaterialInfo> productInfoQueryWrapper = new QueryWrapper<>();
        productInfoQueryWrapper.eq("FACTORY_CODE", factoryCode);
        productInfoQueryWrapper.eq("PRODUCT_CODE", productCode);
        productInfoQueryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        return productInfoEntityMapper.selectCount(productInfoQueryWrapper) > 0;
    }

    /**
     * 去除 0开头的字符串前的0
     *
     * @param str 字符串
     * @return 结果
     */
    public static String removeLeadingZeros(String str) {
        return str.replaceFirst("^0+", "");
    }

    /**
     * 转成表对象存储数据
     *
     * @param inSaleOrderDto   查询参数
     * @param inDataListVoList 接口返回的数据
     */
    @Override
    public void handleInSaleOrderSyncResultData(InSaleOrderDto inSaleOrderDto, List<InDataListVo> inDataListVoList) {
        if (Collections.isEmpty(inDataListVoList)) {
            return;
        }
        long start = System.currentTimeMillis();
        log.info("开始处理内销订单返回数据");
        // 查询客户信息
        List<String> custCodeList = inDataListVoList.stream().map(item -> removeLeadingZeros(item.getClientnum())).collect(Collectors.toList());
        LambdaQueryWrapper<MdmCustomerInfo> custQueryWrapper = new LambdaQueryWrapper<>();
        custQueryWrapper.eq(MdmCustomerInfo::getFactoryCode, FactoryConstant.DEFAULT_FACTORY_CODE);
        custQueryWrapper.in(MdmCustomerInfo::getCustomCode, custCodeList);
        List<MdmCustomerInfo> mdmCustomerInfoList = mdmCustomerInfoEntityMapper.selectList(custQueryWrapper);

        Map<String, MdmCustomerInfo> customerInfoMap = new HashMap<>(16);
        if (CollectionUtils.isNotEmpty(mdmCustomerInfoList)) {
            customerInfoMap = mdmCustomerInfoList.stream().collect(Collectors.toMap(MdmCustomerInfo::getCustomCode, Function.identity(), (v1, v2) -> v1));
        }

        // 查询物料信息
        List<String> productCodeList = inDataListVoList.stream().map(InDataListVo::getGoodsNum).collect(Collectors.toList());
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

        List<MonthPlanSaleOrder> saveList = transFormSyncListToOrderList(inSaleOrderDto, inDataListVoList, customerInfoMap, productInfoMap, locationChannelConfigurationMap);

        // 先查询订单，如果存在，赋值ID更新数据，否则新增
        if (CollectionUtils.isNotEmpty(saveList)) {
            LambdaQueryWrapper<MonthPlanSaleOrder> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(MonthPlanSaleOrder::getYear, inSaleOrderDto.getYears());
            queryWrapper.eq(MonthPlanSaleOrder::getMonth, inSaleOrderDto.getMonths());
            List<MonthPlanSaleOrder> monthPlanSaleOrderList = monthPlanSaleOrderMapper.selectList(queryWrapper);
            Map<String, MonthPlanSaleOrder> monthPlanSaleOrderMap = new HashMap<>(16);
            if (CollectionUtils.isNotEmpty(monthPlanSaleOrderList)) {
                monthPlanSaleOrderMap = monthPlanSaleOrderList.stream().collect(Collectors.toMap(MonthPlanSaleOrder::getImportUpdateKey, Function.identity(), (v1, v2) -> v1));
            }
            for (MonthPlanSaleOrder monthPlanSaleOrder : saveList) {
                String importUpdateKey = monthPlanSaleOrder.getImportUpdateKey();
                if (monthPlanSaleOrderMap.containsKey(importUpdateKey)) {
                    MonthPlanSaleOrder sourceSaleOrder = monthPlanSaleOrderMap.get(importUpdateKey);
                    monthPlanSaleOrder.setBaseVale(sourceSaleOrder.getId());
                    monthPlanSaleOrder.setId(sourceSaleOrder.getId());
                }
            }
        }
        long start1 = System.currentTimeMillis();
        log.info("开始保存内销订单数据,处理数据时间耗时:{}", start1 - start);

        if (CollectionUtils.isNotEmpty(saveList)) {
            baseDao.saveBatch(saveList);
            supplyInfo(FactoryConstant.DEFAULT_FACTORY_CODE, inSaleOrderDto.getYears(), inSaleOrderDto.getMonths());
        }
        long end = System.currentTimeMillis();
        log.info("保存完成,时间耗时:{}", end - start1);
    }

    /**
     * 外销销售订单同步
     *
     * @param inSaleOrderDto 外销销售订单同步参数
     * @return 结果
     */
    @Override
    public AjaxResult syncOutSaleOrder(InSaleOrderDto inSaleOrderDto) {
        int result = monthPlanSaleOrderMapper.updateOutSaleOrder(inSaleOrderDto);
        result += monthPlanSaleOrderMapper.insertOutSaleOrder(inSaleOrderDto);
        return AjaxResult.success();
    }
}
