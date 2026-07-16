package com.zlt.aps.gsq.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gsq.api.domain.dto.GsqChangeMachineDTO;
import com.zlt.aps.gsq.api.domain.dto.GsqInsertOrderDTO;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.api.domain.vo.GsqScheduleShiftDateVO;
import com.zlt.aps.gsq.engine.service.GsqEngineService;
import com.zlt.aps.gsq.mapper.GsqScheduleResultMapper;
import com.zlt.aps.gsq.service.IGsqScheduleResultService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 钢丝圈排程结果Controller
 *
 * <p>6班次制：1班=D日中班，2班=D+1日夜班，3班=D+1日早班，4班=D+1日中班，5班=D+2日夜班，6班=D+2日早班
 * 其中 D+1 = 排程日期（SCHEDULE_DATE）
 *
 * @author APS
 */
@Slf4j
@Api(tags = "钢丝圈排程结果")
@RestController
@RequestMapping("/scheduleResult")
public class GsqScheduleResultController extends AbstractDocBizController<GsqScheduleResult> {

    @Autowired
    private IGsqScheduleResultService gsqScheduleResultService;

    @Resource
    private GsqScheduleResultMapper gsqScheduleResultMapper;

    @Autowired
    private GsqEngineService gsqEngineService;

    /**
     * 查询钢丝圈排程结果列表
     */
    @ApiOperation("查询钢丝圈排程结果列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GsqScheduleResult queryVO) {
        startPage();
        LambdaQueryWrapper<GsqScheduleResult> wrapper = buildQueryWrapper(queryVO);
        List<GsqScheduleResult> list = gsqScheduleResultMapper.selectList(wrapper);
        return getDataTable(list);
    }

    /**
     * 保存（新增/修改）
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody GsqScheduleResult billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public GsqScheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 导入数据
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext,
                                 @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出数据
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody GsqScheduleResult queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected IDocService getDocService() {
        return gsqScheduleResultService;
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
    private LambdaQueryWrapper<GsqScheduleResult> buildQueryWrapper(GsqScheduleResult queryVO) {
        LambdaQueryWrapper<GsqScheduleResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryVO.getScheduleDateQuery() != null,
                GsqScheduleResult::getScheduleDate, queryVO.getScheduleDateQuery());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getSteelRingCode()),
                GsqScheduleResult::getSteelRingCode, queryVO.getSteelRingCode());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getProSize()),
                GsqScheduleResult::getProSize, queryVO.getProSize());
        wrapper.like(PubUtil.isNotEmpty(queryVO.getTwiningDiscCode()),
                GsqScheduleResult::getTwiningDiscCode, queryVO.getTwiningDiscCode());
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getIsRelease()),
                GsqScheduleResult::getIsRelease, queryVO.getIsRelease());
        wrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()),
                GsqScheduleResult::getMachineCode, queryVO.getMachineCode());
        // 默认排序：排程日期倒序、机台号正序、一班次顺序正序
        wrapper.orderByDesc(GsqScheduleResult::getScheduleDate);
        wrapper.orderByAsc(GsqScheduleResult::getMachineCode);
        wrapper.orderByAsc(GsqScheduleResult::getClass1Sequence);
        return wrapper;
    }

    /**
     * 自动排程
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.AUTOPLAN)
    @ApiOperation("自动排程")
    @PostMapping("/autoPlan")
    public AjaxResult autoPlan(@RequestBody GsqScheduleResult queryVO) {
        Date scheduleDate = queryVO.getScheduleDateQuery();
        String factoryCode = queryVO.getFactoryCode();
        if (scheduleDate == null) {
            return AjaxResult.error("排程日期不能为空");
        }
        if (StringUtils.isEmpty(factoryCode)) {
            return AjaxResult.error("分厂不能为空");
        }
        gsqEngineService.autoGsqSchedule(DateUtils.parseDateToStr("yyyy-MM-dd", scheduleDate), factoryCode);
        return AjaxResult.success();
    }

    /**
     * 插单前校验
     */
    @ApiOperation("插单前校验")
    @PostMapping("/validateInsertOrder")
    public AjaxResult validateInsertOrder(@RequestBody GsqInsertOrderDTO dto) {
        return gsqScheduleResultService.validateInsertOrder(dto);
    }

    /**
     * 插单
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("插单")
    @PostMapping("/insertOrder")
    public AjaxResult insertOrder(@RequestBody GsqInsertOrderDTO dto) {
        return gsqScheduleResultService.insertOrder(dto);
    }

    /**
     * 转机台前校验
     */
    @ApiOperation("转机台前校验")
    @PostMapping("/validateChangeMachine")
    public AjaxResult validateChangeMachine(@RequestBody GsqChangeMachineDTO dto) {
        return gsqScheduleResultService.validateChangeMachine(dto);
    }

    /**
     * 转机台
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.CHANGE_MACHINE)
    @ApiOperation("转机台")
    @PostMapping("/changeMachine")
    public AjaxResult changeMachine(@RequestBody GsqChangeMachineDTO dto) {
        return gsqScheduleResultService.changeMachine(dto);
    }

    /**
     * 调量前校验
     */
    @ApiOperation("调量前校验")
    @PostMapping("/validateChangeQty")
    public AjaxResult validateChangeQty(@RequestBody GsqScheduleResult entity) {
        return gsqScheduleResultService.validateChangeQty(entity);
    }

    /**
     * 调量
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.CHANGE_QTY)
    @ApiOperation("调量")
    @PostMapping("/changeQty")
    public AjaxResult changeQty(@RequestBody GsqScheduleResult entity) {
        // 先校验，校验通过再执行调量
        AjaxResult validateResult = gsqScheduleResultService.validateChangeQty(entity);
        if (!validateResult.get(AjaxResult.CODE_TAG).equals(200)) {
            return validateResult;
        }
        return gsqScheduleResultService.changeQty(entity);
    }

    /**
     * 逻辑删除排程记录
     * 只能删除发布成功次数等于0的计划
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.DELETE)
    @ApiOperation("逻辑删除排程记录")
    @PostMapping("/logicDelete")
    public AjaxResult logicDelete(@RequestBody List<Long> ids) {
        return gsqScheduleResultService.logicDeleteByIds(ids);
    }

    /**
     * 发布排程到MES
     * 前端传入选中记录ID列表（ids），后端按发布状态过滤可发布记录：
     * 仅处理"未发布(0)"、"待发布(5)"、"发布失败(2)"三种状态，其余状态忽略。
     *
     * @param queryVO 查询条件（含 scheduleDate、factoryCode、ids）
     * @return 发布结果
     */
    @Log(title = "钢丝圈排程结果", businessType = BusinessType.PUBLISH)
    @ApiOperation("发布排程")
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody GsqScheduleResult queryVO) {
        return gsqScheduleResultService.publish(queryVO);
    }

    /**
     * 查询排程日期是否已发布
     */
    @ApiOperation("查询排程日期是否已发布")
    @PostMapping("/isPublish")
    public Boolean isPublish(@RequestBody GsqScheduleResult queryVO) {
        return gsqScheduleResultService.isPublish(queryVO.getScheduleDateQuery());
    }

    /**
     * 唯一性校验
     */
    @ApiOperation("唯一性校验")
    @PostMapping("/checkUnique")
    public Boolean checkUnique(@RequestBody GsqScheduleResult queryVO) {
        return gsqScheduleResultService.checkUnique(queryVO);
    }

    /**
     * 根据排程日期查询发布中或超时失败的记录数
     */
    @ApiOperation("根据排程日期查询发布中或超时失败的记录数")
    @PostMapping("/isReleasingOrTimeoutByDate")
    public int isReleasingOrTimeoutByDate(@RequestBody GsqScheduleResult queryVO) {
        return gsqScheduleResultService.isReleasingOrTimeoutByDate(queryVO.getScheduleDateQuery());
    }

    /**
     * 根据排程日期构建6个班次的日期展示列表
     * 钢丝圈排程6个班次覆盖D日中班、D+1日夜早中、D+2日夜早（D=排程日期-2，即今天）：
     * 班次1：D日中班，班次2~4：D+1日(夜/早/中)，班次5~6：D+2日(夜/早)
     *
     * @param queryVO 查询条件
     * @return 班次日期列表
     */
    @ApiOperation("获取钢丝圈排程班次日期列表")
    @PostMapping("/listScheduleShiftDates")
    public List<GsqScheduleShiftDateVO> listScheduleShiftDates(@RequestBody GsqScheduleResult queryVO) {
        return gsqScheduleResultService.listScheduleShiftDates(queryVO);
    }
}
