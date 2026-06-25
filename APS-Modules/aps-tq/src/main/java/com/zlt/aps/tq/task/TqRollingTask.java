package com.zlt.aps.tq.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tq.api.domain.entity.TqNewScheduleResult;
import com.zlt.aps.tq.engine.vo.RollingUpdateResult;
import com.zlt.aps.tq.mapper.TqNewScheduleResultMapper;
import com.zlt.aps.tq.service.ITqRollingUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 胎圈排程滚动更新定时任务
 *
 * <p>定时自动触发滚动更新，扫描当天所有有机台排程数据的记录，逐机台执行滚动更新。</p>
 *
 * <p>使用说明：</p>
 * <ol>
 *   <li>在定时任务管理界面配置任务，调用目标示例：tqRollingTask.autoRollingUpdate()</li>
 *   <li>预警频率通过系统定时任务的cron表达式配置</li>
 *   <li>建议在班次开始前一段时间执行，以便提前发现排程冲突</li>
 * </ol>
 *
 * @author APS
 */
@Slf4j
@Component("tqRollingTask")
public class TqRollingTask {

    @Autowired
    private ITqRollingUpdateService tqRollingUpdateService;

    @Resource
    private TqNewScheduleResultMapper tqNewScheduleResultMapper;

    /**
     * 自动滚动更新（当天所有机台）
     *
     * <p>扫描当天所有机台的排程数据，逐机台执行滚动更新。</p>
     * <p>调用目标：tqRollingTask.autoRollingUpdate()</p>
     */
    public void autoRollingUpdate() {
        log.info("胎圈排程自动滚动更新定时任务开始执行");
        long startTime = System.currentTimeMillis();
        try {
            Date today = new Date();

            // 查询当天所有机台编号
            LambdaQueryWrapper<TqNewScheduleResult> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TqNewScheduleResult::getScheduleDate, today)
                   .eq(TqNewScheduleResult::getIsDelete, 0)
                   .select(TqNewScheduleResult::getMachineCode, TqNewScheduleResult::getBeadCode);
            List<TqNewScheduleResult> scheduleList = tqNewScheduleResultMapper.selectList(wrapper);

            if (scheduleList.isEmpty()) {
                log.info("胎圈排程自动滚动更新：当天无排程数据，跳过执行");
                return;
            }

            // 按机台编号去重
            List<String> machineCodes = scheduleList.stream()
                    .map(TqNewScheduleResult::getMachineCode)
                    .filter(machineCode -> machineCode != null && !machineCode.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());

            log.info("胎圈排程自动滚动更新：共{}个机台需要处理", machineCodes.size());

            int successCount = 0;
            int failCount = 0;

            // 逐机台执行滚动更新
            for (String machineCode : machineCodes) {
                try {
                    // 获取该机台当天第一个排程的胎圈代码和班次
                    TqNewScheduleResult firstSchedule = getFirstSchedule(today, machineCode);
                    if (firstSchedule == null) {
                        continue;
                    }

                    // 执行滚动更新（触发类型：0-自动定时）
                    RollingUpdateResult result = tqRollingUpdateService.manualRollingUpdate(
                            "0", null, today, 1, machineCode, firstSchedule.getBeadCode());

                    if (result.isSuccess()) {
                        successCount++;
                        log.info("胎圈排程自动滚动更新成功：机台={}，影响记录数={}",
                                machineCode, result.getAffectedCount());
                    } else {
                        failCount++;
                        log.warn("胎圈排程自动滚动更新失败：机台={}，原因={}",
                                machineCode, result.getErrorMsg());
                    }
                } catch (Exception e) {
                    failCount++;
                    log.error("胎圈排程自动滚动更新异常：机台={}", machineCode, e);
                }
            }

            log.info("胎圈排程自动滚动更新定时任务执行结束：成功{}个，失败{}个，耗时{}ms",
                    successCount, failCount, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("胎圈排程自动滚动更新定时任务执行失败", e);
        }
    }

    /**
     * 自动滚动更新（指定机台）
     *
     * <p>调用目标：tqRollingTask.autoRollingUpdateByMachine('M001')</p>
     *
     * @param machineCode 机台编号
     */
    public void autoRollingUpdateByMachine(String machineCode) {
        log.info("胎圈排程自动滚动更新开始：机台={}", machineCode);
        long startTime = System.currentTimeMillis();
        try {
            Date today = new Date();
            TqNewScheduleResult firstSchedule = getFirstSchedule(today, machineCode);
            if (firstSchedule == null) {
                log.info("胎圈排程自动滚动更新：机台{}当天无排程数据", machineCode);
                return;
            }

            RollingUpdateResult result = tqRollingUpdateService.manualRollingUpdate(
                    "0", null, today, 1, machineCode, firstSchedule.getBeadCode());

            if (result.isSuccess()) {
                log.info("胎圈排程自动滚动更新成功：机台={}，影响记录数={}，耗时{}ms",
                        machineCode, result.getAffectedCount(), System.currentTimeMillis() - startTime);
            } else {
                log.warn("胎圈排程自动滚动更新失败：机台={}，原因={}", machineCode, result.getErrorMsg());
            }
        } catch (Exception e) {
            log.error("胎圈排程自动滚动更新异常：机台={}", machineCode, e);
        }
    }

    /**
     * 获取指定机台当天的第一条排程记录
     */
    private TqNewScheduleResult getFirstSchedule(Date scheduleDate, String machineCode) {
        LambdaQueryWrapper<TqNewScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqNewScheduleResult::getScheduleDate, scheduleDate)
               .eq(TqNewScheduleResult::getMachineCode, machineCode)
               .eq(TqNewScheduleResult::getIsDelete, 0)
               .orderByAsc(TqNewScheduleResult::getClass1Sequence)
               .last("FETCH FIRST 1 ROWS ONLY");
        List<TqNewScheduleResult> list = tqNewScheduleResultMapper.selectList(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }
}
