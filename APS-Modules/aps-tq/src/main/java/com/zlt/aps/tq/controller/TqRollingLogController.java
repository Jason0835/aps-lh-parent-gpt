package com.zlt.aps.tq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqRollingLog;
import com.zlt.aps.tq.mapper.TqRollingLogMapper;
import com.zlt.aps.tq.service.ITqRollingLogService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 胎圈排程滚动更新日志Controller
 *
 * @author APS
 */
@Slf4j
@Api(tags = "胎圈排程滚动更新日志")
@RestController
@RequestMapping("/tqRollingLog")
public class TqRollingLogController extends AbstractDocBizController<TqRollingLog> {

    @Autowired
    private ITqRollingLogService tqRollingLogService;

    @Resource
    private TqRollingLogMapper tqRollingLogMapper;

    /**
     * 查询滚动更新日志列表
     */
    @ApiOperation("查询胎圈排程滚动更新日志列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqRollingLog queryVO) {
        startPage();
        List<TqRollingLog> list = tqRollingLogMapper.selectList(buildQueryWrapper(queryVO));
        return getDataTable(list);
    }

    /**
     * 获取滚动更新日志详情
     */
    @ApiOperation("获取胎圈排程滚动更新日志详情")
    @GetMapping("/{id}")
    @Override
    public TqRollingLog getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 删除滚动更新日志
     */
    @Log(title = "胎圈排程滚动更新日志", businessType = BusinessType.DELETE)
    @ApiOperation("删除胎圈排程滚动更新日志")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @Override
    protected IDocService getDocService() {
        return tqRollingLogService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper<TqRollingLog> buildQueryWrapper(TqRollingLog queryVO) {
        QueryWrapper<TqRollingLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("IS_DELETE", 0);
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getTriggerType()), "TRIGGER_TYPE", queryVO.getTriggerType());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getStatus()), "STATUS", queryVO.getStatus());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getBatchNo()), "BATCH_NO", queryVO.getBatchNo());
        queryWrapper.ge(PubUtil.isNotEmpty(queryVO.getScheduleDate()), "SCHEDULE_DATE", queryVO.getScheduleDate());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBeadCode()), "BEAD_CODE", queryVO.getBeadCode());
        queryWrapper.orderByDesc("CREATE_TIME");
        return queryWrapper;
    }
}
