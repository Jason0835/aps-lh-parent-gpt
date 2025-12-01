package com.zlt.aps.monthplan.factory.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.redissonLock.annotation.RedissonLockAnno;
import com.zlt.aps.monthplan.api.domain.dto.FactoryFinalVersionQueryDto;
import com.zlt.aps.monthplan.api.domain.entity.FactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.vo.*;
import com.zlt.aps.monthplan.factory.dto.FactoryProductionPlanVersionDto;
import com.zlt.aps.monthplan.factory.service.IFactoryConsoleService;
import com.zlt.aps.monthplan.factory.service.IFactoryProductionVersionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分厂月份计划控制台
 *
 * @author ZLT
 * @date 20250213
 */
@RestController
@RequestMapping("/factoryConsole")
@RequiredArgsConstructor
@Api(value = "月度计划控制台业务", tags = "月度计划控制台业务-->ZLT")
public class FactoryConsoleController extends BaseController {

    private final IFactoryConsoleService factoryConsoleService;

    private final IFactoryProductionVersionService factoryProductionVersionService;

    /**
     * 查询分厂的月份排产计划
     *
     * @param queryCondition 查询条件
     * @return 结果集合
     */
    @ApiOperation("查询分厂的月份排产计划----分厂同一计划可写入多个版本")
    @PostMapping("/productionVersionList")
    public TableDataInfo getProductionVersionList(@RequestBody FactoryProductionPlanVo queryCondition) {
        List<FactoryProductionPlanVersionDto> dataList = factoryConsoleService.getProductionVersionList(queryCondition);
        if (CollectionUtils.isEmpty(dataList)) {
            return getDataTable(Collections.emptyList());
        }
        Map<String, FactoryProductionPlanResultVo> saleDemandMap = new HashMap<>();
        dataList.stream().forEach(factoryProductionPlanVersion -> {
            if (StringUtils.isBlank(factoryProductionPlanVersion.getProductTypeCode())) {
                factoryProductionPlanVersion.setProductTypeCode(ProductTypeEnum.SEMI_STEEL.getValue());
            }
            String key = factoryProductionPlanVersion.getMonthPlanVersionKey();
            FactoryProductionPlanResultVo result = saleDemandMap.get(key);
            if (null == result) {
                result = new FactoryProductionPlanResultVo();
                BeanUtils.copyProperties(factoryProductionPlanVersion, result);
                result.setProductVersionList(new ArrayList<>());
            }
            String initVersion = factoryProductionPlanVersion.getInitVersion();
            String productionVersion = factoryProductionPlanVersion.getProductionVersion();
            if (StringUtils.isNotBlank(initVersion)) {
                FactoryProductionVersionVo factoryProductionVersion = new FactoryProductionVersionVo();
                factoryProductionVersion.setInitVersion(initVersion);
                factoryProductionVersion.setCreateTime(factoryProductionPlanVersion.getCreateTime());
                factoryProductionVersion.setProductionVersion(productionVersion);
                factoryProductionVersion.setIsFinal(factoryProductionPlanVersion.getIsFinal());
                if (YesOrNoEnum.NO.getValue().equals(factoryProductionPlanVersion.getIsNaturalMonth())) {
                    factoryProductionVersion.setProductionStartDate(factoryProductionPlanVersion.getProductionStartDate());
                }
                result.getProductVersionList().add(factoryProductionVersion);
            }
            saleDemandMap.put(key, result);
        });
        List<FactoryProductionPlanResultVo> convertList = new ArrayList<>(saleDemandMap.values());
        convertList.stream().forEach(factoryProductionPlan -> {
            List<FactoryProductionVersionVo> productVersionList = factoryProductionPlan.getProductVersionList();
            if (CollectionUtils.isEmpty(productVersionList)) {
                return;
            }
            factoryProductionPlan.setProductVersionList(productVersionList.stream().sorted(Comparator.comparing(FactoryProductionVersionVo::getCreateTime, Comparator.reverseOrder())).collect(Collectors.toList()));
        });

        List<FactoryProductionPlanResultVo> sortList = convertList.stream().sorted(Comparator.comparing(FactoryProductionPlanResultVo::getMonthPlanVersion, Comparator.reverseOrder())).collect(Collectors.toList());
        return getDataTable(sortList);
    }

    /**
     * 创建导入模板的版本信息，主要获取版本周期
     *
     * @param param 分厂编码、年份、月份
     * @return
     */
    @ApiOperation("根据分厂、年、月获取其周期信息")
    @PostMapping("/createImportVersion")
    public FactoryProductionVersion createImportVersion(@RequestBody FactoryProductionParamVo param) {
        if (null == param) {
            return null;
        }
        String factoryCode = param.getFactoryCode();
        Integer year = param.getYear();
        Integer month = param.getMonth();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return null;
        }
        FactoryProductionVersion version = new FactoryProductionVersion();
        version.setFactoryCode(factoryCode);
        version.setYear(year);
        version.setMonth(month);
        factoryProductionVersionService.setProductionVersionCycleDate(version);
        return version;
    }

    /**
     * 按分厂 + 年月的方式生成销售需求月度计划
     * 会进行库存对冲、备货计算
     *
     * @param createCondition
     * @return
     */
    @ApiOperation("按分厂 + 年月的方式生成销售需求月度计划")
    @RedissonLockAnno(uniqueMark = "redissonLock:factoryConsole:createSaleRequirePlan:",
            expressions = {"#createCondition.factoryCode", "#createCondition.year", "#createCondition.month"},
            msgKey = "ui.data.alert.saleRequirePlan.run",
            waitTime = 5,
            leaseTime = 300
    )
    @PostMapping("/createSaleRequirePlan")
    public AjaxResult createSaleRequirePlan(@RequestBody MonthPlanSaleRequirePlanVo createCondition) {
        if (null == createCondition) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.condition.noEmpty"));
        }
        if (StringUtils.isBlank(createCondition.getFactoryCode()) || null == createCondition.getYear() || null == createCondition.getMonth()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        return factoryConsoleService.createSaleRequirePlan(createCondition);
    }

    @ApiOperation("按分厂+ 日期获取分厂的定稿排产版本信息")
    @PostMapping("/getFinalVersionInfo")
    AjaxResult getFinalVersion(@RequestBody FactoryFinalVersionQueryDto queryCondition) {
        if (null == queryCondition) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.condition.noEmpty"));
        }
        String factoryCode = queryCondition.getFactoryCode();
        Date productionDate = queryCondition.getProductionDate();
        if (null == productionDate || StringUtils.isBlank(factoryCode)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.factoryMonthPlanProdFinal.factoryProductionDateNoEmpty"));
        }
        FactoryProductionVersion finalVersion = factoryProductionVersionService.getFinalVersion(queryCondition.getFactoryCode(), queryCondition.getProductionDate());
        if (null == finalVersion) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.factoryMonthPlanProdFinal.factoryNoFinal"));
        }
        return AjaxResult.success(finalVersion);
    }

    /**
     * 按分厂 + 年月+需求计划版本的方式初始化分厂排产信息
     *
     * @param factoryProductionParam
     * @return
     */
    @ApiOperation("按分厂 + 年月 + 需求计划版本 + 排产版本的方式初始化分厂排产")
    @PostMapping("/initFactoryProduction")
    public AjaxResult initFactoryProduction(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        if (null == factoryProductionParam) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.condition.noEmpty"));
        }
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        String productionVersion = factoryProductionParam.getProductionVersion();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion) || StringUtils.isBlank(productionVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.versionNoEmpty"));
        }
        return factoryConsoleService.reinitializeMouldingProduction(factoryProductionParam);
    }

    /**
     * 按分厂 + 年月+需求计划版本的方式初始化分厂排产信息
     *
     * @param factoryProductionParam
     * @return
     */
    @ApiOperation("按分厂 + 年月 + 排产版本的方式进行分厂模具产能排产")
    @PostMapping("/factoryMouldingProduction")
    public AjaxResult factoryMouldingProduction(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        if (null == factoryProductionParam) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.condition.noEmpty"));
        }
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        String productionVersion = factoryProductionParam.getProductionVersion();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion) || StringUtils.isBlank(productionVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.versionNoEmpty"));
        }
        return factoryConsoleService.reMouldingProduction(factoryProductionParam);
    }

    /**
     * 按分厂 + 年月 + 排产版本的方式进行分厂一键模具排产
     *
     * @param factoryProductionParam
     * @return
     */
    @ApiOperation("按分厂 + 年月 + 排产版本的方式进行分厂一键模具排产")
    @PostMapping("/factoryWholeCourseProduction")
    public AjaxResult factoryWholeCourseProduction(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        if (null == factoryProductionParam) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.condition.noEmpty"));
        }
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.requireVersionNoEmpty"));
        }
        return factoryConsoleService.factoryWholeCourseProduction(factoryProductionParam);
    }

    /**
     * 按分厂 + 年月 + 需求版本的方式删除需求计划版本及对应的排产版本
     *
     * @param factoryProductionParam
     * @return
     */
    @ResponseBody
    @ApiOperation("按分厂 + 年月 + 需求版本的方式删除需求计划版本及对应的排产版本")
    @PostMapping("/deleteMonthPlanRequire")
    public AjaxResult deleteMonthPlanRequire(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        if (null == factoryProductionParam) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.condition.noEmpty"));
        }
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.requireVersionNoEmpty"));
        }
        return factoryConsoleService.deleteMonthPlanRequire(factoryProductionParam);
    }

    /**
     * 按分厂 + 年月 + 排产版本的方式删除排产计划版本
     *
     * @param factoryProductionParam
     * @return
     */
    @ResponseBody
    @ApiOperation("按分厂 + 年月 + 排产版本的方式删除排产计划版本")
    @PostMapping("/deleteMonthPlanProductionVersion")
    public AjaxResult deleteMonthPlanProductionVersion(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        if (null == factoryProductionParam) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.condition.noEmpty"));
        }
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        String productionVersion = factoryProductionParam.getProductionVersion();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion) || StringUtils.isBlank(productionVersion)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.versionNoEmpty"));
        }
        return factoryConsoleService.deleteMonthPlanProductionVersion(factoryProductionParam);
    }
}
