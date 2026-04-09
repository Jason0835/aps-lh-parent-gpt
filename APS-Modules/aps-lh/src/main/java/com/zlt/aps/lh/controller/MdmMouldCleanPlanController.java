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
import com.zlt.aps.maindata.mapper.MdmMouldCleanPlanMapper;
import com.zlt.aps.maindata.service.IMdmMouldCleanPlanService;
import com.zlt.aps.mdm.api.domain.entity.MdmMouldCleanPlan;
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
public class MdmMouldCleanPlanController extends AbstractDocBizController<MdmMouldCleanPlan> {

    @Autowired
    private IMdmMouldCleanPlanService mdmMouldCleanPlanService;

    @Resource
    private MdmMouldCleanPlanMapper mdmMouldCleanPlanMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmMouldCleanPlan queryVO) {
        return super.list(queryVO);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmMouldCleanPlan getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    @Log(title = "ui.data.column.mouldCleanPlan.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmMouldCleanPlan billVO){
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        int result = mdmMouldCleanPlanService.save(billVO);
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
        return mdmMouldCleanPlanService.importData(list, updateSupport, importLogId);
    }

    @Log(title = "模具清洗计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmMouldCleanPlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @ApiOperation("从模具清洗预警同步生成计划")
    @Log(title = "ui.data.column.mouldCleanPlan.modelName", businessType = BusinessType.UPDATE)
    @PostMapping("/syncFromWarn")
    public AjaxResult syncFromWarn() {
        try {
            int count = mdmMouldCleanPlanService.syncFromMouldCleanWarn();
            return AjaxResult.success(I18nUtil.getMessage("ui.message.operate.success") + "，" + String.format(I18nUtil.getMessage("ui.mould.clean.plan.sync.success"), count));
        } catch (Exception e) {
            log.error("从模具清洗预警同步生成计划失败", e);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.operate.fail"));
        }
    }

    @Override
    protected List<MdmMouldCleanPlan> listExportData(MdmMouldCleanPlan obj) {
        QueryWrapper<MdmMouldCleanPlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return mdmMouldCleanPlanMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mdmMouldCleanPlanService;
    }

    @Override
    protected void builderCondition(QueryWrapper<MdmMouldCleanPlan> queryWrapper, MdmMouldCleanPlan queryVO) {
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
        return "CLEAN_TIME desc, id desc";
    }
}
