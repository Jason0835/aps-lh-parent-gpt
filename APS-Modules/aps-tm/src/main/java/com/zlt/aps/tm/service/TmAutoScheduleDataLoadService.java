package com.zlt.aps.tm.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmParams;
import com.zlt.aps.tm.engine.domain.TmParamValue;
import com.zlt.aps.tm.engine.domain.TmScheduleContext;
import com.zlt.aps.tm.engine.domain.TmTaskDraft;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.mapper.TmParamsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面自动排程数据加载服务。
 *
 * <p>该服务属于胎面业务模块，负责在自动排程入口事务内读取排程需要的基础数据并填充
 * {@link TmScheduleContext}。服务只做数据加载和任务草稿构造，不写排程结果、不修改任务链。</p>
 */
@Slf4j
@Service
public class TmAutoScheduleDataLoadService {

    private static final String PARAM_ALGORITHM_SWITCH = "TM_ALGORITHM_SWITCH";

    private static final String PARAM_ALGORITHM_TYPE = "DEMAND_QTY_CALCULATE_TYPE";

    private static final String PARAM_STOCK_GUARD_SHIFT_COUNT = "TM_STOCK_GUARD_SHIFT_COUNT";

    private static final String PARAM_MIN_START_QTY = "TM_MIN_START_QTY";

    private static final String PARAM_DEFAULT_CURL_LENGTH = "TM_DEFAULT_CURL_LENGTH";

    private static final String PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED = "TM_SHUTDOWN_REDISTRIBUTION_ENABLED";

    private static final String PROC_CODE_CX = "03";

    private static final String PROC_CODE_TM = "04";

    private static final String YES = "1";

    private static final String NO = "0";

    @Resource
    private TmParamsMapper tmParamsMapper;

    @Resource
    private TmMachineInfoMapper tmMachineInfoMapper;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    /**
     * 加载自动排程所需数据。
     *
     * @param context 自动排程上下文，必须包含工厂和排程日期
     * @throws IllegalArgumentException 上下文、工厂或排程日期为空时抛出
     */
    public void loadAllData(TmScheduleContext context) {
        validateContext(context);
        loadParams(context);
        List<TmMachineInfo> machineList = loadMachineInfo(context);
        List<TmTaskDraft> taskDraftList = loadFormingDemandTasks(context, machineList);
        context.setTaskDraftList(taskDraftList);
        log.info("[TM_AUTO_SCHEDULE_LOAD] factoryCode={}, scheduleDate={}, taskCount={}, machineCount={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()),
                taskDraftList.size(), machineList.size());
    }

    /**
     * 加载胎面排程参数快照。
     *
     * @param context 自动排程上下文
     */
    private void loadParams(TmScheduleContext context) {
        LambdaQueryWrapper<TmParams> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmParams::getFactoryCode, context.getFactoryCode());
        wrapper.eq(TmParams::getEnableStatus, YES);
        List<TmParams> paramsList = tmParamsMapper.selectList(wrapper);
        Map<String, TmParamValue> paramMap = new HashMap<>();
        if (CollUtil.isNotEmpty(paramsList)) {
            for (TmParams params : paramsList) {
                TmParamValue value = new TmParamValue();
                value.setParamCode(params.getParamCode());
                value.setParamValue(params.getParamValue());
                value.setDefaultValue(params.getDefaultValue());
                value.setSource("T_TM_PARAMS");
                paramMap.put(params.getParamCode(), value);
            }
        }
        putDefaultParam(paramMap, PARAM_ALGORITHM_SWITCH, "1");
        putDefaultParam(paramMap, PARAM_ALGORITHM_TYPE, "1");
        putDefaultParam(paramMap, PARAM_STOCK_GUARD_SHIFT_COUNT, "2");
        putDefaultParam(paramMap, PARAM_MIN_START_QTY, "0");
        putDefaultParam(paramMap, PARAM_DEFAULT_CURL_LENGTH, "0");
        putDefaultParam(paramMap, PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED, "1");
        context.setParamMap(paramMap);
    }

    /**
     * 加载胎面机台基础资料。
     *
     * @param context 自动排程上下文
     * @return 已启用或可参与排程的机台列表
     */
    private List<TmMachineInfo> loadMachineInfo(TmScheduleContext context) {
        LambdaQueryWrapper<TmMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMachineInfo::getFactoryCode, context.getFactoryCode());
        wrapper.orderByAsc(TmMachineInfo::getMachineCode);
        return tmMachineInfoMapper.selectList(wrapper).stream()
                .filter(machine -> StrUtil.isNotBlank(machine.getMachineCode()))
                .collect(Collectors.toList());
    }

    /**
     * 从成型计划和施工信息构造胎面待排任务。
     *
     * @param context     自动排程上下文
     * @param machineList 胎面机台列表
     * @return 胎面待排任务列表
     */
    private List<TmTaskDraft> loadFormingDemandTasks(TmScheduleContext context, List<TmMachineInfo> machineList) {
        if (jdbcTemplate == null) {
            log.warn("[TM_AUTO_SCHEDULE_LOAD] JdbcTemplate不存在，无法加载成型计划和施工信息");
            return Collections.emptyList();
        }
        String sql = "SELECT cx.ORDER_NO, cx.EMBRYO_CODE, cx.BOM_DATA_VERSION, "
                + "cx.CLASS1_PLAN_QTY, cx.CLASS2_PLAN_QTY, cx.CLASS3_PLAN_QTY, "
                + "cx.CLASS4_PLAN_QTY, cx.CLASS5_PLAN_QTY, cx.CLASS6_PLAN_QTY, "
                + "ci.TREAD_CODE, ci.TREAD_SHOULDER_LENGTH, ci.TREAD_MOUTH_PLATE, ci.TREAD_RUBBER_CATEGORY "
                + "FROM T_CX_SCHEDULE_RESULT cx "
                + "LEFT JOIN T_MDM_CONSTRUCTION_INFO ci "
                + "ON cx.EMBRYO_CODE = ci.CONSTRUCTION_CODE "
                + "AND cx.BOM_DATA_VERSION = ci.CONSTRUCTION_VERSION "
                + "WHERE cx.FACTORY_CODE = ? AND cx.SCHEDULE_DATE = ?";
        List<Map<String, Object>> rowList;
        try {
            rowList = jdbcTemplate.queryForList(sql, context.getFactoryCode(), context.getScheduleDate());
        } catch (RuntimeException ex) {
            log.warn("[TM_AUTO_SCHEDULE_LOAD] 加载成型计划和施工信息失败，scheduleDate={}，原因={}",
                    DateUtil.formatDate(context.getScheduleDate()), ex.getMessage());
            return Collections.emptyList();
        }
        if (CollUtil.isEmpty(rowList)) {
            return Collections.emptyList();
        }
        // 校验成型关联施工的关键字段是否为空，收集所有有问题的规格统一提示
        List<String> treadCodeEmptyList = new ArrayList<>();
        List<String> treadLengthEmptyList = new ArrayList<>();
        List<String> mouthPlateEmptyList = new ArrayList<>();
        List<String> rubberCategoryEmptyList = new ArrayList<>();
        for (Map<String, Object> row : rowList) {
            String orderNo = valueAsString(row, "ORDER_NO");
            String treadCode = valueAsString(row, "TREAD_CODE");
            BigDecimal treadLength = valueAsDecimal(row, "TREAD_SHOULDER_LENGTH");
            String mouthPlate = valueAsString(row, "TREAD_MOUTH_PLATE");
            String rubberCategory = valueAsString(row, "TREAD_RUBBER_CATEGORY");
            if (StrUtil.isBlank(treadCode)) {
                treadCodeEmptyList.add(orderNo);
            }
            if (treadLength == null || treadLength.compareTo(BigDecimal.ZERO) <= 0) {
                treadLengthEmptyList.add(orderNo);
            }
            if (StrUtil.isBlank(mouthPlate)) {
                mouthPlateEmptyList.add(orderNo);
            }
            if (StrUtil.isBlank(rubberCategory)) {
                rubberCategoryEmptyList.add(orderNo);
            }
        }
        // 统一抛出校验异常
        StringBuilder errorMsg = new StringBuilder();
        if (CollUtil.isNotEmpty(treadCodeEmptyList)) {
            errorMsg.append("成型规格：").append(String.join("、", treadCodeEmptyList)).append("，胎面代码为空；");
        }
        if (CollUtil.isNotEmpty(treadLengthEmptyList)) {
            errorMsg.append("成型规格：").append(String.join("、", treadLengthEmptyList)).append("，胎面长为空；");
        }
        if (CollUtil.isNotEmpty(mouthPlateEmptyList)) {
            errorMsg.append("成型规格：").append(String.join("、", mouthPlateEmptyList)).append("，胎面口型板为空；");
        }
        if (CollUtil.isNotEmpty(rubberCategoryEmptyList)) {
            errorMsg.append("成型规格：").append(String.join("、", rubberCategoryEmptyList)).append("，胎面胶料为空；");
        }
        if (errorMsg.length() > 0) {
            // 移除末尾的分号
            errorMsg.setLength(errorMsg.length() - 1);
            throw new RuntimeException(errorMsg.toString());
        }
        TmMachineInfo defaultMachine = CollUtil.isEmpty(machineList) ? null : machineList.get(0);
        String algorithmCode = getParamValue(context, PARAM_ALGORITHM_SWITCH, getParamValue(context, PARAM_ALGORITHM_TYPE, "1"));
        BigDecimal minStartQty = getDecimalParam(context, PARAM_MIN_START_QTY);
        BigDecimal defaultCurlLength = getDecimalParam(context, PARAM_DEFAULT_CURL_LENGTH);
        Integer guardShiftCount = getIntegerParam(context, PARAM_STOCK_GUARD_SHIFT_COUNT, 2);
        Map<String, Object> tmCalendar = loadWorkCalendar(context, PROC_CODE_TM);
        Map<String, Object> cxCalendar = loadWorkCalendar(context, PROC_CODE_CX);
        List<TmTaskDraft> taskDraftList = new ArrayList<>();
        for (Map<String, Object> row : rowList) {
            String treadCode = valueAsString(row, "TREAD_CODE");
            BigDecimal treadLength = valueAsDecimal(row, "TREAD_SHOULDER_LENGTH");
            if (StrUtil.isBlank(treadCode) || treadLength.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal[] classQtyArray = buildClassQtyArray(row);
            boolean noShutdownAvailableShift = redistributeShutdownDemand(context, classQtyArray, tmCalendar, cxCalendar);
            for (int shiftOrder = 1; shiftOrder <= 6; shiftOrder++) {
                BigDecimal formingQty = resolveFormingQty(classQtyArray, shiftOrder, algorithmCode);
                BigDecimal demandQty = formingQty.multiply(treadLength);
                if (demandQty.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                TmTaskDraft taskDraft = new TmTaskDraft();
                taskDraft.setOrderNo(valueAsString(row, "ORDER_NO") + "-CLASS" + shiftOrder);
                taskDraft.setTreadCode(treadCode);
                taskDraft.setGlueCode(valueAsString(row, "TREAD_RUBBER_CATEGORY"));
                taskDraft.setMouthPlateCode(valueAsString(row, "TREAD_MOUTH_PLATE"));
                taskDraft.setShiftOrder(shiftOrder);
                taskDraft.setCurrentShiftDemandQty(demandQty);
                taskDraft.setGuardDemandQty(calculateGuardDemand(classQtyArray, shiftOrder, guardShiftCount).multiply(treadLength));
                taskDraft.setDemandQty(demandQty);
                taskDraft.setGuardShiftCount(guardShiftCount);
                taskDraft.setMinStartQty(minStartQty);
                taskDraft.setDefaultCurlRollLength(defaultCurlLength);
                if (noShutdownAvailableShift && !isShiftOpen(tmCalendar, shiftOrder) && isShiftOpen(cxCalendar, shiftOrder)) {
                    taskDraft.setUnplannedReasonCode("TM_SHUTDOWN_NO_AVAILABLE_SHIFT");
                    taskDraft.setUnplannedReasonDesc("胎面停产且无可分配班次，成型需求无法重分配");
                } else if (defaultMachine != null) {
                    taskDraft.setMachineCode(defaultMachine.getMachineCode());
                    taskDraft.setMachineRemainCapacity(defaultMachine.getMaxCapacity());
                }
                taskDraftList.add(taskDraft);
            }
        }
        return taskDraftList;
    }

    /**
     * 根据工作日历处理当前排程日停产需求重分配。
     *
     * @param context       自动排程上下文
     * @param classQtyArray 六班成型数量
     * @param tmCalendar    胎面工作日历
     * @param cxCalendar    成型工作日历
     * @return true 表示胎面停产且没有可接收重分配需求的班次
     */
    private boolean redistributeShutdownDemand(TmScheduleContext context, BigDecimal[] classQtyArray,
                                               Map<String, Object> tmCalendar, Map<String, Object> cxCalendar) {
        if (!YES.equals(getParamValue(context, PARAM_SHUTDOWN_REDISTRIBUTION_ENABLED, YES))) {
            return false;
        }
        if (!isShutdownDay(tmCalendar) || isShutdownDay(cxCalendar)) {
            return false;
        }
        List<Integer> shutdownShiftList = new ArrayList<>();
        List<Integer> availableShiftList = new ArrayList<>();
        BigDecimal shutdownQty = BigDecimal.ZERO;
        for (int shiftOrder = 1; shiftOrder <= 6; shiftOrder++) {
            if (isShiftOpen(tmCalendar, shiftOrder)) {
                availableShiftList.add(shiftOrder);
                continue;
            }
            if (isShiftOpen(cxCalendar, shiftOrder)) {
                shutdownShiftList.add(shiftOrder);
                shutdownQty = shutdownQty.add(classQtyArray[shiftOrder - 1]);
            }
        }
        if (CollUtil.isEmpty(shutdownShiftList) || shutdownQty.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        if (CollUtil.isEmpty(availableShiftList)) {
            log.warn("[TM_AUTO_SCHEDULE_SHUTDOWN] factoryCode={}, scheduleDate={} 胎面停产且无可分配班次",
                    context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()));
            return true;
        }
        BigDecimal increaseQty = shutdownQty.divide(new BigDecimal(availableShiftList.size()), 6, RoundingMode.HALF_UP);
        for (Integer shiftOrder : shutdownShiftList) {
            classQtyArray[shiftOrder - 1] = BigDecimal.ZERO;
        }
        for (Integer shiftOrder : availableShiftList) {
            classQtyArray[shiftOrder - 1] = classQtyArray[shiftOrder - 1].add(increaseQty);
        }
        log.info("[TM_AUTO_SCHEDULE_SHUTDOWN] factoryCode={}, scheduleDate={}, shutdownQty={}, availableShiftCount={}",
                context.getFactoryCode(), DateUtil.formatDate(context.getScheduleDate()), shutdownQty, availableShiftList.size());
        return false;
    }

    /**
     * 加载指定工序的当前排程日工作日历。
     *
     * @param context  自动排程上下文
     * @param procCode 工序编码
     * @return 工作日历行，未维护时返回空 Map
     */
    private Map<String, Object> loadWorkCalendar(TmScheduleContext context, String procCode) {
        if (jdbcTemplate == null) {
            return Collections.emptyMap();
        }
        String sql = "SELECT DAY_FLAG, ONE_SHIFT_FLAG, TWO_SHIFT_FLAG, THREE_SHIFT_FLAG "
                + "FROM T_MDM_WORK_CALENDAR "
                + "WHERE FACTORY_CODE = ? AND PROC_CODE = ? AND PRODUCTION_DATE = ?";
        try {
            List<Map<String, Object>> rowList = jdbcTemplate.queryForList(sql,
                    context.getFactoryCode(), procCode, DateUtil.beginOfDay(context.getScheduleDate()));
            return CollUtil.isEmpty(rowList) ? Collections.emptyMap() : rowList.get(0);
        } catch (RuntimeException ex) {
            log.warn("[TM_AUTO_SCHEDULE_LOAD] 加载工作日历失败，procCode={}，scheduleDate={}，原因={}",
                    procCode, DateUtil.formatDate(context.getScheduleDate()), ex.getMessage());
            return Collections.emptyMap();
        }
    }

    private boolean isShutdownDay(Map<String, Object> calendar) {
        if (calendar == null || calendar.isEmpty()) {
            return false;
        }
        return NO.equals(valueAsString(calendar, "DAY_FLAG"))
                || (!isShiftOpen(calendar, 1) && !isShiftOpen(calendar, 2) && !isShiftOpen(calendar, 3));
    }

    private boolean isShiftOpen(Map<String, Object> calendar, int shiftOrder) {
        if (calendar == null || calendar.isEmpty()) {
            return true;
        }
        int calendarShift = ((shiftOrder - 1) % 3) + 1;
        if (calendarShift == 1) {
            return YES.equals(valueAsString(calendar, "ONE_SHIFT_FLAG"));
        }
        if (calendarShift == 2) {
            return YES.equals(valueAsString(calendar, "TWO_SHIFT_FLAG"));
        }
        return YES.equals(valueAsString(calendar, "THREE_SHIFT_FLAG"));
    }

    private BigDecimal resolveFormingQty(BigDecimal[] classQtyArray, int shiftOrder, String algorithmCode) {
        if ("2".equals(algorithmCode)) {
            int nextIndex = Math.min(shiftOrder, classQtyArray.length - 1);
            return classQtyArray[nextIndex];
        }
        return Arrays.stream(classQtyArray, 0, Math.min(3, classQtyArray.length))
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal calculateGuardDemand(BigDecimal[] classQtyArray, int shiftOrder, int guardShiftCount) {
        BigDecimal total = BigDecimal.ZERO;
        int startIndex = Math.max(shiftOrder - 1, 0);
        int endIndex = Math.min(startIndex + Math.max(guardShiftCount, 1), classQtyArray.length);
        for (int i = startIndex; i < endIndex; i++) {
            total = total.add(classQtyArray[i]);
        }
        return total;
    }

    private BigDecimal[] buildClassQtyArray(Map<String, Object> row) {
        return new BigDecimal[]{
                valueAsDecimal(row, "CLASS1_PLAN_QTY"),
                valueAsDecimal(row, "CLASS2_PLAN_QTY"),
                valueAsDecimal(row, "CLASS3_PLAN_QTY"),
                valueAsDecimal(row, "CLASS4_PLAN_QTY"),
                valueAsDecimal(row, "CLASS5_PLAN_QTY"),
                valueAsDecimal(row, "CLASS6_PLAN_QTY")
        };
    }

    private void putDefaultParam(Map<String, TmParamValue> paramMap, String paramCode, String defaultValue) {
        if (paramMap.containsKey(paramCode)) {
            return;
        }
        TmParamValue value = new TmParamValue();
        value.setParamCode(paramCode);
        value.setDefaultValue(defaultValue);
        value.setSource("DEFAULT");
        paramMap.put(paramCode, value);
    }

    private String getParamValue(TmScheduleContext context, String paramCode, String defaultValue) {
        TmParamValue value = context.getParamMap().get(paramCode);
        return value == null || StrUtil.isBlank(value.getEffectiveValue()) ? defaultValue : value.getEffectiveValue();
    }

    private BigDecimal getDecimalParam(TmScheduleContext context, String paramCode) {
        String value = getParamValue(context, paramCode, "0");
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private Integer getIntegerParam(TmScheduleContext context, String paramCode, Integer defaultValue) {
        String value = getParamValue(context, paramCode, String.valueOf(defaultValue));
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private BigDecimal valueAsDecimal(Map<String, Object> row, String key) {
        Object value = value(row, key);
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        if (StrUtil.isBlank(value.toString())) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString());
    }

    private String valueAsString(Map<String, Object> row, String key) {
        Object value = value(row, key);
        return value == null ? null : value.toString();
    }

    private Object value(Map<String, Object> row, String key) {
        if (row.containsKey(key)) {
            return row.get(key);
        }
        if (row.containsKey(key.toLowerCase(Locale.ROOT))) {
            return row.get(key.toLowerCase(Locale.ROOT));
        }
        return row.get(key.toUpperCase(Locale.ROOT));
    }

    private void validateContext(TmScheduleContext context) {
        if (context == null) {
            throw new IllegalArgumentException("自动排程上下文不能为空");
        }
        if (StrUtil.isBlank(context.getFactoryCode())) {
            throw new IllegalArgumentException("自动排程工厂编号不能为空");
        }
        if (context.getScheduleDate() == null) {
            throw new IllegalArgumentException("自动排程日期不能为空");
        }
    }
}
