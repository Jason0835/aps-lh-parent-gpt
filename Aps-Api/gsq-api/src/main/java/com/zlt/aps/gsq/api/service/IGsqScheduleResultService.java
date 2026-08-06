package com.zlt.aps.gsq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqChangeMachineDTO;
import com.zlt.aps.gsq.api.domain.dto.GsqInsertOrderDTO;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultImportDTO;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;
import com.zlt.aps.gsq.api.domain.vo.GsqScheduleShiftDateVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈排程结果Feign Service接口
 *
 * <p>6班次制：1班=D日中班，2班=D+1日夜班，3班=D+1日早班，4班=D+1日中班，5班=D+2日夜班，6班=D+2日早班
 * 其中 D+1 = 排程日期（SCHEDULE_DATE）
 *
 * @author APS
 */
@FeignClient(contextId = "IGsqScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqScheduleResultService {

    /**
     * 查询钢丝圈排程结果列表
     *
     * @param queryVO 查询条件
     * @return 分页列表数据
     */
    @PostMapping("/scheduleResult/list")
    @ApiOperation("查询钢丝圈排程结果列表")
    TableDataInfo list(@RequestBody GsqScheduleResult queryVO);

    /**
     * 获取详细信息
     *
     * @param id 主键id
     * @return 详细信息
     */
    @GetMapping("/scheduleResult/{id}")
    @ApiOperation("获取钢丝圈排程结果详细信息")
    GsqScheduleResult getInfo(@PathVariable("id") Long id);

    /**
     * 保存（新增/修改）
     *
     * @param entity 业务对象
     * @return 结果
     */
    @PostMapping("/scheduleResult/save")
    @ApiOperation("保存钢丝圈排程结果")
    AjaxResult save(@RequestBody GsqScheduleResult entity);

    /**
     * 删除（按ID列表）
     *
     * @param ids 主键ID列表
     * @return 结果
     */
    @PostMapping("/scheduleResult/delete/{ids}")
    @ApiOperation("删除钢丝圈排程结果")
    AjaxResult removeByIds(@PathVariable("ids") List<Long> ids);

    /**
     * 逻辑删除排程记录（已发布成功的计划不允许删除）
     *
     * @param ids 主键ID列表
     * @return 结果
     */
    @PostMapping("/scheduleResult/logicDelete")
    @ApiOperation("逻辑删除排程记录")
    AjaxResult logicDelete(@RequestBody List<Long> ids);

    /**
     * 导出排程结果
     *
     * @param queryVO  查询条件
     * @param fileName 文件名
     * @return 字节流
     */
    @PostMapping("/scheduleResult/exportData/{fileName}")
    @ApiOperation("导出钢丝圈排程结果")
    byte[] exportData(@RequestBody GsqScheduleResult queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导出排程结果列表（无分页）
     *
     * @param queryVO 查询条件
     * @return 列表
     */
    @PostMapping("/scheduleResult/exportList")
    @ApiOperation("导出钢丝圈排程结果列表")
    List<GsqScheduleResult> exportList(@RequestBody GsqScheduleResult queryVO);

    /**
     * 导入数据
     *
     * @param importContext 导入上下文
     * @param updateSupport  是否支持更新
     * @return 结果
     */
    @PostMapping("/scheduleResult/importData")
    @ApiOperation("导入钢丝圈排程结果")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 自动排程
     *
     * @param queryVO 排程参数（含排程日期、分厂）
     * @return 结果
     */
    @PostMapping("/scheduleResult/autoPlan")
    @ApiOperation("自动排程")
    AjaxResult autoPlan(@RequestBody GsqScheduleResult queryVO);

    /**
     * 插单前校验
     *
     * @param dto 插单数据
     * @return 校验结果
     */
    @PostMapping("/scheduleResult/validateInsertOrder")
    @ApiOperation("插单前校验")
    AjaxResult validateInsertOrder(@RequestBody GsqInsertOrderDTO dto);

    /**
     * 插单
     *
     * @param dto 插单数据
     * @return 结果
     */
    @PostMapping("/scheduleResult/insertOrder")
    @ApiOperation("插单")
    AjaxResult insertOrder(@RequestBody GsqInsertOrderDTO dto);

    /**
     * 转机台前校验
     *
     * @param dto 转机台数据
     * @return 校验结果
     */
    @PostMapping("/scheduleResult/validateChangeMachine")
    @ApiOperation("转机台前校验")
    AjaxResult validateChangeMachine(@RequestBody GsqChangeMachineDTO dto);

    /**
     * 转机台
     *
     * @param dto 转机台数据
     * @return 结果
     */
    @PostMapping("/scheduleResult/changeMachine")
    @ApiOperation("转机台")
    AjaxResult changeMachine(@RequestBody GsqChangeMachineDTO dto);

    /**
     * 调量前校验
     *
     * @param entity 调量数据
     * @return 校验结果
     */
    @PostMapping("/scheduleResult/validateChangeQty")
    @ApiOperation("调量前校验")
    AjaxResult validateChangeQty(@RequestBody GsqScheduleResult entity);

    /**
     * 调量
     *
     * @param entity 调量数据
     * @return 结果
     */
    @PostMapping("/scheduleResult/changeQty")
    @ApiOperation("调量")
    AjaxResult changeQty(@RequestBody GsqScheduleResult entity);

    /**
     * 发布排程到MES
     * 6班→3天拆分映射：
     * Day1(D日)：MID=钢丝圈1班
     * Day2(D+1日)：NIGHT=钢丝圈2班, DAY=钢丝圈3班, MID=钢丝圈4班
     * Day3(D+2日)：NIGHT=钢丝圈5班, DAY=钢丝圈6班
     *
     * @param queryVO 查询条件
     * @return 结果
     */
    @PostMapping("/scheduleResult/publish")
    @ApiOperation("发布排程")
    AjaxResult publish(@RequestBody GsqScheduleResult queryVO);

    /**
     * 查询排程日期是否已发布
     *
     * @param queryVO 查询条件
     * @return 是否已发布
     */
    @PostMapping("/scheduleResult/isPublish")
    @ApiOperation("查询排程日期是否已发布")
    Boolean isPublish(@RequestBody GsqScheduleResult queryVO);

    /**
     * 唯一性校验
     *
     * @param queryVO 查询条件
     * @return UserConstants.UNIQUE="0" 唯一，UserConstants.NOT_UNIQUE="1" 不唯一
     */
    @PostMapping("/scheduleResult/checkUnique")
    @ApiOperation("唯一性校验")
    String checkUnique(@RequestBody GsqScheduleResult queryVO);

    /**
     * 根据排程日期构建6个班次的日期展示列表
     * 钢丝圈排程6个班次覆盖D日中班、D+1日夜早中、D+2日夜早（D=排程日期-2，即今天）：
     * 班次1：D日中班，班次2~4：D+1日(夜/早/中)，班次5~6：D+2日(夜/早)
     *
     * @param queryVO 查询条件
     * @return 班次日期列表
     */
    @PostMapping("/scheduleResult/listScheduleShiftDates")
    @ApiOperation("获取钢丝圈排程班次日期列表")
    List<GsqScheduleShiftDateVO> listScheduleShiftDates(@RequestBody GsqScheduleResult queryVO);

    /**
     * 按专用模板导出钢丝圈排程结果
     *
     * @param entity   查询条件（含工厂、排程日期）
     * @param fileName 文件名
     * @return 导出字节流
     */
    @PostMapping("/scheduleResult/exportDataScheduleResult")
    @ApiOperation("按专用模板导出钢丝圈排程结果")
    byte[] exportDataScheduleResult(@RequestBody GsqScheduleResult entity, @RequestParam("fileName") String fileName);

    /**
     * 按专用模板导入钢丝圈排程结果
     *
     * @param importDTO      导入请求（含文件上下文、工厂、排程日期）
     * @param updateSupport  是否允许覆盖更新
     * @return 导入结果
     */
    @PostMapping("/scheduleResult/importDataScheduleResult")
    @ApiOperation("按专用模板导入钢丝圈排程结果")
    AjaxResult importDataScheduleResult(@RequestBody GsqScheduleResultImportDTO importDTO,
                                        @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 根据排程日期查询发布中或超时失败的记录数
     *
     * @param queryVO 查询条件
     * @return 记录数
     */
    @PostMapping("/scheduleResult/isReleasingOrTimeoutByDate")
    @ApiOperation("根据排程日期查询发布中或超时失败的记录数")
    int isReleasingOrTimeoutByDate(@RequestBody GsqScheduleResult queryVO);
}
