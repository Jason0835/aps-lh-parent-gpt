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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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
        validateBusiness(entity);
        LambdaQueryWrapper<Cd15DepthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cd15DepthConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.eq(Cd15DepthConfig::getMachineQty, entity.getMachineQty());
        queryWrapper.eq(Cd15DepthConfig::getMachineRange, entity.getMachineRange());
        queryWrapper.ne(entity.getId() != null, Cd15DepthConfig::getId, entity.getId());
        return mapper.selectCount(queryWrapper) > 0 ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 校验配置规则的交叉情况。
     * 不同规则的范围不允许有交集，确保任意台数值最多只命中一条规则。
     */
    @Override
    public String checkRangeCross(Cd15DepthConfig entity) {
        LambdaQueryWrapper<Cd15DepthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cd15DepthConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.ne(entity.getId() != null, Cd15DepthConfig::getId, entity.getId());
        List<Cd15DepthConfig> existingList = mapper.selectList(queryWrapper);

        if (existingList.isEmpty()) {
            return UserConstants.UNIQUE;
        }

        boolean newIsEq = "EQ".equals(entity.getMachineRange());
        long[] newRange = calculateRange(entity.getMachineRange(), entity.getMachineQty());

        for (Cd15DepthConfig existing : existingList) {
            boolean existIsEq = "EQ".equals(existing.getMachineRange());
            long[] existingRange = calculateRange(existing.getMachineRange(), existing.getMachineQty());

            if (newRange[0] <= existingRange[1] && existingRange[0] <= newRange[1]) {
                if (newIsEq && existIsEq) {
                    if (entity.getMachineQty().equals(existing.getMachineQty())) {
                        return UserConstants.NOT_UNIQUE;
                    }
                    continue;
                }
                if (!newIsEq && !existIsEq) {
                    continue;
                }
                if (newIsEq) {
                    return UserConstants.NOT_UNIQUE;
                }
            }
        }

        return UserConstants.UNIQUE;
    }

    /**
     * 将规则转换为整数范围区间 [start, end]，EQ 视为零宽度点 [qty, qty]。
     */
    private long[] calculateRange(String machineRange, Integer machineQty) {
        int qty = machineQty != null ? machineQty : 0;
        switch (machineRange) {
            case "LT": return new long[]{0, qty - 1L};
            case "LE": return new long[]{0, qty};
            case "EQ": return new long[]{qty, qty};
            case "GE": return new long[]{qty, Integer.MAX_VALUE};
            case "GT": return new long[]{qty + 1L, Integer.MAX_VALUE};
            default: return new long[]{0, 0};
        }
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
                validateBusiness(item);
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
            if (item.getId() != null && item.getId() == -999L) continue;
            Cd15DepthConfig exist = getExist(item);
            if (exist == null) {
                item.setRowState(RowStateEnum.ADDED);
                insertList.add(item);
            } else if (updateSupport) {
                exist.setMachineQty(item.getMachineQty());
                exist.setMachineRange(item.getMachineRange());
                exist.setDepthClassQty(item.getDepthClassQty());
                exist.setRemark(item.getRemark());
                mapper.updateById(exist);
                successNum++;
            } else {
                failNum++;
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, rowNum, String.format(uniqueMsg, rowNum), errorList);
            }
        }

        if (PubUtil.isEmpty(insertList) && successNum == 0)
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failNum, errorList);
        if (CollectionUtils.isNotEmpty(insertList)) successNum += baseDao.saveBatch(insertList);
        if (failNum > 0) return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failNum, errorList);
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    private Cd15DepthConfig getExist(Cd15DepthConfig entity) {
        LambdaQueryWrapper<Cd15DepthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cd15DepthConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.eq(Cd15DepthConfig::getMachineQty, entity.getMachineQty());
        queryWrapper.eq(Cd15DepthConfig::getMachineRange, entity.getMachineRange());
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
        return Arrays.asList("factoryCode", "machineQty", "machineRange");
    }

    /**
     * 业务校验：机台数 >= 0；备库班数 > 0；机台范围不能为空
     */
    private void validateBusiness(Cd15DepthConfig entity) {
        if (entity == null) {
            return;
        }
        Integer machineQty = entity.getMachineQty();
        if (machineQty == null || machineQty < 0) {
            throw new IllegalArgumentException("供成型机台数必须为大于等于0的整数");
        }
        if (entity.getDepthClassQty() == null || entity.getDepthClassQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("备库班数必须为大于0的数字");
        }
        if (entity.getMachineRange() == null || entity.getMachineRange().trim().isEmpty()) {
            throw new IllegalArgumentException("机台范围不能为空");
        }
    }
}
