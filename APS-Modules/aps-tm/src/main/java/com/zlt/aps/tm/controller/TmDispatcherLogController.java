package com.zlt.aps.tm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tm.api.domain.entity.TmDispatcherLog;
import com.zlt.aps.tm.mapper.TmDispatcherLogMapper;
import com.zlt.aps.tm.service.ITmDispatcherLogService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 胎面调度员排程操作日志 控制层
 */
@Slf4j
@Api(tags = "胎面调度员排程操作日志")
@RestController
@RequestMapping("/tmDispatcherLog")
public class TmDispatcherLogController extends AbstractDocBizController<TmDispatcherLog> {

    @Autowired
    private ITmDispatcherLogService tmDispatcherLogService;

    @Resource
    private TmDispatcherLogMapper tmDispatcherLogMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TmDispatcherLog queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "ui.data.column.tm.dispatcherLog.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TmDispatcherLog queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<TmDispatcherLog> listExportData(TmDispatcherLog obj) {
        QueryWrapper<TmDispatcherLog> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return tmDispatcherLogMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return tmDispatcherLogService;
    }

    @Override
    protected void builderCondition(QueryWrapper<TmDispatcherLog> queryWrapper, TmDispatcherLog queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("operType")), "OPER_TYPE", queryVO.getFieldValueByFieldName("operType"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("treadCode")), "TREAD_CODE", queryVO.getFieldValueByFieldName("treadCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("createBy")), "CREATE_BY", queryVO.getFieldValueByFieldName("createBy"));
        // 时间范围查询
        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getStartTime()), "CREATE_TIME", queryVO.getStartTime());
        queryWrapper.le(PubUtil.isNotEmpty(queryVO.getEndTime()), "CREATE_TIME", queryVO.getEndTime() + " 23:59:59");
    }

    @Override
    protected String getTypeCode() {
        return "TM0814";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}
