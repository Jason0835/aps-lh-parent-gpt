package com.zlt.aps.controller.monthplan;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common4ui.exception.BusinessException;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdFinalQueryDto;
import com.zlt.aps.monthplan.api.domain.dto.FactoryMonthPlanProdResultDto;
import com.zlt.aps.monthplan.api.domain.entity.FactoryMonthPlanProdFinal;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanDayProductionInfoVo;
import com.zlt.aps.monthplan.api.domain.vo.FactoryMonthPlanProdFinalVo;
import com.zlt.aps.monthplan.api.service.IFactoryMonthPlanProdFinalRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 分厂月生产计划最终版前端业务接口服务类
 *
 * @author ZLT
 * @date 20250211
 */
@Controller
@Deprecated
@RequestMapping("/factory/monthPlanProdResult")
@Api(tags = "分厂月生产计划最终版前端业务接口服务类")
public class FactoryMonthPlanProdResultUIController extends BaseController {

    private final IFactoryMonthPlanProdFinalRemoteService factoryMonthPlanProdFinalRemoteService;

    public FactoryMonthPlanProdResultUIController(IFactoryMonthPlanProdFinalRemoteService factoryMonthPlanProdFinalRemoteService) {
        this.factoryMonthPlanProdFinalRemoteService = factoryMonthPlanProdFinalRemoteService;
    }

    /**
     * 根据条件查询分厂月生产计划排产结果-排产结果列表
     */
    @ResponseBody
    @RequiresPermissions("monthplan:factoryMonthPlanProdResult:list")
    @PostMapping("/list")
    @ApiOperation("根据条件查询分厂月生产计划排产结果-排产结果列表")
    public TableDataInfo list(FactoryMonthPlanProdResultDto param) {
        if (null == param || null == param.getMonth() || null == param.getYear() || StringUtils.isBlank(param.getFactoryCode())) {
            throw new BusinessException(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        FactoryMonthPlanProdFinal prodFinal = new FactoryMonthPlanProdFinal();
        BeanUtils.copyProperties(param, prodFinal);
        return factoryMonthPlanProdFinalRemoteService.list(prodFinal);
    }

    /**
     * 根据查询条件，获取某日的月计划排产数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    @ResponseBody
    @ApiOperation("根据查询条件，获取某日对应的月计划排产数据")
    @PostMapping("/getMonthPlanProdResult")
    public List<FactoryMonthPlanProdFinalVo> getMonthPlanProdResult(@RequestBody FactoryMonthPlanProdFinalQueryDto queryCondition) {
        if (null == queryCondition || StringUtils.isBlank(queryCondition.getFactoryCode()) || null == queryCondition.getProductionDate()) {
            return Collections.emptyList();
        }
        return factoryMonthPlanProdFinalRemoteService.getMonthPlanProdResult(queryCondition);
    }

    /**
     * 根据查询条件，获取某日的月计划排产数据
     *
     * @param queryCondition 查询条件
     * @return
     */
    @ResponseBody
    @ApiOperation("根据查询条件，获取某日的月计划排产数据")
    @PostMapping("/getDayProductionInfo")
    public List<FactoryMonthPlanDayProductionInfoVo> getMonthPlanProductionInfo(@RequestBody FactoryMonthPlanProdFinalQueryDto queryCondition) {
        if (null == queryCondition || StringUtils.isBlank(queryCondition.getFactoryCode()) || null == queryCondition.getProductionDate()) {
            return Collections.emptyList();
        }
        return factoryMonthPlanProdFinalRemoteService.getMonthPlanProductionInfo(queryCondition);
    }

    @ResponseBody
    @PostMapping("/statistics")
    @ApiOperation("统计分厂月生产计划排产结果-排产结果列表")
    public AjaxResult statistics(FactoryMonthPlanProdResultDto param) {
        if (null == param || null == param.getMonth() || null == param.getYear() || StringUtils.isBlank(param.getFactoryCode())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        FactoryMonthPlanProdFinal prodFinal = new FactoryMonthPlanProdFinal();
        BeanUtils.copyProperties(param, prodFinal);

        return factoryMonthPlanProdFinalRemoteService.statistics(prodFinal);
    }

    @ResponseBody
    @PostMapping("/statisticsDay")
    @ApiOperation("统计分厂月生产计划日排产规格数及日排产总量")
    public AjaxResult statisticsByDay(@RequestBody FactoryMonthPlanProdResultDto param) {
        if (checkParamEmpty(param)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        FactoryMonthPlanProdFinal prodFinal = new FactoryMonthPlanProdFinal();
        BeanUtils.copyProperties(param, prodFinal);
        return factoryMonthPlanProdFinalRemoteService.getStatisticsDay(prodFinal);
    }

    @ResponseBody
    @PostMapping("/getProductionMonthType")
    @ApiOperation("获取月份排产模式--Date 不为空则表示非自然月排产，Date为空表示自然月排产")
    public AjaxResult getProductionMonthType(@RequestBody FactoryMonthPlanProdResultDto param) {
        if (checkParamEmpty(param)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.query.param.checkFactoryYearMonth"));
        }
        FactoryMonthPlanProdFinal prodFinal = new FactoryMonthPlanProdFinal();
        BeanUtils.copyProperties(param, prodFinal);
        return factoryMonthPlanProdFinalRemoteService.getProductionMonthType(prodFinal);
    }

    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }

    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.factoryMonthPlanProdFinal.modelName");
    }

    @ApiOperation("数据导出")
    @RequiresPermissions("monthplan:factoryMonthPlanProdResult:export")
    @GetMapping({"/export"})
    @ResponseBody
    public void export(HttpServletResponse response, FactoryMonthPlanProdResultDto param) throws IOException {
        if (null == param || null == param.getMonth() || null == param.getYear() || StringUtils.isBlank(param.getFactoryCode())) {
            // 分厂、年份、月份不可为空
            String fileName = this.getExportTemplateFileName();
            ExcelUtil<FactoryMonthPlanProdFinal> util = new ExcelUtil<>(FactoryMonthPlanProdFinal.class);
            util.exportExcel(response, null, fileName, fileName);
            return;
        }

        String fileName = this.getExportTemplateFileName();
        FactoryMonthPlanProdFinal prodFinal = new FactoryMonthPlanProdFinal();
        BeanUtils.copyProperties(param, prodFinal);
        byte[] excelBytes = factoryMonthPlanProdFinalRemoteService.exportData(prodFinal, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    /**
     * 校验分厂、年、月份不可为空
     *
     * @param param
     * @return
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
        if (StringUtils.isBlank(param.getFactoryCode())) {
            return true;
        }
        return false;
    }

    /**
     * 下发月计划
     *
     * @param factoryMonthPlanProdFinal 参数
     * @return 结果
     */
    @ApiOperation("下发月计划 - 年月+分厂+需求计划版本+分厂月计划版本")
    @PostMapping("/issueMonthPlan")
    @ResponseBody
    public AjaxResult issueMonthPlan(FactoryMonthPlanProdFinal factoryMonthPlanProdFinal) {
        return factoryMonthPlanProdFinalRemoteService.issueMonthPlan(factoryMonthPlanProdFinal);
    }
}
