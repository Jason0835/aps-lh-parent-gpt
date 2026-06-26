package com.zlt.aps.dj.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.common.engine.enums.MachineRangeEnum;
import com.zlt.aps.dj.api.domain.entity.DjDepthConfig;
import com.zlt.aps.dj.mapper.DjDepthConfigMapper;
import com.zlt.aps.dj.service.IDjDepthConfigService;
import com.zlt.bill.common.service.AbstractDocService;

/**
 * 垫胶备库班数与供成型机数配置Service业务层处理
 *
 * @author zlt
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DjDepthConfigServiceImpl extends AbstractDocService<DjDepthConfig> implements IDjDepthConfigService {

    @Resource
    private DjDepthConfigMapper depthConfigMapper;

    /**
     * 校验配置规则的交叉情况
     * <p>
     * 规则说明：
     * - MACHINE_RANGE 与 MACHINE_QTY 组合构成范围条件
     * - 排程算法按 MACHINE_QTY 降序匹配（越大优先级越高），取第一个满足条件的配置行
     * - 因此高优先级的规则会覆盖（隐藏）低优先级的重叠规则，低优先级规则的备库班数值不会生效
     * - 交叉校验只需检查新规则与同等或更高优先级（MACHINE_QTY ≥ 自身）的现有规则是否有范围重叠且备库班数不同
     * - 对于优先级更低（MACHINE_QTY < 自身）的现有规则，新规则会覆盖它们，无需校验
     * - 例如：已有「EQ 1」备库班数=6，新增「LE 5」备库班数=4，LE 5 优先级更高（5 > 1），会覆盖 EQ 1，允许
     * </p>
     */
    @Override
    public String checkRangeCross(DjDepthConfig entity) {
        // 查询同一工厂下的所有配置（排除自身）
        LambdaQueryWrapper<DjDepthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DjDepthConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.ne(entity.getId() != null, DjDepthConfig::getId, entity.getId());
        List<DjDepthConfig> existingList = depthConfigMapper.selectList(queryWrapper);

        if (existingList.isEmpty()) {
            return UserConstants.UNIQUE;
        }

        long[] newRange = calculateRange(entity.getMachineRange(), entity.getMachineQty());

        for (DjDepthConfig existing : existingList) {
            // 只检查优先级同等或更高的现有规则（MACHINE_QTY ≥ 自身）
            // 优先级更低的规则会被新规则覆盖，其备库班数值不会生效
            if (existing.getMachineQty() < entity.getMachineQty()) {
                continue;
            }
            long[] existingRange = calculateRange(existing.getMachineRange(), existing.getMachineQty());
            // 范围有交集且备库班数不同 → 冲突
            if (newRange[0] <= existingRange[1] && existingRange[0] <= newRange[1]
                    && !entity.getDepthClassQty().equals(existing.getDepthClassQty())) {
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
        MachineRangeEnum rangeEnum = MachineRangeEnum.getByCode(machineRange);
        if (rangeEnum == null) {
            return new long[]{0, 0};
        }
        int qty = machineQty != null ? machineQty : 0;
        switch (rangeEnum) {
            case LT:
                return new long[]{0, qty - 1L};
            case LE:
                return new long[]{0, qty};
            case EQ:
                return new long[]{qty, qty};
            case GE:
                return new long[]{qty, Integer.MAX_VALUE};
            case GT:
                return new long[]{qty + 1L, Integer.MAX_VALUE};
            default:
                return new long[]{0, 0};
        }
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}
