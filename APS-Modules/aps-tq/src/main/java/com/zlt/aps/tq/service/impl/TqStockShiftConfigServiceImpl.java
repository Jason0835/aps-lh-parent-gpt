package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.tq.api.domain.entity.TqStockShiftConfig;
import com.zlt.aps.tq.mapper.TqStockShiftConfigMapper;
import com.zlt.aps.tq.service.ITqStockShiftConfigService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 胎圈备库班数配置 Service实现类
 *
 * @author zlt
 * @date 2026-06-25
 */
@Slf4j
@Service
public class TqStockShiftConfigServiceImpl extends AbstractDocService<TqStockShiftConfig> implements ITqStockShiftConfigService {

    @Resource
    private TqStockShiftConfigMapper tqStockShiftConfigMapper;

    @Override
    protected String getDocTypeCode() {
        return "TQ_STOCK_SHIFT_CONFIG";
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        // 唯一性字段：分厂编码 + 区间起始机台数
        return Arrays.asList("factoryCode", "minMachineQty");
    }

    /**
     * 校验唯一性：分厂编码 + 区间起始机台数
     */
    @Override
    public String checkUnique(TqStockShiftConfig config) {
        LambdaQueryWrapper<TqStockShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(config.getId() != null, TqStockShiftConfig::getId, config.getId());
        wrapper.eq(TqStockShiftConfig::getFactoryCode, config.getFactoryCode());
        wrapper.eq(TqStockShiftConfig::getMinMachineQty, config.getMinMachineQty());
        if (tqStockShiftConfigMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验配置区间的连续性和完整性
     * <p>
     * 规则说明：
     * - 所有区间段必须连续且不重叠
     * - 第1条 MIN_MACHINE_QTY 必须为 1
     * - 后续行 MIN_MACHINE_QTY = 上一行 MAX_MACHINE_QTY + 1
     * - 只有末行允许 MAX_MACHINE_QTY 为 NULL（无上限）
     * - 若有缺口（未被覆盖的正整数）或重叠，校验失败
     * </p>
     */
    @Override
    public String checkRangeCross(TqStockShiftConfig config) {
        if (config.getMinMachineQty() == null) {
            return UserConstants.NOT_UNIQUE;
        }
        // 查询同一工厂下的所有配置（排除自身）
        LambdaQueryWrapper<TqStockShiftConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TqStockShiftConfig::getFactoryCode, config.getFactoryCode());
        queryWrapper.ne(config.getId() != null, TqStockShiftConfig::getId, config.getId());
        List<TqStockShiftConfig> existingList = tqStockShiftConfigMapper.selectList(queryWrapper);

        if (config.getId() == null) {
            // 新增：将新增行并入列表，统一做整表连续性校验
            existingList.add(config);
        } else {
            // 修改：用新数据替换原行（找不到则追加），再统一做整表连续性校验
            boolean replaced = false;
            for (int i = 0; i < existingList.size(); i++) {
                if (existingList.get(i).getId() != null && existingList.get(i).getId().equals(config.getId())) {
                    existingList.set(i, config);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                existingList.add(config);
            }
        }

        // 按 minMachineQty 升序排列
        existingList.sort((a, b) -> {
            int minA = a.getMinMachineQty() != null ? a.getMinMachineQty() : Integer.MAX_VALUE;
            int minB = b.getMinMachineQty() != null ? b.getMinMachineQty() : Integer.MAX_VALUE;
            return Integer.compare(minA, minB);
        });

        return validateContinuity(existingList);
    }

    /**
     * 校验整表区间连续性
     * <p>
     * 第1条 MIN 必须为 1；区间必须连续；只能有 1 条无上限且必须位于末行。
     * </p>
     *
     * @param list 按 minMachineQty 升序排序的配置列表
     * @return UserConstants.UNIQUE 合法 / UserConstants.NOT_UNIQUE 不合法
     */
    private String validateContinuity(List<TqStockShiftConfig> list) {
        if (list.isEmpty()) {
            return UserConstants.UNIQUE;
        }
        // 第1条 MIN 必须为 1
        TqStockShiftConfig first = list.get(0);
        if (first.getMinMachineQty() == null || first.getMinMachineQty() != 1) {
            return UserConstants.NOT_UNIQUE;
        }
        boolean hasUnbounded = false;
        for (int i = 0; i < list.size(); i++) {
            TqStockShiftConfig current = list.get(i);
            if (current.getMinMachineQty() == null || current.getDepthClassQty() == null) {
                return UserConstants.NOT_UNIQUE;
            }
            if (current.getMaxMachineQty() == null) {
                if (hasUnbounded) {
                    return UserConstants.NOT_UNIQUE; // 多个无上限
                }
                hasUnbounded = true;
                if (i != list.size() - 1) {
                    return UserConstants.NOT_UNIQUE; // 非末行无上限
                }
            }
            if (!hasUnbounded && i < list.size() - 1) {
                // 检查与下一行连续性
                TqStockShiftConfig next = list.get(i + 1);
                int expectedNextMin = current.getMaxMachineQty() + 1;
                if (next.getMinMachineQty() == null || next.getMinMachineQty() != expectedNextMin) {
                    return UserConstants.NOT_UNIQUE;
                }
            }
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public List<TqStockShiftConfig> listStockShiftConfig(TqStockShiftConfig config) {
        return tqStockShiftConfigMapper.selectStockShiftConfigList(config);
    }

    @Override
    public void deleteAll() {
        // @TableLogic 注解确保 selectList 自动过滤 IS_DELETE=0，只查询未删除的记录
        LambdaQueryWrapper<TqStockShiftConfig> wrapper = new LambdaQueryWrapper<>();
        List<TqStockShiftConfig> list = tqStockShiftConfigMapper.selectList(wrapper);
        if (CollectionUtils.isNotEmpty(list)) {
            // 使用 removeByIds 批量逻辑删除（@TableLogic 会自动将 DELETE 转为 UPDATE IS_DELETE=1）
            List<Long> ids = list.stream().map(TqStockShiftConfig::getId).collect(Collectors.toList());
            tqStockShiftConfigMapper.deleteBatchIds(ids);
        }
    }

    /**
     * 导入数据
     * 同时校验：
     * 1. 导入数据内部唯一性（分厂+机台范围+机台数）
     * 2. 与数据库已存在数据的唯一性
     * 3. 与数据库已存在数据的范围交叉
     */
    @Override
    public AjaxResult importData(List<TqStockShiftConfig> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        List<TqStockShiftConfig> importList = new ArrayList<>();

        // 按分厂+区间起始机台数分组，校验导入数据内部重复
        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(
                        a -> (a.getFactoryCode() == null ? "" : a.getFactoryCode())
                                + "_" + a.getMinMachineQty(),
                        Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TqStockShiftConfig config = list.get(i);

            String groupKey = (config.getFactoryCode() == null ? "" : config.getFactoryCode())
                    + "_" + config.getMinMachineQty();
            Long hasValue = groupMap.get(groupKey);
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                config.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.factoryCode");
                String columnName2 = I18nUtil.getMessage("ui.tq.depthConfig.column.minMachineQty");
                message = String.format(message, columnName + "+" + columnName2);
                addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, config);

            if (CollectionUtils.isEmpty(validated)) {
                // 校验与数据库已有数据的范围交叉
                if (UserConstants.NOT_UNIQUE.equals(checkRangeCross(config))) {
                    failureNum++;
                    config.setId(-999L);
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.data.column.tq.stockShiftConfig.rangeCross"), importErrorLogs);
                    continue;
                }
                config.setIsDelete(0);
                importList.add(config);
            } else {
                failureNum++;
                config.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        try {
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                tqStockShiftConfigMapper.mergeSql(importList);
            } else {
                for (int i = 0; i < list.size(); i++) {
                    TqStockShiftConfig excelItem = list.get(i);
                    if (excelItem.getId() != null && excelItem.getId().equals(-999L)) {
                        continue;
                    }
                    LambdaQueryWrapper<TqStockShiftConfig> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(TqStockShiftConfig::getFactoryCode, excelItem.getFactoryCode());
                    wrapper.eq(TqStockShiftConfig::getMinMachineQty, excelItem.getMinMachineQty());
                    // IS_DELETE=0 已由 @TableLogic 自动处理，无需手动添加
                    Long unique = tqStockShiftConfigMapper.selectCount(wrapper);
                    if (unique == 0) {
                        successNum++;
                        baseDao.save(excelItem);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.error.message.stockShiftConfig.unique"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
