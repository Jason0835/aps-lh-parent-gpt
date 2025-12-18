package com.zlt.aps.monthplan.raw.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.zlt.aps.maindata.mapper.RawWeekUsageEntityMapper;
import com.zlt.aps.maindata.service.IRawWeekUsageService;
import com.zlt.aps.monthplan.api.domain.entity.RawWeekUsage;
import com.zlt.aps.monthplan.raw.service.impl.RawWeekUsageGenerateServiceImpl;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author Nick
 */
@RestController
@RequestMapping("/raw-week-usage")
@Api(tags = "周维度原材料用量管理")
public class RawWeekUsageController extends AbstractDocBizController<RawWeekUsage> {

    @Autowired
    private RawWeekUsageEntityMapper entityMapper;

    @Autowired
    private IRawWeekUsageService rawWeekUsageService;

    @Autowired
    private RawWeekUsageGenerateServiceImpl rawWeekUsageGenerateService;

    /**
     * 查询周维度原材料用量记录列表
     */
    @RequiresPermissions( "maindata:rawWeekUsage:list")
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody RawWeekUsage queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }



    @Override
    protected List<RawWeekUsage> listExportData(RawWeekUsage obj) {
        QueryWrapper<RawWeekUsage> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return rawWeekUsageService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<RawWeekUsage> queryWrapper, RawWeekUsage queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("week")), "WEEK", queryVO.getFieldValueByFieldName("week"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialName")), "MATERIAL_NAME", queryVO.getFieldValueByFieldName("materialName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("planQty")), "PLAN_QTY", queryVO.getFieldValueByFieldName("planQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("actualQty")), "ACTUAL_QTY", queryVO.getFieldValueByFieldName("actualQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deviationQty")), "DEVIATION_QTY", queryVO.getFieldValueByFieldName("deviationQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("deviationRate")), "DEVIATION_RATE", queryVO.getFieldValueByFieldName("deviationRate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("hasWarning")), "HAS_WARNING", queryVO.getFieldValueByFieldName("hasWarning"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("warningLevel")), "WARNING_LEVEL", queryVO.getFieldValueByFieldName("warningLevel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("startDate")), "START_DATE", queryVO.getFieldValueByFieldName("startDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("endDate")), "END_DATE", queryVO.getFieldValueByFieldName("endDate"));
    }

    @RequiresPermissions( "maindata:rawWeekUsage:generateByMonth")
    @PostMapping("/generate-by-month")
    @ApiOperation("按照月份生成周维度原材料用量记录")
    public AjaxResult generateByMonth(@RequestParam("factoryCode") String factoryCode,
                                      @RequestParam("year") Integer year,
                                      @RequestParam("month") Integer month) {
        return rawWeekUsageGenerateService.generateWeekUsage(factoryCode, year, month);
    }

    @RequiresPermissions( "maindata:rawWeekUsage:generateByWeek")
    @PostMapping("/generate-by-week")
    @ApiOperation("按照周维度份生成周维度原材料用量记录")
    public AjaxResult generateByWeek(@RequestParam("factoryCode") String factoryCode,
                                     @RequestParam("year") Integer year,
                                     @RequestParam("month") Integer month,
                                     @RequestParam("week") Integer week) {
        return rawWeekUsageGenerateService.recalculateWeekUsage(factoryCode, year, month, week);
    }

    @RequiresPermissions( "maindata:rawWeekUsage:statistics")
    @GetMapping("/statistics")
    @ApiOperation("获取周用量统计数据")
    public AjaxResult getStatistics(@RequestParam("factoryCode") String factoryCode,
                                    @RequestParam("year") Integer year,
                                    @RequestParam(value = "month", required = false) Integer month,
                                    @RequestParam(value = "week", required = false) Integer week) {
        Map<String, Object> statistics = rawWeekUsageGenerateService
                .getWeekUsageStatistics(factoryCode, year, month, week);
        return AjaxResult.success(statistics);
    }

    @Override
    protected String getTypeCode(){
        return "S3522";
    }
}