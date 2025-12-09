package com.zlt.aps.factory.utils;

import com.zlt.aps.factory.domain.vo.MouldInfoVO;
import com.zlt.aps.factory.domain.vo.MouldMaintenanceConfigurationVo;
import com.zlt.aps.factory.enums.MouldAirTypeEnum;
import com.zlt.aps.factory.scheduling.ProductionContext;
import com.zlt.aps.monthplan.api.domain.vo.NoProductionDayMouldVo;
import com.zlt.aps.monthplan.api.enums.MouldNoProductionType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 模具基础业务-工具类
 *
 * @author ZLT
 * @date 20250830
 */
@Slf4j
public class MouldBaseUtils {

    /**
     * 根据模具返厂配置，构建模具信息
     * 模具不可排产列表，可排产日列表及总的硫化时间(分)
     *
     * @param maintenanceMould         已维修设置
     * @param maintenanceConfiguration 当前维修设置
     * @param zoneId                   时区
     * @param context                  排产上下文
     * @return
     */
    public static MouldInfoVO buildMouldInfo(MouldInfoVO maintenanceMould, MouldMaintenanceConfigurationVo maintenanceConfiguration, ZoneId zoneId, ProductionContext context) {
        MouldInfoVO mouldInfo;
        if (null == maintenanceMould) {
            mouldInfo = new MouldInfoVO();
            mouldInfo.setMouldCode(maintenanceConfiguration.getMouldCode());
            mouldInfo.setFactoryCode(maintenanceConfiguration.getFactoryCode());
            mouldInfo.setMouldNo(maintenanceConfiguration.getMouldNo());
            mouldInfo = buildBaseMouldInfo(mouldInfo);
        } else {
            mouldInfo = maintenanceMould;
        }
        //每天工作时限
        BigDecimal dayCuringTime = ProductionProcessUtils.getDayWorkHours(context);
        //月份最大天数
        Integer maxDays = context.getMonthDays();
        LocalDate maintenanceBeginDate = maintenanceConfiguration.getBeginDate().toInstant().atZone(zoneId).toLocalDate();
        LocalDate maintenanceEndDate = maintenanceConfiguration.getEndDay().toInstant().atZone(zoneId).toLocalDate();
        //20250519 ZLT 排产月份区分自然月与非自然月
        Map<String, Integer> daysMap = DateUtils.calculateDaysByMonth(context, maintenanceBeginDate, maintenanceEndDate);
        Integer beginDay = daysMap.get(DateUtils.START_DAY);
        Integer endDay = daysMap.get(DateUtils.END_DAY);
        //不可排产日列表
        Map<Integer, NoProductionDayMouldVo> noProductionDayMap = mouldInfo.getNoProductionDayList();
        if (null == noProductionDayMap) {
            noProductionDayMap = new HashMap<>(maxDays);
        }
        //停工日
        Set<Integer> stopDays = context.getFactoryStopDays();
        //先停工
        if (!CollectionUtils.isEmpty(stopDays)) {
            for (Integer stopDay : stopDays) {
                if (noProductionDayMap.containsKey(stopDay)) {
                    continue;
                }
                NoProductionDayMouldVo noProductionDay = new NoProductionDayMouldVo();
                noProductionDay.setDay(stopDay);
                noProductionDay.setNoProductionType(MouldNoProductionType.STOP_DAY);
                noProductionDayMap.put(stopDay, noProductionDay);
            }
        }
        //后维修
        for (int maintenanceDay = beginDay; maintenanceDay <= endDay; maintenanceDay++) {
            if (noProductionDayMap.containsKey(maintenanceDay)) {
                continue;
            }
            NoProductionDayMouldVo noProductionDay = new NoProductionDayMouldVo();
            noProductionDay.setDay(maintenanceDay);
            noProductionDay.setNoProductionType(MouldNoProductionType.MAINTENANCE_DAY);
            noProductionDayMap.put(maintenanceDay, noProductionDay);
        }
        mouldInfo.setNoProductionDayList(noProductionDayMap);
        //可排产日列表
        Map<Integer, BigDecimal> productionDayMap = new HashMap<>(maxDays);
        BigDecimal totalCuringTime = BigDecimal.ZERO;
        for (int productionDay = BigDecimal.ONE.intValue(); productionDay <= maxDays; productionDay++) {
            if (noProductionDayMap.containsKey(productionDay)) {
                continue;
            }
            productionDayMap.put(productionDay, dayCuringTime);
            totalCuringTime = totalCuringTime.add(dayCuringTime);
        }
        mouldInfo.setTotalSeconds(totalCuringTime);
        mouldInfo.setLeftOverSeconds(totalCuringTime);
        mouldInfo.setPreemptLeftOverSeconds(totalCuringTime);
        mouldInfo.setProductionDayList(productionDayMap);
        return mouldInfo;
    }

    /**
     * 构建模具对象最基本信息
     * 模具编码、类型、已排产完毕日
     * 已硫化时间、连续排产日数、日排产列表
     *
     * @param baseInfo
     * @return
     */
    public static MouldInfoVO buildBaseMouldInfo(MouldInfoVO baseInfo) {
        MouldInfoVO mouldInfo = new MouldInfoVO();
        mouldInfo.setMouldCode(baseInfo.getMouldCode());
        mouldInfo.setMouldType(baseInfo.getMouldType());
        mouldInfo.setFactoryCode(baseInfo.getFactoryCode());
        mouldInfo.setMouldNo(baseInfo.getMouldNo());
        mouldInfo.setProductionFinishDayList(new HashSet<>());
        mouldInfo.setUsedSeconds(BigDecimal.ZERO);
        mouldInfo.setContinuousDays(BigDecimal.ZERO.intValue());
        mouldInfo.setDayProductionMap(new HashMap<>());
        mouldInfo.setCleanDayList(new HashMap<>());
        //模具类型转换
        setMouldAirType(mouldInfo);
        return mouldInfo;
    }

    /**
     * 根据模具的汽套类型，设置模具汽套类型枚举实例对象
     *
     * @param mouldInfo
     */
    private static void setMouldAirType(MouldInfoVO mouldInfo) {
        String mouldAirType = mouldInfo.getMouldAirType();
        if (StringUtils.isBlank(mouldAirType)) {
            mouldInfo.setMouldAirType(MouldAirTypeEnum.NORMAL.getValue());
            return;
        }
        //非弹簧汽套模具->普通模具
        MouldAirTypeEnum mouldAirTypeEnum = MouldAirTypeEnum.getEnumByValue(mouldInfo.getMouldAirType());
        if (MouldAirTypeEnum.NO_AIR == mouldAirTypeEnum) {
            mouldInfo.setMouldAirType(MouldAirTypeEnum.NORMAL.getValue());
            return;
        }
        mouldInfo.setMouldAirType(mouldAirTypeEnum.getValue());
    }

    private MouldBaseUtils() {

    }
}
