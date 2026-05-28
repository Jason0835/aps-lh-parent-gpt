package com.zlt.aps.mp.factory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.service.ISysConfigService;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.AjaxResultUtils;
import com.zlt.aps.enums.LocationTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.scm.service.IScmItfService;
import com.zlt.aps.itf.scm.vo.SyncOutFacScheduleVersionVo;
import com.zlt.aps.maindata.enums.ReleaseStatusEnum;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.service.IMpMonthPlanMonitorService;
import com.zlt.aps.mp.api.domain.dto.MonthPlanFinalizedEventDto;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.utils.IncrementService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 月计划推送SCM和MES服务。
 * <p>
 * 统一承载月计划定稿、月计划调整后的外部系统同步逻辑，避免事件监听器和手动按钮接口重复维护推送规则。
 * </p>
 */
@Slf4j
@Service
public class MonthPlanSyncService {

    @Autowired
    private IScmItfService iScmItfService;

    @Autowired
    private IMesItfService mesItfService;

    @Autowired
    private IncrementService incrementService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private MdmMaterialInfoEntityMapper materialInfoEntityMapper;

    @Autowired
    private FactoryMonthPlanProductionFinalResultEntityMapper finalResultMapper;

    @Autowired
    private IFactoryMonthPlanProductionFinalResultService finalResultService;

    @Autowired
    private IMpMonthPlanMonitorService mpMonthPlanMonitorService;

    @Autowired
    private ISysConfigService sysConfigService;

    /**
     * 手动同步月计划调整后的数据到SCM和MES。
     *
     * @param param 月计划查询参数，monthPlanVersion为原需求版本，lastMonthPlanVersion为调整后推送版本
     * @return 同步结果
     * @throws RuntimeException 方法内部捕获异常并返回错误结果，不主动向控制层抛出
     */
    public AjaxResult syncAdjustedMonthPlanToScmAndMes(FactoryMonthPlanProductionFinalResult param) {
        try {
            AjaxResult validateResult = validateAdjustedSyncParam(param);
            if (validateResult != null) {
                return validateResult;
            }
            List<FactoryMonthPlanProductionFinalResult> finalList = queryAdjustedMonthPlanList(param);
            if (CollectionUtils.isEmpty(finalList)) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.finalized.noData"));
            }

            MonthPlanFinalizedEventDto eventDto = buildAdjustedEventDto(param, finalList);
            mpMonthPlanMonitorService.insertMonitorByFinalList(eventDto.getParam(), finalList);
            syncMonthPlanToScmAndMes(eventDto, "月计划调整手动推送", Boolean.TRUE);
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("月计划调整手动推送SCM和MES失败，参数：{}", param, e);
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 根据配置将月计划同步到SCM和MES。
     *
     * @param eventDto         月计划事件参数
     * @param eventName        事件名称，用于区分日志来源
     * @param useAdjustVersion 是否使用调整版本作为外部推送版本
     * @return 无
     * @throws RuntimeException SCM或MES推送失败时向调用方抛出异常
     */
    public void syncMonthPlanToScmAndMes(MonthPlanFinalizedEventDto eventDto, String eventName, Boolean useAdjustVersion) {
        if (Boolean.TRUE.equals(useAdjustVersion)) {
            fillAdjustVersionForAdjust(eventDto);
        }
        List<FactoryMonthPlanProductionFinalResult> finalList = eventDto.getFinalList();
        boolean isSyncScm = isSyncEnabled("final.sync.scm");
        log.info("{}同步SCM参数：{}", eventName, isSyncScm);
        if (CollectionUtils.isNotEmpty(finalList) && isSyncScm) {
            log.info("{}传给SCM-start", eventName);
            iScmItfService.publicFacScheduleVersion(buildOutFacScheduleVersionVoList(eventDto, Boolean.TRUE.equals(useAdjustVersion)));
            log.info("{}传给SCM-end", eventName);
        }
        boolean isSyncMes = isSyncEnabled("final.sync.mes");
        log.info("{}同步MES参数：{}", eventName, isSyncMes);
        if (isSyncMes) {
            log.info("{}传给MES-start", eventName);
            if (Boolean.TRUE.equals(useAdjustVersion)) {
                issueAdjustedMonthPlanToMes(finalList, eventName);
            } else {
                finalResultService.issueMonthPlan(eventDto.getParam());
            }
            log.info("{}传给MES-end", eventName);
        }
    }

    /**
     * 校验月计划调整手动推送参数。
     *
     * @param param 月计划查询参数
     * @return 参数错误时返回错误结果，参数正确时返回null
     * @throws RuntimeException 不抛出异常
     */
    private AjaxResult validateAdjustedSyncParam(FactoryMonthPlanProductionFinalResult param) {
        if (param == null || param.getYear() == null || param.getMonth() == null
                || StringUtils.isBlank(param.getFactoryCode()) || StringUtils.isBlank(param.getProductionVersion())
                || StringUtils.isBlank(param.getLastMonthPlanVersion())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.finalized.checkParam"));
        }
        if (StringUtils.equals(param.getMonthPlanVersion(), param.getLastMonthPlanVersion())) {
            return AjaxResult.error("未找到调整后的最新需求计划版本，无法推送");
        }
        return null;
    }

    /**
     * 查询调整确认后的整月最终月计划。
     *
     * @param param 月计划查询参数
     * @return 调整确认后的整月最终月计划列表
     * @throws RuntimeException 数据库异常由调用方捕获
     */
    private List<FactoryMonthPlanProductionFinalResult> queryAdjustedMonthPlanList(FactoryMonthPlanProductionFinalResult param) {
        LambdaQueryWrapper<FactoryMonthPlanProductionFinalResult> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FactoryMonthPlanProductionFinalResult::getFactoryCode, param.getFactoryCode())
                .eq(FactoryMonthPlanProductionFinalResult::getYear, param.getYear())
                .eq(FactoryMonthPlanProductionFinalResult::getMonth, param.getMonth())
                .eq(FactoryMonthPlanProductionFinalResult::getLastMonthPlanVersion, param.getLastMonthPlanVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getProductionVersion, param.getProductionVersion())
                .eq(FactoryMonthPlanProductionFinalResult::getIsDelete, YesOrNoEnum.NO.getValue());
        return finalResultMapper.selectList(queryWrapper);
    }

    /**
     * 构建月计划调整推送事件参数。
     *
     * @param param     月计划查询参数
     * @param finalList 调整确认后的整月最终月计划
     * @return 月计划推送参数
     * @throws RuntimeException 不主动抛出异常
     */
    private MonthPlanFinalizedEventDto buildAdjustedEventDto(FactoryMonthPlanProductionFinalResult param,
                                                             List<FactoryMonthPlanProductionFinalResult> finalList) {
        String adjustVersion = param.getLastMonthPlanVersion();
        FactoryMonthPlanProductionFinalResult pushParam = new FactoryMonthPlanProductionFinalResult();
        pushParam.setFactoryCode(param.getFactoryCode());
        pushParam.setYear(param.getYear());
        pushParam.setMonth(param.getMonth());
        pushParam.setMonthPlanVersion(adjustVersion);
        pushParam.setProductionVersion(param.getProductionVersion());

        MonthPlanFinalizedEventDto eventDto = new MonthPlanFinalizedEventDto();
        eventDto.setFactoryCode(param.getFactoryCode());
        eventDto.setYear(param.getYear());
        eventDto.setMonth(param.getMonth());
        eventDto.setMonthPlanVersion(adjustVersion);
        eventDto.setProductionVersion(param.getProductionVersion());
        eventDto.setMaterialTotalQtyMap(buildMaterialTotalQtyMap(finalList));
        eventDto.setParam(pushParam);
        eventDto.setFinalList(finalList);
        return eventDto;
    }

    /**
     * 构建物料总量汇总Map。
     *
     * @param finalList 最终月计划列表
     * @return 物料编码与整月计划总量的映射
     * @throws RuntimeException 不抛出异常
     */
    private Map<String, Integer> buildMaterialTotalQtyMap(List<FactoryMonthPlanProductionFinalResult> finalList) {
        Map<String, Integer> materialTotalQtyMap = new HashMap<>();
        if (CollectionUtils.isEmpty(finalList)) {
            return materialTotalQtyMap;
        }
        for (FactoryMonthPlanProductionFinalResult result : finalList) {
            if (result == null || StringUtils.isBlank(result.getMaterialCode())) {
                continue;
            }
            Integer totalQty = result.getTotalQty() == null ? 0 : result.getTotalQty();
            materialTotalQtyMap.merge(result.getMaterialCode(), totalQty, Integer::sum);
        }
        return materialTotalQtyMap;
    }

    /**
     * 下发月计划调整数据到MES，并维护最终月计划发布状态。
     *
     * @param finalList 月计划调整后的整月最终计划
     * @param eventName 事件名称，用于记录日志
     * @return 无
     * @throws RuntimeException MES下发失败或计划行主键为空时抛出异常
     */
    private void issueAdjustedMonthPlanToMes(List<FactoryMonthPlanProductionFinalResult> finalList, String eventName) {
        updateAdjustedMonthPlanReleaseStatus(finalList, ReleaseStatusEnum.RELEASING.getCode(), eventName);
        AjaxResult ajaxResult = mesItfService.issueMonthPlan(finalList);
        if (AjaxResultUtils.checkAjaxError(ajaxResult)) {
            throw new RuntimeException(String.valueOf(ajaxResult.get(AjaxResult.MSG_TAG)));
        }
        updateAdjustedMonthPlanReleaseStatus(finalList, ReleaseStatusEnum.RELEASE.getCode(), eventName);
    }

    /**
     * 按月计划行主键更新调整下发发布状态。
     *
     * @param finalList     月计划调整后的整月最终计划
     * @param releaseStatus 发布状态
     * @param eventName     事件名称，用于记录日志
     * @return 无
     * @throws RuntimeException 月计划行主键为空时抛出异常
     */
    private void updateAdjustedMonthPlanReleaseStatus(List<FactoryMonthPlanProductionFinalResult> finalList,
                                                      String releaseStatus, String eventName) {
        List<Long> idList = Optional.ofNullable(finalList)
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .map(FactoryMonthPlanProductionFinalResult::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(idList)) {
            throw new RuntimeException(eventName + "更新月计划发布状态失败：月计划行主键为空");
        }
        LambdaUpdateWrapper<FactoryMonthPlanProductionFinalResult> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(FactoryMonthPlanProductionFinalResult::getId, idList)
                .set(FactoryMonthPlanProductionFinalResult::getIsRelease, releaseStatus);
        finalResultMapper.update(null, updateWrapper);
        for (FactoryMonthPlanProductionFinalResult finalResult : finalList) {
            if (finalResult != null) {
                finalResult.setIsRelease(releaseStatus);
            }
        }
        log.info("{}更新月计划发布状态成功，状态：{}，行数：{}", eventName, releaseStatus, idList.size());
    }

    /**
     * 将月计划调整事件的对外版本号填充为调整版本。
     *
     * @param eventDto 月计划事件参数
     * @return 无
     * @throws RuntimeException 版本号为空时抛出异常
     */
    private void fillAdjustVersionForAdjust(MonthPlanFinalizedEventDto eventDto) {
        List<FactoryMonthPlanProductionFinalResult> finalList = eventDto.getFinalList();
        if (CollectionUtils.isEmpty(finalList)) {
            return;
        }
        String adjustVersion = eventDto.getMonthPlanVersion();
        if (StringUtils.isBlank(adjustVersion)) {
            throw new RuntimeException("月计划调整推送失败：调整版本号为空");
        }
        if (eventDto.getParam() != null) {
            eventDto.getParam().setMonthPlanVersion(adjustVersion);
        }
        for (FactoryMonthPlanProductionFinalResult finalResult : finalList) {
            if (finalResult == null) {
                continue;
            }
            finalResult.setMonthPlanVersion(adjustVersion);
        }
    }

    /**
     * 查询同步开关配置。
     *
     * @param configKey 配置键
     * @return 未配置时默认返回true，配置为空时默认返回true
     * @throws RuntimeException 配置查询异常时内部记录并返回默认值
     */
    private boolean isSyncEnabled(String configKey) {
        boolean isSync = Boolean.TRUE;
        try {
            String config = sysConfigService.selectConfigByKey(configKey);
            if (StringUtils.isNotBlank(config)) {
                isSync = Boolean.parseBoolean(config);
            }
        } catch (Exception e) {
            log.error("获取配置失败，配置键：{}", configKey, e);
        }
        return isSync;
    }

    /**
     * 构建下发SCM的月计划数据。
     *
     * @param eventDto         月计划事件参数
     * @param useAdjustVersion 是否使用调整版本作为SCM计划版本号
     * @return SCM月计划版本同步数据列表
     * @throws RuntimeException 物料信息查询或字段反射异常时向调用方抛出
     */
    private List<SyncOutFacScheduleVersionVo> buildOutFacScheduleVersionVoList(MonthPlanFinalizedEventDto eventDto,
                                                                                Boolean useAdjustVersion) {
        List<FactoryMonthPlanProductionFinalResult> finalList = eventDto.getFinalList();
        if (CollectionUtils.isEmpty(finalList)) {
            return Collections.emptyList();
        }
        log.info("开始处理月计划下发SCM数据");
        Map<String, Integer> materialTotalQtyMap = Optional.ofNullable(eventDto.getMaterialTotalQtyMap()).orElse(Collections.emptyMap());
        List<String> uniqueKeyList = materialTotalQtyMap.keySet().stream()
                .map(item -> eventDto.getFactoryCode() + "|" + item)
                .collect(Collectors.toList());
        List<MdmMaterialInfo> materialInfoList = CollectionUtils.isEmpty(uniqueKeyList)
                ? Collections.emptyList()
                : materialInfoEntityMapper.selectByUniqueKeyList(uniqueKeyList);
        Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(materialInfoList)) {
            materialInfoMap = materialInfoList.stream()
                    .collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMaterialCode()),
                            Function.identity(), (s1, s2) -> s1));
        }

        // 计算去重后的计划版本号，避免重复推送时版本号冲突
        String planVersion = computeSuffixedPlanVersion(eventDto, useAdjustVersion);
        List<SyncOutFacScheduleVersionVo> syncOutFacScheduleVersionVoList = new ArrayList<>();
        for (FactoryMonthPlanProductionFinalResult result : finalList) {
            SyncOutFacScheduleVersionVo versionVo = new SyncOutFacScheduleVersionVo();
            versionVo.setFactory(result.getFactoryCode());
            versionVo.setPlanVersion(planVersion);
            versionVo.setProductPlanNo(result.getProductionNo());
            versionVo.setStatus(ApsConstant.APS_STRING_0);
            Integer year = result.getYear();
            versionVo.setYear(String.valueOf(year));
            Integer month = result.getMonth();
            versionVo.setMonth(String.valueOf(month));
            versionVo.setProductionCategory(result.getProductTypeCode());
            try {
                LocalDate lastDay = LocalDate.of(year, month, 1).with(TemporalAdjusters.lastDayOfMonth());
                versionVo.setDayNum(String.valueOf(lastDay.getDayOfMonth()));
            } catch (NumberFormatException e) {
                log.error("年、月转换数值失败，年：{}，月：{}", year, month, e);
            }
            versionVo.setMaterialCode(result.getMaterialCode());
            versionVo.setMaterialDesc(result.getMaterialDesc());
            versionVo.setRowNo(incrementService.getBillRowIndex(result.getProductionVersion()));
            versionVo.setBusiType(LocationTypeEnum.FOREIGN_LOCATION.getValue());
            versionVo.setBrand(result.getBrand());
            versionVo.setSpecifications(result.getSpecifications());
            versionVo.setFigure(result.getPattern());

            String mapKey = GenerageMapKeyUtils.createMapKey(result.getFactoryCode(), result.getMaterialCode());
            if (materialInfoMap.containsKey(mapKey)) {
                MdmMaterialInfo materialInfo = materialInfoMap.get(mapKey);
                versionVo.setTireLevel(materialInfo.getHierarchy());
                versionVo.setSpeedLevel(materialInfo.getSpeed());
            }
            versionVo.setUnscheduleQtc(result.getTotalQty());
            versionVo.setProductionClass(result.getProductionType());
            for (int i = 1; i <= 31; i++) {
                String fieldName = "day" + i;
                Object fieldValue = ReflectUtils.getFieldValue(result, fieldName);
                ReflectUtils.setFieldValue(versionVo, fieldName, fieldValue);
            }
            syncOutFacScheduleVersionVoList.add(versionVo);
        }
        log.info("处理月计划下发SCM数据结束，数据行数：{}", syncOutFacScheduleVersionVoList.size());
        return syncOutFacScheduleVersionVoList;
    }

    /**
     * 计算下发SCM的计划版本号，通过 Redis 原子自增检测重复，避免同一版本号重复下发。
     *
     * @param eventDto         月计划事件参数
     * @param useAdjustVersion 是否使用调整版本作为SCM计划版本号
     * @return 去重后的计划版本号，无数据或版本号为空时返回null或原值
     */
    private String computeSuffixedPlanVersion(MonthPlanFinalizedEventDto eventDto, Boolean useAdjustVersion) {
        List<FactoryMonthPlanProductionFinalResult> finalList = eventDto.getFinalList();
        if (CollectionUtils.isEmpty(finalList)) {
            return null;
        }
        String originalVersion = Boolean.TRUE.equals(useAdjustVersion)
                ? finalList.get(0).getLastMonthPlanVersion()
                : eventDto.getProductionVersion();
        if (StringUtils.isBlank(originalVersion)) {
            return originalVersion;
        }
        String redisKey = String.format("mp:sync:version:%s:%d:%d:%s",
                eventDto.getFactoryCode(), eventDto.getYear(), eventDto.getMonth(), originalVersion);
        Long seq = stringRedisTemplate.opsForValue().increment(redisKey);
        if (seq != null && seq == 1) {
            stringRedisTemplate.expire(redisKey, 30, TimeUnit.DAYS);
        }
        if (seq != null && seq > 1) {
            return originalVersion + "-" + (seq - 1);
        }
        return originalVersion;
    }
}
