package com.zlt.aps.tq.service.loader;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.entity.TqMachineSpecSpeed;
import com.zlt.aps.tq.engine.domain.manual.TqManualRollingCommandBatch;
import com.zlt.aps.tq.engine.domain.manual.TqManualRollingContext;
import com.zlt.aps.tq.mapper.TqMachineInfoMapper;
import com.zlt.aps.tq.mapper.TqMachineSpecSpeedMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;

/**
 * 胎圈人工滚动约束数据加载服务。
 *
 * <p>加载机台定额、机台规格速度等约束数据，供 EngineService 使用。</p>
 */
@Service
public class TqManualConstraintDataLoadService {

    @Resource
    private TqMachineInfoMapper tqMachineInfoMapper;

    @Resource
    private TqMachineSpecSpeedMapper tqMachineSpecSpeedMapper;

    /**
     * 加载约束数据并填充到上下文。
     *
     * @param context      人工滚动上下文
     * @param machineCodes 锁定机台集合
     * @param commandBatch 命令批次（可用于按需加载）
     */
    public void enrich(TqManualRollingContext context, List<String> machineCodes,
                       TqManualRollingCommandBatch commandBatch) {
        if (machineCodes == null || machineCodes.isEmpty()) {
            return;
        }
        // 1. 加载机台定额
        for (String machineCode : machineCodes) {
            LambdaQueryWrapper<TqMachineInfo> machineWrapper = new LambdaQueryWrapper<>();
            machineWrapper.eq(TqMachineInfo::getMachineCode, machineCode);
            machineWrapper.eq(TqMachineInfo::getIsDelete, 0);
            TqMachineInfo machine = tqMachineInfoMapper.selectOne(machineWrapper);
            if (machine != null && machine.getQuota() != null) {
                context.getMachineCapacityMap().put(machineCode, BigDecimal.valueOf(machine.getQuota()));
            }
        }
        // 2. 机台规格速度（从施工表加载，key=machineCode|beadCode）
        // 查询涉及机台的所有规格速度配置
        LambdaQueryWrapper<TqMachineSpecSpeed> specSpeedWrapper = new LambdaQueryWrapper<>();
        specSpeedWrapper.in(TqMachineSpecSpeed::getMachineCode, machineCodes);
        specSpeedWrapper.eq(TqMachineSpecSpeed::getIsDelete, 0);
        List<TqMachineSpecSpeed> specSpeedList = tqMachineSpecSpeedMapper.selectList(specSpeedWrapper);
        for (TqMachineSpecSpeed specSpeed : specSpeedList) {
            if (specSpeed.getMachineCode() != null && specSpeed.getBeadCode() != null
                    && specSpeed.getStandardSpeed() != null) {
                String key = specSpeed.getMachineCode() + "|" + specSpeed.getBeadCode();
                context.getMachineSpecSpeedMap().put(key, specSpeed.getStandardSpeed());
            }
        }
        // 3. 班次产能、维修小时数（胎圈无单独配置，使用机台定额兜底）
        // 4. predecessorTaskMap（前日链尾，MVP 阶段暂不加载）
    }
}
