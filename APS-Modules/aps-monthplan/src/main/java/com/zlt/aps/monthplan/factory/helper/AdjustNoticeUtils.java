package com.zlt.aps.monthplan.factory.helper;

import com.alibaba.fastjson.JSON;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.constant.StringConstant;
import com.tlt.aps.enums.ConstructionStageEnum;
import com.tlt.aps.enums.SortHierarchyEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthFinalPlanHelperVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanAdjustNoticeOrderOperateVo;
import com.zlt.aps.monthplan.api.domain.vo.MonthPlanNeedAdjustPlanVo;
import com.zlt.aps.monthplan.api.domain.vo.ProductSpecInfoVo;
import com.zlt.aps.monthplan.api.enums.MonthPlanAdjustNoticeStatusEnum;
import com.zlt.aps.monthplan.api.enums.MonthPlanAdjustTypeEnum;
import com.zlt.aps.monthplan.factory.dto.MouldProductRelationDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 调整通知单业务工具类
 *
 * @author ZLT
 * @date 20250528
 */
@Slf4j
public class AdjustNoticeUtils {

    /**
     * 获取最早的可调整日期
     * 当前日期，根据参数SYS001延后多少天开始进行调整
     *
     * @param delayDays 延迟天数
     * @return
     */
    public static Date getMinStartAdjustDate(Integer delayDays) {
        Date currentDate = new Date();
        String dayFormat = DateUtils.YYYY_MM_DD;
        String currentDateFormat = DateUtils.parseDateToStr(dayFormat, currentDate);
        Date matchDate = DateUtils.dateTime(dayFormat, currentDateFormat);
        return DateUtils.addDays(matchDate, delayDays);
    }

    /**
     * 判断adjustStartDate可否进行调整
     *
     * @param adjustDate        调整日期
     * @param productionVersion 版本信息
     * @return
     */
    public static boolean canAdjust(Date adjustDate, FactoryProductionVersion productionVersion) {
        if (null == adjustDate || null == productionVersion) {
            return false;
        }
        Date startCycleDate = productionVersion.getProductionStartDate();
        Date endCycleDate = productionVersion.getProductionEndDate();
        if (null == startCycleDate || null == endCycleDate) {
            return false;
        }
        if (startCycleDate.after(adjustDate)) {
            return false;
        }
        if (endCycleDate.before(adjustDate)) {
            return false;
        }
        return true;
    }

    /**
     * 更新物料基础信息
     *
     * @param noticeOrder 调整通知单对象
     * @param productInfo 物料基础信息对象
     */
    public static void setProductInfo(MonthPlanNoticeOrder noticeOrder, MdmMaterialInfo productInfo) {
        noticeOrder.setBrand(productInfo.getBrand());
        noticeOrder.setProductDesc(productInfo.getMaterialDesc());
        noticeOrder.setHierarchy(productInfo.getHierarchy());
        noticeOrder.setSpecifications(productInfo.getSpecifications());
        noticeOrder.setProSize(productInfo.getProSize());
        noticeOrder.setProductTypeCode(productInfo.getProductTypeCode());
        noticeOrder.setProductTypeName(productInfo.getProductTypeName());
        noticeOrder.setPattern(productInfo.getPattern());
    }

    /**
     * 判断调整通知单调整的数量方向是否一致
     * 即调增通知单的调整量是 > 0
     * 调减通知单的调整量是 < 0
     *
     * @param noticeOrderOperate 调整操作对象
     * @param noticeOrder        调整通知单信息
     * @return
     */
    public static boolean isSameDirection(MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate, MonthPlanNoticeOrder noticeOrder) {
        Long planQty = noticeOrder.getPlanQty();
        Long adjustNumber = noticeOrderOperate.getAdjustNumber();
        if (planQty > BigDecimal.ZERO.longValue() && adjustNumber >= BigDecimal.ZERO.longValue()) {
            return true;
        }
        if (planQty < BigDecimal.ZERO.longValue() && adjustNumber <= BigDecimal.ZERO.longValue()) {
            return true;
        }
        return false;
    }

    /**
     * 设置自动确认信息，自动确认的备注信息
     *
     * @param noticeOrder   调整通知单
     * @param operateRemark 自动确认提示信息
     */
    public static void setAutoConfirm(MonthPlanNoticeOrder noticeOrder, String operateRemark) {
        String remark = noticeOrder.getRemark();
        if (StringUtils.isBlank(remark)) {
            noticeOrder.setRemark(operateRemark);
        } else {
            noticeOrder.setRemark(String.format("%s;%s", remark, operateRemark));
        }
        noticeOrder.setProductionQty(BigDecimal.ZERO.longValue());
        noticeOrder.setStatus(MonthPlanAdjustNoticeStatusEnum.CONFIRM.getStatus());
    }

    /**
     * 校验调减计划参数是否无效
     * 排产单号不能为空，
     * 开始调整日期不能为空，
     * 调减数量不能为空或是零
     *
     * @param subtractPlan
     * @return
     */
    public static boolean isEffective(MonthPlanNeedAdjustPlanVo subtractPlan) {
        if (null == subtractPlan) {
            return false;
        }
        if (null == subtractPlan.getStartAdjustDate()) {
            return false;
        }
        if (StringUtils.isBlank(subtractPlan.getProductionNo())) {
            return false;
        }
        Long subtractNumber = subtractPlan.getNeedAdjustNumber();
        if (null == subtractNumber || subtractNumber.equals(BigDecimal.ZERO.longValue())) {
            return false;
        }
        return true;
    }

    /**
     * 获取排产顺序的库位顺序
     *
     * @param sortConfigurationList
     * @return
     */
    public static List<PlanOrderSortConfiguration> getLocationSortConfiguration(List<PlanOrderSortConfiguration> sortConfigurationList) {
        if (CollectionUtils.isEmpty(sortConfigurationList)) {
            return Collections.emptyList();
        }
        Map<SortHierarchyEnum, List<PlanOrderSortConfiguration>> hierarchyMap = getGroupProductionConfiguration(sortConfigurationList);
        if (CollectionUtils.isEmpty(hierarchyMap)) {
            return Collections.emptyList();
        }
        List<PlanOrderSortConfiguration> locationSortConfiguration = hierarchyMap.get(SortHierarchyEnum.THIRD_HIERARCHY);
        if (CollectionUtils.isEmpty(locationSortConfiguration)) {
            return Collections.emptyList();
        }
        return locationSortConfiguration;
    }

    /**
     * 根据库位类别顺序配置，设置其排序值
     *
     * @param thirdSortConfiguration 第三排产顺序
     * @param productionPlan         排产计划
     * @return
     */
    public static void setSortValue(FactoryMonthFinalPlanHelperVo productionPlan, List<PlanOrderSortConfiguration> thirdSortConfiguration) {
        if (CollectionUtils.isEmpty(thirdSortConfiguration)) {
            productionPlan.setSortValue(Integer.MAX_VALUE);
            return;
        }
        for (PlanOrderSortConfiguration sortConfiguration : thirdSortConfiguration) {
            String optionCode = sortConfiguration.getOptionCode();
            String[] options = optionCode.split(StringConstant.SPLIT_SEMICOLON);
            if (check(productionPlan, options[0], options[1], options[2])) {
                productionPlan.setSortValue(sortConfiguration.getPriority());
                break;
            }
        }
        //没有匹配到，设置成最大值，即最低
        if (null == productionPlan.getSortValue()) {
            productionPlan.setSortValue(Integer.MAX_VALUE);
        }
    }

    /**
     * 根据版本信息，获取对应版本排产的周期天数
     *
     * @param productionVersion 版本信息
     * @return
     */
    public static Integer getProductionVersionCycleDays(FactoryProductionVersion productionVersion) {
        if (null == productionVersion) {
            return BigDecimal.ZERO.intValue();
        }
        Date productionStartDate = productionVersion.getProductionStartDate();
        Date productionEndDate = productionVersion.getProductionEndDate();
        if (null == productionStartDate || null == productionEndDate) {
            return BigDecimal.ZERO.intValue();
        }
        Long diffDay = Duration.between(productionStartDate.toInstant(), productionEndDate.toInstant()).toDays();
        return Math.abs(diffDay.intValue()) + BigDecimal.ONE.intValue();
    }

    /**
     * 获取两日期相差天数，并转化成字段数值，故而会加1
     *
     * @param startDate 开始
     * @param endDate   结束
     * @return
     */
    public static Integer getDatePhaseDiff(Date startDate, Date endDate) {
        if (null == startDate || null == endDate) {
            return BigDecimal.ZERO.intValue();
        }
        Long diff = Duration.between(startDate.toInstant(), endDate.toInstant()).toDays();
        return Math.abs(diff.intValue()) + BigDecimal.ONE.intValue();
    }

    /**
     * 获取需要调整的计划信息集合
     *
     * @param startDays           起始调整日
     * @param maxDays             最大调整日
     * @param originPlanList      原计划集合
     * @param adjustAfterPlanList 调整后计划集合
     * @param defaultAdjustType   默认的调整方式
     * @return
     */
    public static AjaxResult getNeedAdjustPlanInfoList(Integer startDays, Integer maxDays, List<FactoryMonthPlanProdFinal> originPlanList, List<FactoryMonthPlanProdFinal> adjustAfterPlanList, MonthPlanAdjustTypeEnum defaultAdjustType) {
        Map<String, FactoryMonthPlanProdFinal> originPlanMap = originPlanList.stream().collect(Collectors.toMap(FactoryMonthPlanProdFinal::getProductionNo, Function.identity()));
        List<MonthPlanNeedAdjustPlanVo> needAdjustPlanInfoList = new ArrayList<>();
        for (FactoryMonthPlanProdFinal adjustAfterPlan : adjustAfterPlanList) {
            String productionNo = adjustAfterPlan.getProductionNo();
            FactoryMonthPlanProdFinal originPlan = originPlanMap.get(productionNo);
            if (null == originPlan) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.adjust.data.noExistPlan"));
            }
            MonthPlanNeedAdjustPlanVo needAdjustPlan = AdjustNoticeUtils.getDifference(startDays, maxDays, adjustAfterPlan, originPlan);
            if (null == needAdjustPlan.getAdjustType()) {
                needAdjustPlan.setAdjustType(defaultAdjustType);
            }
            needAdjustPlanInfoList.add(needAdjustPlan);
        }
        return AjaxResult.success(needAdjustPlanInfoList);
    }

    /**
     * 对象转换
     *
     * @param noticeOrder
     * @param needAdjustInfo
     * @return
     */
    public static MonthPlanAdjustDetail buildAdjustDetail(MonthPlanNoticeOrder noticeOrder, MonthPlanNeedAdjustPlanVo needAdjustInfo, String workNo) {
        MonthPlanAdjustDetail detail = new MonthPlanAdjustDetail();
        detail.setNoticeNo(noticeOrder.getNoticeNo());
        detail.setYear(noticeOrder.getYear());
        detail.setMonth(noticeOrder.getMonth());
        detail.setMonthPlanVersion(noticeOrder.getMonthPlanVersion());
        detail.setProductionVersion(noticeOrder.getProductionVersion());
        detail.setFactoryCode(noticeOrder.getFactoryCode());
        detail.setWorkNo(workNo);
        detail.setProductionNo(needAdjustInfo.getProductionNo());
        detail.setAdjustQty(needAdjustInfo.getNeedAdjustNumber());
        detail.setAdjustType(needAdjustInfo.getAdjustType().getAdjustType());
        detail.setProductCode(needAdjustInfo.getProductCode());
        detail.setProductDesc(needAdjustInfo.getProductDesc());
        detail.setIsImport(YesOrNoEnum.NO.getValue());
        return detail;
    }

    /**
     * 构建增量计划的调增对象
     *
     * @param noticeOrderOperate 调增信息
     * @param productionNo       新的排产制造单号
     * @return
     */
    public static MonthPlanNeedAdjustPlanVo buildAddAdjustPlan(MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate, String productionNo) {
        MonthPlanNeedAdjustPlanVo add = new MonthPlanNeedAdjustPlanVo();
        add.setAdjustType(MonthPlanAdjustTypeEnum.ADD);
        add.setProductionNo(productionNo);
        add.setProductCode(noticeOrderOperate.getProductCode());
        add.setProductDesc(noticeOrderOperate.getProductDesc());
        add.setNeedAdjustNumber(noticeOrderOperate.getAdjustNumber());
        add.setStartAdjustDate(noticeOrderOperate.getStartDate());
        return add;
    }

    /**
     * 创建新的排产计划
     *
     * @param productionVersion 版本信息
     * @param addPlan           调增计划信息
     * @param helper            施工信息
     * @return
     */
    public static FactoryMonthPlanProdFinal buildNewProductionPlan(FactoryProductionVersion productionVersion, MonthPlanAdjustNoticeOrderOperateVo addPlan, AdjustProductConstructionInfoHelper helper) {
        FactoryMonthPlanProdFinal productionPlan = new FactoryMonthPlanProdFinal();
        Integer year = productionVersion.getYear();
        Integer month = productionVersion.getMonth();
        String yearAndMonth = String.format("%s%02d", year, month);
        //版本信息
        productionPlan.setFactoryCode(productionVersion.getFactoryCode());
        productionPlan.setYear(year);
        productionPlan.setMonth(month);
        productionPlan.setYearMonth(Integer.valueOf(yearAndMonth));
        productionPlan.setMonthPlanVersion(productionVersion.getMonthPlanVersion());
        productionPlan.setProductionVersion(productionVersion.getProductionVersion());
        //物料信息
        productionPlan.setProductCode(addPlan.getProductCode());
        productionPlan.setCuringTime(helper.getCuringTime());
        productionPlan.setLocationType(addPlan.getLocationType());
        productionPlan.setChannel(addPlan.getChannel());
        //排产量
        Long adjustNumber = Long.valueOf(addPlan.getAdjustNumber());
        productionPlan.setProdReqPlan(adjustNumber);
        productionPlan.setFactProdReqQty(adjustNumber);
        productionPlan.setTotalQty(adjustNumber);
        productionPlan.setDifferenceQty(BigDecimal.ZERO.longValue());
        //模具、规格代号、生胎代码
        String specCodeInfo = helper.getSpecCodeInfo();
        String specCode = addPlan.getSpecCode();
        productionPlan.setMouldNo(addPlan.getMouldNo());
        productionPlan.setSpecCodeInfo(specCodeInfo);
        productionPlan.setSpecCode(specCode);
        setEmbryoCodeInfo(specCodeInfo, productionPlan, specCode);
        //施工信息
        ConstructionStageEnum stage = ConstructionStageEnum.matchByConstructionCode(helper.getConstructionCode());
        productionPlan.setConstructionStage(stage.getStage());
        productionPlan.setMergeInfo("");
        productionPlan.setIsImport(YesOrNoEnum.NO.getValue());
        productionPlan.setIsDeliveryDate(YesOrNoEnum.NO.getValue());
        BigDecimal totalCuringTime = helper.getCuringTime().multiply(BigDecimal.valueOf(adjustNumber));
        productionPlan.setTotalVulcanizationMinutes(totalCuringTime.divide(BigDecimal.valueOf(FactoryConstant.MINUTE_SECOND), 2, RoundingMode.HALF_UP));
        return productionPlan;
    }

    /**
     * 填充物料相关信息
     * 规格描述、花纹、层级、寸口、品牌
     * 胎别、规格
     *
     * @param productionPlan 新增的调整计划
     * @param productInfo    物料信息
     */
    public static void fillProductInfo(FactoryMonthPlanProdFinal productionPlan, MdmMaterialInfo productInfo) {
        productionPlan.setProductDesc(productInfo.getMaterialDesc());
        productionPlan.setPattern(productInfo.getPattern());
        productionPlan.setHierarchy(productInfo.getHierarchy());
        productionPlan.setSpecifications(productInfo.getSpecifications());
        productionPlan.setProSize(productInfo.getProSize());
        productionPlan.setBrand(productInfo.getBrand());
        productionPlan.setProductTypeCode(productInfo.getProductTypeCode());
        productionPlan.setProductTypeName(productInfo.getProductTypeName());
    }

    /**
     * 对象转换
     *
     * @param noticeOrder    调整通知单信息
     * @param needAdjustInfo 调整计划信息
     * @return
     */
    public static MonthPlanAdjustDetail buildAdjustDetail(MonthPlanNoticeOrder noticeOrder, MonthPlanNeedAdjustPlanVo needAdjustInfo) {
        MonthPlanAdjustDetail detail = new MonthPlanAdjustDetail();
        detail.setNoticeNo(noticeOrder.getNoticeNo());
        detail.setYear(noticeOrder.getYear());
        detail.setMonth(noticeOrder.getMonth());
        detail.setMonthPlanVersion(noticeOrder.getMonthPlanVersion());
        detail.setProductionVersion(noticeOrder.getProductionVersion());
        detail.setFactoryCode(noticeOrder.getFactoryCode());
        detail.setProductionNo(needAdjustInfo.getProductionNo());
        detail.setStartDate(needAdjustInfo.getStartAdjustDate());
        detail.setAdjustQty(needAdjustInfo.getNeedAdjustNumber());
        detail.setAdjustType(needAdjustInfo.getAdjustType().getAdjustType());
        detail.setProductCode(needAdjustInfo.getProductCode());
        detail.setProductDesc(needAdjustInfo.getProductDesc());
        detail.setIsImport(YesOrNoEnum.NO.getValue());
        return detail;
    }

    /**
     * 根据物料的月度可用及模具维修信息，得到其物料最大可用模具信息
     *
     * @param monthEnableList      物料模具关系月度可用模具
     * @param monthMaintenanceList 物料模具关系月度维修模具
     * @param productionStartDate  版本起始日
     * @return key 模具编码 value 模具关联信息
     */
    public static Map<String, MouldProductRelationDto> getMaxEnableMould(List<MouldProductRelationDto> monthEnableList, List<MouldProductRelationDto> monthMaintenanceList, Date productionStartDate) {
        Map<String, MouldProductRelationDto> maxEnableMouldMap = new HashMap<>();
        //月度可用
        if (!CollectionUtils.isEmpty(monthEnableList)) {
            monthEnableList.stream().forEach(monthEnable -> maxEnableMouldMap.put(monthEnable.getMouldCode(), monthEnable));
        }
        if (CollectionUtils.isEmpty(monthMaintenanceList)) {
            return maxEnableMouldMap;
        }
        //合并维修
        monthMaintenanceList.stream().forEach(maintenance -> {
            String mouldCode = maintenance.getMouldCode();
            MouldProductRelationDto exist = maxEnableMouldMap.get(mouldCode);
            if (null == exist) {
                maintenance.setNoProductionList(maintenance.getNoProductionDayByCycle(productionStartDate));
                maxEnableMouldMap.put(mouldCode, maintenance);
                return;
            }
            if (null == exist.getBeginDate()) {
                maintenance.setNoProductionList(maintenance.getNoProductionDayByCycle(productionStartDate));
                maxEnableMouldMap.put(mouldCode, maintenance);
                return;
            }
            maintenance.getNoProductionList().addAll(maintenance.getNoProductionDayByCycle(productionStartDate));
        });
        return maxEnableMouldMap;
    }

    /**
     * 构建增量计划信息辅助对象实例
     *
     * @param noticeOrderOperate 调增操作类
     * @param checkHelper        通知单及版本信息辅助类
     * @return
     */
    public static AddQtyAdjustPlanHelper buildAddQtyInfo(MonthPlanAdjustNoticeOrderOperateVo noticeOrderOperate, AdjustNoticeCheckHelper checkHelper) {
        AddQtyAdjustPlanHelper addQtyInfo = new AddQtyAdjustPlanHelper();
        addQtyInfo.setAddQty(noticeOrderOperate.getAdjustNumber());
        addQtyInfo.setMouldNo(noticeOrderOperate.getMouldNo());
        Date startDate = noticeOrderOperate.getStartDate();
        FactoryProductionVersion productionVersion = checkHelper.getProductionVersion();
        Date productionStartDate = productionVersion.getProductionStartDate();
        Date productionEndDate = productionVersion.getProductionEndDate();
        Long diff = Duration.between(startDate.toInstant(), productionStartDate.toInstant()).toDays();
        Integer startAdjustDay = Math.abs(diff.intValue()) + BigDecimal.ONE.intValue();
        addQtyInfo.setStartAdjustDay(startAdjustDay);
        MonthPlanNoticeOrder noticeOrder = checkHelper.getNoticeOrder();
        addQtyInfo.setProductCode(noticeOrder.getProductCode());
        addQtyInfo.setProductionVersion(productionVersion);
        Long maxDiff = Duration.between(productionStartDate.toInstant(), productionEndDate.toInstant()).toDays();
        Integer maxDays = Math.abs(maxDiff.intValue()) + BigDecimal.ONE.intValue();
        addQtyInfo.setMonthMaxDays(maxDays);
        return addQtyInfo;
    }

    /**
     * 按层级维度，构建排产顺序分组信息
     *
     * @return
     */
    private static Map<SortHierarchyEnum, List<PlanOrderSortConfiguration>> getGroupProductionConfiguration(List<PlanOrderSortConfiguration> sortConfigurationList) {
        Map<SortHierarchyEnum, List<PlanOrderSortConfiguration>> hierarchyMap = new HashMap<>();
        if (CollectionUtils.isEmpty(sortConfigurationList)) {
            return hierarchyMap;
        }
        sortConfigurationList.stream().forEach(sortConfiguration -> {
            Integer hierarchy = sortConfiguration.getHierarchy();
            SortHierarchyEnum sortHierarchy = SortHierarchyEnum.getInstance(hierarchy);
            if (null == sortHierarchy) {
                return;
            }
            List<PlanOrderSortConfiguration> hierarchyConfigurationList = hierarchyMap.get(sortHierarchy);
            if (null == hierarchyConfigurationList) {
                hierarchyConfigurationList = new ArrayList<>();
            }
            hierarchyConfigurationList.add(sortConfiguration);
            hierarchyMap.put(sortHierarchy, hierarchyConfigurationList);
        });
        return hierarchyMap;
    }

    /**
     * 获取前后计划量的差异信息
     *
     * @param startDays         起始天数
     * @param maxDays           最大天数
     * @param adjustConfirmPlan 确认调整后的计划
     * @param originPlan        确认调整前的计划
     * @return
     */
    private static MonthPlanNeedAdjustPlanVo getDifference(Integer startDays, Integer maxDays, FactoryMonthPlanProdFinal adjustConfirmPlan, FactoryMonthPlanProdFinal originPlan) {
        MonthPlanNeedAdjustPlanVo needAdjustPlan = new MonthPlanNeedAdjustPlanVo();
        needAdjustPlan.setProductionNo(originPlan.getProductionNo());
        needAdjustPlan.setProductCode(originPlan.getProductCode());
        needAdjustPlan.setProductDesc(originPlan.getProductDesc());
        Long needAdjustNumber = BigDecimal.ZERO.longValue();
        String fieldName;
        for (int day = startDays; day <= maxDays; day++) {
            fieldName = String.format("day%d", day);
            //原有计划日排产量
            Long originProductionQty = (Long) originPlan.getFieldValueByFieldName(fieldName);
            if (null == originProductionQty) {
                originProductionQty = BigDecimal.ZERO.longValue();
            }
            //新计划日排产量
            Long confirmAfterProductionQty = (Long) adjustConfirmPlan.getFieldValueByFieldName(fieldName);
            if (null == confirmAfterProductionQty) {
                confirmAfterProductionQty = BigDecimal.ZERO.longValue();
            }
            if (originProductionQty.equals(confirmAfterProductionQty)) {
                continue;
            }
            needAdjustNumber = needAdjustNumber + confirmAfterProductionQty - originProductionQty;
        }
        needAdjustPlan.setNeedAdjustNumber(needAdjustNumber);
        if (needAdjustNumber > BigDecimal.ZERO.longValue()) {
            needAdjustPlan.setAdjustType(MonthPlanAdjustTypeEnum.ADD);
        }
        if (needAdjustNumber < BigDecimal.ZERO.longValue()) {
            needAdjustPlan.setAdjustType(MonthPlanAdjustTypeEnum.SUBTRACT);
        }
        return needAdjustPlan;
    }

    /**
     * 校验是否匹配
     * 库位类别严格匹配
     * 渠道*表示全匹配，
     * 品牌*表示全匹配。
     *
     * @param productionPlan 销售提报订单
     * @param locationType   库位类型
     * @param channelCode    渠道编码
     * @param brandCode      品牌编码
     * @return
     */
    private static boolean check(FactoryMonthFinalPlanHelperVo productionPlan, String locationType, String channelCode, String brandCode) {
        if (!locationType.equals(productionPlan.getLocationType())) {
            return false;
        }
        if (StringConstant.ALL_MATCH.equals(channelCode) && StringConstant.ALL_MATCH.equals(brandCode)) {
            return true;
        }
        if (StringConstant.ALL_MATCH.equals(channelCode) && !StringConstant.ALL_MATCH.equals(brandCode)) {
            return brandCode.equals(productionPlan.getBrand());
        }
        if (!StringConstant.ALL_MATCH.equals(channelCode) && StringConstant.ALL_MATCH.equals(brandCode)) {
            return channelCode.equals(productionPlan.getChannel());
        }
        return channelCode.equals(productionPlan.getChannel()) && brandCode.equals(productionPlan.getBrand());
    }

    /**
     * 设置生胎代码
     *
     * @param specCodeInfo   硫化施工信息
     * @param productionPlan 排产计划
     * @param specCode       硫化规格代码
     */
    private static void setEmbryoCodeInfo(String specCodeInfo, FactoryMonthPlanProdFinal productionPlan, String specCode) {
        if (StringUtils.isBlank(specCodeInfo) || StringUtils.isBlank(specCode)) {
            return;
        }
        List<ProductSpecInfoVo> productSpecCodeInfoList = JSON.parseArray(specCodeInfo, ProductSpecInfoVo.class);
        if (CollectionUtils.isEmpty(productSpecCodeInfoList)) {
            return;
        }
        Map<String, ProductSpecInfoVo> productSpecInfoMap = productSpecCodeInfoList.stream().collect(Collectors.toMap(ProductSpecInfoVo::getSpecCode, Function.identity()));
        if (CollectionUtils.isEmpty(productSpecInfoMap)) {
            return;
        }
        ProductSpecInfoVo productSpecInfo = productSpecInfoMap.get(specCode);
        if (null == productSpecInfo) {
            return;
        }
        productionPlan.setMouldMethod(productSpecInfo.getMouldMethod());
        productionPlan.setEmbryoCode(productSpecInfo.getEmbryoCode());
    }

    private AdjustNoticeUtils() {

    }
}
