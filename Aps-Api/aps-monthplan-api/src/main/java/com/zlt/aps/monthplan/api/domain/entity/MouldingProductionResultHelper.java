package com.zlt.aps.monthplan.api.domain.entity;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.aps.monthplan.api.domain.vo.DayLeftOverCuringTimeVo;
import com.zlt.aps.monthplan.api.domain.vo.DayProductionInfoVo;
import com.zlt.aps.monthplan.api.domain.vo.NoProductionDayMouldVo;
import com.zlt.aps.monthplan.api.domain.vo.ProductProductionInfoVo;
import com.zlt.aps.monthplan.api.enums.MouldNoProductionType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MouldingProductionResultHelper.java
 * 描    述：分厂月生产计划排产结果-模具排产辅助信息对象 t_mp_moulding_helper
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-18
 */

@Data
@TableName(value = "T_MP_MOULDING_HELPER")
@ApiModel(value = "分厂月生产计划排产结果-模具排产辅助信息对象", description = "分厂月生产计划排产结果-模具排产辅助信息对象 ")
public class MouldingProductionResultHelper extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 生产分厂编号
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.factoryCode")
    @ApiModelProperty(value = "生产分厂编号", name = "factoryCode")
    @TableField(value = "FACTORY_CODE")
    private String factoryCode;

    /**
     * 年份
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.year")
    @ApiModelProperty(value = "年份", name = "year")
    @TableField(value = "YEAR")
    private Integer year;

    /**
     * 月份
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.month")
    @ApiModelProperty(value = "月份", name = "month")
    @TableField(value = "MONTH")
    private Integer month;

    /**
     * 销售生产需求计划版本
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.monthPlanVersion")
    @ApiModelProperty(value = "销售生产需求计划版本", name = "monthPlanVersion")
    @TableField(value = "MONTH_PLAN_VERSION")
    private String monthPlanVersion;

    /**
     * 分厂版本
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.productionVersion")
    @ApiModelProperty(value = "分厂版本", name = "productionVersion")
    @TableField(value = "PRODUCTION_VERSION")
    private String productionVersion;

    /**
     * 模具
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.mouldNo")
    @ApiModelProperty(value = "模具", name = "mouldNo")
    @TableField(value = "MOULD_NO")
    private String mouldNo;

    /**
     * 模具号
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.mouldCode")
    @ApiModelProperty(value = "模具号", name = "mouldCode")
    @TableField(value = "MOULD_CODE")
    private String mouldCode;
    /**
     * 标记是否为续作模具
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.isContinue")
    @ApiModelProperty(value = "是否续作模具", name = "isContinue")
    @TableField(value = "IS_CONTINUE")
    private Integer isContinue;
    /**
     * 模具类型 1-1 2-2 3-3
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.mouldType")
    @ApiModelProperty(value = "模具类型 1-1 2-2 3-3", name = "mouldType")
    @TableField(value = "MOULD_TYPE")
    private String mouldType;
    /**
     * 排产方向 0 正向 1 反向
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.productionOrient")
    @ApiModelProperty(value = "排产方向", name = "productionOrient")
    @TableField(value = "PRODUCTION_ORIENT")
    private Integer productionOrient;
    /**
     * 总硫化时间-到秒
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.totalSeconds")
    @ApiModelProperty(value = "硫化总工时", name = "totalSeconds")
    @TableField(value = "TOTAL_SECONDS")
    private Integer totalSeconds;
    /**
     * 已经硫化时间-到秒
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.usedSeconds")
    @ApiModelProperty(value = "已经硫化时间", name = "usedSeconds")
    @TableField(value = "USED_SECONDS")
    private Integer usedSeconds;
    /**
     * 关联的物料号-多个以,分隔
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.relationProductInfo")
    @ApiModelProperty(value = "关联的物料号-多个以,分隔", name = "relationProductInfo")
    @TableField(value = "RELATION_PRODUCT_INFO")
    private String relationProductInfo;
    /**
     * 日排产信息json格式
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.productionInfo")
    @ApiModelProperty(value = "日排产信息json格式", name = "productionInfo")
    @TableField(value = "PRODUCTION_INFO")
    private String productionInfo;
    /**
     * 不排产日信息json格式
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.noProductionInfo")
    @ApiModelProperty(value = "不排产日信息json格式", name = "noProductionInfo")
    @TableField(value = "NO_PRODUCTION_INFO")
    private String noProductionInfo;
    /**
     * 排产日剩余硫化时间信息json格式
     */
    @Excel(name = "ui.data.column.mouldingProductionResultHelper.productionCuringTime")
    @ApiModelProperty(value = "排产日剩余硫化时间信息json格式", name = "productionCuringTime")
    @TableField(value = "PRODUCTION_CURING_TIME")
    private String productionCuringTime;

    /**
     * 设置排产日剩余硫化时间信息-转化json格式
     *
     * @param leftOverCuringTimeMap
     */
    public void setProductionCuringTimeInfo(Map<Integer, BigDecimal> leftOverCuringTimeMap) {
        if (CollectionUtils.isEmpty(leftOverCuringTimeMap)) {
            productionCuringTime = "";
            return;
        }
        List<DayLeftOverCuringTimeVo> dayLeftOverCuringTimeList = new ArrayList<>();
        leftOverCuringTimeMap.forEach((day, dayLeftOverCuringTime) -> {
            if (null == day) {
                return;
            }
            DayLeftOverCuringTimeVo dayTime = new DayLeftOverCuringTimeVo();
            dayTime.setDay(day);
            dayTime.setLeftOverCuringTime(dayLeftOverCuringTime);
            dayLeftOverCuringTimeList.add(dayTime);
        });
        if (CollectionUtils.isEmpty(dayLeftOverCuringTimeList)) {
            productionCuringTime = "";
            return;
        }
        productionCuringTime = JSON.toJSONString(dayLeftOverCuringTimeList);
    }

    /**
     * 获取日剩余硫化时间
     *
     * @return
     */
    public Map<Integer, BigDecimal> getDayLeftOverCuringTimeMap() {
        if (StringUtils.isBlank(productionCuringTime)) {
            return Collections.emptyMap();
        }
        List<DayLeftOverCuringTimeVo> dayLeftOverCuringTimeList = JSON.parseArray(productionCuringTime, DayLeftOverCuringTimeVo.class);
        if (CollectionUtils.isEmpty(dayLeftOverCuringTimeList)) {
            return Collections.emptyMap();
        }
        return dayLeftOverCuringTimeList.stream().collect(Collectors.toMap(DayLeftOverCuringTimeVo::getDay, DayLeftOverCuringTimeVo::getLeftOverCuringTime));
    }

    /**
     * 获取模具固定不可排产日
     * 即停工日和维修日，剔除洗模日
     *
     * @return
     */
    public Set<Integer> getFixedNoProductionDayList() {
        if (StringUtils.isBlank(noProductionInfo)) {
            return Collections.emptySet();
        }
        List<NoProductionDayMouldVo> noProductionDayList = JSON.parseArray(noProductionInfo, NoProductionDayMouldVo.class);
        if (CollectionUtils.isEmpty(noProductionDayList)) {
            return Collections.emptySet();
        }
        Set<Integer> fixedNoProductionSet = new HashSet<>();
        noProductionDayList.forEach(noProductionDay -> {
            if (MouldNoProductionType.MAINTENANCE_DAY == noProductionDay.getNoProductionType()) {
                return;
            }
            fixedNoProductionSet.add(noProductionDay.getDay());
        });
        return fixedNoProductionSet;
    }

    /**
     * 获取日剩余硫化时间
     *
     * @return
     */
    public Map<Integer, DayLeftOverCuringTimeVo> getDayLeftOverCuringTime() {
        if (StringUtils.isBlank(productionCuringTime)) {
            return Collections.emptyMap();
        }
        List<DayLeftOverCuringTimeVo> dayLeftOverCuringTimeList = JSON.parseArray(productionCuringTime, DayLeftOverCuringTimeVo.class);
        if (CollectionUtils.isEmpty(dayLeftOverCuringTimeList)) {
            return Collections.emptyMap();
        }
        return dayLeftOverCuringTimeList.stream().collect(Collectors.toMap(DayLeftOverCuringTimeVo::getDay, Function.identity()));
    }

    /**
     * 设置日排产信息，转化成json格式
     *
     * @param dayProductionInfo
     */
    public void setDayProductionInfo(Map<Integer, List<ProductProductionInfoVo>> dayProductionInfo) {
        if (CollectionUtils.isEmpty(dayProductionInfo)) {
            productionInfo = "";
            return;
        }
        List<DayProductionInfoVo> dayProductionInfoList = new ArrayList<>();
        dayProductionInfo.forEach((day, dayProductionList) -> {
            if (null == day || CollectionUtils.isEmpty(dayProductionList)) {
                return;
            }
            DayProductionInfoVo dayProduction = new DayProductionInfoVo();
            dayProduction.setDay(day);
            dayProduction.setProductionList(dayProductionList);
            dayProductionInfoList.add(dayProduction);
        });
        if (CollectionUtils.isEmpty(dayProductionInfoList)) {
            productionInfo = "";
            return;
        }
        productionInfo = JSON.toJSONString(dayProductionInfoList);
    }

    /**
     * 判断模具在productionDay是否已排
     *
     * @param productionDay 排产日
     * @param productCode   排产物料
     * @return
     */
    public boolean isProductionProductByDay(Integer productionDay, String productCode) {
        if (null == productionDay || StringUtils.isBlank(productCode)) {
            return false;
        }
        Map<Integer, List<ProductProductionInfoVo>> dayProductionMap = getDayProductionInfo();
        if (CollectionUtils.isEmpty(dayProductionMap)) {
            return false;
        }
        List<ProductProductionInfoVo> dayProductionList = dayProductionMap.get(productionDay);
        if (CollectionUtils.isEmpty(dayProductionList)) {
            return false;
        }
        Set<String> dayProductionSet = dayProductionList.stream().map(ProductProductionInfoVo::getProductCode).collect(Collectors.toSet());
        return dayProductionSet.contains(productCode);
    }

    /**
     * 获取模具日排产规格及数量
     * 有顺序
     *
     * @return
     */
    public Map<Integer, List<ProductProductionInfoVo>> getDayProductionInfo() {
        if (StringUtils.isBlank(productionInfo)) {
            return Collections.emptyMap();
        }
        List<DayProductionInfoVo> dayProductionList = JSON.parseArray(productionInfo, DayProductionInfoVo.class);
        if (CollectionUtils.isEmpty(dayProductionList)) {
            return Collections.emptyMap();
        }
        return dayProductionList.stream().collect(Collectors.toMap(DayProductionInfoVo::getDay, DayProductionInfoVo::getProductionList));
    }
}