package com.zlt.aps.dj.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.UserConstants;
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
    public String checkRangeCross(DjDepthConfig entity) {
        if (entity.getMinMachineQty() == null) {
            return UserConstants.NOT_UNIQUE;
        }
        // 查询同一工厂下的所有配置（排除自身）
        LambdaQueryWrapper<DjDepthConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DjDepthConfig::getFactoryCode, entity.getFactoryCode());
        queryWrapper.ne(entity.getId() != null, DjDepthConfig::getId, entity.getId());
        List<DjDepthConfig> existingList = depthConfigMapper.selectList(queryWrapper);

        if (existingList.isEmpty()) {
            // 只有一条数据时，MIN 必须为 1
            if (entity.getMinMachineQty() != 1) {
                return UserConstants.NOT_UNIQUE;
            }
            // 非末行时 MAX 不能为空
            if (entity.getMaxMachineQty() == null) {
                return UserConstants.UNIQUE; // 仅一条且无上限，合法
            }
            return UserConstants.UNIQUE;
        }

        // 按 minMachineQty 升序排列
        existingList.sort((a, b) -> {
            int minA = a.getMinMachineQty() != null ? a.getMinMachineQty() : Integer.MAX_VALUE;
            int minB = b.getMinMachineQty() != null ? b.getMinMachineQty() : Integer.MAX_VALUE;
            return Integer.compare(minA, minB);
        });

        if (entity.getId() == null) {
            // 新增：检查新增行是否破坏连续性
            // 找到插入位置
            int insertIdx = 0;
            for (int i = 0; i < existingList.size(); i++) {
                DjDepthConfig existing = existingList.get(i);
                if (existing.getMinMachineQty() != null && existing.getMinMachineQty() < entity.getMinMachineQty()) {
                    insertIdx = i + 1;
                }
            }
            // 检查与前一行（insertIdx-1）的连续性
            if (insertIdx > 0) {
                DjDepthConfig prev = existingList.get(insertIdx - 1);
                int expectedMin = (prev.getMaxMachineQty() != null) ? prev.getMaxMachineQty() + 1 : prev.getMinMachineQty() + 1;
                if (entity.getMinMachineQty() != expectedMin) {
                    return UserConstants.NOT_UNIQUE;
                }
            } else {
                // 插入在开头，MIN 必须为 1
                if (entity.getMinMachineQty() != 1) {
                    return UserConstants.NOT_UNIQUE;
                }
            }
            // 检查与后一行（insertIdx）的连续性
            if (insertIdx < existingList.size()) {
                DjDepthConfig next = existingList.get(insertIdx);
                int expectedNextMin = (entity.getMaxMachineQty() != null) ? entity.getMaxMachineQty() + 1 : entity.getMinMachineQty() + 1;
                if (next.getMinMachineQty() != null && next.getMinMachineQty() != expectedNextMin) {
                    return UserConstants.NOT_UNIQUE;
                }
            } else {
                // 插入在末尾，检查是否为末行
                if (entity.getMaxMachineQty() != null) {
                    // 不是末行，不能有无上限
                    DjDepthConfig last = existingList.get(existingList.size() - 1);
                    if (last.getMaxMachineQty() == null) {
                        return UserConstants.NOT_UNIQUE; // 前面已有末行
                    }
                }
            }
            // 检查前面是否有行已有无上限
            for (DjDepthConfig existing : existingList) {
                if (existing.getMaxMachineQty() == null) {
                    return UserConstants.NOT_UNIQUE;
                }
            }
        } else {
            // 修改：检查修改后整表的连续性
            // 将修改后的数据合并到列表中重新排序
            DjDepthConfig updated = entity;
            boolean replaced = false;
            for (int i = 0; i < existingList.size(); i++) {
                if (existingList.get(i).getId() != null && existingList.get(i).getId().equals(entity.getId())) {
                    existingList.set(i, updated);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                existingList.add(updated);
            }
            existingList.sort((a, b) -> {
                int minA = a.getMinMachineQty() != null ? a.getMinMachineQty() : Integer.MAX_VALUE;
                int minB = b.getMinMachineQty() != null ? b.getMinMachineQty() : Integer.MAX_VALUE;
                return Integer.compare(minA, minB);
            });
            return validateContinuity(existingList);
        }

        return UserConstants.UNIQUE;
    }

    /**
     * 校验整表连续性
     */
    private String validateContinuity(List<DjDepthConfig> list) {
        if (list.isEmpty()) {
            return UserConstants.UNIQUE;
        }
        // 第1条 MIN 必须为 1
        DjDepthConfig first = list.get(0);
        if (first.getMinMachineQty() == null || first.getMinMachineQty() != 1) {
            return UserConstants.NOT_UNIQUE;
        }
        boolean hasUnbounded = false;
        for (int i = 0; i < list.size(); i++) {
            DjDepthConfig current = list.get(i);
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
                DjDepthConfig next = list.get(i + 1);
                int expectedNextMin = current.getMaxMachineQty() + 1;
                if (next.getMinMachineQty() == null || next.getMinMachineQty() != expectedNextMin) {
                    return UserConstants.NOT_UNIQUE;
                }
            }
        }
        return UserConstants.UNIQUE;
    }

    @Override
    protected String getDocTypeCode() {
        return "";
    }
}
