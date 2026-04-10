package com.zlt.aps.lh.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.api.domain.dto.LhScheduleRequestDTO;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResponseDTO;
import com.zlt.aps.lh.api.enums.ReleaseStatusEnum;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhScheduleShiftDateVO;
import com.zlt.aps.lh.engine.rule.IScheduleRuleEngine;
import com.zlt.aps.lh.engine.decorator.IScheduleExecutor;
import com.zlt.aps.lh.engine.observer.ScheduleEvent;
import com.zlt.aps.lh.engine.observer.ScheduleEventPublisher;
import com.zlt.aps.lh.mapper.LhScheduleResultMapper;
import com.zlt.aps.lh.service.ILhScheduleService;
import com.zlt.bill.common.service.AbstractDocService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 硫化排程主服务实现
 * <p>排程入口，负责构建上下文并委托给排程执行器</p>
 *
 * @author APS
 */
@Slf4j
@Service
public class LhScheduleServiceImpl extends AbstractDocService<LhScheduleResult> implements ILhScheduleService {

    @Resource
    private IScheduleExecutor scheduleExecutor;

    @Resource
    private IScheduleRuleEngine scheduleRuleEngine;

    @Resource
    private LhScheduleResultMapper scheduleResultMapper;

    @Resource
    private ScheduleEventPublisher scheduleEventPublisher;

    @Override
    public LhScheduleResponseDTO executeSchedule(LhScheduleRequestDTO request) {
        log.info("接收排程请求, 工厂: {}, 日期: {}", request.getFactoryCode(), request.getScheduleDate());
        LhScheduleContext context = buildContext(request);
        return scheduleExecutor.execute(context);
    }

    @Override
    public LhScheduleResponseDTO publishSchedule(String batchNo) {
        log.info("发布排程结果, 批次号: {}", batchNo);
        try {
            // 1. 查询批次号对应的排程结果
            List<LhScheduleResult> results = scheduleResultMapper.selectByBatchNo(batchNo);
            if (results == null || results.isEmpty()) {
                return LhScheduleResponseDTO.fail(batchNo, "批次号[" + batchNo + "]对应的排程结果不存在");
            }

            // 2. 更新发布状态为"已发布"（1）
            for (LhScheduleResult result : results) {
                result.setIsRelease(ReleaseStatusEnum.RELEASED.getCode());
            }
            scheduleResultMapper.updateReleaseStatus(batchNo, ReleaseStatusEnum.RELEASED.getCode());

            // 3. 发布排程结果发布事件（通知MES系统）
            LhScheduleContext publishContext = new LhScheduleContext();
            publishContext.setBatchNo(batchNo);
            publishContext.setScheduleResultList(results);
            scheduleEventPublisher.publish(ScheduleEvent.published(publishContext));

            log.info("排程结果发布成功, 批次号: {}, 发布记录数: {}", batchNo, results.size());
            return LhScheduleResponseDTO.success(batchNo, "发布成功，共发布" + results.size() + "条记录");

        } catch (Exception e) {
            log.error("发布排程结果异常, 批次号: {}", batchNo, e);
            return LhScheduleResponseDTO.fail(batchNo, "发布失败: " + e.getMessage());
        }
    }

    private LhScheduleContext buildContext(LhScheduleRequestDTO request) {
        LhScheduleContext context = new LhScheduleContext();
        context.setFactoryCode(request.getFactoryCode());
        // 请求日期为排程目标日
        Date target = LhScheduleTimeUtil.clearTime(
                request.getScheduleDate() != null ? request.getScheduleDate() : new Date());
        context.setScheduleTargetDate(target);
        int scheduleDays = scheduleRuleEngine.getScheduleDays(request.getFactoryCode());
        int offsetDays = Math.max(0, scheduleDays - 1);
        // 引擎使用 T 日 = 目标日 − (连续排程日历跨度 − 1)；与 DataInit 中按参数校正后一致
        context.setScheduleDate(LhScheduleTimeUtil.addDays(target, -offsetDays));
        return context;
    }

    @Override
    public String getDocTypeCode() {
        return "OUT2046";
    }

    @Override
    public List<LhScheduleShiftDateVO> listScheduleShiftDates(String scheduleDate) {
        if (StringUtils.isEmpty(scheduleDate)) {
            log.warn("listScheduleShiftDates: scheduleDate 为空");
            return new ArrayList<>();
        }
        Date end;
        try {
            end = DateUtil.beginOfDay(DateUtil.parse(scheduleDate.trim(), DatePattern.NORM_DATE_PATTERN));
        } catch (Exception e) {
            log.warn("listScheduleShiftDates: 排程日期解析失败, scheduleDate={}", scheduleDate, e);
            return new ArrayList<>();
        }
        Date start = DateUtil.offsetDay(end, -(LhScheduleConstant.SCHEDULE_DAYS - 1));
        List<LhScheduleShiftDateVO> result = new ArrayList<>(LhScheduleConstant.MAX_SHIFT_SLOT_COUNT);
        int shiftNo = 1;
        for (int dayIndex = 0; dayIndex < LhScheduleConstant.SCHEDULE_DAYS; dayIndex++) {
            int shiftsThisDay = dayIndex == 0
                    ? LhScheduleConstant.SCHEDULE_SHIFT_DATE_WINDOW_FIRST_DAY_SHIFT_COUNT
                    : LhScheduleConstant.SCHEDULE_SHIFT_DATE_WINDOW_OTHER_DAY_SHIFT_COUNT;
            Date current = DateUtil.offsetDay(start, dayIndex);
            String shiftDateStr = DateUtil.format(current, LhScheduleConstant.SCHEDULE_SHIFT_DATE_DISPLAY_PATTERN);
            for (int i = 0; i < shiftsThisDay; i++) {
                LhScheduleShiftDateVO vo = new LhScheduleShiftDateVO();
                vo.setShift(shiftNo);
                vo.setShiftDate(shiftDateStr);
                result.add(vo);
                shiftNo++;
            }
        }
        return result;
    }

}
