package com.zlt.aps.tm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;
import com.zlt.aps.tm.mapper.TmScheduleResultMapper;
import com.zlt.aps.tm.service.ITmScheduleResultService;
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

/**
 * 胎面排程结果表 控制层
 */
@Slf4j
@Api(tags = "胎面排程结果表")
@RestController
@RequestMapping("/tmScheduleResult")
public class TmScheduleResultController extends AbstractDocBizController<TmScheduleResult> {

    @Autowired
    private ITmScheduleResultService tmScheduleResultService;

    @Resource
    private TmScheduleResultMapper tmScheduleResultMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TmScheduleResult queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TmScheduleResult billVO) {
        if (StringUtil.isBlank(billVO.getFactoryCode())) {
            billVO.setFactoryCode(FactoryConstant.DEFAULT_FACTORY_CODE);
        }
        return super.save(billVO);
    }

    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{id}")
    @Override
    public TmScheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TmScheduleResult query) {
        return tmScheduleResultService.checkUnique(query);
    }

    /**
     * 转机台
     */
    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.UPDATE)
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody TmScheduleResult scheduleResult) {
        int releasingOrTimeoutByDate = tmScheduleResultService.isReleasingOrTimeoutByIds(new Long[]{scheduleResult.getId()});
        if (releasingOrTimeoutByDate > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        scheduleResult.setBaseVale(scheduleResult.getId());
        tmScheduleResultService.insetDispatcherLog(ApsConstant.DISPATCHER_OPER_MACHINE, scheduleResult);
        return toAjax(tmScheduleResultService.updateTmScheduleResult(scheduleResult));
    }

    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.tm.scheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TmScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<TmScheduleResult> listExportData(TmScheduleResult obj) {
        QueryWrapper<TmScheduleResult> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return tmScheduleResultMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return tmScheduleResultService;
    }

    @Override
    protected void builderCondition(QueryWrapper<TmScheduleResult> queryWrapper, TmScheduleResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("batchNo")), "BATCH_NO", queryVO.getFieldValueByFieldName("batchNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("machineCode")), "MACHINE_CODE", queryVO.getFieldValueByFieldName("machineCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("treadCode")), "TREAD_CODE", queryVO.getFieldValueByFieldName("treadCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("glueCode")), "GLUE_CODE", queryVO.getFieldValueByFieldName("glueCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("releaseStatus")), "RELEASE_STATUS", queryVO.getFieldValueByFieldName("releaseStatus"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dataSource")), "DATA_SOURCE", queryVO.getFieldValueByFieldName("dataSource"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("tailFlag")), "TAIL_FLAG", queryVO.getFieldValueByFieldName("tailFlag"));
    }

    @Override
    protected String getTypeCode() {
        return "TM0815";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}
