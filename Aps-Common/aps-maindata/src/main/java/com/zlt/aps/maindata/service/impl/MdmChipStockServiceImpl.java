package com.zlt.aps.maindata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.mdm.api.domain.entity.MdmChipStock;
import com.zlt.aps.maindata.mapper.MdmChipStockEntityMapper;
import com.zlt.aps.maindata.service.IMdmChipStockService;
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
public class MdmChipStockServiceImpl extends AbstractDocService<MdmChipStock> implements IMdmChipStockService {

    @Resource
    private MdmChipStockEntityMapper mdmChipStockEntityMapper;

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
    private void calculateRemainStock(MdmChipStock entity) {
        int stock = entity.getStockNum() != null ? entity.getStockNum() : 0;
        int finish = entity.getFinishQty() != null ? entity.getFinishQty() : 0;
        entity.setRemainStockNum(stock - finish);
    }

    /**
     * 检查库存量 >= 完成量
     */
    private boolean checkStockVsFinish(MdmChipStock entity) {
        int stock = entity.getStockNum() != null ? entity.getStockNum() : 0;
        int finish = entity.getFinishQty() != null ? entity.getFinishQty() : 0;
        return stock >= finish;
    }

    @Override
    public int updateFinishQty(String factoryCode, String chipCode, Integer finishQty) {
        LambdaQueryWrapper<MdmChipStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MdmChipStock::getFactoryCode, factoryCode);
        wrapper.eq(MdmChipStock::getChipCode, chipCode);
        MdmChipStock exist = mdmChipStockEntityMapper.selectOne(wrapper);
        if (exist == null) {
            return 0;
        }
        LambdaUpdateWrapper<MdmChipStock> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(MdmChipStock::getFactoryCode, factoryCode);
        updateWrapper.eq(MdmChipStock::getChipCode, chipCode);
        updateWrapper.set(MdmChipStock::getFinishQty, finishQty);
        int result = mdmChipStockEntityMapper.update(null, updateWrapper);
        // 重新计算剩余可用量会在查询的时候自动计算，这里只需要更新数据库
        return result;
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<MdmChipStock> list, boolean updateSupport, Long importLogId) {
        // 0.初始化
        int successNum = 0;
        int failureNum = 0;
        List<MdmChipStock> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        // 1.进行非空校验,Excel中数据重复校验
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MdmChipStock docEntity = list.get(i);
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
            MdmChipStock docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            // 计算剩余可用量
            calculateRemainStock(docEntity);

            // 检查库存量 >= 完成量
            if (!checkStockVsFinish(docEntity)) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.mdmChipStock.stockLessThanFinish");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            // 唯一性检查
            String checkResult = checkUnique(docEntity);
            if (UserConstants.UNIQUE.equals(checkResult)) {
                docEntity.setRowState(RowStateEnum.ADDED);
                if (StringUtil.isBlank(docEntity.getFactoryCode())) {
                    docEntity.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
                }
                importList.add(docEntity);
                successNum++;
            } else {
                // 已存在，根据updateSupport处理
                if (updateSupport) {
                    // 找到已存在的记录，累加库存量
                    QueryWrapper<MdmChipStock> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("FACTORY_CODE", docEntity.getFactoryCode());
                    queryWrapper.eq("CHIP_CODE", docEntity.getChipCode());
                    MdmChipStock exist = mdmChipStockEntityMapper.selectOne(queryWrapper);
                    if (exist != null) {
                        // 累加库存量
                        exist.setStockNum(exist.getStockNum() + (docEntity.getStockNum() != null ? docEntity.getStockNum() : 0));
                        // 重新计算剩余可用量
                        calculateRemainStock(exist);
                        // 再次检查
                        if (!checkStockVsFinish(exist)) {
                            failureNum++;
                            String message = I18nUtil.getMessage("ui.data.alert.mdmChipStock.stockLessThanFinishAfterAdd");
                            ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                                    errorNum, String.format(message, errorNum), importErrorLogs);
                        } else {
                            mdmChipStockEntityMapper.updateById(exist);
                            successNum++;
                        }
                    }
                } else {
                    // 不允许更新，直接报错
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

        // 返回提示信息及错误集合
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
    public String checkUnique(MdmChipStock docEntityVO) {
        // 唯一性判断依据: factoryCode + chipCode
        QueryWrapper<MdmChipStock> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(docEntityVO.getId()), "ID", docEntityVO.getId());
        queryWrapper.eq("FACTORY_CODE", docEntityVO.getFactoryCode().trim());
        queryWrapper.eq("CHIP_CODE", docEntityVO.getChipCode().trim());

        if (mdmChipStockEntityMapper.selectCount(queryWrapper) > 0) {
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
    public AjaxResult mergeSave(MdmChipStock mdmChipStock) {
        // 查找已存在的记录
        QueryWrapper<MdmChipStock> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", mdmChipStock.getFactoryCode());
        queryWrapper.eq("CHIP_CODE", mdmChipStock.getChipCode());
        MdmChipStock exist = mdmChipStockEntityMapper.selectOne(queryWrapper);

        if (exist != null) {
            // 累加库存量和完成量
            int newStockNum = (exist.getStockNum() != null ? exist.getStockNum() : 0)
                + (mdmChipStock.getStockNum() != null ? mdmChipStock.getStockNum() : 0);
            int newFinishQty = (exist.getFinishQty() != null ? exist.getFinishQty() : 0)
                + (mdmChipStock.getFinishQty() != null ? mdmChipStock.getFinishQty() : 0);

            // 检查库存量 >= 完成量
            if (newStockNum < newFinishQty) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.mdmChipStock.stockLessThanFinishAfterMerge"));
            }

            exist.setStockNum(newStockNum);
            exist.setFinishQty(newFinishQty);
            exist.setDataVersion(mdmChipStock.getDataVersion());
            exist.setRemark(mdmChipStock.getRemark());

            int result = mdmChipStockEntityMapper.updateById(exist);
            return result > 0 ? AjaxResult.success() : AjaxResult.error();
        } else {
            // 不存在，直接新增
            int result = baseDao.save(mdmChipStock);
            return result > 0 ? AjaxResult.success() : AjaxResult.error();
        }
    }
}
