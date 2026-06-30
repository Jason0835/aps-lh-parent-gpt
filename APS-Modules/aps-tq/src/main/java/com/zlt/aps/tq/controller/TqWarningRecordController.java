package com.zlt.aps.tq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqWarningRecord;
import com.zlt.aps.tq.mapper.TqWarningRecordMapper;
import com.zlt.aps.tq.service.ITqWarningRecordService;
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
 * 胎圈排程预警记录Controller
 *
 * <p>提供预警记录的查询、详情、导出和处理功能。</p>
 *
 * @author APS
 */
@Slf4j
@Api(tags = "胎圈排程预警记录")
@RestController
@RequestMapping("/tqWarningRecord")
public class TqWarningRecordController extends AbstractDocBizController<TqWarningRecord> {

    @Autowired
    private ITqWarningRecordService tqWarningRecordService;

    @Resource
    private TqWarningRecordMapper tqWarningRecordMapper;

    /**
     * 查询胎圈排程预警记录列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqWarningRecord queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 获取胎圈排程预警记录详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public TqWarningRecord getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 导出列表
     */
    @Log(title = "胎圈排程预警记录", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqWarningRecord queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected IDocService getDocService() {
        return tqWarningRecordService;
    }

    @Override
    protected List<TqWarningRecord> listExportData(TqWarningRecord obj) {
        QueryWrapper<TqWarningRecord> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return tqWarningRecordMapper.selectList(wrapper);
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper 查询条件构造器
     * @param queryVO      查询参数
     */
    @Override
    protected void builderCondition(QueryWrapper<TqWarningRecord> queryWrapper, TqWarningRecord queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getWarningType()), "WARNING_TYPE", queryVO.getWarningType());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBeadCode()), "BEAD_CODE", queryVO.getBeadCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getWarningLevel()), "WARNING_LEVEL", queryVO.getWarningLevel());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getStatus()), "STATUS", queryVO.getStatus());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getShiftIndex()), "SHIFT_INDEX", queryVO.getShiftIndex());
        queryWrapper.ge(queryVO.getScheduleDate() != null, "SCHEDULE_DATE", queryVO.getScheduleDate());
    }

    @Override
    protected String getTypeCode() {
        return "TQ_WARNING_RECORD";
    }

    /**
     * 处理预警记录
     *
     * @param id      预警记录ID
     * @param handler 处理人
     * @param opinion 处理意见
     * @return 操作结果
     */
    @ApiOperation("处理预警记录")
    @PostMapping("/handleWarning")
    public AjaxResult handleWarning(@RequestParam("id") Long id,
                                    @RequestParam("handler") String handler,
                                    @RequestParam("opinion") String opinion) {
        try {
            int result = tqWarningRecordService.handleWarning(id, handler, opinion);
            if (result > 0) {
                return AjaxResult.success();
            }
            return AjaxResult.error("处理预警记录失败");
        } catch (Exception e) {
            log.error("处理预警记录失败：id={}", id, e);
            return AjaxResult.error("处理预警记录异常：" + e.getMessage());
        }
    }
}
