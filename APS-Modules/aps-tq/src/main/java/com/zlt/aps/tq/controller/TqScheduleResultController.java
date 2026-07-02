package com.zlt.aps.tq.controller;

import cn.hutool.core.date.DateUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.dto.TqChangeMachineDTO;
import com.zlt.aps.tq.api.domain.dto.TqInsertOrderDTO;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResult;
import com.zlt.aps.tq.api.domain.vo.TqScheduleShiftDateVO;
import com.zlt.aps.tq.engine.service.TqEngineService;
import com.zlt.aps.tq.mapper.TqScheduleResultMapper;
import com.zlt.aps.tq.service.ITqScheduleResultService;
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
 * 胎圈排程结果Controller
 *
 * @author APS
 */
@Slf4j
@Api(tags = "胎圈排程结果")
@RestController
@RequestMapping("/scheduleResult")
public class TqScheduleResultController extends AbstractDocBizController<TqScheduleResult> {

    @Autowired
    private ITqScheduleResultService tqScheduleResultService;

    @Resource
    private TqScheduleResultMapper tqScheduleResultMapper;

    @Autowired
    private TqEngineService tqEngineService;

    @ApiOperation("查询胎圈排程结果列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqScheduleResult queryVO) {
        startPage();
        LambdaQueryWrapper<TqScheduleResult> wrapper = buildQueryWrapper(queryVO);
        List<TqScheduleResult> list = tqScheduleResultMapper.selectList(wrapper);
        return getDataTable(list);
    }

    @Log(title = "胎圈排程结果", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqScheduleResult billVO) {
        return super.save(billVO);
    }

    @Log(title = "胎圈排程结果", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqScheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈排程结果", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈排程结果", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected IDocService getDocService() {
        return tqScheduleResultService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        // 默认排序：排程日期倒序、机台号正序、一班次顺序正序（用于导出等场景）
        return "SCHEDULE_DATE desc, MACHINE_CODE asc, CLASS1_SEQUENCE asc";
    }

    /**
     * 构建查询条件
     * 注意：isDelete 已由框架通过注解自动过滤，禁止手动追加条件
     */
    private LambdaQueryWrapper<TqScheduleResult> buildQueryWrapper(TqScheduleResult queryVO) {
        LambdaQueryWrapper<TqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryVO.getScheduleDateQuery() != null, TqScheduleResult::getScheduleDate, queryVO.getScheduleDateQuery());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getBeadCode()), TqScheduleResult::getBeadCode, queryVO.getBeadCode());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getProSize()), TqScheduleResult::getProSize, queryVO.getProSize());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getTriangleGlueCode()), TqScheduleResult::getTriangleGlueCode, queryVO.getTriangleGlueCode());
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getIsRelease()), TqScheduleResult::getIsRelease, queryVO.getIsRelease());
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), TqScheduleResult::getMachineCode, queryVO.getMachineCode());
        // 默认排序：排程日期倒序、机台号正序、一班次顺序正序
        wrapper.orderByDesc(TqScheduleResult::getScheduleDate);
        wrapper.orderByAsc(TqScheduleResult::getMachineCode);
        wrapper.orderByAsc(TqScheduleResult::getClass1Sequence);
        return wrapper;
    }

    /**
     * 自动排程
     */
    @Log(title = "胎圈排程结果", businessType = BusinessType.AUTOPLAN)
    @ApiOperation("自动排程")
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody TqScheduleResult queryVO) {
        Date scheduleDate = queryVO.getScheduleDateQuery();
        String factoryCode = queryVO.getFactoryCode();
        if (scheduleDate == null) {
            return AjaxResult.error("排程日期不能为空");
        }
        if (StringUtils.isEmpty(factoryCode)) {
            return AjaxResult.error("分厂不能为空");
        }
        tqEngineService.autoTqSchedule(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate), factoryCode);
        return AjaxResult.success();
    }

    /**
     * 插单前校验
     */
    @ApiOperation("插单前校验")
    @PostMapping("/validateInsertOrder")
    public AjaxResult validateInsertOrder(@RequestBody TqInsertOrderDTO dto) {
        return tqScheduleResultService.validateInsertOrder(dto);
    }

    /**
     * 插单
     */
    @Log(title = "胎圈排程结果", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("插单")
    @PostMapping("/insertOrder")
    public AjaxResult insertOrder(@RequestBody TqInsertOrderDTO dto) {
        return tqScheduleResultService.insertOrder(dto);
    }

    /**
     * 转机台前校验
     */
    @ApiOperation("转机台前校验")
    @PostMapping("/validateChangeMachine")
    public AjaxResult validateChangeMachine(@RequestBody TqChangeMachineDTO dto) {
        return tqScheduleResultService.validateChangeMachine(dto);
    }

    /**
     * 转机台
     */
    @Log(title = "胎圈排程结果", businessType = BusinessType.CHANGE_MACHINE)
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody TqChangeMachineDTO dto) {
        return tqScheduleResultService.changeMachine(dto);
    }

    /**
     * 调量前校验
     */
    @ApiOperation("调量前校验")
    @PostMapping("/validateChangeQty")
    public AjaxResult validateChangeQty(@RequestBody TqScheduleResult entity) {
        return tqScheduleResultService.validateChangeQty(entity);
    }

    /**
     * 调量
     */
    @Log(title = "胎圈排程结果", businessType = BusinessType.CHANGE_QTY)
    @ApiOperation("调量")
    @PostMapping("/changeQty")
    public AjaxResult changeQty(@RequestBody TqScheduleResult entity) {
        // 先校验，校验通过再执行调量
        AjaxResult validateResult = tqScheduleResultService.validateChangeQty(entity);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }
        return tqScheduleResultService.changeQty(entity);
    }

    /**
     * 逻辑删除排程记录
     * 只能删除发布成功次数等于0的计划
     */
    @Log(title = "胎圈排程结果", businessType = BusinessType.DELETE)
    @ApiOperation("逻辑删除排程记录")
    @PostMapping("/logicDelete")
    public AjaxResult logicDelete(@RequestBody List<Long> ids) {
        return tqScheduleResultService.logicDeleteByIds(ids);
    }

    /**
     * 发布排程到MES
     */
    @Log(title = "胎圈排程结果", businessType = BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody TqScheduleResult queryVO) {
        return tqScheduleResultService.publish(queryVO);
    }

    /**
     * 查询排程日期是否已发布
     */
    @ApiOperation("查询排程日期是否已发布")
    @PostMapping("/isPublish")
    public Boolean isPublish(@RequestBody TqScheduleResult queryVO) {
        return tqScheduleResultService.isPublish(queryVO.getScheduleDateQuery());
    }

    /**
     * 根据排程日期构建6个班次的日期展示列表
     * 胎圈排程6个班次覆盖D日中班、D+1日夜早中、D+2日夜早（D=排程日期-2，即今天）：
     * 班次1：D日中班，班次2~4：D+1日夜早中，班次5~6：D+2日夜早
     *
     * @param queryVO 查询条件
     * @return 班次日期列表
     */
    @ApiOperation("获取胎圈排程班次日期列表")
    @PostMapping("/listScheduleShiftDates")
    public List<TqScheduleShiftDateVO> listScheduleShiftDates(@RequestBody TqScheduleResult queryVO) {
        Date scheduleDate = queryVO.getScheduleDateQuery();
        if (scheduleDate == null) {
            // 默认排程日期 = 今天 + 1（T+1），与前端默认值保持一致
            scheduleDate = DateUtil.offsetDay(new Date(), 1);
        }
        // D = 排程日期 - 1（即今天）
        // 胎圈1班=D日中班，胎圈2-4班=D+1日(夜/早/中)，胎圈5-6班=D+2日(夜/早)
        Date dDay = DateUtil.offsetDay(scheduleDate, -1);
        Date dPlus1Day = DateUtil.offsetDay(dDay, 1);
        Date dPlus2Day = DateUtil.offsetDay(dDay, 2);
        String dDateStr = DateUtil.format(dDay, "MM/dd");
        String dPlus1DateStr = DateUtil.format(dPlus1Day, "MM/dd");
        String dPlus2DateStr = DateUtil.format(dPlus2Day, "MM/dd");

        List<TqScheduleShiftDateVO> result = new ArrayList<>(6);
        result.add(buildShiftDateVO(1, "afternoon", dDateStr));       // D日中班
        result.add(buildShiftDateVO(2, "night", dPlus1DateStr));      // D+1日夜班
        result.add(buildShiftDateVO(3, "morning", dPlus1DateStr));    // D+1日早班
        result.add(buildShiftDateVO(4, "afternoon", dPlus1DateStr));  // D+1日中班
        result.add(buildShiftDateVO(5, "night", dPlus2DateStr));      // D+2日夜班
        result.add(buildShiftDateVO(6, "morning", dPlus2DateStr));    // D+2日早班
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
