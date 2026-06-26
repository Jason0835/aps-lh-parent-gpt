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
        // 唯一性字段：分厂编码 + 机台范围 + 机台数
        return Arrays.asList("factoryCode", "machineRange", "machineCount");
    }

    /**
     * 校验唯一性：分厂编码 + 机台范围 + 机台数
     */
    @Override
    public String checkUnique(TqStockShiftConfig config) {
        LambdaQueryWrapper<TqStockShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.ne(config.getId() != null, TqStockShiftConfig::getId, config.getId());
        wrapper.eq(TqStockShiftConfig::getFactoryCode, config.getFactoryCode());
        wrapper.eq(TqStockShiftConfig::getMachineRange, config.getMachineRange());
        wrapper.eq(TqStockShiftConfig::getMachineCount, config.getMachineCount());
        if (tqStockShiftConfigMapper.selectCount(wrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验配置规则的交叉情况
     * <p>
     * 规则说明：
     * - MACHINE_RANGE 与 MACHINE_COUNT 组合构成范围条件
     * - 不同规则的范围不允许有交集，确保任意台数值最多只命中一条规则
     * - 例如：已有「GE 3」(≥3)，不允许再新增「LE 5」(≤5)，因为台数4同时满足两条规则
     * </p>
     */
    @Override
    public String checkRangeCross(TqStockShiftConfig config) {
        // 查询同一工厂下的所有配置（排除自身）
        LambdaQueryWrapper<TqStockShiftConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TqStockShiftConfig::getFactoryCode, config.getFactoryCode());
        queryWrapper.ne(config.getId() != null, TqStockShiftConfig::getId, config.getId());
        List<TqStockShiftConfig> existingList = tqStockShiftConfigMapper.selectList(queryWrapper);

        if (existingList.isEmpty()) {
            return UserConstants.UNIQUE;
        }

        // 计算新规则的范围区间 [start, end]
        long[] newRange = calculateRange(config.getMachineRange(), config.getMachineCount());

        for (TqStockShiftConfig existing : existingList) {
            long[] existingRange = calculateRange(existing.getMachineRange(), existing.getMachineCount());
            // 两个区间有交集则视为交叉
            if (newRange[0] <= existingRange[1] && existingRange[0] <= newRange[1]) {
                return UserConstants.NOT_UNIQUE;
            }
        }

        return UserConstants.UNIQUE;
    }

    /**
     * 将规则转换为整数范围区间 [start, end]
     * <p>
     * 各范围对应的区间（MACHINE_COUNT 为非负整数）：
     * - LT(N): [0, N-1]      小于
     * - LE(N): [0, N]        小于等于
     * - EQ(N): [N, N]        等于
     * - GE(N): [N, +∞) 用 [N, Integer.MAX_VALUE] 表示
     * - GT(N): [N+1, +∞) 用 [N+1, Integer.MAX_VALUE] 表示
     * </p>
     *
     * @param machineRange 范围条件（LT/LE/EQ/GE/GT）
     * @param machineCount 台数值
     * @return 长度2的数组，[start, end]
     */
    private long[] calculateRange(String machineRange, Integer machineCount) {
        int qty = machineCount != null ? machineCount : 0;
        switch (machineRange) {
            case "LT": // 小于 N
                return new long[]{0, qty - 1L};
            case "LE": // 小于等于 N
                return new long[]{0, qty};
            case "EQ": // 等于 N
                return new long[]{qty, qty};
            case "GE": // 大于等于 N
                return new long[]{qty, Integer.MAX_VALUE};
            case "GT": // 大于 N
                return new long[]{qty + 1L, Integer.MAX_VALUE};
            default:
                return new long[]{0, 0};
        }
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

        // 按分厂+机台范围+机台数分组，校验导入数据内部重复
        Map<String, Long> groupMap = list.stream()
                .collect(Collectors.groupingBy(
                        a -> (a.getFactoryCode() == null ? "" : a.getFactoryCode())
                                + "_" + (a.getMachineRange() == null ? "" : a.getMachineRange())
                                + "_" + a.getMachineCount(),
                        Collectors.counting()));

        for (int i = 0; i < list.size(); i++) {
            TqStockShiftConfig config = list.get(i);

            String groupKey = (config.getFactoryCode() == null ? "" : config.getFactoryCode())
                    + "_" + (config.getMachineRange() == null ? "" : config.getMachineRange())
                    + "_" + config.getMachineCount();
            Long hasValue = groupMap.get(groupKey);
            if (hasValue != null && hasValue > 1) {
                failureNum++;
                config.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.factoryCode");
                String columnName2 = I18nUtil.getMessage("ui.data.column.stockShiftConfig.machineRange");
                String columnName3 = I18nUtil.getMessage("ui.data.column.stockShiftConfig.machineCount");
                message = String.format(message, columnName + "+" + columnName2 + "+" + columnName3);
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
                    wrapper.eq(TqStockShiftConfig::getMachineRange, excelItem.getMachineRange());
                    wrapper.eq(TqStockShiftConfig::getMachineCount, excelItem.getMachineCount());
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
