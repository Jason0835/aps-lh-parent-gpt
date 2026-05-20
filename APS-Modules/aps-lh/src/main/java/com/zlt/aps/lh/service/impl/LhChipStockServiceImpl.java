package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.lh.api.domain.entity.LhChipStock;
import com.zlt.aps.lh.mapper.LhChipStockMapper;
import com.zlt.aps.lh.service.ILhChipStockService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 芯片库存 Service实现
 *
 * @author APS Team
 * @date 2026-04-02
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
public class LhChipStockServiceImpl extends AbstractDocService<LhChipStock> implements ILhChipStockService {

    @Resource
    private LhChipStockMapper lhChipStockMapper;

    @Override
    public String[] getQueryFormulas() {
        return new String[0];
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }

    /**
     * 计算剩余可用量
     */
    private void calculateRemainStock(LhChipStock entity) {
        int stock = entity.getStockNum() != null ? entity.getStockNum() : 0;
        int finish = entity.getFinishQty() != null ? entity.getFinishQty() : 0;
        entity.setRemainStockNum(stock - finish);
    }

    /**
     * 检查库存量 >= 完成量
     */
    private boolean checkStockVsFinish(LhChipStock entity) {
        int stock = entity.getStockNum() != null ? entity.getStockNum() : 0;
        int finish = entity.getFinishQty() != null ? entity.getFinishQty() : 0;
        return stock >= finish;
    }

    /**
     * 导入时先统一关键字段，避免空分厂或前后空格导致唯一性校验与更新查询不一致。
     */
    private void normalizeImportKey(LhChipStock entity) {
        if (StringUtil.isBlank(entity.getFactoryCode())) {
            entity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        } else {
            entity.setFactoryCode(entity.getFactoryCode().trim());
        }
        if (StringUtil.isNotBlank(entity.getChipCode())) {
            entity.setChipCode(entity.getChipCode().trim());
        }
    }

    @Override
    public int updateFinishQty(String factoryCode, String chipCode, Integer finishQty) {
        LambdaUpdateWrapper<LhChipStock> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LhChipStock::getFactoryCode, factoryCode);
        updateWrapper.eq(LhChipStock::getChipCode, chipCode);
        updateWrapper.set(LhChipStock::getFinishQty, finishQty);
        return lhChipStockMapper.update(null, updateWrapper);
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<LhChipStock> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        int insertNum = 0;
        int updateNum = 0;
        List<LhChipStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhChipStock docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
//            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated,this.getCheckUniqueFields().toArray(new String[0]));
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhChipStock docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            normalizeImportKey(docEntity);
            calculateRemainStock(docEntity);

            if (!checkStockVsFinish(docEntity)) {
                failureNum++;
                //第{0}行，库存量不能小于完成量
                String message = I18nUtil.getMessage("ui.data.alert.lhChipStock.stockLessThanFinish");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            String checkResult = checkUnique(docEntity);
            if (UserConstants.UNIQUE.equals(checkResult)) {
                docEntity.setRowState(RowStateEnum.ADDED);
                importList.add(docEntity);
            } else {
                if (updateSupport) {
                    LambdaQueryWrapper<LhChipStock> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(LhChipStock::getFactoryCode, docEntity.getFactoryCode());
                    queryWrapper.eq(LhChipStock::getChipCode, docEntity.getChipCode());
                    queryWrapper.orderByAsc(LhChipStock::getId);
                    List<LhChipStock> exists = lhChipStockMapper.selectList(queryWrapper);
                    if (CollectionUtils.size(exists) > 1) {
                        failureNum++;
                        String message = I18nUtil.getMessage("ui.data.alert.lhChipStock.importDuplicateData");
                        ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                errorNum, String.format(message, errorNum), importErrorLogs);
                        continue;
                    }
                    LhChipStock exist = CollectionUtils.isEmpty(exists) ? null : exists.get(0);
                    if (exist != null) {
                        exist.setStockNum((exist.getStockNum() != null ? exist.getStockNum() : 0)
                                + (docEntity.getStockNum() != null ? docEntity.getStockNum() : 0));
                        exist.setFinishQty((exist.getFinishQty() != null ? exist.getFinishQty() : 0)
                                + (docEntity.getFinishQty() != null ? docEntity.getFinishQty() : 0));
                        calculateRemainStock(exist);
                        if (!checkStockVsFinish(exist)) {
                            failureNum++;
                            String message = I18nUtil.getMessage("ui.data.alert.lhChipStock.stockLessThanFinishAfterAdd");
                            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                    errorNum, String.format(message, errorNum), importErrorLogs);
                        } else {
                            lhChipStockMapper.updateById(exist);
                            updateNum++;
                        }
                    }
                } else {
                    failureNum++;
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                            String.format(uniqueMsg, errorNum), importErrorLogs);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(importList)) {
            insertNum = baseDao.saveBatch(importList);
        }

        successNum = insertNum + updateNum;

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 校验唯一性
     */
    @Override
    public String checkUnique(LhChipStock docEntityVO) {
        if (PubUtil.isEmpty(docEntityVO.getFactoryCode()) || PubUtil.isEmpty(docEntityVO.getChipCode())) {
            return UserConstants.UNIQUE;
        }
        QueryWrapper<LhChipStock> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(docEntityVO.getId()), "ID", docEntityVO.getId());
        queryWrapper.eq("FACTORY_CODE", docEntityVO.getFactoryCode().trim());
        queryWrapper.eq("CHIP_CODE", docEntityVO.getChipCode().trim());

        if (lhChipStockMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "chipCode");
    }

    /**
     * 合并保存 - 新增时检测到重复，将库存量和完成量累加到已有数据上
     */
    @Override
    public AjaxResult mergeSave(LhChipStock lhChipStock) {
        LambdaQueryWrapper<LhChipStock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LhChipStock::getFactoryCode, lhChipStock.getFactoryCode());
        queryWrapper.eq(LhChipStock::getChipCode, lhChipStock.getChipCode());
        queryWrapper.orderByAsc(LhChipStock::getId);
        List<LhChipStock> exists = lhChipStockMapper.selectList(queryWrapper);
        if (CollectionUtils.size(exists) > 1) {
            return AjaxResult.error("分厂、芯片编码对应多条库存数据，请先清理重复数据后再保存。");
        }
        LhChipStock exist = CollectionUtils.isEmpty(exists) ? null : exists.get(0);

        if (exist != null) {
            int newStockNum = (exist.getStockNum() != null ? exist.getStockNum() : 0)
                + (lhChipStock.getStockNum() != null ? lhChipStock.getStockNum() : 0);
            int newFinishQty = (exist.getFinishQty() != null ? exist.getFinishQty() : 0)
                + (lhChipStock.getFinishQty() != null ? lhChipStock.getFinishQty() : 0);

            if (newStockNum < newFinishQty) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.lhChipStock.stockLessThanFinishAfterMerge"));
            }

            exist.setStockNum(newStockNum);
            exist.setFinishQty(newFinishQty);
            exist.setDataVersion(lhChipStock.getDataVersion());
            exist.setRemark(lhChipStock.getRemark());

            int result = lhChipStockMapper.updateById(exist);
            return result > 0 ? AjaxResult.success() : AjaxResult.error();
        } else {
            int result = baseDao.save(lhChipStock);
            return result > 0 ? AjaxResult.success() : AjaxResult.error();
        }
    }

    @Override
    public void logicDeleteAndSaveBatch(String factoryCode, String dataSource, String updateBy, List<LhChipStock> insertList) {
        log.info("芯片库存同步-事务开始：逻辑删除分厂{}数据来源为{}的旧数据，待插入数量={}", factoryCode, dataSource, CollectionUtils.size(insertList));
        lhChipStockMapper.logicDeleteByFactoryCodeAndDataSource(factoryCode, dataSource, updateBy, new Date());
        log.info("芯片库存同步-逻辑删除完成，开始批量插入");
        if (CollectionUtils.isNotEmpty(insertList)) {
            int batchSize = 1000;
            for (int i = 0; i < insertList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, insertList.size());
                List<LhChipStock> subList = insertList.subList(i, end);
                baseDao.saveBatch(subList);
                log.info("芯片库存同步-插入批次：{}/{}, 本批数量={}", (i / batchSize + 1),
                        (insertList.size() + batchSize - 1) / batchSize, subList.size());
            }
        }
        log.info("芯片库存同步-事务完成：分厂{}，插入数量={}", factoryCode, CollectionUtils.size(insertList));
    }

    /**
     * 增量更新芯片库存完成量
     * 根据分厂编号+芯片编码匹配：已存在则累加完成量，不存在则新增记录
     *
     * @param factoryCode 分厂编号
     * @param list        待更新的芯片库存列表（需设置chipCode和finishQty）
     */
    @Override
    public void upsertFinishQty(String factoryCode, List<LhChipStock> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        List<String> chipCodes = list.stream()
                .map(LhChipStock::getChipCode)
                .filter(StringUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(chipCodes)) {
            return;
        }
        LambdaQueryWrapper<LhChipStock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(LhChipStock::getFactoryCode, factoryCode);
        queryWrapper.in(LhChipStock::getChipCode, chipCodes);
        List<LhChipStock> existingList = lhChipStockMapper.selectList(queryWrapper);
        Map<String, LhChipStock> existingMap = existingList.stream()
                .collect(Collectors.toMap(LhChipStock::getChipCode, e -> e, (v1, v2) -> v1));

        List<LhChipStock> insertList = new ArrayList<>();
        for (LhChipStock item : list) {
            if (StringUtil.isBlank(item.getChipCode())) {
                continue;
            }
            LhChipStock existing = existingMap.get(item.getChipCode());
            if (existing != null) {
                int newFinishQty = (existing.getFinishQty() != null ? existing.getFinishQty() : 0)
                        + (item.getFinishQty() != null ? item.getFinishQty() : 0);
                existing.setFinishQty(newFinishQty);
                lhChipStockMapper.updateById(existing);
                log.info("芯片库存增量更新：分厂={}, 芯片编码={}, 累加完成量={}, 更新后完成量={}",
                        factoryCode, item.getChipCode(), item.getFinishQty(), newFinishQty);
            } else {
                item.setFactoryCode(factoryCode);
                item.setDataSource(ApsConstant.DATA_SOURCE_MES);
                item.setCreateBy("MES");
                item.setUpdateBy("MES");
                item.setCreateTime(DateUtils.getNowDate());
                item.setUpdateTime(DateUtils.getNowDate());
                insertList.add(item);
                log.info("芯片库存新增：分厂={}, 芯片编码={}, 完成量={}", factoryCode, item.getChipCode(), item.getFinishQty());
            }
        }
        if (CollectionUtils.isNotEmpty(insertList)) {
            baseDao.saveBatch(insertList);
            log.info("芯片库存增量更新-批量插入完成：分厂={}, 新增数量={}", factoryCode, insertList.size());
        }
    }
}
