package com.zlt.aps.factory.scheduling;

import com.tlt.aps.enums.ConstructionStageEnum;
import com.zlt.aps.factory.constant.ProductionConstant;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.domain.dto.ProductionGroupInfoDto;
import com.zlt.aps.factory.domain.vo.*;
import com.zlt.aps.monthplan.api.domain.entity.FactoryNoProduction;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanNoProductionRecord;
import com.zlt.aps.monthplan.api.domain.entity.MouldProductionLog;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 排产上下文
 *
 * @author ZLT
 * @date 20250229
 */
@Data
public class ProductionContext extends Context {

    /**
     * 操作批次号
     */
    private String operationWorkNo;
    /**
     * 月份天数
     */
    private Integer monthDays;
    /**
     * 月可排产天数
     */
    private Integer monthWorkDays;
    /**
     * 理论月可排产天明细
     */
    private Set<Integer> wholeMonthWorkDaySet;
    /**
     * 分厂停工日列表<日期>
     */
    private Set<Integer> factoryStopDays;
    /**
     * 分厂参数配置
     */
    private Map<String, Object> factoryParams;
    /**
     * 物料基础信息，Map<物料编码,物料信息>
     */
    private Map<String, ProductBaseInfoVo> productInfoMap;
    /**
     * 拆A率Map，Map<物料编码,拆A率>
     */
    private Map<String, BigDecimal> productDamageMap;
    /**
     * 物料库位的利润等级值Map<物料编码|*|库位编码,利润值>
     */
    private Map<String, Integer> productLocationProfitGradeMap;
    /**
     * 续作规格信息 key ProductCode value 模具列表
     */
    private Map<String, List<MouldProductionProductVo>> continueProductMap;
    /**
     * 物料的施工阶段
     * key 物料编码 value 施工阶段
     */
    private Map<String, ConstructionStageEnum> constructionStageMap;
    /**
     * 基础的施工信息
     * key 生胎代码 value 施工信息--胎体布信息
     */
    private Map<String, BaseConstructionVersionInfoVo> baseConstructionInfoMap;
    /**
     * 物料的施工关系配置
     * key 物料编码 value <硫化规格代号,施工信息></硫化规格代号,施工信息>
     */
    private Map<String, Map<String, ProductConstructionInfoVo>> constructionConfigurationMap;
    /**
     * 开始年月
     */
    private Integer startYearMonth;

    /**
     * 结束年月
     */
    private Integer finallyYearMonth;
    /**
     * 月度模具信息
     * key:模具号-》实例
     */
    private Map<String, MouldInfoVO> mouldInfoMap;

    /***
     *  模具大类分组信息
     *
     *  key:规格+花纹-》模具
     */
    private Map<String, List<MouldInfoVO>> sameMouldMap;
    /**
     * 物料配置的模具列表
     * key：物料编码
     * 值：模具号|*|规格代码
     */
    private Map<String, Set<String>> productRelationMouldMap;
    /**
     * 物料配置的模具列表，
     * 按规格代码再分组
     * key：物料编码
     * 值：{规格代号:模具号}-> key：规格代码 值 模具号
     */
    private Map<String, Map<String, Set<String>>> productRelationSpecCodeMouldMap;
    /**
     * 模具配置的物料列表
     * key：模具号
     * 值：物料编码
     */
    private Map<String, Set<String>> mouldRelationProductMap;
    /**
     * 分厂月计划初始化数据
     */
    private Map<Long, MonthPlanManufacturingRequirementVo> monthPlanInitMap;
    /**
     * 分厂不排产配置--搭配排产时需要再次判断
     */
    private Map<String, FactoryNoProduction> factoryNoProductionMap;
    /**
     * 不排产记录，用于未排计划使用
     */
    private Map<Long, MonthPlanNoProductionRecord> noProductionRecordMap;
    /**
     * 记录排产日志
     */
    private List<MouldProductionLog> productionLogs;
    /**
     * 一键排产时，临时存储使用
     */
    private List<MonthPlanManufacturingRequirementVo> monthPlanInitList;
    /**
     * 一键排产时，临时存储使用
     */
    private List<MonthPlanNoProductionRecord> noProductionRecordList;
    /**
     * 日志存储器
     */
    private StringBuilder logBuilder;
    /**
     * 排产参数配置项
     */
    private ProductionParamConfiguration productionParam;
    /**
     * 排产分组信息对象集合
     * key 分组编号 value 分组排产对象
     */
    private Map<String, ProductionGroupInfoDto> productionGroupInfoMap;
    /**
     * 续作排产分组信息对象集合
     */
    private Map<String, ContinueProductionGroupVo> continueProductionGroupMap;
    /**
     * 每日排产量汇总
     * 20250605 细化到每日按寸口|*|成型法|*|胎体布层级
     */
    private Map<Integer, Map<String, Long>> dayProductionQtyMap;
    /**
     * 每日排产规格数汇总
     */
    private Map<Integer, Set<String>> dayProductCodeMap;
    /**
     * S型排产-第二组双模已经出现超出成型产能的规格信息
     */
    private Map<String, Boolean> exceedCapacityProductMap;
    /**
     * 交期预排使用：--每次预排结束都需要清空
     * 日预排量
     */
    private Map<Integer, Map<String, Long>> preProductionDateQtyMap;
    /**
     * 交期预排使用：--每次预排结束都需要清空
     * 模具日预排量
     */
    private Map<String, Map<Integer, Long>> mouldPreProductionDateQtyMap;
    /**
     * 已排产计划ID及真实排产顺序
     */
    private Map<Long, Integer> productionSchedulePlanMap;
    /**
     * 续作模具需满月排产的规格，需开启SYS038，且月平均销量需大于SYS042
     */
    private Set<String> continueFullMonthProductionSet;
    /**
     * 排产周期--排产开始日
     */
    private Date productionStartDate;
    /**
     * 排产周期--排产结束日
     */
    private Date productionEndDate;
    /**
     * 寸口|*|工装类型|*|成型法|*|胎体布层级，月产能控制
     * key proSize|*|成型法|*|胎体布层级 value 月总产能
     */
    private Map<String, Long> sizeMonthCapacityMap;
    /**
     * 寸口|*|工装类型|*|成形法|*|胎体布层级，天产能控制
     * key day : value proSize|*|胎体布层级成型法|*|胎体布层级, 产能
     */
    private Map<Integer, Map<String, Long>> daySizeCapacityMap;
    /**
     * 寸口|*|工装类型|*|成形法|*|胎体布层级，天最大模具数--产能对等
     * key day : value proSize|*|工装类型|*|胎体布层级成型法|*|胎体布层级, 最大模具数
     * 20251010 ZLT 成型产能对等-使用模具数控制
     */
    private Map<Integer, Map<String, Integer>> dayMaxMouldQtyMap;
    /**
     * 寸口|*|工装类型|*|成形法|*|胎体布层级，天排产模具数--产能对等
     * key day : value proSize|*|工装类型|*|胎体布层级成型法|*|胎体布层级, 排产模具数
     * 20251011 ZLT 成型产能对等-使用模具数控制
     */
    private Map<Integer, Map<String, Integer>> dayProductionMouldQtyMap;
    /**
     * 20251014 ZLT
     * 成型产能对等使用模具数控制使用，计算日模具排产数使用，防止一个模具多次计算天排产模具数
     */
    private Map<Integer, Set<String>> dayProductionFinishMouldMap;
    /**
     * 每天的最大产能控制量
     */
    private Map<Integer, Long> dayMaxCapacityMap;
    /**
     * 轮胎类型 + 寸口 月产能控制
     * key 轮胎类型|*|proSize value 月总产能
     */
    private Map<String, Long> tireCapacityMap;
    /**
     * 最小批量
     */
    private Map<String, Long> minimumLotSizeMap;
    /**
     * 每日新增规格数限制量
     */
    private Map<Integer, Integer> dayAddedProductLimitMap;
    /**
     * 当前每日新增的规格数
     */
    private Map<Integer, Set<String>> dayAddProductMap;
    /**
     * 排产起始日--在排产周期的第几天
     */
    private Integer firstProductionDay;
    /**
     * 是否拼模排产--拼模排产使用
     */
    private boolean assemblingMouldProduction;
    /**
     * 拼模排产起始日--拼模排产使用
     */
    private Integer assemblingMouldStartDay;
    /**
     * 拼模排产下一个规格--拼模排产使用
     */
    private boolean assemblingMouldNextProductCode;
    /**
     * 已经不可拼的规格--拼模排产使用
     */
    private Set<String> noAssemblingMouldProductSet;

    /**
     * 判断排产日是否为周期第一个可排产日
     *
     * @param productionDate
     * @return
     */
    public boolean isProductionFirstDay(Integer productionDate) {
        if (null != firstProductionDay) {
            return firstProductionDay.equals(productionDate);
        }
        if (null == productionDate) {
            return false;
        }
        if (productionDate < ProductionConstant.MONTH_START_DAY) {
            return false;
        }
        if (productionDate > monthDays) {
            return false;
        }
        for (Integer day = ProductionConstant.MONTH_START_DAY; day <= monthDays; day++) {
            if (factoryStopDays.contains(day)) {
                continue;
            }
            if (null == firstProductionDay) {
                firstProductionDay = day;
                break;
            }
        }
        return productionDate.equals(firstProductionDay);
    }

    /**
     * 判断当前是否为夏季月份
     * <p>
     * summerMonth <= month < winterMonth
     *
     * @return
     */
    public boolean isSummerMonth() {
        Integer currentMonth = getMonth();
        if (null == currentMonth) {
            return false;
        }
        if (null == productionParam) {
            return false;
        }
        Integer summerMonth = productionParam.getSummerMonth();
        Integer winterMonth = productionParam.getWinterMonth();
        if (null == summerMonth || null == winterMonth) {
            return false;
        }
        return currentMonth >= summerMonth && currentMonth < winterMonth;
    }

}