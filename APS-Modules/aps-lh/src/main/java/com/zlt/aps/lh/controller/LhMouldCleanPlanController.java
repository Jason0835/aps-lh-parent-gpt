package com.zlt.aps.lh.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanPlan;
import com.zlt.aps.lh.mapper.LhMouldCleanPlanMapper;
import com.zlt.aps.lh.service.ILhMouldCleanPlanService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jodd.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
@Api(tags = "模具清洗计划")
@RestController
@RequestMapping("/mouldCleanPlan")
public class LhMouldCleanPlanController extends AbstractDocBizController<LhMouldCleanPlan> {

    @Autowired
    private ILhMouldCleanPlanService lhMouldCleanPlanService;

    @Resource
    private LhMouldCleanPlanMapper lhMouldCleanPlanMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhMouldCleanPlan queryVO) {
        return super.list(queryVO);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhMouldCleanPlan getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    @Log(title = "ui.data.column.mouldCleanPlan.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody LhMouldCleanPlan billVO){
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        int result = lhMouldCleanPlanService.save(billVO);
        return result > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    @Log(title = "ui.data.column.mouldCleanPlan.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids){
        return super.removeByIds(ids);
    }

    @Log(title = "ui.data.column.mouldCleanPlan.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext,updateSupport);
    }

    public AjaxResult doImportData(List list, boolean updateSupport, long importLogId) {
        return lhMouldCleanPlanService.importData(list, updateSupport, importLogId);
    }

    @Log(title = "模具清洗计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhMouldCleanPlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @ApiOperation("从模具清洗预警同步生成计划")
    @Log(title = "ui.data.column.mouldCleanPlan.modelName", businessType = BusinessType.UPDATE)
    @PostMapping("/syncFromWarn")
    public AjaxResult syncFromWarn() {
        try {
            int count = lhMouldCleanPlanService.syncFromMouldCleanWarn();
            return AjaxResult.success("操作成功，成功生成" + count + "条模具清洗计划");
        } catch (Exception e) {
            log.error("从模具清洗预警同步生成计划失败", e);
            return AjaxResult.error("操作失败");
        }
    }

    @Override
    protected List<LhMouldCleanPlan> listExportData(LhMouldCleanPlan obj) {
        QueryWrapper<LhMouldCleanPlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return lhMouldCleanPlanMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return lhMouldCleanPlanService;
    }

    @Override
    protected void builderCondition(QueryWrapper<LhMouldCleanPlan> queryWrapper, LhMouldCleanPlan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhCode")), "LH_CODE", queryVO.getFieldValueByFieldName("lhCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cleanType")), "CLEAN_TYPE", queryVO.getFieldValueByFieldName("cleanType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dataSource")), "DATA_SOURCE", queryVO.getFieldValueByFieldName("dataSource"));

        if (queryVO.getCleanTimeBegin() != null) {
            queryWrapper.ge("CLEAN_TIME", DateUtil.beginOfDay(queryVO.getCleanTimeBegin()));
        }
        if (queryVO.getCleanTimeEnd() != null) {
            queryWrapper.le("CLEAN_TIME", DateUtil.endOfDay(queryVO.getCleanTimeEnd()));
        }
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "LH_CODE asc, CLEAN_TIME desc, id desc";
    }
}
