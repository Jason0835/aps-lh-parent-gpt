package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.constant.FactoryConstant;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.StockHedgingOptionsEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.utils.GenerageMapKeyUtils;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.service.IMonthPlanProductionSchedulingService;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.service.IMdmMaterialInfoService;
import com.zlt.aps.maindata.service.IProductMinConfigurationService;
import com.zlt.aps.monthplan.api.domain.entity.*;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanVersionVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionParamVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionPlanVo;
import com.zlt.aps.monthplan.demand.mapper.MonthPlanSaleOrderMapper;
import com.zlt.aps.monthplan.enums.StockHedgingComparatorEnum;
import com.zlt.aps.monthplan.factory.dto.FactoryProductionPlanVersionDto;
import com.zlt.aps.monthplan.factory.mapper.FactoryConsoleMapper;
import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryConsoleService;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分厂控制台业务实现
 *
 * @author ZLT
 * @date 20250213
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryConsoleServiceImpl implements IFactoryConsoleService {

    private final MonthPlanSaleOrderMapper monthPlanSaleOrderMapper;

    private final MpFactoryProductionVersionMapper factoryProductionVersionMapper;

    private final FactoryConsoleMapper factoryConsoleMapper;

    private final BaseDao baseDao;

    private final IProductMinConfigurationService productMinConfigurationService;

    private final IFactoryParamService iFactoryParamService;

    private final IMonthPlanProductionSchedulingService monthPlanProductionSchedulingService;

    private final IMdmMaterialInfoService iMdmMaterialInfoService;


    /**
     * 不加超欠产量
     */
    private final static String NO_ADD_SHORT = "N";
    /**
     * 是否自动生成备货计划
     */
    private final static String IS_AUTO_CREATE_STOCK_UP_PLAN = "Y";

    @Override
    public List<FactoryProductionPlanVersionDto> getProductionVersionList(FactoryProductionPlanVo queryCondition) {
        if (null == queryCondition) {
            return Collections.emptyList();
        }
        if (null == queryCondition.getYear() || null == queryCondition.getMonth() || StringUtils.isBlank(queryCondition.getFactoryCode())) {
            return Collections.emptyList();
        }
        return factoryConsoleMapper.getProductionVersionList(queryCondition);
    }

    @Override
    public List<FactoryMonthPlanVersionVo> getNoSelectedVersionList(FactoryProductionPlanVo queryCondition) {
        if (null == queryCondition) {
            return Collections.emptyList();
        }
        if (null == queryCondition.getYear() || null == queryCondition.getMonth() || StringUtils.isBlank(queryCondition.getFactoryCode())) {
            return Collections.emptyList();
        }
        return factoryConsoleMapper.getNoSelectedVersionList(queryCondition);
    }

    @Override
    public AjaxResult oneClickProductionProcess(FactoryProductionParamVo factoryProductionParam) {
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.requireVersionNoEmpty"));
        }
        QueryWrapper<MpFactoryProductionVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("YEAR", year);
        queryWrapper.eq("MONTH", month);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.eq("IS_FINAL", YesOrNoEnum.YES.getCode());
        MpFactoryProductionVersion version = factoryProductionVersionMapper.selectOne(queryWrapper);
        if (null != version) {
            //分厂在%s-%s年月已定稿，不可重新排产
            String factoryIsFinalVersion = I18nUtil.getMessage("ui.data.query.param.factoryIsFinalVersion");
            return AjaxResult.error(String.format(factoryIsFinalVersion, year, month));
        }
        queryWrapper.clear();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("YEAR", year);
        queryWrapper.eq("MONTH", month);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.eq("MONTH_PLAN_VERSION", monthPlanVersion);
        List<MpFactoryProductionVersion> requireVersionList = factoryProductionVersionMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(requireVersionList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.noExistVersion"));
        }
        factoryProductionParam.setProductTypeCode(requireVersionList.get(BigDecimal.ZERO.intValue()).getProductTypeCode());
        Context context = buildContext(factoryProductionParam);
        monthPlanProductionSchedulingService.general(context);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult reinitializeMouldingProduction(FactoryProductionParamVo factoryProductionParam) {
        AjaxResult checkResult = checkParam(factoryProductionParam);
        //校验没通过
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        Context context = buildContext(factoryProductionParam);
        monthPlanProductionSchedulingService.init(context);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult reMouldingProduction(FactoryProductionParamVo factoryProductionParam) {
        AjaxResult checkResult = checkParam(factoryProductionParam);
        //校验没通过
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        Context context = buildContext(factoryProductionParam);
        monthPlanProductionSchedulingService.mouldingScheduling(context);
        return AjaxResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult deleteMonthPlanRequire(FactoryProductionParamVo factoryProductionParam) {
        factoryProductionParam.setProductionVersion(null);
        boolean hasFinalVersion = isHasFinalVersion(factoryProductionParam);
        if (hasFinalVersion) {
            return AjaxResult.error(I18nUtil.getMessage("ui.check.monthPlan.finalVersion"));
        }
        factoryProductionVersionMapper.deletedMonthPlanRequireVersion(factoryProductionParam);
        factoryProductionVersionMapper.deletedProductionVersion(factoryProductionParam);
        return AjaxResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult deleteMonthPlanProductionVersion(FactoryProductionParamVo factoryProductionParam) {
        boolean isFinalVersion = isHasFinalVersion(factoryProductionParam);
        if (isFinalVersion) {
            return AjaxResult.error(I18nUtil.getMessage("ui.check.monthPlan.isFinalVersion"));
        }
        QueryWrapper<MpFactoryProductionVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryProductionParam.getFactoryCode());
        queryWrapper.eq("YEAR", factoryProductionParam.getYear());
        queryWrapper.eq("MONTH", factoryProductionParam.getMonth());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        queryWrapper.eq("MONTH_PLAN_VERSION", monthPlanVersion);
        Long count = factoryProductionVersionMapper.selectCount(queryWrapper);
        if (count > 1) {
            factoryProductionVersionMapper.deletedProductionVersion(factoryProductionParam);
        } else {
            factoryProductionVersionMapper.deletedProductionVersionByLast(factoryProductionParam);
        }
        return AjaxResult.success();
    }

    /**
     * 判断是否存在定稿版本，存在则返回false
     *
     * @param factoryProductionParam
     * @return
     */
    private boolean isHasFinalVersion(FactoryProductionParamVo factoryProductionParam) {
        QueryWrapper<MpFactoryProductionVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryProductionParam.getFactoryCode());
        queryWrapper.eq("YEAR", factoryProductionParam.getYear());
        queryWrapper.eq("MONTH", factoryProductionParam.getMonth());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.eq("IS_FINAL", YesOrNoEnum.YES.getValue());
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        String productionVersion = factoryProductionParam.getProductionVersion();
        if (!StringUtils.isBlank(monthPlanVersion)) {
            queryWrapper.eq("MONTH_PLAN_VERSION", monthPlanVersion);
        }
        if (!StringUtils.isBlank(productionVersion)) {
            queryWrapper.eq("PRODUCTION_VERSION", productionVersion);
        }
        Long count = factoryProductionVersionMapper.selectCount(queryWrapper);
        return count > 0;
    }

    /**
     * 获取总的库存冲销值
     *
     * @param factoryCode    分厂编码
     * @param allocationList 库存冲销明细
     * @return
     */
    private Long getTotalAllocationQty(String factoryCode, List<OrderPlanAllocation> allocationList) {
        if (CollectionUtils.isEmpty(allocationList) || StringUtils.isBlank(factoryCode)) {
            return BigDecimal.ZERO.longValue();
        }
        Long totalQty = BigDecimal.ZERO.longValue();
        for (OrderPlanAllocation orderPlanAllocation : allocationList) {
            if (!factoryCode.equals(orderPlanAllocation.getFactoryCode())) {
                continue;
            }
            Long allocationQty = orderPlanAllocation.getAllocationQty();
            if (null == allocationQty) {
                allocationQty = BigDecimal.ZERO.longValue();
            }
            if (allocationQty > BigDecimal.ZERO.longValue()) {
                totalQty = totalQty + allocationQty;
            }
        }
        return totalQty;
    }

    /**
     * 获取月结库存：库存>0的值
     *
     * @param factoryCode    分厂编码
     * @param monthStockList 月结库存信息
     * @return
     */
    private Long getTotalMonthStockQty(String factoryCode, List<ProductStockMonth> monthStockList) {
        if (CollectionUtils.isEmpty(monthStockList) || StringUtils.isBlank(factoryCode)) {
            return BigDecimal.ZERO.longValue();
        }
        Long totalStockQty = BigDecimal.ZERO.longValue();
        for (ProductStockMonth monthStock : monthStockList) {
            if (!factoryCode.equals(monthStock.getFactoryCode())) {
                continue;
            }
            Integer stockQty = monthStock.getStockQty();
            if (null == stockQty) {
                stockQty = BigDecimal.ZERO.intValue();
            }
            if (stockQty > BigDecimal.ZERO.intValue()) {
                totalStockQty = totalStockQty + stockQty;
            }
        }
        return totalStockQty;
    }

    /**
     * 是否加入超欠产，根据控制参数SYS018
     * N表示不加，其它都是加入
     *
     * @param factoryCode 分厂编码
     * @return
     */
    private boolean isAddShort(String factoryCode) {
        FactoryParam factoryParam = new FactoryParam();
        factoryParam.setFactoryCode(factoryCode);
        factoryParam.setProductTypeCode(ProductTypeEnum.SEMI_STEEL.getValue());
        factoryParam.setParamCode(FactoryConstant.SYS_PARAM_IS_ADD_SHORT);
        FactoryParam addShortParam = iFactoryParamService.getFacParamSingle(factoryParam);
        if (null == addShortParam) {
            return true;
        }
        String addShortParamValue = addShortParam.getParamValue();
        if (StringUtils.isBlank(addShortParamValue)) {
            return true;
        }
        if (NO_ADD_SHORT.equalsIgnoreCase(addShortParamValue)) {
            return false;
        }
        return true;
    }

    /**
     * 根据备货查询对应物料信息Map(分厂+SAP代码)
     */
    private Map<String, MdmMaterialInfo> getProductInfoMapByStockUp(List<MdmStockUpPlan> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyMap();
        }
        List<String> factoryCodeList = list.stream().map(MdmStockUpPlan::getFactoryCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<String> productCodeList = list.stream().map(MdmStockUpPlan::getProductCode).filter(StringUtils::isNotBlank).distinct().collect(Collectors.toList());
        List<MdmMaterialInfo> productInfoList = iMdmMaterialInfoService.selectListByFactoryProductCode(factoryCodeList, productCodeList);
        return productInfoList.stream().collect(Collectors.toMap(v -> GenerageMapKeyUtils.createMapKey(v.getFactoryCode(), v.getMaterialCode())
                , Function.identity(), (v1, v2) -> v1));
    }


    /**
     * 根据库存对冲顺序配置，构建排序比较器对象
     * 优先值越小则排序优先级越高，则表示越优先进行对冲
     *
     * @param sortConfigurations
     * @return
     */
    private Comparator getComparator(List<PlanOrderSortConfiguration> sortConfigurations) {
        //对配置按优先值升序排序，优先值越小的配置项排序优先级越高
        sortConfigurations.sort(Comparator.comparing(PlanOrderSortConfiguration::getPriority));
        PlanOrderSortConfiguration firstSort = sortConfigurations.get(0);
        StockHedgingComparatorEnum firstOptionComparator = StockHedgingComparatorEnum.getInstance(StockHedgingOptionsEnum.getInstance(firstSort.getOptionCode()));
        Comparator first = firstOptionComparator.getComparator();
        for (int index = 1; index < sortConfigurations.size(); index++) {
            PlanOrderSortConfiguration sortConfiguration = sortConfigurations.get(index);
            StockHedgingOptionsEnum optionEnum = StockHedgingOptionsEnum.getInstance(sortConfiguration.getOptionCode());
            Comparator optionComparator = StockHedgingComparatorEnum.getInstance(optionEnum).getComparator();
            first = first.thenComparing(optionComparator);
        }
        return first;
    }


    /**
     * 操作前的校验
     *
     * @param factoryProductionParam
     * @return
     */
    private AjaxResult checkParam(FactoryProductionParamVo factoryProductionParam) {
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        String productionVersion = factoryProductionParam.getProductionVersion();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion) || StringUtils.isBlank(productionVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.versionNoEmpty"));
        }
        QueryWrapper<MpFactoryProductionVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryCode);
        queryWrapper.eq("YEAR", year);
        queryWrapper.eq("MONTH", month);
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.eq("MONTH_PLAN_VERSION", monthPlanVersion);
        queryWrapper.eq("PRODUCTION_VERSION", productionVersion);
        MpFactoryProductionVersion version = factoryProductionVersionMapper.selectOne(queryWrapper);
        if (null == version) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.noExistVersion"));
        }
        if (YesOrNoEnum.YES.getCode().equals(version.getIsFinal())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.finalVersionNoOperate"));
        }
        return AjaxResult.success();
    }

    /**
     * 构建半钢排产上下文对象
     *
     * @param factoryProductionParam
     * @return
     */
    private Context buildContext(FactoryProductionParamVo factoryProductionParam) {
        Context context = new Context();
        context.setFactoryCode(factoryProductionParam.getFactoryCode());
        context.setYear(factoryProductionParam.getYear());
        context.setMonth(factoryProductionParam.getMonth());
        context.setMonthPlanVersion(factoryProductionParam.getMonthPlanVersion());
        context.setProductionVersion(factoryProductionParam.getProductionVersion());
        context.setPrefixVersion(factoryProductionParam.getPrefixVersion());
        context.setProductType(ProductTypeEnum.getEnumByValue(factoryProductionParam.getProductTypeCode()));
        return context;
    }

}
