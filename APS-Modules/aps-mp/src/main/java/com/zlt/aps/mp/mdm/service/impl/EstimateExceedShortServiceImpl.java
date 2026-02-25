package com.zlt.aps.mp.mdm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.constant.Constant;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.utils.LambdaWrapperBuilder;
import com.zlt.aps.maindata.utils.RemoteImportExcelUtils;
import com.zlt.aps.mp.api.domain.entity.EstimateExceedShort;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.api.service.IRemoteImportErrorLogService;
import com.zlt.aps.mp.api.service.IRemoteImportLogService;
import com.zlt.aps.mp.mdm.mapper.EstimateExceedShortMapper;
import com.zlt.aps.mp.mdm.service.IEstimateExceedShortService;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：EstimateExceedShortServiceImpl.java
 * 描    述：EstimateExceedShortServiceImpl预计超欠产业务层处理
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EstimateExceedShortServiceImpl implements IEstimateExceedShortService {

    private final EstimateExceedShortMapper estimateExceedShortMapper;
    private final MdmMaterialInfoEntityMapper productInfoEntityMapper;

    private final IRemoteImportLogService iRemoteImportLogService;
    private final IRemoteImportErrorLogService iRemoteImportErrorLogService;

    private final BaseDao baseDao;

    @Override
    public List<EstimateExceedShort> getEstimateExceedShortByYearAndMonth(Integer year, Integer month) {
        if (null == year || null == month) {
            return Collections.emptyList();
        }
        QueryWrapper<EstimateExceedShort> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(true, "YEAR", year);
        queryWrapper.eq(true, "MONTH", month);
        queryWrapper.lt("EXCEED_SHORT_QTY", BigDecimal.ZERO.longValue());
        return estimateExceedShortMapper.selectList(queryWrapper);
    }

    /**
     * 查询超欠产列表
     */
    @Override
    public List<EstimateExceedShort> selectEstimateExceedShortList(EstimateExceedShort query) {
        LambdaQueryWrapper<EstimateExceedShort> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(query.getYear() != null, EstimateExceedShort::getYear, query.getYear());
        wrapper.eq(query.getMonth() != null, EstimateExceedShort::getMonth, query.getMonth());
        wrapper.eq(StringUtils.isNotBlank(query.getProductCode()), EstimateExceedShort::getProductCode, query.getProductCode());
        wrapper.eq(StringUtils.isNotBlank(query.getFactoryCode()), EstimateExceedShort::getFactoryCode, query.getFactoryCode());
        wrapper.eq(StringUtils.isNotBlank(query.getLocationType()), EstimateExceedShort::getLocationType, query.getLocationType());
        return estimateExceedShortMapper.selectList(wrapper);
    }

    /**
     * 校验唯一性
     */
    @Override
    public String checkUnique(EstimateExceedShort query) {
        if (query == null) {
            return UserConstants.NOT_UNIQUE;
        }
        LambdaQueryWrapper<EstimateExceedShort> wrapper = Wrappers.lambdaQuery();
        wrapper.ne(query.getId() != null, EstimateExceedShort::getId, query.getId());
        wrapper.eq(query.getYear() != null, EstimateExceedShort::getYear, query.getYear());
        wrapper.eq(query.getMonth() != null, EstimateExceedShort::getMonth, query.getMonth());
        wrapper.eq(StringUtils.isNotBlank(query.getProductCode()), EstimateExceedShort::getProductCode, query.getProductCode());
        wrapper.eq(StringUtils.isNotBlank(query.getFactoryCode()), EstimateExceedShort::getFactoryCode, query.getFactoryCode());
        wrapper.eq(StringUtils.isNotBlank(query.getLocationType()), EstimateExceedShort::getLocationType, query.getLocationType());
        Long count = estimateExceedShortMapper.selectCount(wrapper);
        if (count > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 保存超欠产管理数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int save(EstimateExceedShort billVO) {
        // 设置品号、寸口
        setProductInfo(Collections.singletonList(billVO));
        return billVO.getId() != null ? estimateExceedShortMapper.updateById(billVO) : estimateExceedShortMapper.insert(billVO);
    }

    /**
     * 根据ID列表删除
     */
    @Override
    public int removeByIds(List<Long> ids) {
        return estimateExceedShortMapper.deleteBatchIds(ids);
    }

    /**
     * 获取预计超欠产管理数据
     */
    @Override
    public EstimateExceedShort getInfo(Long billId) {
        return estimateExceedShortMapper.selectById(billId);
    }

    /**
     * 导入预计超欠产管理数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importData(List<EstimateExceedShort> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<EstimateExceedShort> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        String rowCountStr = I18nUtil.getMessage("ui.data.alert.rowcount");
        String noOnlyStr = I18nUtil.getMessage("ui.data.alert.TEstimateExceedShort.noOnly");
        String notExistProductInfo = I18nUtil.getMessage("ui.data.column.TEstimateExceedShort.notExist.productInfo");
        // 唯一键分组
        Function<EstimateExceedShort, String> keyFunc = item -> GenerageMapKeyUtils.createMapKey(item.getYear(), item.getMonth(), item.getFactoryCode(), item.getProductCode(), item.getLocationType());
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(keyFunc, Collectors.counting()));

        // 获取物料信息
        Map<String, MdmMaterialInfo> productInfoMap = getMdmMaterialInfoMap(list);

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            EstimateExceedShort tEstimateExceedShort = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, tEstimateExceedShort);
            if (CollectionUtils.isNotEmpty(validated)) {
                tEstimateExceedShort.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
                continue;
            }

            // 重复记录校验
            Long hasValue = groupMap.get(keyFunc.apply(tEstimateExceedShort));
            if (hasValue > 1) {
                failureNum++;
                tEstimateExceedShort.setId(-999L);
                String message = String.format(rowCountStr, i + 2) + noOnlyStr;
                addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                continue;
            }

            // 物料信息不存在跳过
            MdmMaterialInfo productInfo = productInfoMap.get(GenerageMapKeyUtils.createMapKey(tEstimateExceedShort.getFactoryCode(), tEstimateExceedShort.getProductCode()));
            if (productInfo == null) {
                failureNum++;
                tEstimateExceedShort.setId(-999L);
                String message = String.format(rowCountStr, i + 2) + notExistProductInfo;
                addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                continue;
            }
            //tEstimateExceedShort.setProSize(productInfo.getProSize());
            tEstimateExceedShort.setProductName(productInfo.getProductTypeCode());
            tEstimateExceedShort.setIsImport(Constant.TRUE);

            importList.add(tEstimateExceedShort);
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            successNum = importList.size();
            this.mergeByList(importList);
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 导入预计超欠产管理数据
     */
    @Override
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void importDataAsync(List<EstimateExceedShort> list, boolean updateSupport, Long importLogId, ImportLog importLog, Date beginTime, ServletRequestAttributes attributes) {
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
     * 查询对应分厂编号+物料编号的Map
     */
    private Map<String, MdmMaterialInfo> getMdmMaterialInfoMap(List<EstimateExceedShort> list) {
        if (CollectionUtils.isEmpty(list)) {
            return new HashMap<>();
        }
        List<String> factoryCodeList = list.stream().map(EstimateExceedShort::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> productCodeList = list.stream().map(EstimateExceedShort::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(factoryCodeList) && CollectionUtils.isEmpty(productCodeList)) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery(MdmMaterialInfo.class)
                .in(CollectionUtils.isNotEmpty(factoryCodeList), MdmMaterialInfo::getFactoryCode, factoryCodeList)
                .in(CollectionUtils.isNotEmpty(productCodeList), MdmMaterialInfo::getMaterialCode, productCodeList);
        return productInfoEntityMapper.selectList(wrapper).stream().filter(v -> v.getMaterialDesc() != null)
                .collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getMaterialCode()), Function.identity(), (v1, v2) -> v1));
    }

    /**
     * 修改预计超欠数
     */
    @Override
    public int updateExceedShortQty(EstimateExceedShort estimateExceedShort) {
        if (estimateExceedShort.getId() == null || estimateExceedShort.getExceedShortQty() == null) {
            return 0;
        }
        EstimateExceedShort updateShort = new EstimateExceedShort();
        updateShort.setId(estimateExceedShort.getId());
        updateShort.setExceedShortQty(estimateExceedShort.getExceedShortQty());
        return estimateExceedShortMapper.updateById(updateShort);
    }

    /**
     * 删除历史年月、分厂的数据，插入新数据
     */
    private void mergeByList(List<EstimateExceedShort> importList) {
        if (CollectionUtils.isEmpty(importList)) {
            return;
        }

        LambdaQueryWrapper<EstimateExceedShort> wrapper = LambdaWrapperBuilder.buildWrapperByFunction(importList,
                EstimateExceedShort::getYear,
                EstimateExceedShort::getMonth,
                EstimateExceedShort::getFactoryCode
        );
        estimateExceedShortMapper.delete(wrapper);

        baseDao.insertBatch(importList);
    }

    /**
     * 设置品号、寸口
     */
    public void setProductInfo(List<EstimateExceedShort> shortList) {
        if (CollectionUtils.isEmpty(shortList)) {
            return;
        }
        List<String> productCodeList = shortList.stream().map(EstimateExceedShort::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> factoryCodeList = shortList.stream().map(EstimateExceedShort::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(productCodeList)) {
            return;
        }

        List<MdmMaterialInfo> productInfoList = productInfoEntityMapper.selectList(Wrappers.lambdaQuery(MdmMaterialInfo.class)
                .in(MdmMaterialInfo::getMaterialCode, productCodeList)
                .in(CollectionUtils.isNotEmpty(factoryCodeList), MdmMaterialInfo::getFactoryCode, factoryCodeList));
        if (CollectionUtils.isEmpty(productInfoList)) {
            return;
        }
        Map<String, MdmMaterialInfo> infoMap = productInfoList.stream()
                .collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMaterialCode()), Function.identity(), (v1, v2) -> v1));
        for (EstimateExceedShort item : shortList) {
            MdmMaterialInfo mdmMaterialInfo = infoMap.getOrDefault(GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getProductCode()), new MdmMaterialInfo());
            //item.setProSize(mdmMaterialInfo.getProSize());
            item.setProductName(mdmMaterialInfo.getProductTypeCode());
        }
    }
}
