package com.zlt.aps.mp.factory.listener;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.service.ISysConfigService;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.AjaxResultUtils;
import com.zlt.aps.enums.LocationTypeEnum;
import com.zlt.aps.itf.mes.IMesItfService;
import com.zlt.aps.itf.scm.service.IScmItfService;
import com.zlt.aps.itf.scm.vo.SyncOutFacScheduleVersionVo;
import com.zlt.aps.maindata.enums.ReleaseStatusEnum;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.service.IMpMonthPlanMonitorService;
import com.zlt.aps.mp.api.domain.dto.MonthPlanFinalizedEventDto;
import com.zlt.aps.mp.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.mp.demand.service.impl.OrderAllocationServiceImpl;
import com.zlt.aps.mp.factory.event.MonthPlanAdjustedEvent;
import com.zlt.aps.mp.factory.event.MonthPlanFinalizedEvent;
import com.zlt.aps.mp.factory.mapper.FactoryMonthPlanProductionFinalResultEntityMapper;
import com.zlt.aps.mp.factory.service.IFactoryMonthPlanProductionFinalResultService;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.utils.IncrementService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * 月计划定稿事件监听器
 *
 * @author Chen
 */
@Slf4j
@Component
public class MonthPlanFinalizedChangeEventListeners {

    @Autowired
    private OrderAllocationServiceImpl orderAllocationService;

    @Autowired
    private IMpMonthPlanMonitorService mpMonthPlanMonitorService;

    @Autowired
    private IScmItfService iScmItfService;

    @Autowired
    private IMesItfService mesItfService;

    @Autowired
    private IncrementService incrementService;

    @Autowired
    private MdmMaterialInfoEntityMapper materialInfoEntityMapper;

    @Autowired
    private FactoryMonthPlanProductionFinalResultEntityMapper finalResultMapper;

    @Autowired
    private IFactoryMonthPlanProductionFinalResultService finalResultService;

    @Autowired
    private ISysConfigService sysConfigService;

    /**
     * 异步处理月计划定稿事件
     */
    @Async
    @EventListener
    public void handleMonthPlanFinalizedEvent(MonthPlanFinalizedEvent event) {
        try {
            MonthPlanFinalizedEventDto eventDto = event.getEventDto();
            log.info("月计划定稿事件开始执行，事件ID：{}，事件参数：{}", event.getEventId(), JSONObject.toJSONString(eventDto));
            // 4、调用世超的分摊接口
            // 4.1、OrderAllocationServiceImpl.allocateProductionByMonth
            orderAllocationService.allocateProductionByMonth(eventDto.getYear(), eventDto.getMonth(),
                    eventDto.getFactoryCode(), eventDto.getMonthPlanVersion(), eventDto.getMaterialTotalQtyMap());
            // 4.2、调用生成原材料需求计划 -- TODO

            // 5、写入月度硫化监控表
            // t_mp_month_plan_monitor
            // 上机日期 = 排产周期的开始日 +  (startDay -1 )
            List<FactoryMonthPlanProductionFinalResult> finalList = eventDto.getFinalList();
            mpMonthPlanMonitorService.insertMonitorByFinalList(eventDto.getParam(), finalList);
            // 6、推送SCM和MES
            syncMonthPlanToScmAndMes(eventDto, "月计划定稿事件", Boolean.FALSE);
            log.info("月计划定稿事件执行完成");
        } catch (Exception e) {
            log.error("月计划定稿事件执行失败，事件ID：{}", event.getEventId(), e);
        }
    }

    /**
     * 异步处理月计划调整确认事件。
     *
     * @param event 月计划调整确认事件
     * @return 无
     * @throws RuntimeException 监听器内部捕获异常，不向确认调整主流程反抛
     */
    @Async
    @EventListener
    public void handleMonthPlanAdjustedEvent(MonthPlanAdjustedEvent event) {
        try {
            MonthPlanFinalizedEventDto eventDto = event.getEventDto();
            log.info("月计划调整事件开始执行，事件ID：{}，事件参数：{}", event.getEventId(), JSONObject.toJSONString(eventDto));
            syncMonthPlanToScmAndMes(eventDto, "月计划调整事件", Boolean.TRUE);
            log.info("月计划调整事件执行完成");
        } catch (Exception e) {
            log.error("月计划调整事件执行失败，事件ID：{}", event.getEventId(), e);
        }
    }

    /**
     * 根据配置将月计划同步到SCM和MES。
     *
     * @param eventDto  月计划事件参数
     * @param eventName        事件名称，用于区分日志来源
     * @param useAdjustVersion 是否使用调整版本作为外部推送版本
     * @return 无
     * @throws RuntimeException SCM或MES推送失败时由调用方统一捕获记录
     */
    private void syncMonthPlanToScmAndMes(MonthPlanFinalizedEventDto eventDto, String eventName, Boolean useAdjustVersion) {
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
     * 下发月计划调整数据到MES，并维护最终月计划发布状态。
     * <p>
     * 调整场景需要把对外版本号改为调整版本，不能复用按monthPlanVersion二次查询的下发方法；
     * 因此这里按本次整月计划行主键补齐“发布中 -> 已发布”的状态流转。
     * </p>
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
     * <p>
     * MES下发会读取列表首行的monthPlanVersion作为MQ通知参数，SCM调整下发也使用该版本作为计划版本号。
     * 调整事件的monthPlanVersion由发布方填充为{@code MpRollAdjustContextDTO.version}，这里统一覆盖到推送数据。
     * </p>
     *
     * @param eventDto 月计划事件参数
     * @return 无
     * @throws RuntimeException 版本号为空时抛出异常，由事件监听器统一记录
     */
    private void fillAdjustVersionForAdjust(MonthPlanFinalizedEventDto eventDto) {
        List<FactoryMonthPlanProductionFinalResult> finalList = eventDto.getFinalList();
        if (CollectionUtils.isEmpty(finalList)) {
            return;
        }
        String adjustVersion = eventDto.getMonthPlanVersion();
        if (StringUtils.isBlank(adjustVersion)) {
            throw new RuntimeException("月计划调整事件推送失败：调整版本号为空");
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
     * @param eventDto 月计划事件参数
     * @param useAdjustVersion 是否使用调整版本作为SCM计划版本号
     * @return SCM月计划版本同步数据列表
     * @throws RuntimeException 物料信息查询或字段反射异常时向调用方抛出
     */
    private List<SyncOutFacScheduleVersionVo> buildOutFacScheduleVersionVoList(MonthPlanFinalizedEventDto eventDto, Boolean useAdjustVersion) {
        List<FactoryMonthPlanProductionFinalResult> finalList = eventDto.getFinalList();
        if (CollectionUtils.isEmpty(finalList)) {
            return Collections.emptyList();
        }
        log.info("开始处理月计划下发SCM数据");
        Map<String, Integer> materialTotalQtyMap = Optional.ofNullable(eventDto.getMaterialTotalQtyMap()).orElse(Collections.emptyMap());
        List<String> uniqueKeyList = materialTotalQtyMap.keySet().stream().map(item -> eventDto.getFactoryCode() + "|" + item).collect(Collectors.toList());
        List<MdmMaterialInfo> materialInfoList = CollectionUtils.isEmpty(uniqueKeyList)
                ? Collections.emptyList()
                : materialInfoEntityMapper.selectByUniqueKeyList(uniqueKeyList);
        Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(materialInfoList)) {
            materialInfoMap = materialInfoList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMaterialCode()), Function.identity(), (s1, s2) -> s1));
        }

        List<SyncOutFacScheduleVersionVo> syncOutFacScheduleVersionVoList = new ArrayList<>();
        for (FactoryMonthPlanProductionFinalResult result : finalList) {
            SyncOutFacScheduleVersionVo versionVo = new SyncOutFacScheduleVersionVo();
            versionVo.setFactory(result.getFactoryCode());
            versionVo.setPlanVersion(Boolean.TRUE.equals(useAdjustVersion) ? result.getMonthPlanVersion() : result.getProductionVersion());
            versionVo.setProductPlanNo(result.getProductionNo());
            // 默认草拟
            versionVo.setStatus(ApsConstant.APS_STRING_0);
            Integer year = result.getYear();
            versionVo.setYear(String.valueOf(year));
            Integer month = result.getMonth();
            versionVo.setMonth(String.valueOf(month));
            versionVo.setProductionCategory(result.getProductTypeCode());
            Integer lastDayDayOfMonth = 0;
            try {
                LocalDate of = LocalDate.of(year, month, 1);
                LocalDate lastDay = of.with(TemporalAdjusters.lastDayOfMonth());
                lastDayDayOfMonth = lastDay.getDayOfMonth();
                // 月份对应最后一天
                versionVo.setDayNum(String.valueOf(lastDayDayOfMonth));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                log.error("年、月转换数值失败，年：{}，月：{}", year, month);
            }
            versionVo.setMaterialCode(result.getMaterialCode());
            versionVo.setMaterialDesc(result.getMaterialDesc());
            versionVo.setRowNo(incrementService.getBillRowIndex(result.getProductionVersion()));
            versionVo.setBusiType(LocationTypeEnum.FOREIGN_LOCATION.getValue());
//            versionVo.setArchiSystem(); 内销日缺货报表才需要用到，不传值
            versionVo.setBrand(result.getBrand());
            versionVo.setSpecifications(result.getSpecifications());
            versionVo.setFigure(result.getPattern());

            String mapKey = GenerageMapKeyUtils.createMapKey(result.getFactoryCode(), result.getMaterialCode());
            if (materialInfoMap.containsKey(mapKey)) {
                MdmMaterialInfo materialInfo = materialInfoMap.get(mapKey);
                versionVo.setTireLevel(materialInfo.getHierarchy());
                versionVo.setSpeedLevel(materialInfo.getSpeed());
//                versionVo.setLoadIndex();
            }
//            versionVo.setTotalOrdQtc();供应链值为空
//            versionVo.setShipQtc();供应链值为空
//            versionVo.setPlanUnshipQtc();供应链值为空
//            versionVo.setStockQtc();供应链值为空
            versionVo.setUnscheduleQtc(result.getTotalQty());
//            versionVo.setScheduleQtc();供应链值为空
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
}
