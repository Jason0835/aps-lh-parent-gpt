package com.zlt.aps.cd90.engine.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.zlt.aps.cd90.api.domain.entity.Cd90DepthConfig;
import com.zlt.aps.cd90.api.domain.entity.Cd90Stock;
import com.zlt.aps.cd90.api.domain.entity.Cd90StorageLaneLimit;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineCxScheduleMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineDepthConfigMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineStockMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineStorageLaneMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineXwyyScheduleResultMapper;
import com.zlt.aps.cd90.engine.mapper.Cd90EngineXwyyStockMapper;
import com.zlt.aps.cd90.engine.service.Cd90AutoScheduleInputVersionService;
import com.zlt.aps.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyScheduleResult;
import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.stream.Collectors;

/** 基于数据主键、批次和更新时间生成关键输入版本指纹。 */
@Service
public class Cd90AutoScheduleInputVersionServiceImpl implements Cd90AutoScheduleInputVersionService {

    private final Cd90EngineCxScheduleMapper cxMapper;
    private final Cd90EngineStockMapper stockMapper;
    private final Cd90EngineDepthConfigMapper depthConfigMapper;
    private final Cd90EngineStorageLaneMapper laneMapper;
    private final Cd90EngineXwyyStockMapper xwyyStockMapper;
    private final Cd90EngineXwyyScheduleResultMapper xwyyScheduleResultMapper;

    public Cd90AutoScheduleInputVersionServiceImpl(Cd90EngineCxScheduleMapper cxMapper,
                                                   Cd90EngineStockMapper stockMapper,
                                                   Cd90EngineDepthConfigMapper depthConfigMapper,
                                                   Cd90EngineStorageLaneMapper laneMapper,
                                                   Cd90EngineXwyyStockMapper xwyyStockMapper,
                                                   Cd90EngineXwyyScheduleResultMapper xwyyScheduleResultMapper) {
        this.cxMapper = cxMapper;
        this.stockMapper = stockMapper;
        this.depthConfigMapper = depthConfigMapper;
        this.laneMapper = laneMapper;
        this.xwyyStockMapper = xwyyStockMapper;
        this.xwyyScheduleResultMapper = xwyyScheduleResultMapper;
    }

    @Override
    public String fingerprint(String factoryCode, LocalDate scheduleDate) {
        return this.fingerprint(factoryCode, scheduleDate, true);
    }

    @Override
    public String fingerprintWithoutStock(String factoryCode, LocalDate scheduleDate) {
        return this.fingerprint(factoryCode, scheduleDate, false);
    }

    private String fingerprint(String factoryCode, LocalDate scheduleDate,
                               boolean includeStock) {
        String forming = cxMapper.selectList(Wrappers.<CxScheduleResult>lambdaQuery()
                        // 版本指纹只依赖主键、批次和更新时间，不加载成型结果的其他业务字段。
                        .select(CxScheduleResult::getId,
                                CxScheduleResult::getCxBatchNo,
                                CxScheduleResult::getCxMachineCode,
                                CxScheduleResult::getUpdateTime)
                        .eq(CxScheduleResult::getFactoryCode, factoryCode)
                        .between(CxScheduleResult::getScheduleDate, Date.valueOf(scheduleDate.minusDays(1)),
                                Date.valueOf(scheduleDate.plusDays(3)))
                        .orderByAsc(CxScheduleResult::getId))
                .stream().map(item -> item.getId() + ":" + item.getCxBatchNo() + ":"
                        + item.getCxMachineCode() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String depthConfig = depthConfigMapper.selectList(Wrappers.<Cd90DepthConfig>lambdaQuery()
                        .select(Cd90DepthConfig::getId,
                                Cd90DepthConfig::getMinMachineQty,
                                Cd90DepthConfig::getMaxMachineQty,
                                Cd90DepthConfig::getDepthClassQty,
                                Cd90DepthConfig::getUpdateTime)
                        .eq(Cd90DepthConfig::getFactoryCode, factoryCode)
                        .orderByAsc(Cd90DepthConfig::getId))
                .stream().map(item -> item.getId() + ":" + item.getMinMachineQty() + ":"
                        + item.getMaxMachineQty() + ":" + item.getDepthClassQty() + ":"
                        + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String stock = includeStock ? stockMapper.selectList(Wrappers.<Cd90Stock>lambdaQuery()
                        .select(Cd90Stock::getId,
                                Cd90Stock::getShiftCode,
                                Cd90Stock::getSnapshotTime,
                                Cd90Stock::getUpdateTime)
                        .eq(Cd90Stock::getFactoryCode, factoryCode)
                        .eq(Cd90Stock::getStockDate, Date.valueOf(scheduleDate))
                        .orderByAsc(Cd90Stock::getShiftCode)
                        .orderByAsc(Cd90Stock::getId))
                .stream().map(item -> item.getId() + ":" + item.getShiftCode() + ":" + item.getSnapshotTime() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|")) : "";
        String xwyyStock = xwyyStockMapper.selectList(Wrappers.<XwyyStock>lambdaQuery()
                        .select(XwyyStock::getId,
                                XwyyStock::getBigRollCode,
                                XwyyStock::getBigRollBarcode,
                                XwyyStock::getStockInTime,
                                XwyyStock::getStockNum,
                                XwyyStock::getModifyNum,
                                XwyyStock::getBadNum,
                                XwyyStock::getStockMeters,
                                XwyyStock::getEstimateStockFlag,
                                XwyyStock::getUpdateTime)
                        .eq(XwyyStock::getFactoryCode, factoryCode)
                        .orderByAsc(XwyyStock::getId))
                .stream().map(item -> item.getId() + ":" + item.getBigRollCode() + ":"
                        + item.getBigRollBarcode() + ":" + item.getStockInTime() + ":"
                        + item.getStockNum() + ":" + item.getModifyNum() + ":" + item.getBadNum() + ":"
                        + item.getStockMeters() + ":" + item.getEstimateStockFlag() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String xwyyPlan = xwyyScheduleResultMapper.selectList(
                        Wrappers.<XwyyScheduleResult>lambdaQuery()
                                .select(XwyyScheduleResult::getId,
                                        XwyyScheduleResult::getBatchNo,
                                        XwyyScheduleResult::getUpdateTime)
                                .eq(XwyyScheduleResult::getFactoryCode, factoryCode)
                                .between(XwyyScheduleResult::getScheduleDate,
                                        Date.valueOf(scheduleDate.minusDays(1)),
                                        Date.valueOf(scheduleDate.plusDays(2)))
                                .orderByAsc(XwyyScheduleResult::getId))
                .stream().map(item -> item.getId() + ":" + item.getBatchNo() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        String lanes = laneMapper.selectList(Wrappers.<Cd90StorageLaneLimit>lambdaQuery()
                        .eq(Cd90StorageLaneLimit::getFactoryCode, factoryCode)
                        .eq(Cd90StorageLaneLimit::getLaneDate, Date.valueOf(scheduleDate))
                        .orderByAsc(Cd90StorageLaneLimit::getId))
                .stream().map(item -> item.getId() + ":" + item.getShiftCode() + ":" + item.getUpdateTime())
                .collect(Collectors.joining("|"));
        return sha256(forming + "#" + depthConfig + "#" + stock + "#" + lanes
                + "#" + xwyyStock + "#" + xwyyPlan);
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) result.append(String.format("%02x", item & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前JVM不支持SHA-256", exception);
        }
    }
}
