package com.zlt.aps.cd15.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd15.api.domain.entity.Cd15DepthConfig;
import com.zlt.aps.cd15.mapper.Cd15DepthConfigMapper;
import com.zlt.aps.cd15.service.ICd15DepthConfigService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.sysdef.domain.SysDocType;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 斜裁备库班数与供成型机数配置业务实现。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd15DepthConfigServiceImpl extends AbstractDocService<Cd15DepthConfig> implements ICd15DepthConfigService {

    @Resource
    private Cd15DepthConfigMapper mapper;

    @Override
    protected String getDocTypeCode() {
        return "CD15_DEPTH_CONFIG";
    }

    @Override
    public String checkUnique(Cd15DepthConfig entity) {
        this.validateBusiness(entity);
        LambdaQueryWrapper<Cd15DepthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cd15DepthConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.eq(Cd15DepthConfig::getMinMachineQty, entity.getMinMachineQty());
        queryWrapper.ne(entity.getId() != null, Cd15DepthConfig::getId, entity.getId());
        return mapper.selectCount(queryWrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 校验同一工厂的有效区间从1开始、相邻连续且互不重叠。
     * 上限为空表示无上限，因此只允许出现在最后一行。
     */
    @Override
    public String checkRangeCross(Cd15DepthConfig entity) {
        if (!this.isValidRange(entity)) {
            return UserConstants.NOT_UNIQUE;
        }
        LambdaQueryWrapper<Cd15DepthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cd15DepthConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.ne(entity.getId() != null, Cd15DepthConfig::getId, entity.getId());
        List<Cd15DepthConfig> configs = new ArrayList<>(mapper.selectList(queryWrapper));
        configs.add(entity);
        configs.sort(Comparator.comparing(Cd15DepthConfig::getMinMachineQty));
        return this.isContinuous(configs) ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
    }

    /** 校验排序后的整组区间。 */
    private boolean isContinuous(List<Cd15DepthConfig> configs) {
        if (CollectionUtils.isEmpty(configs)
                || !Integer.valueOf(1).equals(configs.get(0).getMinMachineQty())) {
            return false;
        }
        for (int index = 0; index < configs.size(); index++) {
            Cd15DepthConfig current = configs.get(index);
            if (!this.isValidRange(current)) {
                return false;
            }
            if (index < configs.size() - 1) {
                Cd15DepthConfig next = configs.get(index + 1);
                if (current.getMaxMachineQty() == null
                        || current.getMaxMachineQty() + 1 != next.getMinMachineQty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 校验单个区间和备库班数的基础值。 */
    private boolean isValidRange(Cd15DepthConfig entity) {
        if (entity == null || entity.getFactoryCode() == null
                || entity.getFactoryCode().trim().isEmpty()
                || entity.getMinMachineQty() == null
                || entity.getMinMachineQty() <= 0) {
            return false;
        }
        if (entity.getMaxMachineQty() != null
                && entity.getMaxMachineQty() < entity.getMinMachineQty()) {
            return false;
        }
        return entity.getDepthClassQty() != null
                && entity.getDepthClassQty().signum() > 0
                && entity.getDepthClassQty().stripTrailingZeros().scale() <= 2;
    }

    @Override
    public AjaxResult importData(List<Cd15DepthConfig> list, boolean updateSupport, Long importLogId) {
        int successNum = 0, failNum = 0;
        List<Cd15DepthConfig> insertList = new ArrayList<>();
        List<ImportErrorLog> errorList = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int rowNum = i + 2;
            Cd15DepthConfig item = list.get(i);
            List<ImportErrorLog> errors = ImportExcelValidatedUtils.validated(importLogId, rowNum, item);
            ImportExcelValidatedUtils.validatedRepeat(list, item, i, 2, importLogId, errors);
            try {
                this.validateBusiness(item);
            } catch (IllegalArgumentException ex) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(), rowNum, ex.getMessage(), errors);
            }
            if (CollectionUtils.isNotEmpty(errors)) {
                failNum++;
                item.setId(-999L);
                errorList.addAll(errors);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int rowNum = i + 2;
            Cd15DepthConfig item = list.get(i);
            if (Long.valueOf(-999L).equals(item.getId())) {
                continue;
            }
            Cd15DepthConfig exist = this.getExist(item);
            if (exist == null) {
                item.setRowState(RowStateEnum.ADDED);
                insertList.add(item);
            } else if (updateSupport) {
                exist.setMinMachineQty(item.getMinMachineQty());
                exist.setMaxMachineQty(item.getMaxMachineQty());
                exist.setDepthClassQty(item.getDepthClassQty());
                exist.setRemark(item.getRemark());
                mapper.updateById(exist);
                successNum++;
            } else {
                failNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, rowNum,
                        String.format(uniqueMsg, rowNum), errorList);
            }
        }

        if (PubUtil.isEmpty(insertList) && successNum == 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failNum, errorList);
        }
        if (CollectionUtils.isNotEmpty(insertList)) {
            successNum += baseDao.saveBatch(insertList);
        }
        if (failNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failNum, errorList);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    private Cd15DepthConfig getExist(Cd15DepthConfig entity) {
        LambdaQueryWrapper<Cd15DepthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cd15DepthConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.eq(Cd15DepthConfig::getMinMachineQty, entity.getMinMachineQty());
        return mapper.selectOne(queryWrapper);
    }

    @Override
    protected SysDocType getSysDocType() {
        SysDocType sysDocType = new SysDocType();
        sysDocType.setDocTypeCode("CD15_DEPTH_CONFIG");
        return sysDocType;
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "minMachineQty");
    }

    /** 保存和导入时校验单条业务数据。 */
    private void validateBusiness(Cd15DepthConfig entity) {
        if (!this.isValidRange(entity)) {
            throw new IllegalArgumentException(
                    I18nUtil.getMessage("ui.data.column.cd15DepthConfig.invalidRange"));
        }
    }
}
