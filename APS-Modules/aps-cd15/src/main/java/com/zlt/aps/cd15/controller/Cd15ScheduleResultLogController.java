package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleResultLog;
import com.zlt.aps.cd15.mapper.Cd15ScheduleResultLogMapper;
import com.zlt.aps.cd15.service.ICd15ScheduleResultLogService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/** CD15斜裁排程结果日志Controller。 */
@Api(tags = "CD15斜裁排程结果日志")
@RestController
@RequestMapping("/cd15ScheduleResultLog")
public class Cd15ScheduleResultLogController extends AbstractDocBizController<Cd15ScheduleResultLog> {

    @Resource
    private ICd15ScheduleResultLogService service;

    @Resource
    private Cd15ScheduleResultLogMapper mapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15ScheduleResultLog query) {
        return super.list(query);
    }

    @Log(title = "ui.data.column.cd15ScheduleResultLog.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15ScheduleResultLog getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "ui.data.column.cd15ScheduleResultLog.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15ScheduleResultLog query, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(query, fileName, response);
    }

    @Override
    protected List<Cd15ScheduleResultLog> listExportData(Cd15ScheduleResultLog obj) {
        QueryWrapper<Cd15ScheduleResultLog> queryWrapper = new QueryWrapper<>();
        this.builderCondition(queryWrapper, obj);
        List<Cd15ScheduleResultLog> list = mapper.selectList(queryWrapper);
        AppUtils.formatData(list, this.getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return service;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15ScheduleResultLog> queryWrapper, Cd15ScheduleResultLog vo) {
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        queryWrapper.eq(vo.getScheduleDate() != null, "SCHEDULE_DATE", vo.getScheduleDate());
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getBatchNo()), "BATCH_NO", vo.getBatchNo());
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getTaskId()), "TASK_ID", vo.getTaskId());
        queryWrapper.like(PubUtil.isNotEmpty(vo.getSteelStripCode()), "STEEL_STRIP_CODE", vo.getSteelStripCode());
        queryWrapper.like(PubUtil.isNotEmpty(vo.getBigRollCode()), "BIG_ROLL_CODE", vo.getBigRollCode());
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getCuttingAngle()), "CUTTING_ANGLE", vo.getCuttingAngle());
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getClassField()), "CLASS_FIELD", vo.getClassField());
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getLogType()), "LOG_TYPE", vo.getLogType());
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getReasonCode()), "REASON_CODE", vo.getReasonCode());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_SCHEDULE_RESULT_LOG";
    }

    @Override
    protected String getOrderBy() {
        return "LOG_TIME desc";
    }
}