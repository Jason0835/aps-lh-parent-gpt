package com.zlt.aps.cxlh.cx.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxTransferDeskDTO;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.cxlh.cx.api.domain.vo.CxGanttVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 成型排程结果Service接口
 *
 * @author zlt
 * @date 2021-07-12
 */
@FeignClient(contextId = "iCxScheduleResultService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cxlh:cxlh}")
public interface ICxScheduleResultService {

    /**
     * 查询列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/cxScheduleResult/list")
    TableDataInfo list(@RequestBody CxScheduleResult queryVO);

    /**
     * 成型硫化
     * */
    @ApiOperation("成型硫化")
    @PostMapping("/cxScheduleResult/sulfur")
    AjaxResult sulfur(@RequestBody CxScheduleResult portVO);

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/cxScheduleResult/save")
    AjaxResult save(@RequestBody CxScheduleResult port);

    /**
     * 删除
     */
    @ApiOperation("删除")
    @DeleteMapping("/cxScheduleResult/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/cxScheduleResult/{id}")
    CxScheduleResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/cxScheduleResult/checkUnique")
    String checkUnique(@RequestBody CxScheduleResult portVO);

    /**
     * 导出目的港+目的国关系列表
     */
    @ApiOperation("导出列表")
    @PostMapping("/cxScheduleResult/exportData/{fileName}")
    byte[] exportData(@RequestBody CxScheduleResult queryVO, @PathVariable("fileName") String fileName);


    @PostMapping("/cxScheduleResult/exportData2/{fileName}")
     byte[] exportData2(@RequestBody CxScheduleResult obj, @PathVariable("fileName")String fileName);

        /**
         * 导入
         */
    @ApiOperation("导入")
    @PostMapping("/cxScheduleResult/importData/{updateSupport}")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 导入数据
     */
    @PostMapping("/cxScheduleResult/importData2")
     AjaxResult importData2(@RequestBody ImportContext importContext, @RequestParam("importLogId") Long importLogId,@RequestParam("scheduleDate")String scheduleDate);


    /**
     * 导入数据
     */
    @PostMapping("/cxScheduleResult/importData3")
    AjaxResult importData3(@RequestBody ImportContext importContext, @RequestParam("importLogId") Long importLogId,@RequestParam("scheduleDate")String scheduleDate);


    /**
     * 查询所有的列表
     */
    @ApiOperation("查询所有的列表")
    @PostMapping("/port/getList")
    List<CxScheduleResult> getList(@RequestBody CxScheduleResult queryVO);


    /**
     * 插单校验
     */
    @PostMapping("/cxScheduleResult/validateAdd")
    AjaxResult validateAdd(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 转机台校验校验
     */
    @PostMapping("/cxScheduleResult/validateChangeMachine")
    AjaxResult validateChangeMachine(@RequestBody CxScheduleResult cxScheduleResult);

    @PostMapping("/cxScheduleResult/changeMachine")
    AjaxResult changeMachine(@RequestBody CxTransferDeskDTO dto);

    /**
     * 调量校验
     */
    @PostMapping("/cxScheduleResult/validateChangeQty")
    public AjaxResult validateChangeQty(@RequestBody CxScheduleResult entity);

    /**
     * 获取Bom版本
     */
    @PostMapping("/cxScheduleResult/getBomData")
    public AjaxResult getBomData(@RequestBody CxScheduleResult entity);

    /**
     * 排程发布校验
     */
    @PostMapping("/cxScheduleResult/publishValidate")
    AjaxResult publishValidate(@RequestBody CxScheduleResult cxScheduleResult);


    /**
     * 删除成型排程结果
     */
    @DeleteMapping("/cxScheduleResult/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 更改发布状态
     * @param entity 排程日期对象
     * @return 结果
     */
    @PostMapping("/cxScheduleResult/changeReleaseStatus")
    public AjaxResult changeReleaseStatus(@RequestBody CxScheduleResult entity);

    /**
     * 排程发布
     */
    @PostMapping("/cxScheduleResult/publish")
    AjaxResult publish(@RequestBody CxScheduleResult cxScheduleResult);


    /**
     * 将成型排程解析成月度剩余量，胎胚库存，月度完成量
     */
    @PostMapping("/cxScheduleResult/parseCxScheduleResult")
    AjaxResult parseCxScheduleResult(@RequestBody CxScheduleResult cxScheduleResult);

    /**
     * 查询成型机台甘特图
     *
     * @param queryVO 查询参数
     * @return 结果
     */
    @ApiOperation("查询成型机台甘特图")
    @PostMapping("/cxScheduleResult/selectMachineGantt")
    public AjaxResult selectMachineGantt(@RequestBody CxGanttVo queryVO);

    /**
     * 成型调整硫化
     */
    @ApiOperation("成型调整硫化")
    @PostMapping("/cxScheduleResult/updateLhScheduleResult")
    public AjaxResult updateCxScheduleResult(@RequestBody CxScheduleResult cxScheduleResult);

}
