package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.maindata.mapper.ProductALevelMapper;
import com.zlt.aps.maindata.service.IProductALevelService;
import com.zlt.aps.maindata.utils.ScmListUtils;
import com.zlt.aps.monthplan.api.domain.entity.ProductALevel;
import com.zlt.aps.monthplan.api.domain.vo.ProductALevelVo;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：ProductALevelServiceImpl.java
 * 描    述：ProductALevelServiceImpl基础数据-SAP-OEE率业务层处理
 *
 * @author ZLT
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：ZLT
 * 修改内容：...
 * @date 2025-02-20
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class ProductALevelServiceImpl extends AbstractDocService<ProductALevel> implements IProductALevelService {

    @Autowired
    private ProductALevelMapper productALevelMapper;

    @Override
    protected String getDocTypeCode() {
        return "DOC0104";
    }

    @Override
    public List<ProductALevel> selectDocProductALevelList(ProductALevel productALevel) {
        return productALevelMapper.selectDocProductALevelList(productALevel);
    }

    @Override
    public List<ProductALevelVo> getProductALevelList(ProductALevel productALevel) {
        return productALevelMapper.getProductALevelList(productALevel);
    }

    @Override
    public String checkUnique(ProductALevel docEntityVO) {
        if (docEntityVO == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<ProductALevel> list = productALevelMapper.selectDocProductALevelList(docEntityVO);
        if (CollectionUtils.isNotEmpty(list)) {
            long iCount = list.stream().filter(x -> !x.getId().equals(docEntityVO.getId())).count();
            return iCount == 0 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public AjaxResult importData(List<ProductALevel> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<ProductALevel> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //读取缓存
        String rowcountStr = I18nUtil.getMessage("system.msg.rowcount");
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            ProductALevel docProductALevel = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docProductALevel);
            ImportExcelValidatedUtils.validatedRepeat(list, docProductALevel, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                docProductALevel.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                docProductALevel.setBaseVale(null);
            }
        }

        //重复校验
        Map<String, Long> groupMap = list.stream().filter(item -> item.getId() == null || !item.getId().equals(-999L))
                .collect(Collectors.groupingBy(item -> getUniqueKey(item), Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            ProductALevel docProductALevel = list.get(i);
            // 错误记录跳过
            if (docProductALevel.getId() != null && docProductALevel.getId().equals(-999L)) {
                continue;
            }

            // 表格内数据重复记录校验
            Long hasValue = groupMap.get(getUniqueKey(docProductALevel));
            if (hasValue > 1) {
                failureNum++;
                String message = String.format(rowcountStr, i + 2) + uniqueMsg;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.REPEAT.getCode(), i + 2,
                        message, importErrorLogs);
                continue;
            }

            successNum++;
            importList.add(docProductALevel);
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();

                //查询旧的数据
                LambdaQueryWrapper<ProductALevel> wrapper = new LambdaQueryWrapper<>();
                List<String> uniqueKeyList = importList.stream().map(item -> getUniqueKey(item)).collect(Collectors.toList());
                List<ProductALevel> oldList = productALevelMapper.selectByUniqueKeyList(uniqueKeyList);

                if (CollectionUtils.isNotEmpty(oldList)) {

                    //对比新旧子列表，构造出 新增集、更新集、删除集
                    List<ProductALevel> addList = importList.stream().filter(a -> a.getId() == null).collect(Collectors.toList());
                    List<ProductALevel> updateList = importList.stream().filter(a -> a.getId() != null).collect(Collectors.toList());
                    List<Long> newIdList = importList.stream().map(BaseEntity::getId).filter(Objects::nonNull).collect(Collectors.toList());
                    List<ProductALevel> deleteList = new ArrayList<>();
                    for (ProductALevel oldEntity : oldList) {
                        if (!newIdList.contains(oldEntity.getId())) {
                            deleteList.add(oldEntity);
                        }
                    }
                    //保存子表数据
                    if (CollectionUtils.isNotEmpty(addList)) {
                        baseDao.insertBatch(addList);
                    }
                    if (CollectionUtils.isNotEmpty(updateList)) {
                        baseDao.updateBatch(updateList);
                    }
                    if (CollectionUtils.isNotEmpty(deleteList)) {
                        baseDao.deleteBatch(deleteList);
                    }
                } else {
                    //旧子表数据不存在，直接新增子表数据
                    baseDao.insertBatch(importList);
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
     * 导入获取分组的维度
     */
    public String getUniqueKey(ProductALevel productALevel) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(productALevel.getFactoryCode());
        stringBuilder.append(productALevel.getProductTypeCode());
        stringBuilder.append(productALevel.getProductCode());
        return stringBuilder.toString();
    }

    /**
     * 不备货
     *
     * @param ids   选中的数据
     * @param year  年
     * @param month 月
     * @return 结果
     */
    @Override
    public AjaxResult noStockUp(List<Long> ids, Integer year, Integer month) {
        if (CollectionUtils.isNotEmpty(ids)) {
            List<List<Long>> splitList = ScmListUtils.getSplitList(ids, 1000);
            for (List<Long> list : splitList) {
                productALevelMapper.updateStockUpPlan(list, year, month);
            }
        } else {
            productALevelMapper.updateStockUpPlan(ids, year, month);
        }
        return AjaxResult.success();
    }
}
