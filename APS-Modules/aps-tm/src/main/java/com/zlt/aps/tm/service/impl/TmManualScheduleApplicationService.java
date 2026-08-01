package com.zlt.aps.tm.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.tm.api.constant.TmScheduleConstants;
import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.api.domain.entity.TmScheduleUnplanned;
import com.zlt.aps.tm.api.domain.vo.TmInsertTaskRequestVo;
import com.zlt.aps.tm.api.enums.TmYesNoEnum;
import com.zlt.aps.tm.domain.vo.TmConstructionTreadRowVo;
import com.zlt.aps.tm.mapper.TmAutoScheduleDataLoadMapper;
import com.zlt.aps.tm.mapper.TmMachineInfoMapper;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.mapper.TmScheduleUnplannedMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 胎面人工插单应用服务。
 *
 * <p>负责校验公开请求、从可信施工资料生成排程结果快照，并把生成结果交给人工操作门面。
 * 分布式锁、数据库短事务、滚动重排和调度日志仍由 {@link TmManualOperationFacade} 统一处理。</p>
 */
@Service
public class TmManualScheduleApplicationService {

    /** 人工插单无既有批次时使用的批次前缀。 */
    private static final String MANUAL_BATCH_PREFIX = "TMMANUAL";

    /** 人工工单号标识。 */
    private static final String MANUAL_ORDER_MARK = "-MANUAL-";

    /** 人工工单随机码长度。 */
    private static final int MANUAL_ORDER_RANDOM_LENGTH = 8;

    /** 原因分析最大长度。 */
    private static final int ANALYSIS_MAX_LENGTH = 200;

    /** 备注最大长度。 */
    private static final int REMARK_MAX_LENGTH = 500;

    private final TmAutoScheduleDataLoadMapper autoScheduleDataLoadMapper;

    private final TmMachineInfoMapper machineInfoMapper;

    private final TmScheduleResultMapper scheduleResultMapper;

    private final TmScheduleUnplannedMapper scheduleUnplannedMapper;

    private final TmManualOperationFacade manualOperationFacade;

    /**
     * 构造胎面人工插单应用服务。
     *
     * @param autoScheduleDataLoadMapper 自动排程基础资料 Mapper
     * @param machineInfoMapper 机台资料 Mapper
     * @param scheduleResultMapper 排程结果 Mapper
     * @param scheduleUnplannedMapper 未排结果 Mapper
     * @param manualOperationFacade 人工操作安全门面
     */
    public TmManualScheduleApplicationService(TmAutoScheduleDataLoadMapper autoScheduleDataLoadMapper,
                                              TmMachineInfoMapper machineInfoMapper,
                                              TmScheduleResultMapper scheduleResultMapper,
                                              TmScheduleUnplannedMapper scheduleUnplannedMapper,
                                              TmManualOperationFacade manualOperationFacade) {
        this.autoScheduleDataLoadMapper = autoScheduleDataLoadMapper;
        this.machineInfoMapper = machineInfoMapper;
        this.scheduleResultMapper = scheduleResultMapper;
        this.scheduleUnplannedMapper = scheduleUnplannedMapper;
        this.manualOperationFacade = manualOperationFacade;
    }

    /**
     * 执行胎面人工插单。
     *
     * @param requestVo 插单请求
     * @return 新增任务级排程结果数量
     * @throws ServiceException 请求、机台或施工资料不合法时抛出
     */
    public int insertTask(TmInsertTaskRequestVo requestVo) {
        TmScheduleResult insertResult = this.buildRequestResult(requestVo);
        this.validateShiftFields(insertResult);
        this.validateMachine(insertResult.getFactoryCode(), insertResult.getMachineCode());
        this.fillConstructionSnapshot(insertResult);
        insertResult.setBatchNo(this.resolveCurrentBatchNo(insertResult.getFactoryCode(), insertResult.getScheduleDate()));
        insertResult.setOrderNo(insertResult.getBatchNo() + MANUAL_ORDER_MARK
                + IdUtil.fastSimpleUUID().substring(0, MANUAL_ORDER_RANDOM_LENGTH));
        insertResult.setTailFlag(TmYesNoEnum.NO.getCode());
        insertResult.setDataSource("INSERT");
        insertResult.setReleaseStatus(ApsConstant.NO_RELEASE);
        return this.manualOperationFacade.insertTask(insertResult);
    }

    /**
     * 将公开请求复制为只包含允许字段的排程结果。
     *
     * @param requestVo 插单请求
     * @return 待补全排程结果
     * @throws ServiceException 基础必填字段为空或文本超长时抛出
     */
    private TmScheduleResult buildRequestResult(TmInsertTaskRequestVo requestVo) {
        if (requestVo == null || StrUtil.isBlank(requestVo.getFactoryCode()) || requestVo.getScheduleDate() == null
                || StrUtil.isBlank(requestVo.getMachineCode()) || StrUtil.isBlank(requestVo.getTreadCode())) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.insertInvalidRequest"));
        }
        TmScheduleResult result = new TmScheduleResult();
        result.setFactoryCode(StrUtil.trim(requestVo.getFactoryCode()));
        result.setScheduleDate(DateUtil.beginOfDay(requestVo.getScheduleDate()));
        result.setMachineCode(StrUtil.trim(requestVo.getMachineCode()));
        result.setTreadCode(StrUtil.trim(requestVo.getTreadCode()));
        result.setClass1PlanQty(requestVo.getClass1PlanQty());
        result.setClass1Sequence(requestVo.getClass1Sequence());
        result.setClass1Analysis(StrUtil.trim(requestVo.getClass1Analysis()));
        result.setClass2PlanQty(requestVo.getClass2PlanQty());
        result.setClass2Sequence(requestVo.getClass2Sequence());
        result.setClass2Analysis(StrUtil.trim(requestVo.getClass2Analysis()));
        result.setClass3PlanQty(requestVo.getClass3PlanQty());
        result.setClass3Sequence(requestVo.getClass3Sequence());
        result.setClass3Analysis(StrUtil.trim(requestVo.getClass3Analysis()));
        result.setRemark(StrUtil.trim(requestVo.getRemark()));
        if (this.hasOverLengthText(result, ANALYSIS_MAX_LENGTH) || StrUtil.length(result.getRemark()) > REMARK_MAX_LENGTH) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.insertTextTooLong"));
        }
        return result;
    }

    /**
     * 校验三个可插单班次的计划量、顺序和原因分析配对关系。
     *
     * @param scheduleResult 待校验排程结果
     * @throws ServiceException 没有有效计划量或班次字段未按规则配对时抛出
     */
    private void validateShiftFields(TmScheduleResult scheduleResult) {
        boolean hasPlanQty = false;
        for (int shiftOrder = 1; shiftOrder <= 3; shiftOrder++) {
            String planQtyField = String.format(TmScheduleConstants.SHIFT_PLAN_QTY_FIELD_TEMPLATE, shiftOrder);
            String sequenceField = String.format(TmScheduleConstants.SHIFT_SEQUENCE_FIELD_TEMPLATE, shiftOrder);
            String analysisField = String.format(TmScheduleConstants.SHIFT_ANALYSIS_FIELD_TEMPLATE, shiftOrder);
            BigDecimal planQty = (BigDecimal) scheduleResult.getFieldValueByFieldName(planQtyField);
            Integer sequence = (Integer) scheduleResult.getFieldValueByFieldName(sequenceField);
            String analysis = (String) scheduleResult.getFieldValueByFieldName(analysisField);
            if (planQty == null) {
                if (sequence != null || StrUtil.isNotBlank(analysis)) {
                    throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.insertShiftPairRequired"));
                }
                continue;
            }
            if (planQty.compareTo(BigDecimal.ZERO) <= 0 || sequence == null || sequence < 1) {
                throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.insertShiftPairRequired"));
            }
            scheduleResult.setFieldValueByFieldName(
                    String.format(TmScheduleConstants.SHIFT_FINISH_QTY_FIELD_TEMPLATE, shiftOrder), BigDecimal.ZERO);
            hasPlanQty = true;
        }
        if (!hasPlanQty) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.insertPlanQtyRequired"));
        }
    }

    /**
     * 校验原因分析和备注长度。
     *
     * @param scheduleResult 排程结果
     * @param maxLength 原因分析最大长度
     * @return true 表示至少一个原因分析超长
     */
    private boolean hasOverLengthText(TmScheduleResult scheduleResult, int maxLength) {
        return Arrays.asList(scheduleResult.getClass1Analysis(), scheduleResult.getClass2Analysis(),
                        scheduleResult.getClass3Analysis()).stream()
                .filter(Objects::nonNull).anyMatch(value -> value.length() > maxLength);
    }

    /**
     * 校验人工插单目标机台有效。
     *
     * @param factoryCode 工厂编码
     * @param machineCode 机台编码
     * @throws ServiceException 机台不存在或未启用时抛出
     */
    private void validateMachine(String factoryCode, String machineCode) {
        LambdaQueryWrapper<TmMachineInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TmMachineInfo::getFactoryCode, factoryCode);
        wrapper.eq(TmMachineInfo::getMachineCode, machineCode);
        wrapper.eq(TmMachineInfo::getMachineStatus, TmYesNoEnum.YES.getCode());
        List<TmMachineInfo> machineInfoList = this.machineInfoMapper.selectList(wrapper);
        if (machineInfoList == null || machineInfoList.isEmpty()) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.insertMachineInvalid"));
        }
    }

    /**
     * 查询最新有效施工版本并生成可信胎面快照。
     *
     * @param scheduleResult 待补全排程结果
     * @throws ServiceException 胎面施工不存在或关键资料为空时抛出
     */
    private void fillConstructionSnapshot(TmScheduleResult scheduleResult) {
        TmConstructionTreadRowVo construction = this.autoScheduleDataLoadMapper.selectLatestConstructionByTread(
                scheduleResult.getFactoryCode(), scheduleResult.getTreadCode());
        if (construction == null) {
            throw new ServiceException(I18nUtil.getMessage("ui.data.alert.tm.schedule.insertConstructionNotFound"));
        }
        List<String> glueCodeList = StrUtil.isBlank(construction.getTreadRubberCategory())
                ? Collections.emptyList() : Arrays.stream(construction.getTreadRubberCategory().split(","))
                .map(String::trim).filter(StrUtil::isNotBlank).collect(Collectors.toList());
        if (construction.getTreadShoulderLength() == null
                || construction.getTreadShoulderLength().compareTo(BigDecimal.ZERO) <= 0
                || StrUtil.isBlank(construction.getTreadMouthPlate()) || glueCodeList.isEmpty()) {
            throw new ServiceException(this.buildConstructionInvalidMessage(construction));
        }
        scheduleResult.setTreadShoulderLength(construction.getTreadShoulderLength());
        scheduleResult.setMouthPlateCode(StrUtil.trim(construction.getTreadMouthPlate()));
        scheduleResult.setGlueCode(glueCodeList.get(0));
        scheduleResult.setBaseGlueCode(glueCodeList.size() > 1
                ? String.join(",", glueCodeList.subList(1, glueCodeList.size())) : null);
        scheduleResult.setWholeGlueCode(String.join(",", glueCodeList));
    }

    /**
     * 构造包含施工定位信息和缺失字段的错误提示。
     *
     * @param construction 最新胎面施工资料
     * @return 已完成国际化参数替换的错误提示
     */
    private String buildConstructionInvalidMessage(TmConstructionTreadRowVo construction) {
        List<String> missingFieldList = new ArrayList<>();
        if (construction.getTreadShoulderLength() == null
                || construction.getTreadShoulderLength().compareTo(BigDecimal.ZERO) <= 0) {
            missingFieldList.add(I18nUtil.getMessage(
                    "ui.data.alert.tm.schedule.constructionFieldShoulderLength"));
        }
        if (StrUtil.isBlank(construction.getTreadMouthPlate())) {
            missingFieldList.add(I18nUtil.getMessage(
                    "ui.data.alert.tm.schedule.constructionFieldMouthPlate"));
        }
        if (StrUtil.isBlank(construction.getTreadRubberCategory())) {
            missingFieldList.add(I18nUtil.getMessage(
                    "ui.data.alert.tm.schedule.constructionFieldRubber"));
        }
        return MessageFormat.format(I18nUtil.getMessage(
                        "ui.data.alert.tm.schedule.insertConstructionInvalid"),
                StrUtil.blankToDefault(construction.getConstructionCode(), "-"),
                StrUtil.blankToDefault(construction.getConstructionVersion(), "-"),
                StrUtil.blankToDefault(construction.getTreadCode(), "-"),
                String.join(", ", missingFieldList));
    }

    /**
     * 复用同工厂同日期最新有效批次，没有结果时生成当天人工批次。
     *
     * @param factoryCode 工厂编码
     * @param scheduleDate 排程日期
     * @return 当前批次号
     */
    private String resolveCurrentBatchNo(String factoryCode, java.util.Date scheduleDate) {
        LambdaQueryWrapper<TmScheduleResult> resultWrapper = new LambdaQueryWrapper<>();
        resultWrapper.select(TmScheduleResult::getBatchNo);
        resultWrapper.eq(TmScheduleResult::getFactoryCode, factoryCode);
        resultWrapper.eq(TmScheduleResult::getScheduleDate, scheduleDate);
        resultWrapper.orderByDesc(TmScheduleResult::getCreateTime, TmScheduleResult::getId);
        resultWrapper.last("LIMIT 1");
        List<TmScheduleResult> resultList = this.scheduleResultMapper.selectList(resultWrapper);
        String resultBatchNo = resultList == null ? null : resultList.stream().map(TmScheduleResult::getBatchNo)
                .filter(StrUtil::isNotBlank).findFirst().orElse(null);
        if (StrUtil.isNotBlank(resultBatchNo)) {
            return resultBatchNo;
        }

        LambdaQueryWrapper<TmScheduleUnplanned> unplannedWrapper = new LambdaQueryWrapper<>();
        unplannedWrapper.select(TmScheduleUnplanned::getBatchNo);
        unplannedWrapper.eq(TmScheduleUnplanned::getFactoryCode, factoryCode);
        unplannedWrapper.eq(TmScheduleUnplanned::getScheduleDate, scheduleDate);
        unplannedWrapper.orderByDesc(TmScheduleUnplanned::getCreateTime, TmScheduleUnplanned::getId);
        unplannedWrapper.last("LIMIT 1");
        List<TmScheduleUnplanned> unplannedList = this.scheduleUnplannedMapper.selectList(unplannedWrapper);
        String unplannedBatchNo = unplannedList == null ? null : unplannedList.stream()
                .map(TmScheduleUnplanned::getBatchNo).filter(StrUtil::isNotBlank).findFirst().orElse(null);
        return StrUtil.blankToDefault(unplannedBatchNo,
                MANUAL_BATCH_PREFIX + DateUtil.format(scheduleDate, "yyyyMMdd"));
    }
}
