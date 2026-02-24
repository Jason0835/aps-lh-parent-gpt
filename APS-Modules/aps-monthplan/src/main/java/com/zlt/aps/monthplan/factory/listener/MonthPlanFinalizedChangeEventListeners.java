package com.zlt.aps.monthplan.factory.listener;

import com.alibaba.fastjson.JSONObject;
import com.ruoyi.api.gateway.system.service.ISysConfigService;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.zlt.aps.enums.LocationTypeEnum;
import com.zlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.utils.IncrementService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.itf.scm.service.IScmItfService;
import com.zlt.aps.itf.scm.vo.SyncOutFacScheduleVersionVo;
import com.zlt.aps.maindata.mapper.MdmMaterialInfoEntityMapper;
import com.zlt.aps.maindata.service.IMpMonthPlanMonitorService;
import com.zlt.aps.monthplan.api.domain.dto.MonthPlanFinalizedEventDto;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.demand.service.impl.OrderAllocationServiceImpl;
import com.zlt.aps.monthplan.factory.event.MonthPlanFinalizedEvent;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanProductionFinalResultService;
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
    private IncrementService incrementService;

    @Autowired
    private MdmMaterialInfoEntityMapper materialInfoEntityMapper;

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
            // 6、推送SCM
            boolean isSyncScm = Boolean.TRUE;
            try {
                String config = sysConfigService.selectConfigByKey("final.sync.scm");
                if (StringUtils.isNotBlank(config)) {
                    isSyncScm = Boolean.parseBoolean(config);
                }
            } catch (Exception e) {
                log.error("获取配置失败", e);
            }
            if (CollectionUtils.isNotEmpty(finalList) && isSyncScm) {
                iScmItfService.publicFacScheduleVersion(buildOutFacScheduleVersionVoList(eventDto));
            }
            boolean isSyncMes = Boolean.TRUE;
            try {
                String config = sysConfigService.selectConfigByKey("final.sync.mes");
                if (StringUtils.isNotBlank(config)) {
                    isSyncMes = Boolean.parseBoolean(config);
                }
            } catch (Exception e) {
                log.error("获取配置失败", e);
            }
            // 7、传给MES
            if (isSyncMes) {
                finalResultService.issueMonthPlan(eventDto.getParam());
            }
            log.info("月计划定稿事件执行完成");
        } catch (Exception e) {
            log.error("月计划定稿事件执行失败，事件ID：{}", event.getEventId(), e);
        }
    }

    private List<SyncOutFacScheduleVersionVo> buildOutFacScheduleVersionVoList(MonthPlanFinalizedEventDto eventDto) {
        List<FactoryMonthPlanProductionFinalResult> finalList = eventDto.getFinalList();
        if (CollectionUtils.isEmpty(finalList)) {
            return Collections.emptyList();
        }
        List<String> uniqueKeyList = eventDto.getMaterialTotalQtyMap().keySet().stream().map(item -> eventDto.getFactoryCode() + "|" + item).collect(Collectors.toList());
        List<MdmMaterialInfo> materialInfoList = materialInfoEntityMapper.selectByUniqueKeyList(uniqueKeyList);
        Map<String, MdmMaterialInfo> materialInfoMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(materialInfoList)) {
            materialInfoMap = materialInfoList.stream().collect(Collectors.toMap(item -> GenerageMapKeyUtils.createMapKey(item.getFactoryCode(), item.getMaterialCode()), Function.identity(), (s1, s2) -> s1));
        }

        List<SyncOutFacScheduleVersionVo> syncOutFacScheduleVersionVoList = new ArrayList<>();
        for (FactoryMonthPlanProductionFinalResult result : finalList) {
            SyncOutFacScheduleVersionVo versionVo = new SyncOutFacScheduleVersionVo();
            versionVo.setFactory(result.getFactoryCode());
            versionVo.setPlanVersion(result.getProductionVersion());
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
        return syncOutFacScheduleVersionVoList;
    }
}
