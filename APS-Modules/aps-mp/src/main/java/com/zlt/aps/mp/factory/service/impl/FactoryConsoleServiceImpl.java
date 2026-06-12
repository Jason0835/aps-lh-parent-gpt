package com.zlt.aps.mp.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.google.common.collect.Lists;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.enums.ProductTypeEnum;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.enums.MonthPlanEnums;
import com.zlt.aps.maindata.mapper.ProductionModeParamMapper;
import com.zlt.aps.maindata.service.IFactoryParamService;
import com.zlt.aps.maindata.utils.FactoryParamUtils;
import com.zlt.aps.mp.api.domain.entity.FactoryParam;
import com.zlt.aps.mp.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.mp.api.domain.entity.ProductionModeParam;
import com.zlt.aps.mp.api.domain.vo.*;
import com.zlt.aps.mp.api.enums.ProductionModeEnum;
import com.zlt.aps.mp.engine.check.service.IMpCheckItemService;
import com.zlt.aps.mp.engine.domain.Context;
import com.zlt.aps.mp.engine.service.IMonthPlanProductionSchedulingService;
import com.zlt.aps.mp.factory.dto.FactoryProductionPlanVersionDto;
import com.zlt.aps.mp.factory.mapper.FactoryConsoleMapper;
import com.zlt.aps.mp.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.mp.factory.service.IFactoryConsoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 工厂控制台业务实现
 *
 * @author ZLT
 * @date 20251205
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryConsoleServiceImpl implements IFactoryConsoleService {

    private final MpFactoryProductionVersionMapper factoryProductionVersionMapper;

    private final FactoryConsoleMapper factoryConsoleMapper;

    private final IMonthPlanProductionSchedulingService monthPlanProductionSchedulingService;

    private final IMpCheckItemService mpCheckItemService;

    private final IFactoryParamService factoryParamService;

    private final ProductionModeParamMapper productionModeParamMapper;

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
    public MpProductionModeInfoVo getCurrentProductionMode(FactoryProductionPlanVo queryCondition) {
        if (null == queryCondition) {
            return null;
        }
        String factoryCode = queryCondition.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            return null;
        }
        String productionTypeCode = queryCondition.getProductTypeCode();
        if (StringUtils.isBlank(productionTypeCode)) {
            productionTypeCode = ProductTypeEnum.WHOLE_STEEL.getValue();
        }
        FactoryParam currentMode = getCurrentProductionMode(factoryCode, productionTypeCode);
        if (null == currentMode) {
            return null;
        }
        Integer productionMode = (Integer) FactoryParamUtils.getParamValue(currentMode);
        ProductionModeEnum modeEnum = ProductionModeEnum.getInstance(productionMode);
        return MpProductionModeInfoVo.build(modeEnum, factoryCode, productionTypeCode);
    }

    @Override
    public List<MpProductionModeInfoVo> getAllProductionModeInfo(FactoryProductionPlanVo queryCondition) {
        if (null == queryCondition) {
            return Collections.emptyList();
        }
        String factoryCode = queryCondition.getFactoryCode();
        if (StringUtils.isBlank(factoryCode)) {
            return Collections.emptyList();
        }
        String productionTypeCode = queryCondition.getProductTypeCode();
        if (StringUtils.isBlank(productionTypeCode)) {
            productionTypeCode = ProductTypeEnum.WHOLE_STEEL.getValue();
        }
        String realProductionType = productionTypeCode;
        List<MpProductionModeInfoVo> allData = Lists.newArrayList();
        Arrays.stream(ProductionModeEnum.values()).forEach(single -> {
            MpProductionModeInfoVo modeInfo = MpProductionModeInfoVo.build(single, factoryCode, realProductionType);
            if (null == modeInfo) {
                return;
            }
            allData.add(modeInfo);
        });
        return allData;
    }

    @Override
    public List<ProductionModeParam> getProductionModeList(MpProductionModeInfoVo productionModeInfo) {
        if (null == productionModeInfo || null == productionModeInfo.getProductionMode()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<ProductionModeParam> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProductionModeParam::getProductionMode, String.valueOf(productionModeInfo.getProductionMode()));
        queryWrapper.eq(ProductionModeParam::getFactoryCode, productionModeInfo.getFactoryCode());
        queryWrapper.eq(ProductionModeParam::getProductTypeCode, productionModeInfo.getProductTypeCode());
        queryWrapper.eq(ProductionModeParam::getIsDelete, YesOrNoEnum.NO.getValue());
        return productionModeParamMapper.selectList(queryWrapper);
    }

    @Override
    public boolean updateProductionModeInfo(ProductionModeParam saveParam) {
        return SqlHelper.retBool(productionModeParamMapper.updateById(saveParam));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult applyProductionModeConfiguration(MpProductionModeInfoVo param) {
        //排产模式参数配置项
        param.setProductionModeParamCode(MonthPlanEnums.PRODUCTION_MODE.getCode());
        ProductionModeEnum productionMode = ProductionModeEnum.getInstance(param.getProductionMode());
        if (null == productionMode) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.noHasProductionMode"));
        }
        if (StringUtils.isBlank(param.getProductTypeCode())) {
            param.setProductTypeCode(ProductTypeEnum.SEMI_STEEL.getValue());
        }
        Integer isCheckChange = param.getIsCheckChange();
        if (null != isCheckChange && YesOrNoEnum.YES.getValue().equals(isCheckChange)) {
            String productionModeDesc = I18nUtil.getMessage(productionMode.getI18nKey());
            //校验是否变换
            FactoryParam currentProductionMode = getCurrentProductionMode(param.getFactoryCode(), param.getProductTypeCode());
            String warnMessageFormat = I18nUtil.getMessage("ui.data.query.param.isChangeProductionMode");
            AjaxResult warnResult = new AjaxResult(AjaxResult.Type.WARN, String.format(warnMessageFormat, productionModeDesc));
            if (null == currentProductionMode) {
                return warnResult;
            }
            Integer modeValue = (Integer) FactoryParamUtils.getParamValue(currentProductionMode);
            if (null == modeValue) {
                return warnResult;
            }
            if (!productionMode.getMode().equals(modeValue)) {
                return warnResult;
            }
        }
        //更新参数配置项
        productionModeParamMapper.changeProductionMode(param);
        productionModeParamMapper.updateFactoryParamByProductionMode(param);
        return AjaxResult.success();
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
        // 定稿不允许再次生成月计划的限制先取消，用于前期比对 20260504 hak
//        queryWrapper.eq("FACTORY_CODE", factoryCode);
//        queryWrapper.eq("YEAR", year);
//        queryWrapper.eq("MONTH", month);
//        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
//        queryWrapper.eq("IS_FINAL", YesOrNoEnum.YES.getCode());
//        MpFactoryProductionVersion version = factoryProductionVersionMapper.selectOne(queryWrapper);
//        if (null != version) {
//            //分厂在%s-%s年月已定稿，不可重新排产
//            String factoryIsFinalVersion = I18nUtil.getMessage("ui.data.query.param.factoryIsFinalVersion");
//            return AjaxResult.error(String.format(factoryIsFinalVersion, year, month));
//        }
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
    public AjaxResult groupPlanCapacityResetAllocationProduction(FactoryProductionParamVo factoryProductionParam) {
        AjaxResult checkResult = checkParam(factoryProductionParam);
        //校验没通过
        if (AjaxResult.Type.ERROR.value() == (Integer) checkResult.get(AjaxResult.CODE_TAG)) {
            return checkResult;
        }
        Context context = buildContext(factoryProductionParam);
        monthPlanProductionSchedulingService.groupCapacityScheduling(context);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult rescheduleMouldingProduction(FactoryProductionParamVo factoryProductionParam) {
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
        if (null == factoryProductionParam || StringUtils.isBlank(factoryProductionParam.getMonthPlanVersion())) {
            return AjaxResult.success();
        }
        List<MpFactoryProductionVersion> findProductionList = getProVersionList(factoryProductionParam);
        if (CollectionUtils.isEmpty(findProductionList)) {
            return AjaxResult.success();
        }
        List<MpFactoryProductionVersion> hasFinalVersion = findProductionList.stream().filter(singleVersion -> YesOrNoEnum.YES.getCode().equals(singleVersion.getIsFinal())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(hasFinalVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.check.monthPlan.finalVersion"));
        }
        MpFactoryProductionVersion productionVersion = findProductionList.stream().filter(item -> StringUtils.isNotBlank(item.getProductionVersion())).findFirst().orElse(null);
        if (null != productionVersion) {
            productionVersion.setProductionInitVersion(StringUtils.EMPTY);
            productionVersion.setProductionVersion(StringUtils.EMPTY);
            productionVersion.setProductionStVersion(StringUtils.EMPTY);
            productionVersion.setIsSelectedDemand(YesOrNoEnum.NO.getCode());
            this.factoryProductionVersionMapper.updateById(productionVersion);
            Long id = productionVersion.getId();
            List<Long> ids = findProductionList.stream().filter(item -> !Objects.equals(id, item.getId())).map(MpFactoryProductionVersion::getId).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(ids)) {
                this.factoryProductionVersionMapper.deleteBatchIds(ids);
            }
            return AjaxResult.success();
        }
        productionVersion = findProductionList.get(0);
        productionVersion.setProductionInitVersion(StringUtils.EMPTY);
        productionVersion.setProductionVersion(StringUtils.EMPTY);
        productionVersion.setProductionStVersion(StringUtils.EMPTY);
        productionVersion.setIsSelectedDemand(YesOrNoEnum.NO.getCode());
        this.factoryProductionVersionMapper.updateById(productionVersion);
        Long id = productionVersion.getId();
        List<Long> ids = findProductionList.stream().filter(item -> !Objects.equals(id, item.getId())).map(MpFactoryProductionVersion::getId).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(ids)) {
            this.factoryProductionVersionMapper.deleteBatchIds(ids);
        }
        return AjaxResult.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult deleteMonthPlanProductionVersion(FactoryProductionParamVo factoryProductionParam) {
        boolean isFinalVersion = isHasFinalVersion(factoryProductionParam);
        if (isFinalVersion) {
            return AjaxResult.error(I18nUtil.getMessage("ui.check.monthPlan.isFinalVersion"));
        }
        int count = factoryProductionVersionMapper.selectCountByProductionVersion(factoryProductionParam);
        if (count <= 1) {
            factoryProductionVersionMapper.deletedLastVersionByProductionVersion(factoryProductionParam);
        } else {
            factoryProductionVersionMapper.deletedByProductionVersion(factoryProductionParam);
        }
        return AjaxResult.success();
    }

    /**
     * 查询对应年月+分厂的需求计划版本
     *
     * @param query
     */
    @Override
    public List<String> versionList(MpFactoryProductionVersion query) {
        if (query.getYear() == null || query.getMonth() == null || StringUtils.isBlank(query.getFactoryCode())) {
            return Collections.emptyList();
        }
        return factoryProductionVersionMapper.versionList(query);
    }

    /**
     * 查询对应年月+分厂+需求计划版本的分厂月计划版本
     *
     * @param query 查询条件
     * @return
     */
    @Override
    public List<String> productionVersionList(MpFactoryProductionVersion query) {
        if (query.getYear() == null || query.getMonth() == null || StringUtils.isBlank(query.getFactoryCode()) || StringUtils.isBlank(query.getMonthPlanVersion())) {
            return Collections.emptyList();
        }
        return factoryProductionVersionMapper.productionVersionList(query);
    }

    /**
     * 获取月份排产模式--Date 不为空则表示非自然月排产，Date为空表示自然月排产
     *
     * @param query 参数
     * @return 结果
     */
    @Override
    public FactoryMonthPlanTypeVo getProductionMonthType(FactoryMonthPlanTypeVo query) {
        FactoryMonthPlanTypeVo type = new FactoryMonthPlanTypeVo();
        FactoryMonthPlanFinalVersionInfoVo finalVersion = getFinalVersionInfo(query.getFactoryCode(), query.getYear(), query.getMonth());
        if (null == finalVersion) {
            return type;
        }
        String productionVersion = finalVersion.getProductionVersion();
        if (StringUtils.isBlank(productionVersion)) {
            return type;
        }
        if (YesOrNoEnum.YES.getValue().equals(finalVersion.getIsNaturalMonth())) {
            return type;
        }
        type.setYear(finalVersion.getYear());
        type.setMonth(finalVersion.getMonth());
        type.setFactoryCode(finalVersion.getFactoryCode());
        type.setMonthPlanVersion(finalVersion.getMonthPlanVersion());
        type.setProductionVersion(finalVersion.getProductionVersion());
        type.setProductionStartDate(finalVersion.getProductionStartDate());
        return type;
    }

    @Override
    public FactoryMonthPlanFinalVersionInfoVo getFinalVersionInfo(String factoryCode, Integer year, Integer month) {
        if (com.ruoyi.common.utils.StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return null;
        }
        QueryWrapper<MpFactoryProductionVersion> queryVersion = new QueryWrapper<>();
        queryVersion.eq("FACTORY_CODE", factoryCode);
        queryVersion.eq("YEAR", year);
        queryVersion.eq("MONTH", month);
        queryVersion.eq("IS_FINAL", YesOrNoEnum.YES.getValue());
        queryVersion.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        MpFactoryProductionVersion result = factoryProductionVersionMapper.selectOne(queryVersion);
        if (null == result) {
            return null;
        }
        FactoryMonthPlanFinalVersionInfoVo info = new FactoryMonthPlanFinalVersionInfoVo();
        BeanUtils.copyProperties(result, info);
        return info;
    }

    @Override
    public AjaxResult checkProductionDemandPlan(FactoryProductionParamVo factoryProductionParam) {
        Context context = buildContext(factoryProductionParam);
        List<MpCheckItemVo> mpCheckItemVos = mpCheckItemService.check(context);
        if (CollectionUtils.isEmpty(mpCheckItemVos)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.check.result.empty"));
        }
        return AjaxResult.success(mpCheckItemVos);
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
     * 根据需求计划版本，获取对应需求计划的所有排产版本信息
     *
     * @param factoryProductionParam 需求查询条件
     * @return
     */
    private List<MpFactoryProductionVersion> getProVersionList(FactoryProductionParamVo factoryProductionParam) {
        QueryWrapper<MpFactoryProductionVersion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("FACTORY_CODE", factoryProductionParam.getFactoryCode());
        queryWrapper.eq("YEAR", factoryProductionParam.getYear());
        queryWrapper.eq("MONTH", factoryProductionParam.getMonth());
        queryWrapper.eq("IS_DELETE", YesOrNoEnum.NO.getValue());
        queryWrapper.eq("MONTH_PLAN_VERSION", factoryProductionParam.getMonthPlanVersion());
        return factoryProductionVersionMapper.selectList(queryWrapper);
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
        //正式需要排产版本号
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


    /**
     * 获取工厂+产品品类 当前排产模式
     *
     * @param factoryCode        工厂编码
     * @param productionTypeCode 产品品类
     * @return
     */
    private FactoryParam getCurrentProductionMode(String factoryCode, String productionTypeCode) {
        FactoryParam query = new FactoryParam();
        query.setFactoryCode(factoryCode);
        query.setProductTypeCode(productionTypeCode);
        query.setParamCode(MonthPlanEnums.PRODUCTION_MODE.getCode());
        return factoryParamService.getFacParamSingle(query);
    }

}
