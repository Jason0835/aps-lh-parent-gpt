package com.zlt.aps.lh.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.dto.*;
import com.zlt.aps.lh.api.domain.entity.LhDayFinishQty;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhGanttVo;
import com.zlt.aps.lh.api.domain.vo.LhMachineInfoVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 硫化排程结果Service接口
 *
 * @author xh
 * @date 2025-02-13
 */
@FeignClient(contextId = "ILhScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface ILhScheduleResultRemoteService {


    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/lhScheduleResult/list")
    TableDataInfo list(@RequestBody LhScheduleResult queryVO);

    /**
     * 插单查询可用机台列表
     */
    @ApiOperation("插单查询可用机台列表")
    @PostMapping("/lhScheduleResult/getScheduleMachineInfo")
    List<LhMachineInfoVo> getScheduleMachineInfo(@RequestBody LhOrderInsertParamDTO insertParamDTO);

    /**
     * 插单
     * @param insertDTO
     * @return
     */
    @ApiOperation("插单")
    @PostMapping("/lhScheduleResult/insertOrder")
    AjaxResult insertOrder(@RequestBody LhOrderInsertDTO insertDTO);

    /**
     * 自动排程
     */
    @ApiOperation("自动排程")
    @PostMapping("/lhScheduleResult/autoLhScheduleResult")
    AjaxResult autoLhScheduleResult(@RequestBody AutoLhScheduleResultDTO autoLhScheduleResultDTO);

    /**
     * 自动排程
     */
    @ApiOperation("自动排程测试")
    @PostMapping("/lhScheduleResult/autoLhScheduleResultTest")
    AjaxResult autoLhScheduleResultTest(@RequestBody AutoLhScheduleResultDTO autoLhScheduleResultDTO);
    /**
     * 导入
     */
    @ApiOperation("导入")
    @PostMapping("/lhScheduleResult/importData/{updateSupport}")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 导入数据
     */
    @PostMapping("/lhScheduleResult/importData2")
    @ApiOperation("导入硫化排程结果信息2")
    public AjaxResult importData2(@RequestBody LhScheduleImportFileDTO lhScheduleImportFileDTO);

    /**
     * 导出数据
     */
    @ApiOperation("导出列表")
    @PostMapping("/lhScheduleResult/exportData/{fileName}")
    byte[] exportData(@RequestBody LhScheduleResult queryVO, @PathVariable("fileName") String fileName);

    /**
     * 保存
     */
    @PostMapping("/lhScheduleResult/save")
    AjaxResult save(@RequestBody LhScheduleResult lhSpecifyMachine);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/lhScheduleResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    @ApiOperation("硫化排程结果转机台校验")
    @PostMapping("/lhScheduleResult/validateChangeMachine")
    AjaxResult validateChangeMachine(@RequestBody LhTransferDeskDTO dto);

    @ApiOperation("转机台")
    @PostMapping("/lhScheduleResult/changeMachine")
    AjaxResult changeMachine(@RequestBody LhTransferDeskDTO dto);

    @ApiOperation("调量")
    @PostMapping("/lhScheduleResult/adjustQuantity")
    AjaxResult adjustQuantity(@RequestBody LhScheduleResultUpdateDTO dto);

    @ApiOperation("根据排程时间获取批次号")
    @PostMapping("/lhScheduleResult/getBatchNo")
    String getBatchNo(@RequestBody AutoLhScheduleResultDTO dto);

    /**
     * 导入完成量
     *
     * @param list        完成量集合
     * @param importLogId 导入记录id
     * @return 结果
     */
    @PostMapping("/lhDayFinishQty/importFinishQty")
    @ApiOperation("导入完成量")
    AjaxResult importFinishQty(@RequestBody List<LhDayFinishQty> list, @RequestParam("importLogId") Long importLogId);

    /**
     * 发布所有排程结果
     *
     * @param dto 查询条件
     * @return 结果
     */
    @PostMapping("/lhScheduleResult/publish")
    public AjaxResult publish(@RequestBody LhScheduleResult dto);

    /**
     * 查询硫化机台甘特图
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    @ApiOperation("查询硫化机台甘特图")
    @PostMapping("/lhScheduleResult/selectMachineGantt")
    public AjaxResult selectMachineGantt(@RequestBody LhGanttVo queryVO);

}
