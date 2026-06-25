package com.zlt.aps.tq.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.maindata.enums.MsgTemplateEnums;
import com.zlt.aps.maindata.utils.MessageServiceUtils;
import com.zlt.aps.tq.api.domain.entity.TqNewScheduleResult;
import com.zlt.aps.tq.api.domain.entity.TqScheFinishQty;
import com.zlt.aps.tq.api.domain.entity.TqStock;
import com.zlt.aps.tq.entity.TqParams;
import com.zlt.aps.tq.mapper.TqNewScheduleResultMapper;
import com.zlt.aps.tq.mapper.TqScheFinishQtyMapper;
import com.zlt.aps.tq.mapper.TqStockMapper;
import com.zlt.aps.tq.service.ITqWarningService;
import com.zlt.aps.tq.service.TqParamsService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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
    private TqNewScheduleResultMapper tqNewScheduleResultMapper;

    @Resource
    private TqScheFinishQtyMapper tqScheFinishQtyMapper;

    @Resource
    private TqParamsService tqParamsService;

    @Resource
    private MessageServiceUtils messageServiceUtils;

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

            // 检查每条库存是否低于阈值
            int warningCount = 0;
            for (TqStock stock : stockList) {
                BigDecimal stockNum = stock.getStockNum();
                if (stockNum == null) {
                    continue;
                }
                if (stockNum.doubleValue() < threshold) {
                    // 发送库存预警消息
                    sendStockWarning(stock, threshold);
                    warningCount++;
                }
            }

            log.info("胎圈库存预警检查完成：共检查{}条库存，预警{}条", stockList.size(), warningCount);

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
            LambdaQueryWrapper<TqNewScheduleResult> scheduleWrapper = new LambdaQueryWrapper<>();
            scheduleWrapper.eq(TqNewScheduleResult::getScheduleDate, scheduleDate)
                           .eq(TqNewScheduleResult::getIsDelete, 0);
            List<TqNewScheduleResult> scheduleList = tqNewScheduleResultMapper.selectList(scheduleWrapper);

            if (scheduleList.isEmpty()) {
                log.info("胎圈完成量预警：当天无排程数据，跳过检查");
                return;
            }

            // 查询当天完成量数据
            LambdaQueryWrapper<TqScheFinishQty> finishWrapper = new LambdaQueryWrapper<>();
            finishWrapper.eq(TqScheFinishQty::getScheduleDate, scheduleDate)
                         .eq(TqScheFinishQty::getIsDelete, 0);
            List<TqScheFinishQty> finishList = tqScheFinishQtyMapper.selectList(finishWrapper);

            // 检查每条排程的完成情况
            int warningCount = 0;
            for (TqNewScheduleResult schedule : scheduleList) {
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
                    // 发送完成量预警消息
                    sendFinishQtyWarning(schedule, shiftIndex, planQty, finishQty, warningRatio);
                    warningCount++;
                }
            }

            log.info("胎圈班次完成量预警检查完成：共检查{}条排程，预警{}条", scheduleList.size(), warningCount);

        } catch (Exception e) {
            log.error("胎圈班次完成量预警检查失败", e);
        }
    }

    /**
     * 发送库存预警消息
     */
    private void sendStockWarning(TqStock stock, double threshold) {
        try {
            String templateCode = MsgTemplateEnums.TQ_STOCK_WARNING.getCode();
            messageServiceUtils.sendWarning(templateCode, (String) null,
                    stock.getBeadCode(),
                    stock.getStockNum() == null ? "0" : stock.getStockNum().toString(),
                    String.valueOf(threshold));
            log.info("胎圈库存预警消息已发送：物料={}，库存={}，阈值={}",
                    stock.getBeadCode(), stock.getStockNum(), threshold);
        } catch (Exception e) {
            log.error("胎圈库存预警消息发送失败：物料={}", stock.getBeadCode(), e);
        }
    }

    /**
     * 发送完成量预警消息
     */
    private void sendFinishQtyWarning(TqNewScheduleResult schedule, int shiftIndex,
                                       int planQty, BigDecimal finishQty, double warningRatio) {
        try {
            String templateCode = MsgTemplateEnums.TQ_FINISH_QTY_WARNING.getCode();
            messageServiceUtils.sendWarning(templateCode, (String) null,
                    schedule.getMachineCode(),
                    schedule.getBeadCode(),
                    String.valueOf(shiftIndex),
                    String.valueOf(planQty),
                    finishQty.toString(),
                    String.format("%.2f%%", finishQty.doubleValue() / planQty * 100),
                    String.format("%.2f%%", warningRatio * 100));
            log.info("胎圈完成量预警消息已发送：机台={}，胎圈={}，班次={}，计划量={}，完成量={}",
                    schedule.getMachineCode(), schedule.getBeadCode(), shiftIndex, planQty, finishQty);
        } catch (Exception e) {
            log.error("胎圈完成量预警消息发送失败：机台={}，胎圈={}",
                    schedule.getMachineCode(), schedule.getBeadCode(), e);
        }
    }

    /**
     * 查找排程对应的完成量
     */
    private BigDecimal findFinishQty(TqNewScheduleResult schedule, List<TqScheFinishQty> finishList, int shiftIndex) {
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
    private Integer getPlanQtyByShiftIndex(TqNewScheduleResult entity, int shiftIndex) {
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
