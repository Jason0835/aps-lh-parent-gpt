package com.zlt.aps.monthplan.factory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.factory.domain.Context;
import com.zlt.aps.factory.service.IMonthPlanProductionSchedulingService;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.factory.dto.FactoryProductionPlanVersionDto;
import com.zlt.aps.monthplan.factory.mapper.FactoryConsoleMapper;
import com.zlt.aps.monthplan.factory.mapper.MpFactoryProductionVersionMapper;
import com.zlt.aps.monthplan.factory.service.IFactoryConsoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
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
        //对第一条排产版本记录清除排产版本信息，其它排产版本记录删除
        String leaveProductionVersion = findProductionList.get(BigDecimal.ZERO.intValue()).getProductionVersion();
        factoryProductionParam.setProductionVersion(leaveProductionVersion);
        //更新排产版本记录及删除所有需求的排产信息
        factoryProductionVersionMapper.deletedProductionVersionAndUpdateLastFlag(factoryProductionParam);
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
        //正式需要排产版本号 queryWrapper.eq(StringUtils.isNotBlank(productionVersion), "PRODUCTION_VERSION", productionVersion);
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
    public FactoryMonthPlanTypeVo getProductionMonthType(FactoryMonthPlanProdFinal query) {
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
}
