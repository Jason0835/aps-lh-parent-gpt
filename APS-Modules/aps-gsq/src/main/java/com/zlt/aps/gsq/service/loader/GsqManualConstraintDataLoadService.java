package com.zlt.aps.gsq.service.loader;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingCommandBatch;
import com.zlt.aps.gsq.engine.domain.manual.GsqManualRollingContext;
import com.zlt.aps.gsq.mapper.GsqMachineInfoMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

/**
 * 钢丝圈人工滚动约束数据加载服务。
 *
 * <p>加载机台定额等约束数据，供 EngineService 使用。</p>
 *
 * <p>钢丝圈与胎圈差异：</p>
 * <ul>
 *   <li>钢丝圈没有独立的机台规格速度表（GsqMachineSpecSpeed），机台速度仅依赖机台定额 quata</li>
 *   <li>机台定额字段为 quata（BigDecimal 类型，非胎圈的 quota Double 类型）</li>
 *   <li>机台规格速度由 EngineService 的 repackMachine 中通过 getOrDefault(key, task.getMachineSpeed()) 兜底，
 *       本服务只需将机台定额放入 machineCapacityMap</li>
 * </ul>
 */
@Service
public class GsqManualConstraintDataLoadService {

    @Resource
    private GsqMachineInfoMapper gsqMachineInfoMapper;

    /**
     * 加载约束数据并填充到上下文。
     *
     * @param context      人工滚动上下文
     * @param machineCodes 锁定机台集合
     * @param commandBatch 命令批次（可用于按需加载）
     */
    public void enrich(GsqManualRollingContext context, List<String> machineCodes,
                       GsqManualRollingCommandBatch commandBatch) {
        if (machineCodes == null || machineCodes.isEmpty()) {
            return;
        }
        // 1. 加载机台定额（quata 字段，BigDecimal 类型）
        // 机台速度 = quata / 8小时，由 EngineService 的 repackMachine 中通过 getOrDefault 兜底计算
        for (String machineCode : machineCodes) {
            LambdaQueryWrapper<GsqMachineInfo> machineWrapper = new LambdaQueryWrapper<>();
            machineWrapper.eq(GsqMachineInfo::getMachineCode, machineCode);
            machineWrapper.eq(GsqMachineInfo::getIsDelete, 0);
            GsqMachineInfo machine = gsqMachineInfoMapper.selectOne(machineWrapper);
            if (machine != null && machine.getQuata() != null) {
                context.getMachineCapacityMap().put(machineCode, machine.getQuata());
            }
        }
        // 2. 机台规格速度（钢丝圈无独立规格速度表，由 EngineService 的 repackMachine 兜底处理）
        // 3. 班次产能、维修小时数（钢丝圈无单独配置，使用机台定额兜底）
        // 4. predecessorTaskMap（前日链尾，MVP 阶段暂不加载）
    }
}
