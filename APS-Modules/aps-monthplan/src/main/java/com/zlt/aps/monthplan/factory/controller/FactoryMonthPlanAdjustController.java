package com.zlt.aps.monthplan.factory.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdResultDto;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanAdjustPlanVo;
import com.zlt.aps.monthplan.factory.service.IFactoryMonthPlanAdjustService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * 分厂月计划调整业务
 *
 * @author ZLT
 * @date 20250320
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/monthPlanAdjust")
@Api(tags = "分厂月生产计划调整业务后端业务实现服务类-->ZLT")
public class FactoryMonthPlanAdjustController extends BaseController {

    private final IFactoryMonthPlanAdjustService factoryMonthPlanAdjustService;

    /**
     * 根据分厂、年份、月份获取计划调整控制信息
     */
    @ResponseBody
    @PostMapping("/getAdjustControlInfo")
    @ApiOperation("根据分厂、年份、月份获取计划调整控制信息")
    public AjaxResult getMonthPlanAdjustControlInfo(@RequestBody FactoryMonthPlanProdResultDto param) {
        if (null == param || StringUtils.isBlank(param.getFactoryCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryNoEmpty"));
        }
        return AjaxResult.success(factoryMonthPlanAdjustService.getAdjustControlInfo(param));
    }

    /**
     * 对分厂月计划执行调整
     * 对单条计划执行调整：计划维度：制造单号、SAP代码、模具、调整起始日、调整数量(正数调增，负数调减)
     * 根据计划制造单号，如果计划制造单号为空，则为新计划调增，否则是对原有计划调整
     * 如果调整数量为正数，则为原有规格调增，调整数量为负数，则为原有规格调减
     * 1、如果为调减，则从起始日开始，直接对其量数量调减，从起始日开始，直到所有的量调减完毕。(不可超出其最大安排量)
     * 2.如果为原规格调增，则判断模具从起始日开始的剩余硫化量是否可排产完毕调增量，如可以，则直接调增，否则
     * 根据模具号，查找其模具号的排产计划，用户确认其它计划的调减量
     * 3、如果为新规格调整，同样判断模具从起始日开始的剩余硫化量是否可排产完毕调增量，如可以，则直接调增，否则根据模具号，查找其模具号
     * 其它的排产计划，用户确认其它计划的调减量
     *
     * @param adjustPlan 计划调整
     * @return
     */
    @ResponseBody
    @ApiOperation("对分厂月计划执行调整")
    @PostMapping("/adjustFactoryMonthPlan")
    public AjaxResult adjustMonthPlan(@RequestBody FactoryMonthPlanAdjustPlanVo adjustPlan) {
        if (null == adjustPlan) {
            return AjaxResult.success();
        }
        AjaxResult checkParamResult = checkParam(adjustPlan);
        //校验没通过
        if (AjaxResult.Type.ERROR.value() == (Integer) checkParamResult.get(AjaxResult.CODE_TAG)) {
            return checkParamResult;
        }
        return factoryMonthPlanAdjustService.adjustMonthPlan(adjustPlan);
    }

    /**
     * 校验参数
     *
     * @return
     */
    private AjaxResult checkParam(FactoryMonthPlanAdjustPlanVo adjustPlan) {
        String factoryCode = adjustPlan.getFactoryCode();
        Integer year = adjustPlan.getYear();
        Integer month = adjustPlan.getMonth();
        if (StringUtils.isBlank(factoryCode) || null == year || null == month) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        String productCode = adjustPlan.getProductCode();
        String mouldNo = adjustPlan.getMouldNo();
        Integer adjustNumber = adjustPlan.getAdjustNumber();
        if (isEmptyAdjustInfo(productCode, mouldNo, adjustNumber)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkAdjustInfo"));
        }
        if (null == adjustPlan.getStartDate()) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkStartDate"));
        }
        String productionNo = adjustPlan.getProductionNo();
        //新增插入规格
        if (StringUtils.isBlank(productionNo)) {
            if (adjustNumber < 0) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.addProductNoSubtract"));
            }
            String locationType = adjustPlan.getLocationType();
            String specCode = adjustPlan.getSpecCode();
            if (StringUtils.isBlank(locationType) || StringUtils.isBlank(specCode)) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkSpecCodeInfo"));
            }
        }
        List<FactoryMonthPlanAdjustPlanVo> confirmSubtractLis = adjustPlan.getConfirmSubtractList();
        if (CollectionUtils.isEmpty(confirmSubtractLis)) {
            return AjaxResult.success();
        }
        for (FactoryMonthPlanAdjustPlanVo confirmSubtract : confirmSubtractLis) {
            if (isNoSubtract(confirmSubtract)) {
                return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.confirmSubtractNoEmpty"));
            }
        }
        return AjaxResult.success();
    }

    /**
     * 调整计划调整的关键信息是否为空，物料编码、模具、调整数量
     *
     * @param productCode  物料编码
     * @param mouldNo      模具
     * @param adjustNumber 调整数量
     * @return
     */
    private boolean isEmptyAdjustInfo(String productCode, String mouldNo, Integer adjustNumber) {
        if (StringUtils.isBlank(productCode)) {
            return true;
        }
        if (StringUtils.isBlank(mouldNo)) {
            return true;
        }
        return null == adjustNumber || adjustNumber == 0;
    }

    /**
     * 调减计划不可调减
     * 计划编号为空，或是调整量>0
     *
     * @param confirmSubtract
     * @return
     */
    private boolean isNoSubtract(FactoryMonthPlanAdjustPlanVo confirmSubtract) {
        String productionNo = confirmSubtract.getProductionNo();
        if (StringUtils.isBlank(productionNo)) {
            return true;
        }
        Integer adjustNumber = confirmSubtract.getAdjustNumber();
        return null == adjustNumber || adjustNumber > 0;
    }
}
