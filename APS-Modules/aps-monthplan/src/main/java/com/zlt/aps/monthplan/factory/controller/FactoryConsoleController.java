package com.zlt.aps.monthplan.factory.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.tlt.aps.enums.ProductTypeEnum;
import com.tlt.aps.enums.YesOrNoEnum;
import com.tlt.aps.redissonLock.annotation.DistributedLock;
import com.zlt.aps.monthplan.api.domain.dto.FactoryFinalVersionQueryDto;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
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
 * 工厂月份计划排产控制台
 * 后台业务服务入口
 *
 * @author ZLT
 * @date 20251201
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
        //处理排结构、排模具版本信息，按年+月份+分厂+需求计划版本+产品品类分组
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
                factoryProductionVersion.setProductionStVersion(factoryProductionPlanVersion.getProductionStVersion());
                factoryProductionVersion.setProductionVersion(productionVersion);
                factoryProductionVersion.setCreateTime(factoryProductionPlanVersion.getCreateTime());
                factoryProductionVersion.setIsFinal(factoryProductionPlanVersion.getIsFinal());
                factoryProductionVersion.setIsNaturalMonth(factoryProductionPlanVersion.getIsNaturalMonth());
                if (YesOrNoEnum.NO.getCode().equals(factoryProductionPlanVersion.getIsNaturalMonth())) {
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
        //排序，按需求版本排序
        List<FactoryProductionPlanResultVo> sortList = convertList.stream().sorted(Comparator.comparing(FactoryProductionPlanResultVo::getMonthPlanVersion, Comparator.reverseOrder())).collect(Collectors.toList());
        return getDataTable(sortList);
    }

    /**
     * 查询工厂可选择的月份需求计划
     *
     * @param queryCondition 查询条件
     * @return 结果集合
     */
    @ApiOperation("查询工厂月份对应还没选择的需求计划版本列表")
    @PostMapping("/getNoSelectedVersionList")
    public TableDataInfo getNoSelectedVersionList(@RequestBody FactoryProductionPlanVo queryCondition) {
        List<FactoryMonthPlanVersionVo> dataList = factoryConsoleService.getNoSelectedVersionList(queryCondition);
        if (CollectionUtils.isEmpty(dataList)) {
            return getDataTable(Collections.emptyList());
        }
        return getDataTable(dataList);
    }

    /**
     * 确认对工厂 + 年月 + 需求计划版本进行工厂排产
     *
     * @param confirmParam 查询条件
     * @return 结果信息
     */
    @ApiOperation("确认对工厂 + 年月 + 需求计划版本进行工厂排产")
    @PostMapping("/confirmProductionRequireVersion")
    public AjaxResult confirmProductionRequireVersion(@RequestBody FactoryProductionPlanVo confirmParam) {
        AjaxResult checkParamResult = checkEmptyMonthPlanVersion(confirmParam);
        //校验没通过
        if (AjaxResult.Type.ERROR.value() == (Integer) checkParamResult.get(AjaxResult.CODE_TAG)) {
            return checkParamResult;
        }
        return factoryProductionVersionService.flagProductionRequireVersion(confirmParam);
    }

    /**
     * 按工厂 + 年月 + 需求版本的方式进行工厂一键排产
     * 初始化->排结构->排模具
     *
     * @param factoryProductionParam
     * @return
     */
    @PostMapping("/oneClickProductionProcess")
    @ApiOperation("按工厂 + 年月 + 需求版本的方式进行工厂一键排产 初始化->排结构->排模具")
    @DistributedLock(key = "'redissonLock:factoryConsole:oneClickProductionProcess:'#factoryProductionParam.factoryCode" + "#factoryProductionParam.year" + "#factoryProductionParam.month" + "#factoryProductionParam.monthPlanVersion",
            failMsg = "ui.data.alert.factoryConsole.oneClickProductionProcess.run",
            args = {"#factoryProductionParam.monthPlanVersion", "#factoryProductionParam.factoryCode"},
            waitTime = 5,
            leaseTime = 300
    )
    public AjaxResult oneClickProductionProcess(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        AjaxResult checkParamResult = checkEmptyMonthPlanVersion(factoryProductionParam);
        //校验没通过
        if (AjaxResult.Type.ERROR.value() == (Integer) checkParamResult.get(AjaxResult.CODE_TAG)) {
            return checkParamResult;
        }
        return factoryConsoleService.oneClickProductionProcess(factoryProductionParam);
    }

    /**
     * 按工厂 + 年月 + 需求版本 + 排产版本的方式进行排产数据的重新初始化
     *
     * @param factoryProductionParam
     * @return
     */
    @PostMapping("/resetConfigurationInitProduction")
    @ApiOperation("按工厂 + 年月 + 需求版本 + 排产版本的方式进行排产数据的重新初始化")
    @DistributedLock(key = "'redissonLock:factoryConsole:resetConfigurationInitProduction:'#factoryProductionParam.factoryCode" + "#factoryProductionParam.year" + "#factoryProductionParam.month" + "#factoryProductionParam.monthPlanVersion" + "#factoryProductionParam.productionVersion",
            failMsg = "ui.data.alert.factoryConsole.resetConfigurationInitProduction.run",
            args = {"#factoryProductionParam.productionVersion", "#factoryProductionParam.factoryCode"},
            waitTime = 5,
            leaseTime = 300
    )
    public AjaxResult resetConfigurationInitProduction(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        AjaxResult checkParamResult = checkEmptyProductionVersion(factoryProductionParam);
        //校验没通过
        if (AjaxResult.Type.ERROR.value() == (Integer) checkParamResult.get(AjaxResult.CODE_TAG)) {
            return checkParamResult;
        }
        return factoryConsoleService.reinitializeMouldingProduction(factoryProductionParam);
    }

    /**
     * 创建导入模板的版本信息，主要获取版本周期
     *
     * @param param 分厂编码、年份、月份
     * @return
     */
    @ApiOperation("根据分厂、年、月获取其周期信息")
    @PostMapping("/createImportVersion")
    public MpFactoryProductionVersion createImportVersion(@RequestBody FactoryProductionParamVo param) {
        if (null == param) {
            return null;
        }
        String factoryCode = param.getFactoryCode();
        Integer year = param.getYear();
        Integer month = param.getMonth();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return null;
        }
        MpFactoryProductionVersion version = new MpFactoryProductionVersion();
        version.setFactoryCode(factoryCode);
        version.setYear(year);
        version.setMonth(month);
        factoryProductionVersionService.setProductionVersionCycleDate(version);
        return version;
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
        MpFactoryProductionVersion finalVersion = factoryProductionVersionService.getFinalVersion(queryCondition.getFactoryCode(), queryCondition.getProductionDate());
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

    /**
     * 校验空的排产版本信息
     *
     * @param checkParam
     * @return
     */
    private AjaxResult checkEmptyProductionVersion(FactoryProductionParamVo checkParam) {
        if (null == checkParam) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.condition.noEmpty"));
        }
        //正式需要加入排产版本号
        if (StringUtils.isBlank(checkParam.getFactoryCode()) || null == checkParam.getYear() || null == checkParam.getMonth() || StringUtils.isBlank(checkParam.getMonthPlanVersion()) || StringUtils.isBlank(checkParam.getProductionVersion())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.versionNoEmpty"));
        }
        return AjaxResult.success();
    }

    /**
     * 校验空的需求版本信息
     *
     * @param checkParam
     * @return
     */
    private AjaxResult checkEmptyMonthPlanVersion(FactoryProductionPlanVo checkParam) {
        if (null == checkParam) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.condition.noEmpty"));
        }
        if (StringUtils.isBlank(checkParam.getFactoryCode()) || null == checkParam.getYear() || null == checkParam.getMonth() || StringUtils.isBlank(checkParam.getMonthPlanVersion())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.requireVersionNoEmpty"));
        }
        return AjaxResult.success();
    }
}
