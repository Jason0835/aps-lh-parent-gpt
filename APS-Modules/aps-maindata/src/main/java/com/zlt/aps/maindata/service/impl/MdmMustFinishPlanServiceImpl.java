package com.zlt.aps.maindata.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.datasource.service.BaseService;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.maindata.mapper.MdmCustomerInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.mapper.MdmMustFinishPlanEntityMapper;
import com.zlt.aps.maindata.service.IMdmMustFinishPlanService;
import com.zlt.aps.maindata.utils.LambdaWrapperBuilder;
import com.zlt.aps.monthplan.api.domain.entity.MdmCustomerInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMustFinishPlan;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zlt.common.utils.ImportExcelValidatedUtils.addImportErrorLog;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmMustFinishPlanServiceImpl.java
 * 描    述：MdmMustFinishPlanServiceImpl必须保证的客户月计划业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MdmMustFinishPlanServiceImpl extends BaseService<MdmMustFinishPlan> implements IMdmMustFinishPlanService {
    private final MdmMustFinishPlanEntityMapper mdmMustFinishPlanEntityMapper;
    private final MdmCustomerInfoEntityMapper mdmCustomerInfoEntityMapperm;
    private final MdmMaterialInfoEntityMapper mdmMaterialInfoEntityMapper;

    private final BaseDao baseDao;

    /**
     * 查询必须保证的客户月计划
     *
     * @param id 必须保证的客户月计划主键
     * @return 必须保证的客户月计划
     */
    @Override
    public MdmMustFinishPlan selectMdmMustFinishPlanById(Long id) {
        return mdmMustFinishPlanEntityMapper.selectById(id);
    }

    /**
     * 查询必须保证的客户月计划列表
     *
     * @param mdmMustFinishPlan 必须保证的客户月计划
     * @return 必须保证的客户月计划
     */
    @Override
    public List<MdmMustFinishPlan> selectMdmMustFinishPlanList(MdmMustFinishPlan mdmMustFinishPlan) {
        LambdaQueryWrapper<MdmMustFinishPlan> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(StringUtils.isNotBlank(mdmMustFinishPlan.getFactoryCode()), MdmMustFinishPlan::getFactoryCode, mdmMustFinishPlan.getFactoryCode());
        wrapper.eq(mdmMustFinishPlan.getYear() != null, MdmMustFinishPlan::getYear, mdmMustFinishPlan.getYear());
        wrapper.eq(mdmMustFinishPlan.getMonth() != null, MdmMustFinishPlan::getMonth, mdmMustFinishPlan.getMonth());
        wrapper.eq(StringUtils.isNotBlank(mdmMustFinishPlan.getCustomCode()), MdmMustFinishPlan::getCustomCode, mdmMustFinishPlan.getCustomCode());
        wrapper.eq(StringUtils.isNotBlank(mdmMustFinishPlan.getProductCode()), MdmMustFinishPlan::getProductCode, mdmMustFinishPlan.getProductCode());
        wrapper.eq(StringUtils.isNotBlank(mdmMustFinishPlan.getLocationType()), MdmMustFinishPlan::getLocationType, mdmMustFinishPlan.getLocationType());
        List<MdmMustFinishPlan> finishPlanList = mdmMustFinishPlanEntityMapper.selectList(wrapper);
        echoFieldList(finishPlanList);
        return finishPlanList;
    }

    /**
     * 回显字段：客户名称
     */
    private void echoFieldList(List<MdmMustFinishPlan> finishPlanList) {
        if (CollectionUtils.isEmpty(finishPlanList)) {
            return;
        }

        Map<String, String> infoMap = getMdmCustomerInfoMap(finishPlanList);
        finishPlanList.forEach(item -> item.setCustomName(infoMap.get(GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getCustomCode()))));
    }

    /**
     * 新增必须保证的客户月计划
     *
     * @param mdmMustFinishPlan 必须保证的客户月计划
     * @return 结果
     */
    @Override
    public int insertMdmMustFinishPlan(MdmMustFinishPlan mdmMustFinishPlan) {
        checkMdmMustFinishPlan(mdmMustFinishPlan);
        return mdmMustFinishPlanEntityMapper.insert(mdmMustFinishPlan);
    }

    /**
     * 校验客户编号、物料编号存在
     */
    private void checkMdmMustFinishPlan(MdmMustFinishPlan mdmMustFinishPlan) {
        // 校验客户编号
        Map<String, String> customerInfoMap = getMdmCustomerInfoMap(Collections.singletonList(mdmMustFinishPlan));
        if (!customerInfoMap.containsKey(GenerageMapKeyUtils.createMapKey(mdmMustFinishPlan.getFactoryCode(), mdmMustFinishPlan.getCustomCode()))) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.column.mustFinishPlan.notExist.customCode"));
        }

        // 校验物料编号
        Map<String, String> productInfoMap = getMdmMaterialInfoMap(Collections.singletonList(mdmMustFinishPlan));
        String desc = productInfoMap.get(GenerageMapKeyUtils.createMapKey(mdmMustFinishPlan.getFactoryCode(), mdmMustFinishPlan.getProductCode()));
        if (desc == null) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.column.mustFinishPlan.notExist.productInfo"));
        }
        mdmMustFinishPlan.setProductDesc(desc);
    }

    /**
     * 查询对应的【分厂编号+物料编号=规格描述】的Map
     */
    private Map<String, String> getMdmMaterialInfoMap(List<MdmMustFinishPlan> list) {
        List<String> factoryCodeList = list.stream().map(MdmMustFinishPlan::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> productCodeList = list.stream().map(MdmMustFinishPlan::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(factoryCodeList) && CollectionUtils.isEmpty(productCodeList)) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<MdmMaterialInfo> wrapper = Wrappers.lambdaQuery(MdmMaterialInfo.class)
                .in(CollectionUtils.isNotEmpty(factoryCodeList), MdmMaterialInfo::getFactoryCode, factoryCodeList)
                .in(CollectionUtils.isNotEmpty(productCodeList), MdmMaterialInfo::getMaterialCode, productCodeList);
        return mdmMaterialInfoEntityMapper.selectList(wrapper).stream().filter(v -> v.getMaterialDesc() != null)
                .collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getMaterialCode()), MdmMaterialInfo::getMaterialDesc, (v1, v2) -> v1));
    }

    /**
     * 查询对应的【分厂编号+客户编号=客户名称】的Map
     */
    private Map<String, String> getMdmCustomerInfoMap(List<MdmMustFinishPlan> list) {
        List<String> factoryCodeList = list.stream().map(MdmMustFinishPlan::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> customCodeList = list.stream().map(MdmMustFinishPlan::getCustomCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(factoryCodeList) && CollectionUtils.isEmpty(customCodeList)) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<MdmCustomerInfo> wrapper = Wrappers.lambdaQuery(MdmCustomerInfo.class)
                .in(CollectionUtils.isNotEmpty(factoryCodeList), MdmCustomerInfo::getFactoryCode, factoryCodeList)
                .in(CollectionUtils.isNotEmpty(customCodeList), MdmCustomerInfo::getCustomCode, customCodeList);
        return mdmCustomerInfoEntityMapperm.selectList(wrapper).stream().filter(v -> v.getCustomName() != null)
                .collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getCustomCode()), MdmCustomerInfo::getCustomName, (v1, v2) -> v1));
    }

    /**
     * 修改必须保证的客户月计划
     *
     * @param mdmMustFinishPlan 必须保证的客户月计划
     * @return 结果
     */
    @Override
    public int updateMdmMustFinishPlan(MdmMustFinishPlan mdmMustFinishPlan) {
        checkMdmMustFinishPlan(mdmMustFinishPlan);
        return mdmMustFinishPlanEntityMapper.updateById(mdmMustFinishPlan);
    }

    /**
     * 批量删除必须保证的客户月计划
     *
     * @param ids 需要删除的必须保证的客户月计划主键
     * @return 结果
     */
    @Override
    public int deleteMdmMustFinishPlanByIds(Long[] ids) {
        return mdmMustFinishPlanEntityMapper.deleteBatchIds(Arrays.asList(ids));
    }

    /**
     * 校验必须保证的客户月计划唯一性
     */
    @Override
    public String checkMdmMustFinishPlanUnique(MdmMustFinishPlan mdmMustFinishPlan) {
        if (mdmMustFinishPlan == null) {
            return UserConstants.NOT_UNIQUE;
        }
        LambdaQueryWrapper<MdmMustFinishPlan> wrapper = Wrappers.lambdaQuery();
        wrapper.ne(mdmMustFinishPlan.getId() != null, MdmMustFinishPlan::getId, mdmMustFinishPlan.getId());
        wrapper.eq(StringUtils.isNotBlank(mdmMustFinishPlan.getFactoryCode()), MdmMustFinishPlan::getFactoryCode, mdmMustFinishPlan.getFactoryCode());
        wrapper.eq(mdmMustFinishPlan.getYear() != null, MdmMustFinishPlan::getYear, mdmMustFinishPlan.getYear());
        wrapper.eq(mdmMustFinishPlan.getMonth() != null, MdmMustFinishPlan::getMonth, mdmMustFinishPlan.getMonth());
        wrapper.eq(StringUtils.isNotBlank(mdmMustFinishPlan.getCustomCode()), MdmMustFinishPlan::getCustomCode, mdmMustFinishPlan.getCustomCode());
        wrapper.eq(StringUtils.isNotBlank(mdmMustFinishPlan.getProductCode()), MdmMustFinishPlan::getProductCode, mdmMustFinishPlan.getProductCode());
        wrapper.eq(StringUtils.isNotBlank(mdmMustFinishPlan.getLocationType()), MdmMustFinishPlan::getLocationType, mdmMustFinishPlan.getLocationType());
        return mdmMustFinishPlanEntityMapper.selectCount(wrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 导入必须保证的客户月计划数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MdmMustFinishPlan> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MdmMustFinishPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String rowCountStr = I18nUtil.getMessage("ui.data.alert.rowcount");
        String customCodeMsg = I18nUtil.getMessage("ui.data.column.mustFinishPlan.notExist.customCode");
        String productInfoMsg = I18nUtil.getMessage("ui.data.column.mustFinishPlan.notExist.productInfo");
        String uniqueMsg = rowCountStr + I18nUtil.getMessage("ui.data.column.mdmMustFinishPlan.checkUnique");
        String repeat = I18nUtil.getMessage("ui.data.column.mdmMustFinishPlan.repeat");

        // 重复记录
        Function<MdmMustFinishPlan, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getYear(), v.getMonth(), v.getCustomCode(), v.getProductCode(), v.getLocationType());
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(keyFunc, Collectors.counting()));

        // 获取对应客户信息、物料信息
        Map<String, String> customerInfoMap = getMdmCustomerInfoMap(list);
        Map<String, String> productInfoMap = getMdmMaterialInfoMap(list);

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmMustFinishPlan mdmMustFinishPlan = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, mdmMustFinishPlan);
            ImportExcelValidatedUtils.validatedRepeat(list, mdmMustFinishPlan, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                mdmMustFinishPlan.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
                continue;
            }
            // 客户信息校验
            if (!customerInfoMap.containsKey(GenerageMapKeyUtils.createMapKey(mdmMustFinishPlan.getFactoryCode(), mdmMustFinishPlan.getCustomCode()))) {
                mdmMustFinishPlan.setId(-999L);
                failureNum++;
                String message = String.format(rowCountStr, i + 2) + customCodeMsg;
                addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                continue;
            }

            // 物料信息校验
            String productDesc = productInfoMap.get(GenerageMapKeyUtils.createMapKey(mdmMustFinishPlan.getFactoryCode(), mdmMustFinishPlan.getProductCode()));
            if (productDesc == null) {
                mdmMustFinishPlan.setId(-999L);
                failureNum++;
                String message = String.format(rowCountStr, i + 2) + productInfoMsg;
                addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                continue;
            } else {
                mdmMustFinishPlan.setProductDesc(productDesc);
            }

            // 重复记录校验
            String key = keyFunc.apply(mdmMustFinishPlan);
            Long count = groupMap.get(key);
            if (count != null && count > 1) {
                mdmMustFinishPlan.setId(-999L);
                failureNum++;
                String message = String.format(rowCountStr, i + 2) + repeat;
                addImportErrorLog(importLogId, errorNum, message, importErrorLogs);
                continue;
            }

            mdmMustFinishPlan.setBaseVale(null);
            importList.add(mdmMustFinishPlan);
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                this.mergeByList(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MdmMustFinishPlan mdmMustFinishPlan = list.get(i);
                    // 错误记录跳过
                    if (mdmMustFinishPlan.getId() != null && mdmMustFinishPlan.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMdmMustFinishPlanUnique(mdmMustFinishPlan);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMdmMustFinishPlan(mdmMustFinishPlan);
                    } else {
                        failureNum++;
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.REPEAT.getCode(), i + 2,
                                String.format(uniqueMsg, i + 2), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            ImportExcelValidatedUtils.addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 有则合并，无则更新
     */
    private void mergeByList(List<MdmMustFinishPlan> importList) {
        if (CollectionUtils.isEmpty(importList)) {
            return;
        }

        LambdaQueryWrapper<MdmMustFinishPlan> wrapper = LambdaWrapperBuilder.buildWrapperByFunction(importList, MdmMustFinishPlan::getFactoryCode,
                MdmMustFinishPlan::getYear,
                MdmMustFinishPlan::getMonth,
                MdmMustFinishPlan::getCustomCode,
                MdmMustFinishPlan::getProductCode,
                MdmMustFinishPlan::getLocationType);
        List<MdmMustFinishPlan> finishPlanList = mdmMustFinishPlanEntityMapper.selectList(wrapper);
        Function<MdmMustFinishPlan, String> keyFunc = v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getYear(), v.getMonth(), v.getCustomCode(), v.getProductCode(), v.getLocationType());
        Map<String, Long> oldMap = finishPlanList.stream().collect(Collectors.toMap(keyFunc, MdmMustFinishPlan::getId, (v1, v2) -> v1));

        List<MdmMustFinishPlan> updateList = new ArrayList<>();
        List<MdmMustFinishPlan> insertList = new ArrayList<>();
        for (MdmMustFinishPlan item : importList) {
            String key = keyFunc.apply(item);
            if (oldMap.containsKey(key)) {
                item.setId(oldMap.get(key));
                updateList.add(item);
            } else {
                insertList.add(item);
            }
        }

        baseDao.insertBatch(insertList);
        baseDao.updateBatch(updateList);
    }
}
