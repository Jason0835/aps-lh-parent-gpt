package com.zlt.aps.cd90.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.RowStateEnum;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cd90.api.domain.entity.Cd90DepthConfig;
import com.zlt.aps.cd90.mapper.Cd90DepthConfigMapper;
import com.zlt.aps.cd90.service.ICd90DepthConfigService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** 直裁备库班数连续区间配置业务处理。 */
@Service
@Transactional(rollbackFor = Exception.class)
public class Cd90DepthConfigServiceImpl extends AbstractDocService<Cd90DepthConfig>
        implements ICd90DepthConfigService {

    @Resource
    private Cd90DepthConfigMapper mapper;

    @Override
    protected String getDocTypeCode() {
        return "CD90_DEPTH_CONFIG";
    }

    /** 校验同一工厂下区间起点唯一。 */
    @Override
    public String checkUnique(Cd90DepthConfig entity) {
        this.validateBusiness(entity);
        LambdaQueryWrapper<Cd90DepthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cd90DepthConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.eq(Cd90DepthConfig::getMinMachineQty, entity.getMinMachineQty());
        queryWrapper.ne(entity.getId() != null, Cd90DepthConfig::getId, entity.getId());
        return mapper.selectCount(queryWrapper) > 0
                ? UserConstants.NOT_UNIQUE : UserConstants.UNIQUE;
    }

    /**
     * 校验同一工厂的有效区间从1开始、相邻连续且互不重叠。
     * 上限为空表示无上限，因此只允许出现在最后一行。
     */
    @Override
    public String checkRangeCross(Cd90DepthConfig entity) {
        if (!this.isValidRange(entity)) {
            return UserConstants.NOT_UNIQUE;
        }
        LambdaQueryWrapper<Cd90DepthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cd90DepthConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.ne(entity.getId() != null, Cd90DepthConfig::getId, entity.getId());
        List<Cd90DepthConfig> configs = new ArrayList<>(mapper.selectList(queryWrapper));
        configs.add(entity);
        configs.sort(Comparator.comparing(Cd90DepthConfig::getMinMachineQty));
        return this.isContinuous(configs) ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
    }

    /** 校验排序后的整组区间。 */
    private boolean isContinuous(List<Cd90DepthConfig> configs) {
        if (CollectionUtils.isEmpty(configs)
                || !Integer.valueOf(1).equals(configs.get(0).getMinMachineQty())) {
            return false;
        }
        for (int index = 0; index < configs.size(); index++) {
            Cd90DepthConfig current = configs.get(index);
            if (!this.isValidRange(current)) {
                return false;
            }
            if (index < configs.size() - 1) {
                Cd90DepthConfig next = configs.get(index + 1);
                if (current.getMaxMachineQty() == null
                        || current.getMaxMachineQty() + 1 != next.getMinMachineQty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 校验单个区间和备库班数的基础值。 */
    private boolean isValidRange(Cd90DepthConfig entity) {
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
    public AjaxResult importData(List<Cd90DepthConfig> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failNum = 0;
        List<Cd90DepthConfig> insertList = new ArrayList<>();
        List<ImportErrorLog> errorList = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int index = 0; index < list.size(); index++) {
            int rowNum = index + 2;
            Cd90DepthConfig item = list.get(index);
            List<ImportErrorLog> errors = ImportExcelValidatedUtils.validated(importLogId, rowNum, item);
            ImportExcelValidatedUtils.validatedRepeat(list, item, index, 2, importLogId, errors);
            try {
                this.validateBusiness(item);
            } catch (IllegalArgumentException exception) {
                ImportExcelValidatedUtils.addImportErrorLog(importLogId,
                        ImportErrorTypeEnums.OTHERS.getCode(), rowNum, exception.getMessage(), errors);
            }
            if (CollectionUtils.isNotEmpty(errors)) {
                failNum++;
                item.setId(-999L);
                errorList.addAll(errors);
            }
        }

        for (int index = 0; index < list.size(); index++) {
            int rowNum = index + 2;
            Cd90DepthConfig item = list.get(index);
            if (Long.valueOf(-999L).equals(item.getId())) {
                continue;
            }
            Cd90DepthConfig exist = this.getExist(item);
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
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail")
                    + "," + successNum + "," + failNum, errorList);
        }
        if (CollectionUtils.isNotEmpty(insertList)) {
            successNum += baseDao.saveBatch(insertList);
        }
        if (failNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail")
                    + "," + successNum + "," + failNum, errorList);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    /** 按工厂和区间起点查询已有配置。 */
    private Cd90DepthConfig getExist(Cd90DepthConfig entity) {
        LambdaQueryWrapper<Cd90DepthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Cd90DepthConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.eq(Cd90DepthConfig::getMinMachineQty, entity.getMinMachineQty());
        return mapper.selectOne(queryWrapper);
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("factoryCode", "minMachineQty");
    }

    /** 保存和导入时校验单条业务数据。 */
    private void validateBusiness(Cd90DepthConfig entity) {
        if (!this.isValidRange(entity)) {
            throw new IllegalArgumentException(
                    I18nUtil.getMessage("ui.data.column.cd90DepthConfig.invalidRange"));
        }
    }
}
