package com.zlt.aps.tm.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.tm.api.domain.entity.TmStockCoverClass;
import com.zlt.aps.tm.mapper.TmStockCoverClassMapper;
import com.zlt.aps.tm.service.ITmStockCoverClassService;
import com.zlt.bill.common.service.AbstractDocService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * 备库班数配置Service业务层处理
 *
 * @author zlt
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class TmStockCoverClassServiceImpl extends AbstractDocService<TmStockCoverClass> implements ITmStockCoverClassService {

    @Resource
    private TmStockCoverClassMapper tmStockCoverClassMapper;

    /**
     * 校验配置规则的交叉情况
     * <p>
     * 规则说明：
     * - MACHINE_RANGE 与 MACHINE_QTY 组合构成范围条件
     * - 不同规则的范围不允许有交集，确保任意台数值最多只命中一条规则
     * - 例如：已有「GE 3」(≥3)，不允许再新增「LE 5」(≤5)，因为台数4同时满足两条规则
     * </p>
     */
    @Override
    public String checkRangeCross(TmStockCoverClass entity) {
        // 查询同一工厂下的所有配置（排除自身）
        LambdaQueryWrapper<TmStockCoverClass> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TmStockCoverClass::getFactoryCode, entity.getFactoryCode());
        queryWrapper.ne(entity.getId() != null, TmStockCoverClass::getId, entity.getId());
        List<TmStockCoverClass> existingList = tmStockCoverClassMapper.selectList(queryWrapper);

        if (existingList.isEmpty()) {
            return UserConstants.UNIQUE;
        }

        // 计算新规则的范围区间 [start, end]
        long[] newRange = calculateRange(entity.getMachineRange(), entity.getMachineQty());

        for (TmStockCoverClass existing : existingList) {
            long[] existingRange = calculateRange(existing.getMachineRange(), existing.getMachineQty());
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
     * 各范围对应的区间（MACHINE_QTY 为非负整数）：
     * - LT(N): [0, N-1]      小于
     * - LE(N): [0, N]        小于等于
     * - EQ(N): [N, N]        等于
     * - GE(N): [N, +∞) 用 [N, Integer.MAX_VALUE] 表示
     * - GT(N): [N+1, +∞) 用 [N+1, Integer.MAX_VALUE] 表示
     * </p>
     *
     * @param machineRange 范围条件（LT/LE/EQ/GE/GT）
     * @param machineQty   台数值
     * @return 长度2的数组，[start, end]
     */
    private long[] calculateRange(String machineRange, Integer machineQty) {
        int qty = machineQty != null ? machineQty : 0;
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
    protected String getDocTypeCode() {
        return "TM0816";
    }
}
