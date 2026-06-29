package com.zlt.aps.tq.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.maindata.enums.MsgTemplateEnums;
import com.zlt.aps.maindata.utils.MessageServiceUtils;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResult;
import com.zlt.aps.tq.api.domain.entity.TqScheFinishQty;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import com.zlt.aps.tq.api.domain.entity.TqWarningRecord;
import com.zlt.aps.tq.entity.TqParams;
import com.zlt.aps.tq.mapper.TqScheduleResultMapper;
import com.zlt.aps.tq.mapper.TqScheFinishQtyMapper;
import com.zlt.aps.tq.mapper.TqStockMapper;
import com.zlt.aps.tq.service.ITqWarningRecordService;
import com.zlt.aps.tq.service.ITqWarningService;
import com.zlt.aps.tq.service.TqParamsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎圈排程预警Service实现类
 *
 * <p>预警规则：</p>
 * <ol>
 *   <li>库存预警：查询当天最新库存，若库存量低于安全库存阈值，发送预警消息</li>
 *   <li>完成量预警：班次完全结束后检查，实际完成量低于计划量一定比例时发送预警</li>
 * </ol>
 *
 * <p>参数配置（T_TQ_PARAMS）：</p>
 * <ul>
 *   <li>TQ_STOCK_WARNING_THRESHOLD：库存预警阈值（默认1000）</li>
 *   <li>TQ_FINISH_QTY_WARNING_RATIO：完成量预警比例（默认0.8，即完成量低于计划量80%时预警）</li>
 * </ul>
 *
 * @author APS
 */
@Slf4j
@Service
public class TqWarningServiceImpl implements ITqWarningService {

    @Resource
    private TqStockMapper tqStockMapper;

    @Resource
    private TqScheduleResultMapper tqScheduleResultMapper;

    @Resource
    private TqScheFinishQtyMapper tqScheFinishQtyMapper;

    @Resource
    private TqParamsService tqParamsService;

    @Resource
    private MessageServiceUtils messageServiceUtils;

    @Resource
    private ITqWarningRecordService tqWarningRecordService;

    /** 预警类型：库存预警 */
    private static final String WARNING_TYPE_STOCK = "1";
    /** 预警类型：完成量预警 */
    private static final String WARNING_TYPE_FINISH_QTY = "2";
    /** 预警级别：低 */
    private static final String WARNING_LEVEL_LOW = "1";
    /** 预警级别：中 */
    private static final String WARNING_LEVEL_MIDDLE = "2";
    /** 预警级别：高 */
    private static final String WARNING_LEVEL_HIGH = "3";
    /** 处理状态：未处理 */
    private static final String STATUS_UNHANDLED = "0";
    /** 未通知 */
    private static final Integer NOTIFIED_NO = 0;
    /** 已通知 */
    private static final Integer NOTIFIED_YES = 1;

    /** 库存预警阈值参数代码 */
    private static final String PARAM_STOCK_WARNING_THRESHOLD = "TQ_STOCK_WARNING_THRESHOLD";
    /** 完成量预警比例参数代码 */
    private static final String PARAM_FINISH_QTY_WARNING_RATIO = "TQ_FINISH_QTY_WARNING_RATIO";
    /** 默认库存预警阈值 */
    private static final double DEFAULT_STOCK_WARNING_THRESHOLD = 1000;
    /** 默认完成量预警比例 */
    private static final double DEFAULT_FINISH_QTY_WARNING_RATIO = 0.8;

    /**
     * 执行库存预警检查
     */
    @Override
    public void checkStockWarning() {
        log.info("胎圈库存预警检查开始");
        try {
            // 获取库存预警阈值
            double threshold = getParamValue(PARAM_STOCK_WARNING_THRESHOLD, DEFAULT_STOCK_WARNING_THRESHOLD);

            // 查询当天最新库存
            Date today = new Date();
            LambdaQueryWrapper<TqStock> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TqStock::getStockDate, today)
                   .eq(TqStock::getIsDelete, 0);
            List<TqStock> stockList = tqStockMapper.selectList(wrapper);

            if (stockList.isEmpty()) {
                log.info("胎圈库存预警：当天无库存数据，跳过检查");
                return;
            }

            // 检查每条库存是否低于阈值，收集预警记录
            List<TqWarningRecord> warningRecords = new ArrayList<>();
            for (TqStock stock : stockList) {
                BigDecimal stockNum = stock.getStockNum();
                if (stockNum == null) {
                    continue;
                }
                if (stockNum.doubleValue() < threshold) {
                    // 构建库存预警记录
                    TqWarningRecord record = this.buildStockWarningRecord(stock, threshold);
                    warningRecords.add(record);
                }
            }

            // 批量保存预警记录
            if (!warningRecords.isEmpty()) {
                tqWarningRecordService.saveBatchWarningRecords(warningRecords);
                // 逐条发送预警消息，发送成功后更新通知状态
                for (TqWarningRecord record : warningRecords) {
                    this.sendStockWarning(record);
                }
            }

            log.info("胎圈库存预警检查完成：共检查{}条库存，预警{}条", stockList.size(), warningRecords.size());

        } catch (Exception e) {
            log.error("胎圈库存预警检查失败", e);
        }
    }

    /**
     * 执行班次完成量预警检查
     *
     * <p>在班次完全结束后检查，对比计划量与实际完成量。</p>
     */
    @Override
    public void checkFinishQtyWarning(Date scheduleDate, int shiftIndex) {
        log.info("胎圈班次完成量预警检查开始：排程日期={}，班次={}", scheduleDate, shiftIndex);
        try {
            // 获取完成量预警比例
            double warningRatio = getParamValue(PARAM_FINISH_QTY_WARNING_RATIO, DEFAULT_FINISH_QTY_WARNING_RATIO);

            // 查询当天该班次的排程计划
            LambdaQueryWrapper<TqScheduleResult> scheduleWrapper = new LambdaQueryWrapper<>();
            scheduleWrapper.eq(TqScheduleResult::getScheduleDate, scheduleDate)
                           .eq(TqScheduleResult::getIsDelete, 0);
            List<TqScheduleResult> scheduleList = tqScheduleResultMapper.selectList(scheduleWrapper);

            if (scheduleList.isEmpty()) {
                log.info("胎圈完成量预警：当天无排程数据，跳过检查");
                return;
            }

            // 查询当天完成量数据
            LambdaQueryWrapper<TqScheFinishQty> finishWrapper = new LambdaQueryWrapper<>();
            finishWrapper.eq(TqScheFinishQty::getScheduleDate, scheduleDate)
                         .eq(TqScheFinishQty::getIsDelete, 0);
            List<TqScheFinishQty> finishList = tqScheFinishQtyMapper.selectList(finishWrapper);

            // 检查每条排程的完成情况，收集预警记录
            List<TqWarningRecord> warningRecords = new ArrayList<>();
            for (TqScheduleResult schedule : scheduleList) {
                Integer planQty = getPlanQtyByShiftIndex(schedule, shiftIndex);
                if (planQty == null || planQty <= 0) {
                    continue;
                }

                // 查找对应的完成量记录
                BigDecimal finishQty = findFinishQty(schedule, finishList, shiftIndex);
                if (finishQty == null) {
                    // MES未写入完成量数据，跳过（按用户说明：MES还未写入数据）
                    continue;
                }

                // 计算完成率
                double finishRate = finishQty.doubleValue() / planQty;
                if (finishRate < warningRatio) {
                    // 构建完成量预警记录
                    TqWarningRecord record = this.buildFinishQtyWarningRecord(
                            schedule, shiftIndex, planQty, finishQty, finishRate, warningRatio);
                    warningRecords.add(record);
                }
            }

            // 批量保存预警记录
            if (!warningRecords.isEmpty()) {
                tqWarningRecordService.saveBatchWarningRecords(warningRecords);
                // 逐条发送预警消息，发送成功后更新通知状态
                for (TqWarningRecord record : warningRecords) {
                    this.sendFinishQtyWarning(record);
                }
            }

            log.info("胎圈班次完成量预警检查完成：共检查{}条排程，预警{}条", scheduleList.size(), warningRecords.size());

        } catch (Exception e) {
            log.error("胎圈班次完成量预警检查失败", e);
        }
    }

    /**
     * 构建库存预警记录
     *
     * @param stock     库存数据
     * @param threshold 预警阈值
     * @return 预警记录对象
     */
    private TqWarningRecord buildStockWarningRecord(TqStock stock, double threshold) {
        TqWarningRecord record = new TqWarningRecord();
        record.setFactoryCode(stock.getFactoryCode());
        record.setWarningType(WARNING_TYPE_STOCK);
        record.setBeadCode(stock.getBeadCode());
        record.setWarningLevel(WARNING_LEVEL_MIDDLE);
        record.setWarningTitle("胎圈库存预警");
        record.setWarningContent(StringUtils.format(
                "胎圈[{}]库存量[{}]低于预警阈值[{}]",
                stock.getBeadCode(),
                stock.getStockNum() == null ? "0" : stock.getStockNum().toString(),
                threshold));
        record.setStockNum(stock.getStockNum());
        record.setThreshold(BigDecimal.valueOf(threshold));
        record.setStatus(STATUS_UNHANDLED);
        record.setNotified(NOTIFIED_NO);

        // 构建预警数据JSON
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("beadCode", stock.getBeadCode());
        dataMap.put("stockNum", stock.getStockNum());
        dataMap.put("threshold", threshold);
        dataMap.put("stockDate", stock.getStockDate());
        record.setWarningData(JSON.toJSONString(dataMap));

        return record;
    }

    /**
     * 构建完成量预警记录
     *
     * @param schedule     排程结果
     * @param shiftIndex   班次索引
     * @param planQty      计划量
     * @param finishQty    完成量
     * @param finishRate   完成率
     * @param warningRatio 预警比例
     * @return 预警记录对象
     */
    private TqWarningRecord buildFinishQtyWarningRecord(TqScheduleResult schedule, int shiftIndex,
                                                         int planQty, BigDecimal finishQty,
                                                         double finishRate, double warningRatio) {
        TqWarningRecord record = new TqWarningRecord();
        record.setFactoryCode(schedule.getFactoryCode());
        record.setWarningType(WARNING_TYPE_FINISH_QTY);
        record.setBeadCode(schedule.getBeadCode());
        record.setMachineCode(schedule.getMachineCode());
        record.setScheduleDate(schedule.getScheduleDate());
        record.setShiftIndex(shiftIndex);
        // 根据完成率设置预警级别：完成率<50%为高，<70%为中，其他为低
        if (finishRate < 0.5) {
            record.setWarningLevel(WARNING_LEVEL_HIGH);
        } else if (finishRate < 0.7) {
            record.setWarningLevel(WARNING_LEVEL_MIDDLE);
        } else {
            record.setWarningLevel(WARNING_LEVEL_LOW);
        }
        record.setWarningTitle("胎圈完成量预警");
        record.setWarningContent(StringUtils.format(
                "机台[{}]胎圈[{}]班次[{}]计划量[{}]，实际完成量[{}]，完成率[{}]%，低于预警比例[{}]%",
                schedule.getMachineCode(),
                schedule.getBeadCode(),
                shiftIndex,
                planQty,
                finishQty,
                String.format("%.2f", finishRate * 100),
                String.format("%.2f", warningRatio * 100)));
        record.setPlanQty(planQty);
        record.setFinishQty(finishQty);
        record.setFinishRate(BigDecimal.valueOf(finishRate));
        record.setThreshold(BigDecimal.valueOf(warningRatio));
        record.setStatus(STATUS_UNHANDLED);
        record.setNotified(NOTIFIED_NO);

        // 构建预警数据JSON
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("machineCode", schedule.getMachineCode());
        dataMap.put("beadCode", schedule.getBeadCode());
        dataMap.put("shiftIndex", shiftIndex);
        dataMap.put("planQty", planQty);
        dataMap.put("finishQty", finishQty);
        dataMap.put("finishRate", finishRate);
        dataMap.put("warningRatio", warningRatio);
        dataMap.put("scheduleDate", schedule.getScheduleDate());
        record.setWarningData(JSON.toJSONString(dataMap));

        return record;
    }

    /**
     * 发送库存预警消息
     *
     * @param record 预警记录
     */
    private void sendStockWarning(TqWarningRecord record) {
        try {
            String templateCode = MsgTemplateEnums.TQ_STOCK_WARNING.getCode();
            messageServiceUtils.sendWarning(templateCode, (String) null,
                    record.getBeadCode(),
                    record.getStockNum() == null ? "0" : record.getStockNum().toString(),
                    record.getThreshold() == null ? "0" : record.getThreshold().toString());
            // 更新通知状态为已通知
            this.updateNotifiedStatus(record.getId());
            log.info("胎圈库存预警消息已发送：物料={}，库存={}，阈值={}",
                    record.getBeadCode(), record.getStockNum(), record.getThreshold());
        } catch (Exception e) {
            log.error("胎圈库存预警消息发送失败：物料={}", record.getBeadCode(), e);
        }
    }

    /**
     * 发送完成量预警消息
     *
     * @param record 预警记录
     */
    private void sendFinishQtyWarning(TqWarningRecord record) {
        try {
            String templateCode = MsgTemplateEnums.TQ_FINISH_QTY_WARNING.getCode();
            messageServiceUtils.sendWarning(templateCode, (String) null,
                    record.getMachineCode(),
                    record.getBeadCode(),
                    String.valueOf(record.getShiftIndex()),
                    String.valueOf(record.getPlanQty()),
                    record.getFinishQty().toString(),
                    String.format("%.2f%%", record.getFinishRate().doubleValue() * 100),
                    String.format("%.2f%%", record.getThreshold().doubleValue() * 100));
            // 更新通知状态为已通知
            this.updateNotifiedStatus(record.getId());
            log.info("胎圈完成量预警消息已发送：机台={}，胎圈={}，班次={}，计划量={}，完成量={}",
                    record.getMachineCode(), record.getBeadCode(), record.getShiftIndex(),
                    record.getPlanQty(), record.getFinishQty());
        } catch (Exception e) {
            log.error("胎圈完成量预警消息发送失败：机台={}，胎圈={}",
                    record.getMachineCode(), record.getBeadCode(), e);
        }
    }

    /**
     * 更新预警记录的通知状态为已通知
     *
     * @param recordId 预警记录ID
     */
    private void updateNotifiedStatus(Long recordId) {
        try {
            if (recordId == null) {
                return;
            }
            com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<TqWarningRecord> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
            wrapper.eq(TqWarningRecord::getId, recordId)
                   .set(TqWarningRecord::getNotified, NOTIFIED_YES)
                   .set(TqWarningRecord::getNotifyTime, new Date());
            tqWarningRecordService.update(wrapper);
        } catch (Exception e) {
            log.warn("更新预警记录通知状态失败：id={}", recordId, e);
        }
    }

    /**
     * 查找排程对应的完成量
     */
    private BigDecimal findFinishQty(TqScheduleResult schedule, List<TqScheFinishQty> finishList, int shiftIndex) {
        // 按胎圈代码或工单号匹配
        for (TqScheFinishQty finish : finishList) {
            if (StringUtils.equals(finish.getBeadCode(), schedule.getBeadCode())) {
                // 根据班次索引返回对应班次的完成量
                // 班次映射：1-夜班，2-早班，3-中班（简化映射，实际按班制配置）
                switch (shiftIndex) {
                    case 1: return finish.getNightFinishQty();
                    case 2: return finish.getDayFinishQty();
                    case 3: return finish.getMidFinishQty();
                    default: return finish.getNightFinishQty();
                }
            }
        }
        return null;
    }

    /**
     * 获取参数值
     */
    private double getParamValue(String paramCode, double defaultValue) {
        try {
            LambdaQueryWrapper<TqParams> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TqParams::getParamCode, paramCode)
                   .eq(TqParams::getIsDelete, 0);
            TqParams param = tqParamsService.getOne(wrapper, false);
            if (param != null && StringUtils.isNotBlank(param.getParamValue())) {
                return Double.parseDouble(param.getParamValue());
            }
        } catch (Exception e) {
            log.warn("获取参数[{}]失败，使用默认值{}", paramCode, defaultValue, e);
        }
        return defaultValue;
    }

    /**
     * 按班次索引获取计划量
     */
    private Integer getPlanQtyByShiftIndex(TqScheduleResult entity, int shiftIndex) {
        switch (shiftIndex) {
            case 1: return entity.getClass1PlanQty();
            case 2: return entity.getClass2PlanQty();
            case 3: return entity.getClass3PlanQty();
            case 4: return entity.getClass4PlanQty();
            case 5: return entity.getClass5PlanQty();
            case 6: return entity.getClass6PlanQty();
            default: return null;
        }
    }
}
