package com.zlt.aps.monthplan.demand.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.tlt.aps.enums.LocationTypeEnum;
import com.tlt.aps.enums.ProductCommonTypeEnum;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.utils.LambdaWrapperBuilder;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.ProductStockMonth;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanSaleRequirePlanVo;
import com.zlt.aps.monthplan.api.service.IRemoteImportErrorLogService;
import com.zlt.aps.monthplan.api.service.IRemoteImportLogService;
import com.zlt.aps.monthplan.common.utils.RemoteImportExcelUtils;
import com.zlt.aps.monthplan.demand.mapper.ProductStockMonthMapper;
import com.zlt.aps.monthplan.demand.service.IProductStockMonthService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductStockMonthServiceImpl.java
 * 描    述：ProductStockMonthServiceImpl物料月库存信息业务层处理
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductStockMonthServiceImpl implements IProductStockMonthService {

    private final ProductStockMonthMapper productStockMonthMapper;
    private final MdmMaterialInfoEntityMapper productInfoEntityMapper;

    private final IRemoteImportLogService iRemoteImportLogService;
    private final IRemoteImportErrorLogService iRemoteImportErrorLogService;

    private final BaseDao baseDao;

    @Override
    public List<ProductStockMonth> getMothStock(MonthPlanSaleRequirePlanVo condition) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(true, "FACTORY_CODE", condition.getFactoryCode());
        queryWrapper.eq(true, "YEAR", condition.getYear());
        queryWrapper.eq(true, "MONTH", condition.getMonth());
        queryWrapper.gt("STOCK_QTY", 0);
        return productStockMonthMapper.selectList(queryWrapper);
    }

    /**
     * 列表查询
     */
    @Override
    public List<ProductStockMonth> selectList(ProductStockMonth queryVO) {
        return productStockMonthMapper.selectRelateList(queryVO);
    }

    /**
     * 回显物料信息表-品牌、规格、花纹、寸口
     */
    private void echoFieldList(List<ProductStockMonth> monthList) {
        if (CollectionUtils.isEmpty(monthList)) {
            return;
        }

        List<String> productCodeList = monthList.stream().map(ProductStockMonth::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productCodeList)) {
            return;
        }
        Map<String, MdmMaterialInfo> infoMap = productInfoEntityMapper.selectList(Wrappers.lambdaQuery(MdmMaterialInfo.class)
                        .in(MdmMaterialInfo::getMaterialCode, productCodeList))
                .stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getMaterialCode()), Function.identity(), (v1, v2) -> v1));

        for (ProductStockMonth itemStock : monthList) {
            MdmMaterialInfo productInfo = infoMap.get(GenerageMapKeyUtils.createMapKey(itemStock.getFactoryCode(), itemStock.getProductCode()));
            if (productInfo == null) {
                continue;
            }

            itemStock.setBrand(productInfo.getBrand());
            itemStock.setSpecifications(productInfo.getSpecifications());
            itemStock.setPattern(productInfo.getPattern());
            itemStock.setProSize(productInfo.getProSize());
        }
    }

    /**
     * 导入数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult doImportData(List<ProductStockMonth> list, boolean updateSupport, long importLogId) {
        // 初始化
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<ProductStockMonth> importList = new ArrayList<>();
        // 国际化初始化
        String productCodeError = I18nUtil.getMessage("ui.data.column.monthStock.productCode.notExist");
        String repeatError = I18nUtil.getMessage("ui.data.column.monthStock.repeat");

        // 查询对应物料信息
        List<String> productCodeList = list.stream().map(ProductStockMonth::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        Map<String, MdmMaterialInfo> productInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(productCodeList)) {
            List<String> factoryCodeList = list.stream().map(ProductStockMonth::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
            productInfoMap = productInfoEntityMapper.selectList(Wrappers.lambdaQuery(MdmMaterialInfo.class)
                            .in(MdmMaterialInfo::getMaterialCode, productCodeList)
                            .in(CollectionUtils.isNotEmpty(factoryCodeList), MdmMaterialInfo::getFactoryCode, factoryCodeList))
                    .stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getMaterialCode()), Function.identity(), (v1, v2) -> v1));
        }

        // 唯一键分组
        Function<ProductStockMonth, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getYear(), v.getMonth(), v.getFactoryCode(), v.getProductCode(), v.getLocationType());
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(keyFunc, Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            ProductStockMonth itemStock = list.get(i);
            // 基础字段校验
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, itemStock);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                importErrorLogs.addAll(validated);
                continue;
            }

            // 物料信息校验
            MdmMaterialInfo mdmMaterialInfo = productInfoMap.get(GenerageMapKeyUtils.createMapKey(itemStock.getFactoryCode(), itemStock.getProductCode()));
            if (mdmMaterialInfo == null) {
                failureNum++;
                addImportErrorLog(importLogId, errorNum, String.format(productCodeError, itemStock.getProductCode()), importErrorLogs);
                continue;
            } else {
                itemStock.setProductDesc(mdmMaterialInfo.getMaterialDesc());
                String commonType = mdmMaterialInfo.getCommonType();
                LocationTypeEnum locationTypeEnum = ProductCommonTypeEnum.getLocationTypeByCode(commonType);
                itemStock.setLocationType(locationTypeEnum.getValue());
            }

            // 重复记录校验
            Long l = groupMap.get(keyFunc.apply(itemStock));
            if (l != null && l > 1) {
                failureNum++;
                addImportErrorLog(importLogId, errorNum, repeatError, importErrorLogs);
                continue;
            }

            importList.add(itemStock);
        }
        try {
            successNum = importList.size();
            mergeByList(importList);
        } catch (Exception e) {
            // 执行sql失败，插入导入失败记录
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        successNum = importList.size();  //成功记录数
        failureNum = list.size() - successNum; //失败记录数
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void importDataAsync(List<ProductStockMonth> list, boolean updateSupport, long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes) {
        try {
            RequestContextHolder.setRequestAttributes(attributes, true);

            AjaxResult result = this.doImportData(list, updateSupport, importLogId);
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
     * 删除历史数据，插入新数据，唯一键：分厂、年月、SAP代码、库位
     */
    private void mergeByList(List<ProductStockMonth> importList) {
        if (CollectionUtils.isEmpty(importList)) {
            return;
        }

        // 删除历史数据 分厂+年月+库位
        LambdaQueryWrapper<ProductStockMonth> wrapper = LambdaWrapperBuilder.buildWrapperByFunction(importList, ProductStockMonth::getFactoryCode,
                ProductStockMonth::getYear, ProductStockMonth::getMonth, ProductStockMonth::getLocationType);
        productStockMonthMapper.delete(wrapper);

        baseDao.insertBatch(importList);
    }

    /**
     * 条件拼接
     */
    protected void builderCondition(QueryWrapper<ProductStockMonth> queryWrapper, ProductStockMonth queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productDesc")), "PRODUCT_DESC", queryVO.getFieldValueByFieldName("productDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("locationType")), "LOCATION_TYPE", queryVO.getFieldValueByFieldName("locationType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("stockQty")), "STOCK_QTY", queryVO.getFieldValueByFieldName("stockQty"));
    }
}
