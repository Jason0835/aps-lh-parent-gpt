package com.zlt.aps.monthplan.factory.service.impl;

import com.google.common.collect.Lists;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.datasource.service.BaseService;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.monthplan.api.domain.entity.MdmMonthSurplus;
import com.zlt.aps.monthplan.api.domain.entity.MpMonthPlanProdFinal;
import com.zlt.aps.monthplan.factory.mapper.MpMonthPlanProdFinalEntityMapper;
import com.zlt.aps.monthplan.factory.service.IMpMonthPlanProdFinalService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpMonthPlanProdFinalServiceImpl.java
 * 描    述：MpMonthPlanProdFinalServiceImpl工厂月生产计划-最终排产计划定稿业务层处理
 *
 * @author yelq
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：yelq
 * 修改内容：...
 * @date 2025-12-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpMonthPlanProdFinalServiceImpl extends BaseService<MpMonthPlanProdFinal> implements IMpMonthPlanProdFinalService {
    private final MpMonthPlanProdFinalEntityMapper mpMonthPlanProdFinalEntityMapper;


    /**
     * 查询工厂月生产计划-最终排产计划定稿
     *
     * @param id 工厂月生产计划-最终排产计划定稿主键
     * @return 工厂月生产计划-最终排产计划定稿
     */
    @Override
    public MpMonthPlanProdFinal selectMpMonthPlanProdFinalById(Integer id) {
        return mpMonthPlanProdFinalEntityMapper.selectMpMonthPlanProdFinalById(id);
    }

    /**
     * 查询工厂月生产计划-最终排产计划定稿列表
     *
     * @param mpMonthPlanProdFinal 工厂月生产计划-最终排产计划定稿
     * @return 工厂月生产计划-最终排产计划定稿
     */
    @Override
    public List<MpMonthPlanProdFinal> selectMpMonthPlanProdFinalList(MpMonthPlanProdFinal mpMonthPlanProdFinal) {
        return mpMonthPlanProdFinalEntityMapper.selectMpMonthPlanProdFinalList(mpMonthPlanProdFinal);
    }

    /**
     * 批量查询工厂月生产计划-最终排产计划定稿列表
     *
     * @param ids 需要查询的数据主键集合
     * @return 工厂月生产计划-最终排产计划定稿集合
     */
    @Override
    public List<MpMonthPlanProdFinal> selectMpMonthPlanProdFinalByIds(List<Integer> ids) {
        return super.executeSelectIn(
                mpMonthPlanProdFinalEntityMapper::selectMpMonthPlanProdFinalByIds
                , ids
        );
    }


    /**
     * 新增工厂月生产计划-最终排产计划定稿
     *
     * @param mpMonthPlanProdFinal 工厂月生产计划-最终排产计划定稿
     * @return 结果
     */
    @Override
    public int insertMpMonthPlanProdFinal(MpMonthPlanProdFinal mpMonthPlanProdFinal) {
        mpMonthPlanProdFinal.setBaseVale(null);
        return mpMonthPlanProdFinalEntityMapper.insert(mpMonthPlanProdFinal);
    }

    /**
     * 修改工厂月生产计划-最终排产计划定稿
     *
     * @param mpMonthPlanProdFinal 工厂月生产计划-最终排产计划定稿
     * @return 结果
     */
    @Override
    public int updateMpMonthPlanProdFinal(MpMonthPlanProdFinal mpMonthPlanProdFinal) {
        mpMonthPlanProdFinal.setBaseVale(mpMonthPlanProdFinal.getId());
        return mpMonthPlanProdFinalEntityMapper.update(mpMonthPlanProdFinal);
    }

    /**
     * 批量删除工厂月生产计划-最终排产计划定稿
     *
     * @param ids 需要删除的工厂月生产计划-最终排产计划定稿主键
     * @return 结果
     */
    @Override
    public int deleteMpMonthPlanProdFinalByIds(Integer[] ids) {
        return mpMonthPlanProdFinalEntityMapper.deleteMpMonthPlanProdFinalByIds(ids);
    }

    /**
     * 批量删除工厂月生产计划-最终排产计划定稿
     *
     * @param ids 需要删除的工厂月生产计划-最终排产计划定稿主键
     * @return 结果
     */
    @Override
    public int deleteMpMonthPlanProdFinalByIds(List<Integer> ids) {
        Integer[] arrayids = ids.toArray(new Integer[0]);

        return this.deleteMpMonthPlanProdFinalByIds(arrayids);
    }

    /**
     * 删除工厂月生产计划-最终排产计划定稿信息
     *
     * @param id 工厂月生产计划-最终排产计划定稿主键
     * @return 结果
     */
    @Override
    public int deleteMpMonthPlanProdFinalById(Integer id) {
        return mpMonthPlanProdFinalEntityMapper.deleteMpMonthPlanProdFinalById(id);
    }

    @Override
    public void insertBatchData(Collection<MpMonthPlanProdFinal> dataList) {

        this.insertBatchData(dataList, MpMonthPlanProdFinalEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MpMonthPlanProdFinal> dataList) {

        this.updateBatchData(dataList, MpMonthPlanProdFinalEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MpMonthPlanProdFinal> list) {
        this.mergerIntoBatchData(list, MpMonthPlanProdFinalEntityMapper.class);
    }

    /**
     * 校验工厂月生产计划-最终排产计划定稿唯一性
     */
    @Override
    public String checkMpMonthPlanProdFinalUnique(MpMonthPlanProdFinal mpMonthPlanProdFinal) {
        if (mpMonthPlanProdFinal == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MpMonthPlanProdFinal> list = mpMonthPlanProdFinalEntityMapper.selectMpMonthPlanProdFinalList(mpMonthPlanProdFinal);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x -> !x.getId().equals(mpMonthPlanProdFinal.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入工厂月生产计划-最终排产计划定稿数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MpMonthPlanProdFinal> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MpMonthPlanProdFinal> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MpMonthPlanProdFinal mpMonthPlanProdFinal = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, mpMonthPlanProdFinal);
            ImportExcelValidatedUtils.validatedRepeat(list, mpMonthPlanProdFinal, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                mpMonthPlanProdFinal.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                mpMonthPlanProdFinal.setBaseVale(null);
                importList.add(mpMonthPlanProdFinal);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                mpMonthPlanProdFinalEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MpMonthPlanProdFinal mpMonthPlanProdFinal = list.get(i);
                    // 错误记录跳过
                    if (mpMonthPlanProdFinal.getId() != null && mpMonthPlanProdFinal.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMpMonthPlanProdFinalUnique(mpMonthPlanProdFinal);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMpMonthPlanProdFinal(mpMonthPlanProdFinal);
                    } else {
                        failureNum++;
                        //TODO:此处需手动填写唯一校验失败国际化信息
                        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");
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

    @Override
    public Map<String, Long> calculateMonthSurplus(String requireVersion) {
        // 获取当前年月
        YearMonth currentYearMonth = YearMonth.now();
        String yearMonth = String.format("%s%02d", currentYearMonth.getYear(), currentYearMonth.getMonthValue());
        MpMonthPlanProdFinal param = new MpMonthPlanProdFinal();
        param.setYearMonth(Integer.valueOf(yearMonth));
        param.setIsDelete(ApsConstant.APS_YES_NO_0);
        List<MpMonthPlanProdFinal> factoryMonthPlanProdFinals = this.mpMonthPlanProdFinalEntityMapper.selectMpMonthPlanProdFinalList(param);
        if (CollectionUtils.isEmpty(factoryMonthPlanProdFinals)) {
            return Collections.emptyMap();
        }
        List<MdmMonthSurplus> result = Lists.newArrayList();
        Map<String, List<MpMonthPlanProdFinal>> groupByMaterialCode = this.getGroupMonthProdFinalPlanByMaterialCode(factoryMonthPlanProdFinals);
        groupByMaterialCode.forEach((key, value) -> {
            MdmMonthSurplus entity = new MdmMonthSurplus();
            entity.setBaseVale(null);
            entity.setIsDelete(ApsConstant.APS_YES_NO_0);
            long planSurplusQty = value.stream().mapToLong(MpMonthPlanProdFinal::getTotalQty).sum();
            entity.setPlanSurplusQty(planSurplusQty);
            entity.setFactoryCode(value.get(0).getFactoryCode());
            entity.setYear(value.get(0).getYear());
            entity.setMonth(value.get(0).getMonth());
            entity.setRequireVersion(requireVersion);
            entity.setProductTypeCode(value.get(0).getProductTypeCode());
            entity.setBrand(value.get(0).getBrand());
            entity.setMaterialCode(value.get(0).getMaterialCode());
            entity.setMaterialDesc(value.get(0).getMaterialDesc());
            entity.setStructureName(value.get(0).getStructureName());
            result.add(entity);
        });
        return calculateMonthSurplus(result);
    }

    private Map<String, Long> calculateMonthSurplus(List<MdmMonthSurplus> monthSurpluses) {
        if (CollectionUtils.isEmpty(monthSurpluses)) {
            return Collections.emptyMap();
        }
        return monthSurpluses.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        MdmMonthSurplus::getGroupKey,
                        Collectors.summingLong(MdmMonthSurplus::getPlanSurplusQty)
                ));
    }

    private Map<String, List<MpMonthPlanProdFinal>> getGroupMonthProdFinalPlanByMaterialCode(List<MpMonthPlanProdFinal> factoryMonthPlanProdFinals) {
        if (CollectionUtils.isEmpty(factoryMonthPlanProdFinals)) {
            return Collections.emptyMap();
        }
        return factoryMonthPlanProdFinals
                .parallelStream()
                .filter(Objects::nonNull)
                .filter(item -> StringUtils.isNotBlank(item.getMaterialCode()))
                .collect(Collectors.groupingByConcurrent(
                        MpMonthPlanProdFinal::getMaterialCode,
                        Collectors.toCollection(ArrayList::new)
                ));
    }
}
