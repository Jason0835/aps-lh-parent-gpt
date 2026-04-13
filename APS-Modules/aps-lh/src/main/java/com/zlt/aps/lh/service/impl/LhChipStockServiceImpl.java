package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
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

    @Override
    public int updateFinishQty(String factoryCode, String chipCode, Integer finishQty) {
        LambdaQueryWrapper<LhChipStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LhChipStock::getFactoryCode, factoryCode);
        wrapper.eq(LhChipStock::getChipCode, chipCode);
        LhChipStock exist = lhChipStockMapper.selectOne(wrapper);
        if (exist == null) {
            return 0;
        }
        LambdaUpdateWrapper<LhChipStock> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(LhChipStock::getFactoryCode, factoryCode);
        updateWrapper.eq(LhChipStock::getChipCode, chipCode);
        updateWrapper.set(LhChipStock::getFinishQty, finishQty);
        int result = lhChipStockMapper.update(null, updateWrapper);
        return result;
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<LhChipStock> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<LhChipStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhChipStock docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
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

            calculateRemainStock(docEntity);

            if (!checkStockVsFinish(docEntity)) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.lhChipStock.stockLessThanFinish");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            String checkResult = checkUnique(docEntity);
            if (UserConstants.UNIQUE.equals(checkResult)) {
                docEntity.setRowState(RowStateEnum.ADDED);
                if (StringUtil.isBlank(docEntity.getFactoryCode())) {
                    docEntity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
                }
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    QueryWrapper<LhChipStock> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("FACTORY_CODE", docEntity.getFactoryCode());
                    queryWrapper.eq("CHIP_CODE", docEntity.getChipCode());
                    LhChipStock exist = lhChipStockMapper.selectOne(queryWrapper);
                    if (exist != null) {
                        exist.setStockNum(exist.getStockNum() + (docEntity.getStockNum() != null ? docEntity.getStockNum() : 0));
                        exist.setFinishQty(exist.getFinishQty() + (docEntity.getFinishQty() != null ? docEntity.getFinishQty() : 0));
                        calculateRemainStock(exist);
                        if (!checkStockVsFinish(exist)) {
                            failureNum++;
                            String message = I18nUtil.getMessage("ui.data.alert.lhChipStock.stockLessThanFinishAfterAdd");
                            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                    errorNum, String.format(message, errorNum), importErrorLogs);
                        } else {
                            lhChipStockMapper.updateById(exist);
                            successNum++;
                        }
                    }
                } else {
                    failureNum++;
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                            String.format(uniqueMsg, errorNum), importErrorLogs);
                }
            }
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        successNum = baseDao.saveBatch(importList);

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
        QueryWrapper<LhChipStock> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", lhChipStock.getFactoryCode());
        queryWrapper.eq("CHIP_CODE", lhChipStock.getChipCode());
        LhChipStock exist = lhChipStockMapper.selectOne(queryWrapper);

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
}
