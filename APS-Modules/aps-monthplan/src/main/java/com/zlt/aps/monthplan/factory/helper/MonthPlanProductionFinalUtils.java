package com.zlt.aps.monthplan.factory.helper;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.tlt.aps.enums.ChannelRequirementTypeEnum;
import com.tlt.aps.enums.LocationTypeEnum;
import com.tlt.aps.enums.PlanMarkTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.utils.NoProductionReasonUtils;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdFinalQueryDto;
import com.zlt.aps.monthplan.api.domain.entity.MonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanProductionFinalResultVo;
import com.zlt.aps.monthplan.common.utils.JsonUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.core.util.EntityUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 一个SKU一条记录的定稿工具类
 *
 * @author ZLT
 * @date 20250923
 */
@Slf4j
public class MonthPlanProductionFinalUtils {
    /**
     * 校验是否通过
     * 标记code为500则不通过，
     * 否则通过
     *
     * @param checkResult
     * @return
     */
    public static boolean isPassCheck(AjaxResult checkResult) {
        if (null == checkResult) {
            return false;
        }
        Integer checkCode = (Integer) checkResult.get(AjaxResult.CODE_TAG);
        if (AjaxResult.Type.ERROR.value() == checkCode) {
            return false;
        }
        return true;
    }

    /**
     * 数据转换处理
     *
     * @param dataList
     * @return
     */
    public static List<MonthPlanProductionFinalResultVo> buildData(List<MonthPlanProductionFinalResult> dataList) {
        if (CollectionUtils.isEmpty(dataList)) {
            return Collections.emptyList();
        }
        List<MonthPlanProductionFinalResultVo> resultDataList = new ArrayList<>();
        dataList.stream().forEach(resultData -> {
            MonthPlanProductionFinalResultVo rowData = new MonthPlanProductionFinalResultVo();
            BeanUtils.copyProperties(resultData, rowData);
            rowData.setFactoryName(resultData.getFactoryCode());
            YesOrNoEnum yesOrNoEnum = YesOrNoEnum.getEnumByValue(resultData.getIsTrialProductionPlan());
            rowData.setTrialProductionPlan(null == yesOrNoEnum?YesOrNoEnum.NO.getName():yesOrNoEnum.getName());
            //处理特殊字段--库位
            analysisLocationInfo(rowData);
            //处理特殊字段--渠道
            analysisChannelInfo(rowData);
            //处理特殊字段--标记
            analysisMarkInfo(rowData);
            resultDataList.add(rowData);
        });
        return resultDataList;
    }

    /**
     * 构建页面列表查询条件
     *
     * @param queryWrapper
     * @param queryVO
     */
    public static void builderCondition(QueryWrapper<?> queryWrapper, MonthPlanProductionFinalResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("yearMonth")), "YEAR_MONTH", queryVO.getFieldValueByFieldName("yearMonth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanVersion")), "MONTH_PLAN_VERSION", queryVO.getFieldValueByFieldName("monthPlanVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionVersion")), "PRODUCTION_VERSION", queryVO.getFieldValueByFieldName("productionVersion"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productDesc")), "PRODUCT_DESC", queryVO.getFieldValueByFieldName("productDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specifications")), "SPECIFICATIONS", queryVO.getFieldValueByFieldName("specifications"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("pattern")), "PATTERN", queryVO.getFieldValueByFieldName("pattern"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hierarchy")), "HIERARCHY", queryVO.getFieldValueByFieldName("hierarchy"));

        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("levelCode")), "LEVEL_CODE", queryVO.getFieldValueByFieldName("levelCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("levelName")), "LEVEL_NAME", queryVO.getFieldValueByFieldName("levelName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("proSize")), "PRO_SIZE", queryVO.getFieldValueByFieldName("proSize"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeCode")), "PRODUCT_TYPE_CODE", queryVO.getFieldValueByFieldName("productTypeCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productTypeName")), "PRODUCT_TYPE_NAME", queryVO.getFieldValueByFieldName("productTypeName"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("reason")), "REASON", queryVO.getFieldValueByFieldName("reason"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldNo")), "MOULD_NO", queryVO.getFieldValueByFieldName("mouldNo"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
    }

    /**
     * 构建查服务间查询条件
     *
     * @param queryWrapper
     * @param queryCondition
     */
    public static void builderCondition(QueryWrapper<MonthPlanProductionFinalResult> queryWrapper, FactoryMonthPlanProdFinalQueryDto queryCondition) {
        String factoryCode = queryCondition.getFactoryCode();
        queryWrapper.eq(PubUtil.isNotEmpty(factoryCode), "FACTORY_CODE", factoryCode);
        Integer year = queryCondition.getYear();
        queryWrapper.eq(PubUtil.isNotEmpty(year), "YEAR", year);
        Integer month = queryCondition.getMonth();
        queryWrapper.eq(PubUtil.isNotEmpty(month), "MONTH", month);
        String monthPlanVersion = queryCondition.getMonthPlanVersion();
        queryWrapper.eq(PubUtil.isNotEmpty(monthPlanVersion), "MONTH_PLAN_VERSION", monthPlanVersion);
        String productionVersion = queryCondition.getProductionVersion();
        queryWrapper.eq(PubUtil.isNotEmpty(productionVersion), "PRODUCTION_VERSION", productionVersion);
        String productCode = queryCondition.getProductCode();
        queryWrapper.eq(PubUtil.isNotEmpty(productCode), "PRODUCT_CODE", productCode);
        String productDesc = queryCondition.getProductDesc();
        queryWrapper.eq(PubUtil.isNotEmpty(productDesc), "PRODUCT_DESC", productDesc);
        String specifications = queryCondition.getSpecifications();
        queryWrapper.eq(PubUtil.isNotEmpty(specifications), "SPECIFICATIONS", specifications);
        String pattern = queryCondition.getPattern();
        queryWrapper.eq(PubUtil.isNotEmpty(pattern), "PATTERN", pattern);
        BigDecimal proSize = queryCondition.getProSize();
        queryWrapper.eq(PubUtil.isNotEmpty(proSize), "PRO_SIZE", proSize);
        String brand = queryCondition.getBrand();
        queryWrapper.eq(PubUtil.isNotEmpty(brand), "BRAND", brand);
    }

    /**
     * 设置排序信息
     *
     * @param queryVO
     * @return
     */
    public static String getOrderBy(MonthPlanProductionFinalResult queryVO) {
        Map<String, Object> params = queryVO.getParams();
        if (params != null && params.containsKey("orderBy")) {
            String orderByField = (String) params.get("orderBy");
            String dbField = EntityUtil.getColumnNameByFieldName(MonthPlanProductionFinalResult.class, orderByField);
            String isAscStr = (String) params.get("isAsc");
            return dbField + " " + (isAscStr.equals("1") ? "asc" : "desc");
        } else {
            return null;
        }
    }

    /**
     * 解析不排产原因
     * 剔除 无排产量的原因
     *
     * @param list 数据
     */
    public static void dealList(List<MonthPlanProductionFinalResult> list) {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        String language = SecurityUtils.getUserLang().toString();
        JsonUtils.parseJsonRemarkList(list, language, "reason");

        String noProductionQtyReasonJson = NoProductionReasonUtils.getNoProductionQty();
        String noProductionQtyReason = JsonUtils.parseJsonRemark(noProductionQtyReasonJson, language);
        String firstNoProductionQtyReason = String.format("%s,", noProductionQtyReason);
        String endNoProductionQtyReason = String.format(",%s", noProductionQtyReason);
        //未排原因，剔除
        list.forEach(productionFinal -> {
            String reason = productionFinal.getReason();
            if (StringUtils.isBlank(reason)) {
                return;
            }
            String newReason = reason.replaceAll(firstNoProductionQtyReason, "");
            newReason = newReason.replaceAll(endNoProductionQtyReason, "");
            newReason = newReason.replaceAll(noProductionQtyReason, "");
            productionFinal.setReason(newReason);
        });
    }

    /**
     * 解析库位需求
     *
     * @param rowData
     */
    private static void analysisLocationInfo(MonthPlanProductionFinalResultVo rowData) {
        String locationRequirementInfo = rowData.getLocationRequirementInfo();
        if (StringUtils.isBlank(locationRequirementInfo)) {
            return;
        }
        List<MonthPlanProductionRequirementLocationHelper> locationRequirementList = JSON.parseArray(locationRequirementInfo, MonthPlanProductionRequirementLocationHelper.class);
        if (CollectionUtils.isEmpty(locationRequirementList)) {
            return;
        }
        Map<String, MonthPlanProductionRequirementLocationHelper> locationRequirementMap = locationRequirementList.stream()
            .collect(Collectors.toMap(
                MonthPlanProductionRequirementLocationHelper::getType,
                Function.identity(),
                (existing, replacement) -> existing // 选择保留现有的，或者根据需要选择replacement等策略
            ));
        MonthPlanProductionRequirementLocationHelper oe = locationRequirementMap.get(LocationTypeEnum.OE_LOCATION.getValue());
        if (null != oe) {
            rowData.setOeOrderQty(oe.getQty());
        }
        MonthPlanProductionRequirementLocationHelper domestic = locationRequirementMap.get(LocationTypeEnum.DOMESTIC_LOCATION.getValue());
        if (null != domestic) {
            rowData.setDomesticOrderQty(domestic.getQty());
        }
        MonthPlanProductionRequirementLocationHelper foreign = locationRequirementMap.get(LocationTypeEnum.FOREIGN_LOCATION.getValue());
        if (null != foreign) {
            rowData.setForeignOrderQty(foreign.getQty());
        }
    }

    /**
     * 反向解析库位需求
     *
     * @param rowData 要转换的数据
     */
    public static void convertLocationInfo(MonthPlanProductionFinalResultVo rowData) {
        Long oeOrderQty = rowData.getOeOrderQty();
        Long domesticOrderQty = rowData.getDomesticOrderQty();
        Long foreignOrderQty = rowData.getForeignOrderQty();

        MonthPlanProductionRequirementLocationHelper oe = new MonthPlanProductionRequirementLocationHelper();
        oe.setType(LocationTypeEnum.OE_LOCATION.getValue());
        oe.setQty(oeOrderQty);
        MonthPlanProductionRequirementLocationHelper domestic = new MonthPlanProductionRequirementLocationHelper();
        oe.setType(LocationTypeEnum.DOMESTIC_LOCATION.getValue());
        oe.setQty(domesticOrderQty);
        MonthPlanProductionRequirementLocationHelper foreign = new MonthPlanProductionRequirementLocationHelper();
        oe.setType(LocationTypeEnum.FOREIGN_LOCATION.getValue());
        oe.setQty(foreignOrderQty);

        List<MonthPlanProductionRequirementLocationHelper> list = Arrays.asList(oe, domestic, foreign);
        String jsonString = JSON.toJSONString(list);
        rowData.setLocationRequirementInfo(jsonString);
    }

    /**
     * 解析渠道需求
     *
     * @param rowData
     */
    private static void analysisChannelInfo(MonthPlanProductionFinalResultVo rowData) {
        String channelRequirementInfo = rowData.getChannelRequirementInfo();
        if (StringUtils.isBlank(channelRequirementInfo)) {
            return;
        }
        List<MonthPlanProductionRequirementChannelHelper> channelRequirementList = JSON.parseArray(channelRequirementInfo, MonthPlanProductionRequirementChannelHelper.class);
        if (CollectionUtils.isEmpty(channelRequirementList)) {
            return;
        }
        Map<String, MonthPlanProductionRequirementChannelHelper> channelRequirementMap = channelRequirementList.stream().collect(Collectors.toMap(MonthPlanProductionRequirementChannelHelper::getCode, Function.identity()));
        //OE配套
        MonthPlanProductionRequirementChannelHelper oe = channelRequirementMap.get(ChannelRequirementTypeEnum.OE.getCode());
        if (null != oe) {
            rowData.setOeChannelQty(oe.getQty());
        }
        //内销RT
        MonthPlanProductionRequirementChannelHelper domesticRt = channelRequirementMap.get(ChannelRequirementTypeEnum.DOMESTIC_RT.getCode());
        if (null != domesticRt) {
            rowData.setDomesticRtQty(domesticRt.getQty());
        }
        //内销途虎
        MonthPlanProductionRequirementChannelHelper domesticTf = channelRequirementMap.get(ChannelRequirementTypeEnum.DOMESTIC_TF.getCode());
        if (null != domesticTf) {
            rowData.setDomesticTfQty(domesticTf.getQty());
        }
        //内销快准
        MonthPlanProductionRequirementChannelHelper domesticKz = channelRequirementMap.get(ChannelRequirementTypeEnum.DOMESTIC_KZ.getCode());
        if (null != domesticKz) {
            rowData.setDomesticKzQty(domesticKz.getQty());
        }
        //外销贴牌
        MonthPlanProductionRequirementChannelHelper foreignOem = channelRequirementMap.get(ChannelRequirementTypeEnum.FOREIGN_OEM.getCode());
        if (null != foreignOem) {
            rowData.setForeignOemQty(foreignOem.getQty());
        }
        //外销贴牌
        MonthPlanProductionRequirementChannelHelper foreignNoOem = channelRequirementMap.get(ChannelRequirementTypeEnum.FOREIGN_NO_OEM.getCode());
        if (null != foreignNoOem) {
            rowData.setForeignNoOemQty(foreignNoOem.getQty());
        }
    }

    /**
     * 反向解析渠道需求
     *
     * @param rowData 要转换的数据
     */
    public static void convertChannelInfo(MonthPlanProductionFinalResultVo rowData) {
        Long oeChannelQty = rowData.getOeChannelQty();
        Long domesticRtQty = rowData.getDomesticRtQty();
        Long domesticTfQty = rowData.getDomesticTfQty();
        Long domesticKzQty = rowData.getDomesticKzQty();
        Long foreignOemQty = rowData.getForeignOemQty();
        Long foreignNoOemQty = rowData.getForeignNoOemQty();

        MonthPlanProductionRequirementChannelHelper oe = new MonthPlanProductionRequirementChannelHelper();
        oe.setCode(ChannelRequirementTypeEnum.OE.getCode());
        oe.setQty(oeChannelQty);
        MonthPlanProductionRequirementChannelHelper domesticRt = new MonthPlanProductionRequirementChannelHelper();
        domesticRt.setCode(ChannelRequirementTypeEnum.DOMESTIC_RT.getCode());
        domesticRt.setQty(domesticRtQty);
        MonthPlanProductionRequirementChannelHelper domesticTf = new MonthPlanProductionRequirementChannelHelper();
        domesticTf.setCode(ChannelRequirementTypeEnum.DOMESTIC_TF.getCode());
        domesticTf.setQty(domesticTfQty);
        MonthPlanProductionRequirementChannelHelper domesticKz = new MonthPlanProductionRequirementChannelHelper();
        domesticKz.setCode(ChannelRequirementTypeEnum.DOMESTIC_KZ.getCode());
        domesticKz.setQty(domesticKzQty);
        MonthPlanProductionRequirementChannelHelper foreignOem = new MonthPlanProductionRequirementChannelHelper();
        foreignOem.setCode(ChannelRequirementTypeEnum.FOREIGN_OEM.getCode());
        foreignOem.setQty(foreignOemQty);
        MonthPlanProductionRequirementChannelHelper foreignNoOem = new MonthPlanProductionRequirementChannelHelper();
        foreignNoOem.setCode(ChannelRequirementTypeEnum.FOREIGN_NO_OEM.getCode());
        foreignNoOem.setQty(foreignNoOemQty);

        List<MonthPlanProductionRequirementChannelHelper> list = Arrays.asList(oe, domesticRt, domesticTf, domesticKz, foreignOem, foreignNoOem);
        String jsonString = JSON.toJSONString(list);
        rowData.setChannelRequirementInfo(jsonString);
    }

    /**
     * 解析标记
     *
     * @param rowData
     */
    private static void analysisMarkInfo(MonthPlanProductionFinalResultVo rowData) {
        String markInfo = "";
        //续作
        markInfo = addMarkInfoSingle(markInfo, rowData.getIsContinue(), PlanMarkTypeEnum.CONTINUE_PLAN);
        //交期
        markInfo = addMarkInfoSingle(markInfo, rowData.getIsDeliveryDate(), PlanMarkTypeEnum.DELIVERY_DATE_PLAN);
        //必保
        markInfo = addMarkInfoSingle(markInfo, rowData.getIsEnsurePlan(), PlanMarkTypeEnum.ENSURE_PLAN);
        //急单
        markInfo = addMarkInfoSingle(markInfo, rowData.getIsEmergency(), PlanMarkTypeEnum.EMERGENCY_PLAN);
        //重要客户
        markInfo = addMarkInfoSingle(markInfo, rowData.getIsImportantCustom(), PlanMarkTypeEnum.IMPORTANT_CUSTOM_PLAN);
        //欠产
        markInfo = addMarkInfoSingle(markInfo, rowData.getIsDebitPlan(), PlanMarkTypeEnum.DEBIT_PLAN);
        //备货
        markInfo = addMarkInfoSingle(markInfo, rowData.getIsStockUp(), PlanMarkTypeEnum.STOCK_UP_PLAN);
        rowData.setMarkInfo(markInfo);
    }

    /**
     * 增加标记信息
     *
     * @param markInfo  原有标记信息
     * @param markValue 标记值
     * @param markType  标记类型
     * @return
     */
    private static String addMarkInfoSingle(String markInfo, Integer markValue, PlanMarkTypeEnum markType) {
        if (!YesOrNoEnum.YES.getValue().equals(markValue)) {
            return markInfo;
        }
        if (StringUtils.isBlank(markInfo)) {
            return markType.getName();
        }
        String markFormat = "%s %s";
        return String.format(markFormat, markInfo, markType.getName());
    }

    private MonthPlanProductionFinalUtils() {

    }
}
