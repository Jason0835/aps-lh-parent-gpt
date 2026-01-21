package com.zlt.aps.controller.monthplan;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.I18nConstant;
import com.zlt.aps.monthplan.api.domain.dto.FactoryFinalVersionQueryDto;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdResultDto;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProductionFinalResult;
import com.zlt.aps.monthplan.api.domain.entity.MpFactoryProductionVersion;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionParamVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryProductionPlanVo;
import com.zlt.aps.monthplan.api.service.IFactoryConsoleRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

/**
 * 工厂月生产计划控制台业务服务类
 *
 * @author ZLT
 * @date 20251201
 */
@Controller
@RequestMapping("/factory/console")
@Api(tags = "工厂月生产计划控制台业务-服务类")
@RequiredArgsConstructor
public class FactoryConsoleUIController extends BaseController {

    private final IFactoryConsoleRemoteService factoryConsoleService;

    /**
     * 根据条件查询工厂需要排产及已经排产的销售生产需求计划列表
     */
    @ResponseBody
    @PostMapping("/list")
    @ApiOperation(value = "查询分厂需要排产及已经排产的销售生产需求计划列表", notes = "根据条件查询查询分厂需要排产及已经排产的销售生产需求计划列表")
    public TableDataInfo getProductionVersionList(FactoryProductionPlanVo queryCondition) {
        return factoryConsoleService.getProductionVersionList(queryCondition);
    }

    /**
     * 查询工厂月份对应还没选择的需求计划版本列表
     */
    @ResponseBody
    @PostMapping("/noSelectedVersionList")
    @ApiOperation(value = "查询工厂月份对应还没选择的需求计划版本列表", notes = "查询分厂月份对应还没选择的需求计划版本列表")
    public TableDataInfo getNoSelectedVersionList(FactoryProductionPlanVo queryCondition) {
        return factoryConsoleService.getNoSelectedVersionList(queryCondition);
    }

    /**
     * 按工厂 + 年月 + 需求计划版本确认工厂需求排产的需求版本
     *
     * @param confirmParam 工厂需排产的需求信息
     * @return
     */
    @ResponseBody
    @PostMapping("/confirmProductionRequireVersion")
    @ApiOperation("按工厂 + 年月 + 需求计划版本确认工厂需求排产的需求版本")
    public AjaxResult confirmProductionRequireVersion(@RequestBody FactoryProductionPlanVo confirmParam) {
        AjaxResult checkParamResult = checkEmptyMonthPlanVersion(confirmParam);
        //校验没通过
        if (AjaxResult.Type.ERROR.value() == (Integer) checkParamResult.get(AjaxResult.CODE_TAG)) {
            return checkParamResult;
        }
        return factoryConsoleService.confirmProductionRequireVersion(confirmParam);
    }

    /**
     * 按工厂 + 年月 + 需求版本的方式进行工厂一键排产
     * 初始化->排结构->排模具
     *
     * @param factoryProductionParam
     * @return
     */
    @ResponseBody
    @PostMapping("/oneClickProductionProcess")
    @ApiOperation("按工厂 + 年月 + 需求版本的方式进行工厂一键排产 初始化->排结构->排模具")
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
     * @param factoryProductionParam 分厂排产初始化
     * @return
     */
    @ResponseBody
    @PostMapping("/resetConfigurationInitProduction")
    @ApiOperation("按工厂 + 年月 + 需求版本 + 排产版本的方式进行排产数据的重新初始化")
    public AjaxResult resetConfigurationInitProduction(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        AjaxResult checkParamResult = checkEmptyProductionVersion(factoryProductionParam);
        //校验没通过
        if (AjaxResult.Type.ERROR.value() == (Integer) checkParamResult.get(AjaxResult.CODE_TAG)) {
            return checkParamResult;
        }
        return factoryConsoleService.resetConfigurationInitProduction(factoryProductionParam);
    }

    /**
     * 按工厂 + 排产日期获取分厂的定稿版本信息
     *
     * @param queryCondition 查询条件
     * @return
     */
    @Deprecated
    @ResponseBody
    @PostMapping("/getFinalVersionInfo")
    @ApiOperation("按分厂 + 排产日期获取分厂的定稿版本信息")
    AjaxResult getFinalVersion(@RequestBody FactoryFinalVersionQueryDto queryCondition) {
        if (null == queryCondition) {
            return AjaxResult.error(I18nUtil.getMessage(I18nConstant.CONDITION_NO_EMPTY));
        }
        String factoryCode = queryCondition.getFactoryCode();
        Date productionDate = queryCondition.getProductionDate();
        if (null == productionDate || StringUtils.isBlank(factoryCode)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.factoryMonthPlanProdFinal.factoryProductionDateNoEmpty"));
        }
        return factoryConsoleService.getFinalVersion(queryCondition);
    }

    /**
     * 按分厂 + 年月 + 排产版本的方式进行分厂模具排产
     *
     * @param factoryProductionParam
     * @return
     */
    @ResponseBody
    @PostMapping("/factoryMouldingProduction")
    @ApiOperation("按分厂 + 年月 + 排产版本的方式进行分厂模具排产")
    public AjaxResult factoryMouldingProduction(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        if (null == factoryProductionParam) {
            return AjaxResult.error(I18nUtil.getMessage(I18nConstant.CONDITION_NO_EMPTY));
        }
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        String productionVersion = factoryProductionParam.getProductionVersion();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion) || StringUtils.isBlank(productionVersion)) {
            return AjaxResult.error(I18nUtil.getMessage(I18nConstant.PRODUCTION_VERSION_NO_EMPTY));
        }
        return factoryConsoleService.factoryMouldingProduction(factoryProductionParam);
    }


    /**
     * 按工厂 + 年月 + 需求版本的方式删除需求版本对应的排产版本
     *
     * @param factoryProductionParam
     * @return
     */
//    @RequiresPermissions("monthplan:console:deleteMonthPlanRequire")
    @ResponseBody
    @PostMapping("/deleteMonthPlanRequire")
    @ApiOperation("按工厂 + 年月 + 需求版本的方式删除需求版本对应的排产版本")
    public AjaxResult deleteMonthPlanRequire(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        if (null == factoryProductionParam) {
            return AjaxResult.error(I18nUtil.getMessage(I18nConstant.CONDITION_NO_EMPTY));
        }
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion)) {
            return AjaxResult.error(I18nUtil.getMessage(I18nConstant.REQUIRE_VERSION_NO_EMPTY));
        }
        return factoryConsoleService.deleteMonthPlanRequire(factoryProductionParam);
    }

    /**
     * 按分厂 + 年月 + 排产版本的方式删除排产计划版本
     *
     * @param factoryProductionParam
     * @return
     */
//    @RequiresPermissions("monthplan:console:deleteMonthPlanProductionVersion")
    @ResponseBody
    @PostMapping("/deleteMonthPlanProductionVersion")
    @ApiOperation("按分厂 + 年月 + 排产版本的方式删除排产计划版本")
    public AjaxResult deleteMonthPlanProductionVersion(@RequestBody FactoryProductionParamVo factoryProductionParam) {
        if (null == factoryProductionParam) {
            return AjaxResult.error(I18nUtil.getMessage(I18nConstant.CONDITION_NO_EMPTY));
        }
        String factoryCode = factoryProductionParam.getFactoryCode();
        Integer year = factoryProductionParam.getYear();
        Integer month = factoryProductionParam.getMonth();
        String monthPlanVersion = factoryProductionParam.getMonthPlanVersion();
        String productionVersion = factoryProductionParam.getProductionVersion();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month || StringUtils.isBlank(monthPlanVersion) || StringUtils.isBlank(productionVersion)) {
            return AjaxResult.error(I18nUtil.getMessage(I18nConstant.PRODUCTION_VERSION_NO_EMPTY));
        }
        return factoryConsoleService.deleteMonthPlanProductionVersion(factoryProductionParam);
    }

    /**
     * 定稿
     */
//    @RequiresPermissions("monthplan:console:finalized")
    @ResponseBody
    @PostMapping("/finalized")
    @ApiOperation("定稿 - 对选定的年月 + 工厂+ 需求计划版本 + 工厂月计划排产版本进行定稿")
    public AjaxResult finalized(FactoryMonthPlanProductionFinalResult factoryMonthPlanProdFinal) {
        return factoryConsoleService.finalized(factoryMonthPlanProdFinal);
    }

    /**
     * 参数校验
     * 工厂、年份、月份、需求计划版本
     *
     * @param checkParam
     * @return
     */
    private AjaxResult checkEmptyMonthPlanVersion(FactoryProductionPlanVo checkParam) {
        if (null == checkParam) {
            return AjaxResult.error(I18nUtil.getMessage(I18nConstant.CONDITION_NO_EMPTY));
        }
        if (StringUtils.isBlank(checkParam.getFactoryCode()) || null == checkParam.getYear() || null == checkParam.getMonth() || StringUtils.isBlank(checkParam.getMonthPlanVersion())) {
            return AjaxResult.error(I18nUtil.getMessage(I18nConstant.REQUIRE_VERSION_NO_EMPTY));
        }
        return AjaxResult.success();
    }

    /**
     * 参数校验
     * 工厂、年份、月份、需求计划版本、排产版本号
     *
     * @param checkParam
     * @return
     */
    private AjaxResult checkEmptyProductionVersion(FactoryProductionParamVo checkParam) {
        if (null == checkParam) {
            return AjaxResult.error(I18nUtil.getMessage(I18nConstant.CONDITION_NO_EMPTY));
        }
        //需要加上排产版本号
        if (StringUtils.isBlank(checkParam.getFactoryCode()) || null == checkParam.getYear() || null == checkParam.getMonth() || StringUtils.isBlank(checkParam.getMonthPlanVersion()) || StringUtils.isBlank(checkParam.getProductionVersion())) {
            return AjaxResult.error(I18nUtil.getMessage(I18nConstant.PRODUCTION_VERSION_NO_EMPTY));
        }
        return AjaxResult.success();
    }

    /**
     * 查询对应年月+分厂的需求计划版本
     */
    @ResponseBody
    @PostMapping("/versionList")
    @ApiOperation("查询对应年月+分厂的需求计划版本")
    public AjaxResult versionList(MpFactoryProductionVersion saleMonthPlanRequire) {
        return factoryConsoleService.versionList(saleMonthPlanRequire);
    }

    /**
     * 查询对应年月+分厂+需求计划版本的分厂月计划版本
     */
    @ResponseBody
    @PostMapping("/getProductionVersionList")
    @ApiOperation("查询对应年月+分厂+需求计划版本的分厂月计划版本")
    public AjaxResult getProductionVersionList(MpFactoryProductionVersion query) {
        return factoryConsoleService.getProductionVersionList(query);
    }

    @ResponseBody
    @PostMapping("/getProductionMonthType")
    @ApiOperation("获取月份排产模式--Date 不为空则表示非自然月排产，Date为空表示自然月排产")
    public AjaxResult getProductionMonthType(FactoryMonthPlanProdResultDto param) {
        if (checkParamEmpty(param)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        FactoryMonthPlanProdFinal prodFinal = new FactoryMonthPlanProdFinal();
        BeanUtils.copyProperties(param, prodFinal);
        return factoryConsoleService.getProductionMonthType(prodFinal);
    }

    /**
     * 校验分厂、年、月份不可为空
     *
     * @param param 参数
     * @return 结果
     */
    private boolean checkParamEmpty(FactoryMonthPlanProdResultDto param) {
        if (null == param) {
            return true;
        }
        if (null == param.getMonth()) {
            return true;
        }
        if (null == param.getYear()) {
            return true;
        }
        return StringUtils.isBlank(param.getFactoryCode());
    }
}
