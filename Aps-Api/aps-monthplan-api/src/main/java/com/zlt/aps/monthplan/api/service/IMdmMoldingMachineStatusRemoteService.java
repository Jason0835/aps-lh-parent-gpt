package com.zlt.aps.monthplan.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineStatus;
import com.zlt.aps.monthplan.api.domain.vo.CopyParamVo;
import com.zlt.aps.monthplan.api.domain.vo.MdmMoldingMachineStatusVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(contextId = "IMdmMoldingMachineStatusRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmMoldingMachineStatusRemoteService {
    String prefix = "/moldingMachineStatus";

    /**
     * 查询基础数据-成型机可用信息列表
     */
    @ApiOperation("查询基础数据-成型机可用信息列表")
    @PostMapping("/docMoldingMachineStatus/list")
    TableDataInfo list(@RequestBody MdmMoldingMachineStatus docMoldingMachineStatus);

    /**
     * 新增基础数据-成型机可用信息
     */
    @ApiOperation("新增基础数据-成型机可用信息")
    @PostMapping("/docMoldingMachineStatus/add")
    AjaxResult add(@RequestBody MdmMoldingMachineStatus docMoldingMachineStatus);

    /**
     * 修改基础数据-成型机可用信息
     */
    @ApiOperation("修改基础数据-成型机可用信息")
    @PostMapping("/docMoldingMachineStatus/edit")
    AjaxResult edit(@RequestParam("ids") Long[] ids, @RequestParam("status") String status);

    /**
     * 删除基础数据-成型机可用信息
     */
    @ApiOperation("删除基础数据-成型机可用信息")
    @DeleteMapping("/docMoldingMachineStatus/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/docMoldingMachineStatus/{id}")
    MdmMoldingMachineStatus getInfo(@PathVariable("id") Long id);

    /**
     * 校验基础数据-成型机可用信息唯一性
     */
    @ApiOperation("校验基础数据-成型机可用信息唯一性")
    @PostMapping("/docMoldingMachineStatus/checkDocMoldingMachineStatusUnique")
    String checkDocMoldingMachineStatusUnique(@RequestBody MdmMoldingMachineStatus docMoldingMachineStatus);

    /**
     * 导出基础数据-成型机可用信息列表
     */
    @ApiOperation("导出基础数据-成型机可用信息列表")
    @PostMapping("/docMoldingMachineStatus/getList")
    List<MdmMoldingMachineStatusVo> getList(@RequestBody MdmMoldingMachineStatus docMoldingMachineStatus);

    /**
     * 导入基础数据-成型机可用信息数据
     */
    @ApiOperation("导入基础数据-成型机可用信息")
    @PostMapping("/docMoldingMachineStatus/importData")
    AjaxResult importData(@RequestBody List<MdmMoldingMachineStatusVo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 复制可用台账信息
     */
    @ApiOperation("复制可用台账信息")
    @PostMapping("/docMoldingMachineStatus/copyDocVulcanizingMachStatus")
    AjaxResult copyMoldingMachineStatus(@RequestBody CopyParamVo params);

    /**
     * 复制可用台账信息
     */
    @ApiOperation("合并可用台账信息")
    @PostMapping("/docMoldingMachineStatus/mergeMoldingMachineStatus")
    AjaxResult mergeMoldingMachineStatus(@RequestBody CopyParamVo params);

    /**
     * 生成可用台账信息
     */
    @ApiOperation("生成可用台账信息")
    @PostMapping("/docMoldingMachineStatus/generateDocVulcanizingMachStatus")
    AjaxResult generateMoldingMachineStatus(@RequestBody CopyParamVo params);

    /**
     * 拷贝前校验 1.源月份没有数据 2.复制到的月份有数据是否要覆盖
     *
     * @return 结果
     */
    @ApiOperation("拷贝前校验")
    @PostMapping("/docMoldingMachineStatus/copyValidate")
    AjaxResult copyValidate(@RequestParam Map<String, Object> params);

}
