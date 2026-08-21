package com.zlt.aps.tm.service.impl;

import com.zlt.aps.tm.api.domain.entity.TmMachineInfo;
import com.zlt.aps.tm.api.domain.entity.TmShiftConfig;
import com.zlt.aps.tm.domain.vo.TmWorkCalendarRowVo;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 胎面单次人工操作内的开机班次校验查询缓存。
 *
 * <p>缓存只在一次校验调用生命周期内有效，不跨请求保存，避免机台、班次和工作日历的数据库状态被长期复用。</p>
 */
final class TmMachineOpenShiftValidationCache {

    private final Map<String, TmMachineInfo> machineInfoMap = new HashMap<>();

    private final Map<String, TmShiftConfig> shiftConfigMap = new HashMap<>();

    private final Map<String, List<TmWorkCalendarRowVo>> workCalendarMap = new HashMap<>();

    /**
     * 判断是否已缓存机台资料。
     *
     * @param factoryCode 工厂编码
     * @param machineCode 机台编码
     * @return true 表示已缓存（包含未查询到资料的空值）
     */
    boolean containsMachine(String factoryCode, String machineCode) {
        return this.machineInfoMap.containsKey(this.buildKey(factoryCode, machineCode));
    }

    /**
     * 读取已缓存的机台资料。
     *
     * @param factoryCode 工厂编码
     * @param machineCode 机台编码
     * @return 机台资料；缓存的未命中返回 null
     */
    TmMachineInfo getMachine(String factoryCode, String machineCode) {
        return this.machineInfoMap.get(this.buildKey(factoryCode, machineCode));
    }

    /**
     * 缓存机台资料或未命中结果。
     *
     * @param factoryCode 工厂编码
     * @param machineCode 机台编码
     * @param machineInfo 机台资料，可为空
     */
    void cacheMachine(String factoryCode, String machineCode, TmMachineInfo machineInfo) {
        this.machineInfoMap.put(this.buildKey(factoryCode, machineCode), machineInfo);
    }

    /**
     * 判断是否已缓存班次配置。
     *
     * @param factoryCode 工厂编码
     * @param shiftOrder 班次顺序
     * @return true 表示已缓存（包含未查询到资料的空值）
     */
    boolean containsShiftConfig(String factoryCode, Integer shiftOrder) {
        return this.shiftConfigMap.containsKey(this.buildKey(factoryCode, shiftOrder));
    }

    /**
     * 读取已缓存的班次配置。
     *
     * @param factoryCode 工厂编码
     * @param shiftOrder 班次顺序
     * @return 班次配置；缓存的未命中返回 null
     */
    TmShiftConfig getShiftConfig(String factoryCode, Integer shiftOrder) {
        return this.shiftConfigMap.get(this.buildKey(factoryCode, shiftOrder));
    }

    /**
     * 缓存班次配置或未命中结果。
     *
     * @param factoryCode 工厂编码
     * @param shiftOrder 班次顺序
     * @param shiftConfig 班次配置，可为空
     */
    void cacheShiftConfig(String factoryCode, Integer shiftOrder, TmShiftConfig shiftConfig) {
        this.shiftConfigMap.put(this.buildKey(factoryCode, shiftOrder), shiftConfig);
    }

    /**
     * 判断是否已缓存工作日历。
     *
     * @param factoryCode 工厂编码
     * @param productionDate 生产日期
     * @return true 表示已缓存（包含未查询到日历的空值）
     */
    boolean containsWorkCalendar(String factoryCode, Date productionDate) {
        return this.workCalendarMap.containsKey(this.buildKey(factoryCode, productionDate));
    }

    /**
     * 读取已缓存的工作日历。
     *
     * @param factoryCode 工厂编码
     * @param productionDate 生产日期
     * @return 工作日历列表；缓存的未命中返回 null
     */
    List<TmWorkCalendarRowVo> getWorkCalendar(String factoryCode, Date productionDate) {
        return this.workCalendarMap.get(this.buildKey(factoryCode, productionDate));
    }

    /**
     * 缓存工作日历查询结果。
     *
     * @param factoryCode 工厂编码
     * @param productionDate 生产日期
     * @param calendarList 工作日历列表，可为空
     */
    void cacheWorkCalendar(String factoryCode, Date productionDate, List<TmWorkCalendarRowVo> calendarList) {
        this.workCalendarMap.put(this.buildKey(factoryCode, productionDate), calendarList);
    }

    /**
     * 构建不依赖字段可见字符的稳定缓存键。
     *
     * @param factoryCode 工厂编码
     * @param identity 缓存维度
     * @return 缓存键
     */
    private String buildKey(String factoryCode, Object identity) {
        Object normalizedIdentity = identity instanceof Date ? ((Date) identity).getTime() : identity;
        return String.valueOf(factoryCode) + "\u0001" + String.valueOf(normalizedIdentity);
    }
}
