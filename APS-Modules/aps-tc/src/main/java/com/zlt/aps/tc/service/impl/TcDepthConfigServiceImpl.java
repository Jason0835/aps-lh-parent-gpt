package com.zlt.aps.tc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.tc.api.domain.entity.TcDepthConfig;
import com.zlt.aps.tc.mapper.TcDepthConfigMapper;
import com.zlt.aps.tc.service.ITcDepthConfigService;
import com.zlt.bill.common.service.AbstractDocService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎侧备库班数配置Service业务层处理
 *
 * @author zlt
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class TcDepthConfigServiceImpl extends AbstractDocService<TcDepthConfig> implements ITcDepthConfigService {

    @Resource
    private TcDepthConfigMapper tcDepthConfigMapper;

    /**
     * 校验同一工厂备库班数区间的字段合法性、连续性和完整性。
     *
     * @param entity 待新增或修改的胎侧备库班数配置
     * @return {@link UserConstants#UNIQUE} 表示校验通过，{@link UserConstants#NOT_UNIQUE} 表示校验失败
     */
    @Override
    public String checkRangeCross(TcDepthConfig entity) {
        if (!this.isValidConfig(entity)) {
            return UserConstants.NOT_UNIQUE;
        }
        LambdaQueryWrapper<TcDepthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TcDepthConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.ne(entity.getId() != null, TcDepthConfig::getId, entity.getId());
        List<TcDepthConfig> existingConfigList = tcDepthConfigMapper.selectList(queryWrapper);
        List<TcDepthConfig> configList = existingConfigList == null
                ? new ArrayList<>() : new ArrayList<>(existingConfigList);
        configList.add(entity);
        return this.validateContinuity(configList);
    }

    /**
     * 导入前按工厂校验区间字段、连续性和完整性，防止批量导入绕过页面保存校验。
     *
     * @param configList 导入的胎侧备库班数配置
     * @param updateSupport 是否更新已存在数据
     * @param menuId 菜单标识
     * @return 导入结果
     * @throws RuntimeException 导入校验或批量保存发生异常时抛出
     */
    @Override
    public AjaxResult importData(List<TcDepthConfig> configList, boolean updateSupport, Long menuId) {
        if (UserConstants.NOT_UNIQUE.equals(this.validateImportConfig(configList))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.tc.depthConfig.rangeCross"));
        }
        return super.importData(configList, updateSupport, menuId);
    }

    /**
     * 将导入数据与数据库现有配置合并后校验，编辑导入时排除同主键旧记录。
     *
     * @param importConfigList 导入配置
     * @return 校验结果
     */
    private String validateImportConfig(List<TcDepthConfig> importConfigList) {
        if (importConfigList == null || importConfigList.isEmpty()) {
            return UserConstants.UNIQUE;
        }
        if (importConfigList.stream().anyMatch(config -> !this.isValidConfig(config))) {
            return UserConstants.NOT_UNIQUE;
        }
        Map<String, List<TcDepthConfig>> importConfigMap = importConfigList.stream()
                .collect(Collectors.groupingBy(TcDepthConfig::getFactoryCode));
        for (Map.Entry<String, List<TcDepthConfig>> entry : importConfigMap.entrySet()) {
            LambdaQueryWrapper<TcDepthConfig> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(TcDepthConfig::getFactoryCode, entry.getKey());
            List<TcDepthConfig> existingConfigList = tcDepthConfigMapper.selectList(queryWrapper);
            Set<Long> importIdSet = entry.getValue().stream()
                    .map(TcDepthConfig::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));
            List<TcDepthConfig> combinedConfigList = existingConfigList == null
                    ? new ArrayList<>() : existingConfigList.stream()
                    .filter(config -> config.getId() == null || !importIdSet.contains(config.getId()))
                    .collect(Collectors.toCollection(ArrayList::new));
            combinedConfigList.addAll(entry.getValue());
            if (UserConstants.NOT_UNIQUE.equals(this.validateContinuity(combinedConfigList))) {
                return UserConstants.NOT_UNIQUE;
            }
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 判断单条区间配置的必填值和数值范围是否合法。
     *
     * @param entity 胎侧备库班数配置
     * @return 合法返回 true，否则返回 false
     */
    private boolean isValidConfig(TcDepthConfig entity) {
        if (entity == null || entity.getFactoryCode() == null || entity.getFactoryCode().trim().isEmpty()
                || entity.getMinMachineQty() == null || entity.getMinMachineQty() <= 0
                || !this.isPositiveInteger(entity.getDepthClassQty())) {
            return false;
        }
        return entity.getMaxMachineQty() == null
                || (entity.getMaxMachineQty() > 0 && entity.getMaxMachineQty() >= entity.getMinMachineQty());
    }

    /**
     * 判断保证班数是否为正整数。
     *
     * @param value 保证班数
     * @return 正整数返回 true，否则返回 false
     */
    private boolean isPositiveInteger(BigDecimal value) {
        return value != null && value.signum() > 0 && value.stripTrailingZeros().scale() <= 0;
    }

    /**
     * 校验同一工厂全部区间是否从 1 开始、连续且不重叠。
     *
     * @param configList 包含待保存数据的全部有效配置
     * @return 校验结果
     */
    private String validateContinuity(List<TcDepthConfig> configList) {
        if (configList == null || configList.isEmpty()) {
            return UserConstants.UNIQUE;
        }
        configList.sort(Comparator.comparing(TcDepthConfig::getMinMachineQty));
        if (!this.isValidConfig(configList.get(0)) || configList.get(0).getMinMachineQty() != 1) {
            return UserConstants.NOT_UNIQUE;
        }
        for (int index = 0; index < configList.size(); index++) {
            TcDepthConfig currentConfig = configList.get(index);
            if (!this.isValidConfig(currentConfig)) {
                return UserConstants.NOT_UNIQUE;
            }
            if (currentConfig.getMaxMachineQty() == null) {
                return index == configList.size() - 1 ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
            }
            if (index < configList.size() - 1
                    && configList.get(index + 1).getMinMachineQty() != currentConfig.getMaxMachineQty() + 1) {
                return UserConstants.NOT_UNIQUE;
            }
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected String getDocTypeCode() {
        // 区间连续性由本服务校验，不再读取旧规则模型的单据唯一字段。
        return "";
    }
}
