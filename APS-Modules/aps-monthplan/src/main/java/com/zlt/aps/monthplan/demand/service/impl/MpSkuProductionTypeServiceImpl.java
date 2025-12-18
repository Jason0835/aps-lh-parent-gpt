package com.zlt.aps.monthplan.demand.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.MpSkuProductionType;
import com.zlt.aps.monthplan.demand.mapper.MpSkuProductionTypeEntityMapper;
import com.zlt.aps.monthplan.demand.service.IMpSkuProductionTypeService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.ruoyi.common.datasource.service.BaseService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.collections4.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import lombok.extern.slf4j.Slf4j;

import com.zlt.common.utils.ImportExcelValidatedUtils;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MpSkuProductionTypeServiceImpl.java
 * 描    述：MpSkuProductionTypeServiceImplSKU排产分类业务层处理
 *@author yelq
 *@date 2025-12-17
 *@version 1.0
 *
 *  修改记录：
 *     修改时间：...
 *     修 改 人：yelq
 *     修改内容：...
 */
@Slf4j
@Service
public class MpSkuProductionTypeServiceImpl extends BaseService<MpSkuProductionType>  implements IMpSkuProductionTypeService
{
    @Autowired
    private MpSkuProductionTypeEntityMapper mpSkuProductionTypeEntityMapper;



    /**
     * 查询SKU排产分类
     * 
     * @param id SKU排产分类主键
     * @return SKU排产分类
     */
    @Override
    public MpSkuProductionType selectMpSkuProductionTypeById(Long id)
    {
        return mpSkuProductionTypeEntityMapper.selectMpSkuProductionTypeById(id);
    }

    /**
     * 查询SKU排产分类列表
     * 
     * @param mpSkuProductionType SKU排产分类
     * @return SKU排产分类
     */
    @Override
    public List<MpSkuProductionType> selectMpSkuProductionTypeList(MpSkuProductionType mpSkuProductionType)
    {
        return mpSkuProductionTypeEntityMapper.selectMpSkuProductionTypeList(mpSkuProductionType);
    }

    /**
     * 批量查询SKU排产分类列表
     *
     * @param ids 需要查询的数据主键集合
     * @return SKU排产分类集合
     */
    @Override
    public List<MpSkuProductionType> selectMpSkuProductionTypeByIds(List<Long> ids)
    {
        return super.executeSelectIn(
                    mpSkuProductionTypeEntityMapper::selectMpSkuProductionTypeByIds
                    ,ids
        );
    }


    /**
     * 新增SKU排产分类
     * 
     * @param mpSkuProductionType SKU排产分类
     * @return 结果
     */
    @Override
    public int insertMpSkuProductionType(MpSkuProductionType mpSkuProductionType)
    {
        mpSkuProductionType.setBaseVale(null);
        return mpSkuProductionTypeEntityMapper.insert(mpSkuProductionType);
    }

    /**
     * 修改SKU排产分类
     * 
     * @param mpSkuProductionType SKU排产分类
     * @return 结果
     */
    @Override
    public int updateMpSkuProductionType(MpSkuProductionType mpSkuProductionType)
    {
        mpSkuProductionType.setBaseVale(mpSkuProductionType.getId());
        return mpSkuProductionTypeEntityMapper.update(mpSkuProductionType);
    }

    /**
     * 批量删除SKU排产分类
     * 
     * @param ids 需要删除的SKU排产分类主键
     * @return 结果
     */
    @Override
    public int deleteMpSkuProductionTypeByIds(Long[] ids)
    {
        return mpSkuProductionTypeEntityMapper.deleteMpSkuProductionTypeByIds(ids);
    }

    /**
     * 批量删除SKU排产分类
     *
     * @param ids 需要删除的SKU排产分类主键
     * @return 结果
     */
    @Override
    public int deleteMpSkuProductionTypeByIds(List<Long> ids)
    {
        Long[] arrayids = ids.toArray(new Long[0]);

        return this.deleteMpSkuProductionTypeByIds(arrayids);
    }

    /**
     * 删除SKU排产分类信息
     * 
     * @param id SKU排产分类主键
     * @return 结果
     */
    @Override
    public int deleteMpSkuProductionTypeById(Long id)
    {
        return mpSkuProductionTypeEntityMapper.deleteMpSkuProductionTypeById(id);
    }

    @Override
    public void insertBatchData(Collection<MpSkuProductionType> dataList) {

        this.insertBatchData(dataList, MpSkuProductionTypeEntityMapper.class);
    }

    @Override
    public void updateBatchData(Collection<MpSkuProductionType> dataList) {

        this.updateBatchData(dataList, MpSkuProductionTypeEntityMapper.class);
    }

    @Override
    public void mergerIntoBatchData(List<MpSkuProductionType> list) {
        this.mergerIntoBatchData(list, MpSkuProductionTypeEntityMapper.class);
    }

    /**
     * 校验SKU排产分类唯一性
     */
    @Override
    public String checkMpSkuProductionTypeUnique(MpSkuProductionType mpSkuProductionType) {
        if (mpSkuProductionType == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MpSkuProductionType> list = mpSkuProductionTypeEntityMapper.selectMpSkuProductionTypeList(mpSkuProductionType);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x->!x.getId().equals(mpSkuProductionType.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }
    /**
     * 导入SKU排产分类数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MpSkuProductionType> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MpSkuProductionType> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MpSkuProductionType mpSkuProductionType = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, mpSkuProductionType);
            ImportExcelValidatedUtils.validatedRepeat(list,mpSkuProductionType,i,2,importLogId,validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                mpSkuProductionType.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mpSkuProductionType.setBaseVale(null);
                importList.add(mpSkuProductionType);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mpSkuProductionTypeEntityMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MpSkuProductionType mpSkuProductionType = list.get(i);
                    // 错误记录跳过
                    if (mpSkuProductionType.getId() != null && mpSkuProductionType.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMpSkuProductionTypeUnique(mpSkuProductionType);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMpSkuProductionType(mpSkuProductionType);
                    } else {
                        failureNum++;
                        //TODO:此处需手动填写唯一校验失败国际化信息
                        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.REPEAT.getCode(),i + 2,
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
    public Map<String, String> skuToProductionType() {
        MpSkuProductionType param = new MpSkuProductionType();
        param.setIsDelete(YesOrNoEnum.NO.getValue());
        List<MpSkuProductionType>   list = this.mpSkuProductionTypeEntityMapper.selectMpSkuProductionTypeList(param);
        if(CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        return list.stream()
            .filter(Objects::nonNull)
            .filter(skuProductionType -> StringUtils.isNotBlank(skuProductionType.getMaterialCode()))
            .collect(Collectors.toMap(MpSkuProductionType::getMaterialCode,
                MpSkuProductionType::getProductionType,
                (existing, replacement) -> existing
            ));
    }
}
