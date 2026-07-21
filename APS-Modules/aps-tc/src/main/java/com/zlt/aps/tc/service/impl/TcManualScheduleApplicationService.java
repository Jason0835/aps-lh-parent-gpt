package com.zlt.aps.tc.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.tc.api.constant.TcScheduleConstants;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;
import com.zlt.aps.tc.api.domain.entity.TcScheduleUnplanned;
import com.zlt.aps.tc.api.domain.entity.TcShiftConfig;
import com.zlt.aps.tc.api.domain.vo.*;
import com.zlt.aps.tc.mapper.TcScheduleResultMapper;
import com.zlt.aps.tc.mapper.TcScheduleUnplannedMapper;
import com.zlt.aps.tc.mapper.TcShiftConfigMapper;
import com.zlt.aps.tc.service.query.TcManualOptionsService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 胎侧人工排程应用服务。
 *
 * <p>负责校验公开请求 DTO、解析后端可信施工快照并转换为横表结果，分布式锁、行锁、版本和审计
 * 由 {@link TcManualOperationFacade} 在事务内统一处理。</p>
 */
@Service
public class TcManualScheduleApplicationService {

    private final TcManualOptionsService manualOptionsService;

    private final TcManualOperationFacade manualOperationFacade;

    private final TcScheduleResultMapper scheduleResultMapper;

    private final TcShiftConfigMapper shiftConfigMapper;

    private final TcScheduleUnplannedMapper scheduleUnplannedMapper;

    /**
     * 构造人工排程应用服务。
     *
     * @param manualOptionsService 人工选项服务
     * @param manualOperationFacade 人工操作门面
     * @param scheduleResultMapper 排程结果 Mapper
     * @param shiftConfigMapper 班次配置 Mapper
     * @param scheduleUnplannedMapper 未排任务 Mapper
     */
    public TcManualScheduleApplicationService(TcManualOptionsService manualOptionsService,
                                              TcManualOperationFacade manualOperationFacade,
                                              TcScheduleResultMapper scheduleResultMapper,
                                              TcShiftConfigMapper shiftConfigMapper,
                                              TcScheduleUnplannedMapper scheduleUnplannedMapper) {
        this.manualOptionsService = manualOptionsService;
        this.manualOperationFacade = manualOperationFacade;
        this.scheduleResultMapper = scheduleResultMapper;
        this.shiftConfigMapper = shiftConfigMapper;
        this.scheduleUnplannedMapper = scheduleUnplannedMapper;
    }

    /**
     * 执行多班次人工插单。
     *
     * @param requestVo 插单请求
     * @return 新增结果行数
     */
    public int insertTask(TcInsertTaskRequestVo requestVo) {
        this.validateInsertRequest(requestVo);
        List<TcManualShiftItemVo> shiftItemList = requestVo.getShiftList().stream()
                .filter(Objects::nonNull).filter(item -> item.getPlanQty() != null
                        && item.getPlanQty().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(TcManualShiftItemVo::getShiftOrder)).collect(Collectors.toList());
        this.validateShiftItems(requestVo.getFactoryCode(), requestVo.getScheduleDate(), shiftItemList);

        TcScheduleResult insertResult = this.manualOptionsService.resolveConstruction(requestVo.getFactoryCode(),
                requestVo.getSidewallCode(), requestVo.getConstructionVersion());
        insertResult.setScheduleDate(requestVo.getScheduleDate());
        insertResult.setMachineCode(requestVo.getMachineCode().trim());
        insertResult.setBatchNo(this.resolveCurrentBatchNo(requestVo.getFactoryCode(), requestVo.getScheduleDate()));
        insertResult.setOrderNo(insertResult.getBatchNo() + "-MANUAL-" + IdUtil.fastSimpleUUID().substring(0, 8));
        insertResult.setTailFlag("0");
        shiftItemList.forEach(item -> {
            insertResult.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE,
                    item.getShiftOrder()), item.getPlanQty());
            insertResult.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE,
                    item.getShiftOrder()), item.getSequence());
        });
        return this.manualOperationFacade.insertTask(insertResult, requestVo.getReason().trim());
    }

    /**
     * 执行选中班次调量。
     *
     * @param requestVo 调量请求
     * @return 受影响行数
     */
    public int changeQty(TcChangeQtyRequestVo requestVo) {
        if (requestVo == null || requestVo.getResultId() == null || requestVo.getShiftOrder() == null
                || requestVo.getShiftOrder() < 1
                || requestVo.getShiftOrder() > TcScheduleConstants.TC_MAX_SHIFT_ORDER
                || requestVo.getNewPlanQty() == null || requestVo.getNewPlanQty().compareTo(BigDecimal.ZERO) < 0
                || requestVo.getExpectedTaskVersion() == null || StringUtils.isBlank(requestVo.getReason())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeQty.invalidRequest"));
        }
        TcScheduleResult changeResult = new TcScheduleResult();
        changeResult.setId(requestVo.getResultId());
        changeResult.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE,
                requestVo.getShiftOrder()), requestVo.getNewPlanQty());
        TcScheduleResult current = this.scheduleResultMapper.selectById(requestVo.getResultId());
        if (current == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.resultNotFound"));
        }
        this.validateShiftOrders(current.getFactoryCode(), current.getScheduleDate(),
                Collections.singletonList(requestVo.getShiftOrder()));
        return this.manualOperationFacade.changeQty(changeResult, requestVo.getExpectedTaskVersion(),
                requestVo.getReason().trim());
    }

    /**
     * 原子执行批量普通转机台。
     *
     * @param requestVo 转机请求
     * @return 受影响行数
     */
    public int changeMachine(TcChangeMachineRequestVo requestVo) {
        if (requestVo == null || requestVo.getTaskList() == null || requestVo.getTaskList().isEmpty()
                || StringUtils.isBlank(requestVo.getTargetMachineCode()) || StringUtils.isBlank(requestVo.getReason())) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.invalidRequest"));
        }
        List<TcScheduleResult> transferResultList = new ArrayList<>();
        List<TcScheduleResult> currentResultList = new ArrayList<>();
        List<Long> expectedVersionList = new ArrayList<>();
        List<Integer> shiftOrderList = new ArrayList<>();
        for (TcChangeMachineTaskVo taskVo : requestVo.getTaskList()) {
            if (taskVo == null || taskVo.getResultId() == null || taskVo.getExpectedTaskVersion() == null
                    || taskVo.getShiftOrder() == null || taskVo.getShiftOrder() < 1
                    || taskVo.getShiftOrder() > TcScheduleConstants.TC_MAX_SHIFT_ORDER) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.invalidRequest"));
            }
            TcScheduleResult current = this.scheduleResultMapper.selectById(taskVo.getResultId());
            if (current == null) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.resultNotFound"));
            }
            Object currentPlanQty = current.getFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, taskVo.getShiftOrder()));
            if (!(currentPlanQty instanceof BigDecimal)
                    || ((BigDecimal) currentPlanQty).compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.changeMachine.shiftEmpty"));
            }
            TcScheduleResult transferResult = new TcScheduleResult();
            transferResult.setId(taskVo.getResultId());
            transferResult.setMachineCode(requestVo.getTargetMachineCode().trim());
            transferResult.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE,
                    taskVo.getShiftOrder()), currentPlanQty);
            Object currentSequence = current.getFieldValueByFieldName(String.format(
                    TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, taskVo.getShiftOrder()));
            transferResult.setFieldValueByFieldName(String.format(TcScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE,
                    taskVo.getShiftOrder()), currentSequence);
            transferResultList.add(transferResult);
            currentResultList.add(current);
            expectedVersionList.add(taskVo.getExpectedTaskVersion());
            shiftOrderList.add(taskVo.getShiftOrder());
        }
        TcScheduleResult reference = currentResultList.get(0);
        boolean rangeInvalid = currentResultList.stream().anyMatch(item -> !Objects.equals(
                reference.getFactoryCode(), item.getFactoryCode()) || !Objects.equals(reference.getScheduleDate(),
                item.getScheduleDate()) || !Objects.equals(reference.getBatchNo(), item.getBatchNo()));
        if (rangeInvalid) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.batchRangeInvalid"));
        }
        this.validateShiftOrders(reference.getFactoryCode(), reference.getScheduleDate(), shiftOrderList);
        return this.manualOperationFacade.changeMachine(transferResultList, expectedVersionList,
                requestVo.getReason().trim());
    }

    /**
     * 按结果 ID 整行删除排程结果。
     *
     * @param resultIdList 结果 ID
     * @return 删除行数
     */
    public int remove(List<Long> resultIdList) {
        List<Long> normalizedIdList = resultIdList == null ? Collections.emptyList()
                : resultIdList.stream().filter(Objects::nonNull).distinct().sorted().collect(Collectors.toList());
        if (normalizedIdList.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.remove.idRequired"));
        }
        return this.manualOperationFacade.remove(normalizedIdList,
                I18nUtil.getMessage("ui.tc.schedule.remove.defaultReason"));
    }

    /**
     * 校验插单基础字段和计划量顺序配对。
     *
     * @param requestVo 插单请求
     */
    private void validateInsertRequest(TcInsertTaskRequestVo requestVo) {
        if (requestVo == null || StringUtils.isBlank(requestVo.getFactoryCode())
                || requestVo.getScheduleDate() == null || StringUtils.isBlank(requestVo.getMachineCode())
                || StringUtils.isBlank(requestVo.getSidewallCode())
                || StringUtils.isBlank(requestVo.getConstructionVersion())
                || StringUtils.isBlank(requestVo.getReason()) || requestVo.getShiftList() == null
                || requestVo.getShiftList().isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.insert.invalidRequest"));
        }
        boolean invalidPair = requestVo.getShiftList().stream().anyMatch(item -> item == null
                || item.getShiftOrder() == null || item.getShiftOrder() < 1
                || item.getShiftOrder() > TcScheduleConstants.TC_MAX_SHIFT_ORDER
                || item.getPlanQty() == null || item.getPlanQty().compareTo(BigDecimal.ZERO) <= 0
                || item.getSequence() == null || item.getSequence() < 1);
        long distinctShiftCount = requestVo.getShiftList().stream().filter(Objects::nonNull)
                .map(TcManualShiftItemVo::getShiftOrder).filter(Objects::nonNull).distinct().count();
        if (invalidPair || distinctShiftCount != requestVo.getShiftList().size()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.insert.shiftPairRequired"));
        }
    }

    /**
     * 校验插单班次已开班且尚未结束。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param shiftItemList 插单班次
     */
    private void validateShiftItems(String factoryCode, Date scheduleDate,
                                    List<TcManualShiftItemVo> shiftItemList) {
        this.validateShiftOrders(factoryCode, scheduleDate, shiftItemList.stream()
                .map(TcManualShiftItemVo::getShiftOrder).collect(Collectors.toList()));
    }

    /**
     * 校验人工操作班次已开班且尚未结束。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @param shiftOrderList 待操作班次顺序
     */
    private void validateShiftOrders(String factoryCode, Date scheduleDate,
                                     List<Integer> shiftOrderList) {
        if (StringUtils.isBlank(factoryCode) || scheduleDate == null || shiftOrderList == null
                || shiftOrderList.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.shiftClosed"));
        }
        Date today = DateUtil.beginOfDay(new Date());
        Date targetDate = DateUtil.beginOfDay(scheduleDate);
        if (targetDate.before(today)) {
            throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.pastShiftBlocked"));
        }
        LambdaQueryWrapper<TcShiftConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TcShiftConfig::getFactoryCode, factoryCode);
        List<TcShiftConfig> shiftConfigList = this.shiftConfigMapper.selectList(wrapper);
        Map<Integer, TcShiftConfig> shiftConfigMap = shiftConfigList == null ? Collections.emptyMap()
                : shiftConfigList.stream().filter(item -> item.getShiftOrder() != null)
                .collect(Collectors.toMap(TcShiftConfig::getShiftOrder, Function.identity(), (left, right) -> left));
        for (Integer shiftOrder : shiftOrderList.stream().filter(Objects::nonNull).distinct()
                .collect(Collectors.toList())) {
            TcShiftConfig shiftConfig = shiftConfigMap.get(shiftOrder);
            if (shiftConfig == null || !"1".equals(shiftConfig.getOpenFlag())) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.shiftClosed"));
            }
            if (targetDate.equals(today) && this.resolveShiftEndTime(shiftConfig, scheduleDate).before(new Date())) {
                throw new ServiceException(I18nUtil.getMessage("ui.tc.schedule.manual.pastShiftBlocked"));
            }
        }
    }

    /**
     * 解析班次结束时间，跨天班次顺延一天。
     *
     * @param shiftConfig 班次配置
     * @param scheduleDate 排程日期
     * @return 班次结束时间
     */
    private Date resolveShiftEndTime(TcShiftConfig shiftConfig, Date scheduleDate) {
        String endTime = shiftConfig.getPlanEndTime();
        if (endTime != null && endTime.length() == 5) {
            endTime = endTime + ":00";
        }
        Date shiftEndTime = DateUtil.parseDateTime(DateUtil.formatDate(scheduleDate) + " " + endTime);
        return "1".equals(shiftConfig.getCrossDayFlag()) ? DateUtil.offsetDay(shiftEndTime, 1) : shiftEndTime;
    }

    /**
     * 解析人工插单写入的当前批次；无现有结果时使用日期级人工批次。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 批次号
     */
    private String resolveCurrentBatchNo(String factoryCode, Date scheduleDate) {
        LambdaQueryWrapper<TcScheduleResult> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.select(TcScheduleResult::getBatchNo);
        resultWrapper.eq(TcScheduleResult::getFactoryCode, factoryCode);
        resultWrapper.eq(TcScheduleResult::getScheduleDate, scheduleDate);
        List<TcScheduleResult> resultList = this.scheduleResultMapper.selectList(resultWrapper);

        LambdaQueryWrapper<TcScheduleUnplanned> unplannedWrapper = new LambdaQueryWrapper<>();
        unplannedWrapper.select(TcScheduleUnplanned::getBatchNo);
        unplannedWrapper.eq(TcScheduleUnplanned::getFactoryCode, factoryCode);
        unplannedWrapper.eq(TcScheduleUnplanned::getScheduleDate, scheduleDate);
        List<TcScheduleUnplanned> unplannedList = this.scheduleUnplannedMapper.selectList(unplannedWrapper);

        List<String> batchNoList = new ArrayList<>();
        if (resultList != null) {
            batchNoList.addAll(resultList.stream().map(TcScheduleResult::getBatchNo)
                    .filter(StringUtils::isNotBlank).collect(Collectors.toList()));
        }
        if (unplannedList != null) {
            batchNoList.addAll(unplannedList.stream().map(TcScheduleUnplanned::getBatchNo)
                    .filter(StringUtils::isNotBlank).collect(Collectors.toList()));
        }
        return batchNoList.stream().max(String::compareTo)
                .orElseGet(() -> "TCMANUAL" + DateUtil.format(scheduleDate, "yyyyMMdd"));
    }
}
