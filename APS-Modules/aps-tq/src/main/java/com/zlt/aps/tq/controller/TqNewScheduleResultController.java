package com.zlt.aps.tq.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqNewScheduleResult;
import com.zlt.aps.tq.api.domain.vo.TqScheduleShiftDateVO;
import com.zlt.aps.tq.engine.service.TqEngineService;
import com.zlt.aps.tq.mapper.TqNewScheduleResultMapper;
import com.zlt.aps.tq.service.ITqNewScheduleResultService;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 胎圈排程结果Controller（新版）
 *
 * @author APS
 */
@Slf4j
@Api(tags = "胎圈排程结果(新)")
@RestController
@RequestMapping("/tqNewScheduleResult")
public class TqNewScheduleResultController extends AbstractDocBizController<TqNewScheduleResult> {

    @Autowired
    private ITqNewScheduleResultService tqNewScheduleResultService;

    @Resource
    private TqNewScheduleResultMapper tqNewScheduleResultMapper;

    @Autowired
    private TqEngineService tqEngineService;

    @ApiOperation("查询胎圈排程结果列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqNewScheduleResult queryVO) {
        startPage();
        LambdaQueryWrapper<TqNewScheduleResult> wrapper = buildQueryWrapper(queryVO);
        List<TqNewScheduleResult> list = tqNewScheduleResultMapper.selectList(wrapper);
        return getDataTable(list);
    }

    @Log(title = "胎圈排程结果(新)", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqNewScheduleResult billVO) {
        return super.save(billVO);
    }

    @Log(title = "胎圈排程结果(新)", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqNewScheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈排程结果(新)", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈排程结果(新)", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqNewScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected IDocService getDocService() {
        return tqNewScheduleResultService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "SCHEDULE_DATE desc, MACHINE_CODE, CLASS1_SEQUENCE";
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<TqNewScheduleResult> buildQueryWrapper(TqNewScheduleResult queryVO) {
        LambdaQueryWrapper<TqNewScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TqNewScheduleResult::getIsDelete, 0);
        wrapper.eq(queryVO.getScheduleDateQuery() != null, TqNewScheduleResult::getScheduleDate, queryVO.getScheduleDateQuery());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getBeadCode()), TqNewScheduleResult::getBeadCode, queryVO.getBeadCode());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getProSize()), TqNewScheduleResult::getProSize, queryVO.getProSize());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getTriangleGlueCode()), TqNewScheduleResult::getTriangleGlueCode, queryVO.getTriangleGlueCode());
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getIsRelease()), TqNewScheduleResult::getIsRelease, queryVO.getIsRelease());
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), TqNewScheduleResult::getMachineCode, queryVO.getMachineCode());
        wrapper.orderByDesc(TqNewScheduleResult::getScheduleDate);
        wrapper.orderByAsc(TqNewScheduleResult::getMachineCode);
        wrapper.orderByAsc(TqNewScheduleResult::getClass1Sequence);
        return wrapper;
    }

    /**
     * 自动排程
     */
    @Log(title = "胎圈排程结果(新)", businessType = BusinessType.AUTOPLAN)
    @ApiOperation("自动排程")
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody TqNewScheduleResult queryVO) {
        Date scheduleDate = queryVO.getScheduleDateQuery();
        if (scheduleDate == null) {
            return AjaxResult.error("排程日期不能为空");
        }
        tqEngineService.autoTqSchedule(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate));
        return AjaxResult.success();
    }

    /**
     * 插单（id为空则新增排程记录）
     */
    @Log(title = "胎圈排程结果(新)", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("插单")
    @PostMapping("/insertOrder")
    public AjaxResult insertOrder(@RequestBody TqNewScheduleResult entity) {
        // TODO 插单业务逻辑待实现：规格校验、唯一性校验等
        return tqNewScheduleResultService.insertOrder(entity);
    }

    /**
     * 转机台
     */
    @Log(title = "胎圈排程结果(新)", businessType = BusinessType.CHANGE_MACHINE)
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody TqNewScheduleResult entity) {
        // TODO 转机台业务逻辑待实现：发布状态校验、唯一性校验、调度日志等
        return tqNewScheduleResultService.changeMachine(entity);
    }

    /**
     * 调量
     */
    @Log(title = "胎圈排程结果(新)", businessType = BusinessType.CHANGE_QTY)
    @ApiOperation("调量")
    @PostMapping("/changeQty")
    public AjaxResult changeQty(@RequestBody TqNewScheduleResult entity) {
        // TODO 调量业务逻辑待实现：发布状态校验、调度日志等
        return tqNewScheduleResultService.changeQty(entity);
    }

    /**
     * 发布排程到MES
     */
    @Log(title = "胎圈排程结果(新)", businessType = BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody TqNewScheduleResult queryVO) {
        // TODO 发布业务逻辑待实现：同步锁、状态校验、MES数据同步等
        return tqNewScheduleResultService.publish(queryVO);
    }

    /**
     * 查询排程日期是否已发布
     */
    @ApiOperation("查询排程日期是否已发布")
    @PostMapping("/isPublish")
    public Boolean isPublish(@RequestBody TqNewScheduleResult queryVO) {
        return tqNewScheduleResultService.isPublish(queryVO.getScheduleDateQuery());
    }

    /**
     * 根据排程日期、胎圈代码、机台编号校验唯一性
     */
//    @ApiOperation("唯一性校验")
//    @PostMapping("/checkUnique")
//    public String checkUnique(@RequestBody TqNewScheduleResult entity) {
//        return tqNewScheduleResultService.checkUnique(entity);
//    }

    /**
     * 根据排程日期构建6个班次的日期展示列表
     * 胎圈排程6个班次覆盖排程日期的前一天和当天：
     * 班次1~3属于T-1日（夜班→早班→中班），班次4~6属于T日（夜班→早班→中班）
     *
     * @param queryVO
     * @return 班次日期列表
     */
    @ApiOperation("获取胎圈排程班次日期列表")
    @PostMapping("/listScheduleShiftDates")
    public List<TqScheduleShiftDateVO> listScheduleShiftDates(@RequestBody TqNewScheduleResult queryVO) {
        Date scheduleDate = queryVO.getScheduleDateQuery();
        if (scheduleDate == null) {
            scheduleDate = DateUtil.offsetDay(new Date(), 2);
        }
        Date tMinus1Day = DateUtil.offsetDay(scheduleDate, -1);
        String tMinus1DateStr = DateUtil.format(tMinus1Day, "MM/dd");
        String tDateStr = DateUtil.format(scheduleDate, "MM/dd");

        List<TqScheduleShiftDateVO> result = new ArrayList<>(6);
        result.add(buildShiftDateVO(1, "night", tMinus1DateStr));
        result.add(buildShiftDateVO(2, "morning", tMinus1DateStr));
        result.add(buildShiftDateVO(3, "afternoon", tMinus1DateStr));
        result.add(buildShiftDateVO(4, "night", tDateStr));
        result.add(buildShiftDateVO(5, "morning", tDateStr));
        result.add(buildShiftDateVO(6, "afternoon", tDateStr));
        return result;
    }

    /**
     * 构建班次日期VO
     */
    private TqScheduleShiftDateVO buildShiftDateVO(int shift, String shiftType, String shiftDate) {
        TqScheduleShiftDateVO vo = new TqScheduleShiftDateVO();
        vo.setShift(shift);
        vo.setShiftType(shiftType);
        vo.setShiftDate(shiftDate);
        return vo;
    }
}
