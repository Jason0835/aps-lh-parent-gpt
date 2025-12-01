package com.zlt.aps.factory.scheduling;

import com.zlt.aps.factory.utils.ProductionCycleUtils;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 排产参数对象
 *
 * @author ZLT
 * @date 20250414
 */
@Data
public class ProductionParamConfiguration {
    /**
     * 月份周期起始天 参数 SYS003 <=0则为自然月
     */
    private Integer monthCycleStartDay;
    /**
     * 夏季月份 -- 参数 SYS002
     */
    private Integer summerMonth;
    /**
     * 冬季月份 -- 参数 SYS004
     */
    private Integer winterMonth;
    /**
     * 月度排产单日最大排产量--灵活计算
     * min(参数 SYS008成型均产能 * 成型机机台数,
     * 参数 SYS010硫化机均产能 * 硫化机机台数)
     */
    private Long dayMaxProductionQty;
    /**
     * 月度排产单日最大排产规格数
     * 参数 SYS012
     */
    private Integer dayMaxProductCount;
    /**
     * 月度排产单日最大新增规格数
     * 参数 SYS013
     */
    private Integer dayAddedProductCount;
    /**
     * 月度排产开始按寸口由大到小排产模式
     * 参数 SYS015
     */
    private String openProSizeProductionModel;
    /**
     * 月度排产同规格匹配，计划量小于该值则纳入同规格排产
     * 参数 SYS020
     */
    private Integer sameProductProductionQty;
    /**
     * 月度排产同寸口匹配，计划量小于该值则纳入同寸口排产
     * 参数 SYS021
     */
    private Integer sameProSizeProductionQty;
    /**
     * 月度排产是否开启优先共用生胎排产
     * 参数 SYS024
     */
    private String isSameConstructionProduction;
    /**
     * 排产时，续作规格是否开启满月排产
     * 参数 SYS038
     */
    private String isOpenContinueFullMonthProduction;
    /**
     * 续作规格的月平均销量达到该值时，才进行满月排产
     * 参数 SYS042
     */
    private Integer fullMonthProductionQty;
    /**
     * 外贸贴牌规格的OEE率
     * 参数 SYS007
     */
    private BigDecimal exportOemBrandOee;
    /**
     * 续作规格满月排产时，需要前面的需求量排产到的天
     * 需要用月最大天数 - fullMonthProductionDay得到实际排产到天数
     */
    private Integer fullMonthProductionDay;
    /**
     * 合模压力差值
     * 参数 SYS045
     */
    private Integer mouldClampingPressureDiff;
    /**
     * 硫化时间差值
     * 参数 SYS046
     */
    private Integer curingTimeDiff;
    /**
     * 需排产量差值
     * 参数 SYS047
     */
    private Integer planQtyDiff;
    /**
     * 非单模规格-拼模需求量
     * 参数 SYS048
     */
    private Integer assemblingMouldProductionQty;
    /**
     * 特殊天产能控制限制
     * 参数 SYS031
     */
    private Map<Integer, Long> specialDayLimitMap;

    /**
     * 根据排产周期，调整特殊天产能控制
     * 由日期转换成第几天
     *
     * @param startDate 周期起始日
     * @param endDate   周期结束日
     */
    public void updateSpecialDayLimitByCycle(Date startDate, Date endDate) {
        Map<Integer, Integer> dayNumberMap = ProductionCycleUtils.getDayByCycleNumber(startDate, endDate);
        if (CollectionUtils.isEmpty(dayNumberMap)) {
            specialDayLimitMap = Collections.emptyMap();
        }
        if (CollectionUtils.isEmpty(specialDayLimitMap)) {
            return;
        }
        Map<Integer, Long> realDayLimitMap = new HashMap<>();
        specialDayLimitMap.forEach((day, capacity) -> {
            Integer dayByCycle = dayNumberMap.get(day);
            if (null == dayByCycle) {
                return;
            }
            realDayLimitMap.put(dayByCycle, capacity);
        });
        specialDayLimitMap = realDayLimitMap;
    }
}
