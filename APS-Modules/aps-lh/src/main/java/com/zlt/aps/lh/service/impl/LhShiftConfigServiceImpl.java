package com.zlt.aps.lh.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.lh.api.constant.LhScheduleConstant;
import com.zlt.aps.lh.api.domain.entity.LhShiftConfig;
import com.zlt.aps.lh.api.domain.vo.LhShiftConfigVO;
import com.zlt.aps.lh.api.enums.DeleteFlagEnum;
import com.zlt.aps.lh.api.enums.ShiftEnum;
import com.zlt.aps.lh.api.util.ShiftTimeParseUtil;
import com.zlt.aps.lh.api.util.ShiftTypeParseUtil;
import com.zlt.aps.lh.context.LhScheduleContext;
import com.zlt.aps.lh.mapper.LhShiftConfigMapper;
import com.zlt.aps.lh.service.ILhShiftConfigService;
import com.zlt.aps.lh.util.LhScheduleTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 硫化班次配置加载与解析
 *
 * @author APS
 */
@Slf4j
@Service
public class LhShiftConfigServiceImpl implements ILhShiftConfigService {

    /** 独立后置计划业务班次号。 */
    private static final int NEXT_SHIFT_INDEX = 9;
    /** 班次9归属T+3业务日期。 */
    private static final int NEXT_SHIFT_DATE_OFFSET = 3;

    @Resource
    private LhShiftConfigMapper lhShiftConfigMapper;

    @Override
    public List<LhShiftConfigVO> resolveAndAttachScheduleShifts(LhScheduleContext context) {
        String factoryCode = context.getFactoryCode();
        Date scheduleDate = context.getScheduleDate();
        if (StringUtils.isEmpty(factoryCode) || scheduleDate == null) {
            throw new IllegalArgumentException("工厂编号或排程T日为空，无法加载班次配置");
        }

        List<LhShiftConfig> rows = lhShiftConfigMapper.selectList(
                new LambdaQueryWrapper<LhShiftConfig>()
                        .eq(LhShiftConfig::getFactoryCode, factoryCode)
                        .eq(LhShiftConfig::getIsDelete, DeleteFlagEnum.NORMAL.getCode())
                        .orderByAsc(LhShiftConfig::getShiftIndex));

        List<LhShiftConfigVO> shifts;
        if (CollectionUtils.isEmpty(rows)) {
            log.info("工厂[{}]无班次表数据，使用默认班次模板", factoryCode);
            shifts = LhScheduleTimeUtil.buildDefaultScheduleShifts(context, scheduleDate);
        } else {
            int scheduleDays = LhScheduleTimeUtil.getScheduleDays(context);
            shifts = buildShiftsFromConfig(context, scheduleDate, rows, scheduleDays);
        }

        context.setScheduleWindowShifts(shifts);
        return shifts;
    }

    /**
     * 解析T+3业务日期晚班，供班次9独立后置计划使用。
     *
     * @param context 原8班排程上下文
     * @return 复用当前工厂晚班起止时间的班次9对象
     */
    @Override
    public LhShiftConfigVO resolveNextShiftNight(LhScheduleContext context) {
        if (Objects.isNull(context) || StringUtils.isEmpty(context.getFactoryCode())
                || Objects.isNull(context.getScheduleDate())) {
            throw new IllegalArgumentException("工厂编号或排程T日为空，无法解析班次9");
        }
        List<LhShiftConfig> rows = lhShiftConfigMapper.selectList(
                new LambdaQueryWrapper<LhShiftConfig>()
                        .eq(LhShiftConfig::getFactoryCode, context.getFactoryCode())
                        .orderByAsc(LhShiftConfig::getShiftIndex));
        LhShiftConfig nightTemplate = rows.stream()
                .filter(java.util.Objects::nonNull)
                .filter(row -> ShiftEnum.NIGHT_SHIFT == ShiftTypeParseUtil.parse(row.getShiftType()))
                .findFirst()
                .orElse(null);
        LhShiftConfigVO nextShift = new LhShiftConfigVO();
        if (Objects.nonNull(nightTemplate)) {
            BeanUtil.copyProperties(nightTemplate, nextShift);
        } else {
            LhShiftConfigVO defaultNightShift = LhScheduleTimeUtil
                    .buildDefaultScheduleShifts(context, context.getScheduleDate()).stream()
                    .filter(LhShiftConfigVO::isNightShift)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("默认班次模板缺少晚班"));
            BeanUtil.copyProperties(defaultNightShift, nextShift);
        }
        nextShift.setScheduleBaseDate(context.getScheduleDate());
        nextShift.setShiftIndex(NEXT_SHIFT_INDEX);
        nextShift.setDateOffset(NEXT_SHIFT_DATE_OFFSET);
        nextShift.setShiftType(ShiftEnum.NIGHT_SHIFT.getCode());
        nextShift.setShiftName("T+3日晚班");
        if (nextShift.getShiftStartDateTime() == null || nextShift.getShiftEndDateTime() == null) {
            throw new IllegalArgumentException("班次9起止时间无法按当前工厂晚班配置解析");
        }
        return nextShift;
    }

    /**
     * 将表记录转为 {@link LhShiftConfigVO} 并校验
     */
    private List<LhShiftConfigVO> buildShiftsFromConfig(LhScheduleContext context, Date scheduleDate,
            List<LhShiftConfig> rows, int scheduleDays) {
        int n = rows.size();
        if (n > LhScheduleConstant.MAX_SHIFT_SLOT_COUNT) {
            throw new IllegalArgumentException("班次配置超过上限" + LhScheduleConstant.MAX_SHIFT_SLOT_COUNT + "，当前=" + n);
        }
        for (int i = 0; i < n; i++) {
            LhShiftConfig row = rows.get(i);
            int expect = i + 1;
            if (row.getShiftIndex() == null || row.getShiftIndex() != expect) {
                throw new IllegalArgumentException(
                        "班次序号必须从1连续递增，期望 SHIFT_INDEX=" + expect + "，实际=" + row.getShiftIndex());
            }
            if (row.getDateOffset() == null) {
                throw new IllegalArgumentException("DATE_OFFSET 不能为空，SHIFT_INDEX=" + expect);
            }
            if (row.getDateOffset() < 0 || row.getDateOffset() >= scheduleDays) {
                throw new IllegalArgumentException(
                        "DATE_OFFSET 越界：要求 0≤DATE_OFFSET<排程天数(" + scheduleDays + ")，SHIFT_INDEX=" + expect);
            }
            if (StringUtils.isEmpty(row.getStartTime()) || StringUtils.isEmpty(row.getEndTime())) {
                throw new IllegalArgumentException("START_TIME/END_TIME 不能为空，SHIFT_INDEX=" + expect);
            }
            try {
                ShiftTimeParseUtil.parseToHms(row.getStartTime().trim());
                ShiftTimeParseUtil.parseToHms(row.getEndTime().trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("SHIFT_INDEX=" + expect + " 时刻非法: " + e.getMessage(), e);
            }
            ShiftEnum type = ShiftTypeParseUtil.parse(row.getShiftType());
            if (type == null) {
                throw new IllegalArgumentException("无法识别班次类型：" + row.getShiftType() + "，SHIFT_INDEX=" + expect);
            }
        }

        List<LhShiftConfigVO> list = new ArrayList<>(n);
        for (LhShiftConfig row : rows) {
            ShiftEnum shiftType = ShiftTypeParseUtil.parse(row.getShiftType());
            LhShiftConfigVO vo = new LhShiftConfigVO();
            BeanUtil.copyProperties(row, vo);
            vo.setScheduleBaseDate(scheduleDate);
            vo.setStartTime(row.getStartTime().trim());
            vo.setEndTime(row.getEndTime().trim());
            String shiftName = StringUtils.isNotEmpty(row.getShiftName())
                    ? row.getShiftName()
                    : buildShiftDisplayName(row.getDateOffset(), shiftType);
            vo.setShiftName(shiftName);
            list.add(vo);
        }
        return list;
    }

    private static String buildShiftDisplayName(int dateOffset, ShiftEnum type) {
        String prefix;
        if (dateOffset == 0) {
            prefix = "T日";
        } else {
            prefix = "T+" + dateOffset + "日";
        }
        return prefix + type.getDescription();
    }
}
