package com.zlt.aps.cd15.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd15.api.domain.entity.Cd15AngleWidthMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15CurlLength;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineMaintenancePlan;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineRollMapping;
import com.zlt.aps.cd15.api.domain.entity.Cd15SpecifyMachine;
import com.zlt.aps.cd15.api.domain.entity.Cd15Stock;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineAngleWidthMappingMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineConstructionMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCurlLengthMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineCxScheduleMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineGdyyScheduleResultMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineGdyyStockMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMachineInfoMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMachineRollMappingMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineMaintenanceMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineSpecifyMachineMapper;
import com.zlt.aps.cd15.engine.mapper.Cd15EngineStockMapper;
import com.zlt.aps.cd15.engine.service.Cd15AutoScheduleInputVersionService;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyScheduleResult;
import com.zlt.aps.gdyy.api.domain.entity.GdyyStock;
import com.zlt.aps.mdm.api.domain.entity.MdmConstructionInfo;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 基于 CD15 自动排程关键输入生成版本指纹。
 */
@Service
public class Cd15AutoScheduleInputVersionServiceImpl implements Cd15AutoScheduleInputVersionService {

    private final Cd15EngineCxScheduleMapper cxMapper;
    private final Cd15EngineConstructionMapper constructionMapper;
    private final Cd15EngineStockMapper stockMapper;
    private final Cd15EngineCurlLengthMapper curlLengthMapper;
    private final Cd15EngineAngleWidthMappingMapper angleWidthMappingMapper;
    private final Cd15EngineMachineInfoMapper machineInfoMapper;
    private final Cd15EngineMachineRollMappingMapper machineRollMappingMapper;
    private final Cd15EngineSpecifyMachineMapper specifyMachineMapper;
    private final Cd15EngineMaintenanceMapper maintenanceMapper;
    private final Cd15EngineGdyyStockMapper gdyyStockMapper;
    private final Cd15EngineGdyyScheduleResultMapper gdyyScheduleResultMapper;

    public Cd15AutoScheduleInputVersionServiceImpl(Cd15EngineCxScheduleMapper cxMapper,
                                                   Cd15EngineConstructionMapper constructionMapper,
                                                   Cd15EngineStockMapper stockMapper,
                                                   Cd15EngineCurlLengthMapper curlLengthMapper,
                                                   Cd15EngineAngleWidthMappingMapper angleWidthMappingMapper,
                                                   Cd15EngineMachineInfoMapper machineInfoMapper,
                                                   Cd15EngineMachineRollMappingMapper machineRollMappingMapper,
                                                   Cd15EngineSpecifyMachineMapper specifyMachineMapper,
                                                   Cd15EngineMaintenanceMapper maintenanceMapper,
                                                   Cd15EngineGdyyStockMapper gdyyStockMapper,
                                                   Cd15EngineGdyyScheduleResultMapper gdyyScheduleResultMapper) {
        this.cxMapper = cxMapper;
        this.constructionMapper = constructionMapper;
        this.stockMapper = stockMapper;
        this.curlLengthMapper = curlLengthMapper;
        this.angleWidthMappingMapper = angleWidthMappingMapper;
        this.machineInfoMapper = machineInfoMapper;
        this.machineRollMappingMapper = machineRollMappingMapper;
        this.specifyMachineMapper = specifyMachineMapper;
        this.maintenanceMapper = maintenanceMapper;
        this.gdyyStockMapper = gdyyStockMapper;
        this.gdyyScheduleResultMapper = gdyyScheduleResultMapper;
    }

    @Override
    public String fingerprint(String factoryCode, LocalDate scheduleDate) {
        String forming = cxMapper.selectList(Wrappers.<CxScheduleResult>lambdaQuery()
                        .eq(CxScheduleResult::getFactoryCode, factoryCode)
                        .between(CxScheduleResult::getScheduleDate,
                                Date.valueOf(scheduleDate.minusDays(1)), Date.valueOf(scheduleDate.plusDays(3)))
                        .orderByAsc(CxScheduleResult::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getCxBatchNo() + ":" + item.getCxMachineCode()
                        + ":" + item.getEmbryoCode() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String constructions = constructionMapper.selectList(Wrappers.<MdmConstructionInfo>lambdaQuery()
                        .eq(MdmConstructionInfo::getFactoryCode, factoryCode)
                        .orderByAsc(MdmConstructionInfo::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getConstructionCode() + ":" + item.getConstructionVersion()
                        + ":" + item.getArticleCrownSpec() + ":" + item.getBeltCuttingAngle() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String stock = stockMapper.selectList(Wrappers.<Cd15Stock>lambdaQuery()
                        .eq(Cd15Stock::getFactoryCode, factoryCode)
                        .eq(Cd15Stock::getStockDate, Date.valueOf(scheduleDate))
                        .orderByAsc(Cd15Stock::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getMaterialCode() + ":" + item.getStockDate()
                        + ":" + item.getStockNum() + ":" + item.getModifyNum() + ":" + item.getBadNum()
                        + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String curls = curlLengthMapper.selectList(Wrappers.<Cd15CurlLength>lambdaQuery()
                        .eq(Cd15CurlLength::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15CurlLength::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getSteelStripCode() + ":" + item.getCurlLength()
                        + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String angleWidths = angleWidthMappingMapper.selectList(Wrappers.<Cd15AngleWidthMapping>lambdaQuery()
                        .eq(Cd15AngleWidthMapping::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15AngleWidthMapping::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getCutAngle() + ":" + item.getClothWidthMax()
                        + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String machines = machineInfoMapper.selectList(Wrappers.<Cd15MachineInfo>lambdaQuery()
                        .eq(Cd15MachineInfo::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15MachineInfo::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getMachineCode() + ":" + item.getStatus()
                        + ":" + item.getQuota() + ":" + item.getClothWidthMin() + ":" + item.getClothWidthMax()
                        + ":" + item.getOpenMachineClass() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String machineRolls = machineRollMappingMapper.selectList(Wrappers.<Cd15MachineRollMapping>lambdaQuery()
                        .eq(Cd15MachineRollMapping::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15MachineRollMapping::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getBigRollCode() + ":" + item.getMachineCode()
                        + ":" + item.getShiftCode() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String specifyMachines = specifyMachineMapper.selectList(Wrappers.<Cd15SpecifyMachine>lambdaQuery()
                        .eq(Cd15SpecifyMachine::getFactoryCode, factoryCode)
                        .orderByAsc(Cd15SpecifyMachine::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getSteelStripCode() + ":" + item.getMachineCode()
                        + ":" + item.getJobType() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String maintenances = maintenanceMapper.selectList(Wrappers.<Cd15MachineMaintenancePlan>lambdaQuery()
                        .eq(Cd15MachineMaintenancePlan::getFactoryCode, factoryCode)
                        .between(Cd15MachineMaintenancePlan::getDowntimeDate,
                                Date.valueOf(scheduleDate.minusDays(1)), Date.valueOf(scheduleDate.plusDays(2)))
                        .orderByAsc(Cd15MachineMaintenancePlan::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getMachineCode() + ":" + item.getDowntimeDate()
                        + ":" + item.getDowntimeStartTime() + ":" + item.getDowntimeEndTime()
                        + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String gdyyStock = gdyyStockMapper.selectList(Wrappers.<GdyyStock>lambdaQuery()
                        .eq(GdyyStock::getFactoryCode, factoryCode)
                        .orderByAsc(GdyyStock::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getBigRollCode() + ":" + item.getBigRollBarcode()
                        + ":" + item.getInboundTime() + ":" + item.getStockMeters() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String gdyyPlan = gdyyScheduleResultMapper.selectList(Wrappers.<GdyyScheduleResult>lambdaQuery()
                        .eq(GdyyScheduleResult::getFactoryCode, factoryCode)
                        .between(GdyyScheduleResult::getScheduleDate,
                                Date.valueOf(scheduleDate.minusDays(1)), Date.valueOf(scheduleDate.plusDays(2)))
                        .orderByAsc(GdyyScheduleResult::getId))
                .stream()
                .map(item -> item.getId() + ":" + item.getBatchNo() + ":" + item.getBigRollCode()
                        + ":" + item.getMachineCode() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        return this.sha256(String.join("#", forming, constructions, stock, curls, angleWidths, machines,
                machineRolls, specifyMachines, maintenances, gdyyStock, gdyyPlan));
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return IntStream.range(0, bytes.length)
                    .mapToObj(index -> String.format("%02x", bytes[index] & 0xff))
                    .collect(Collectors.joining());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前JVM不支持SHA-256", exception);
        }
    }
}