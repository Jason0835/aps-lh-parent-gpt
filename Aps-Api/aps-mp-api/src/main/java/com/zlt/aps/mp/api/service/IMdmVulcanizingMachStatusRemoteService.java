package com.zlt.aps.mp.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmVulcanizingMachStatus;
import com.zlt.aps.mp.api.domain.vo.CopyParamVo;
import com.zlt.aps.mp.api.domain.vo.MdmVulcanizingMachStatusVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(contextId = "IMdmVulcanizingMachStatusRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmVulcanizingMachStatusRemoteService {

    /**
     * 查询基础数据-硫化机可用信息列表
     */
    @ApiOperation("查询基础数据-硫化机可用信息列表")
    @PostMapping("/docVulcanizationMachStatus/list")
    TableDataInfo list(@RequestBody MdmVulcanizingMachStatusVo docVulcanizingMachStatus);

    /**
     * 新增基础数据-硫化机可用信息
     */
    @ApiOperation("新增基础数据-硫化机可用信息")
    @PostMapping("/docVulcanizationMachStatus/add")
    AjaxResult add(@RequestBody MdmVulcanizingMachStatusVo docVulcanizingMachStatus);

    /**
     * 修改基础数据-硫化机可用信息
     */
    @ApiOperation("修改基础数据-硫化机可用信息")
    @PostMapping("/docVulcanizationMachStatus/edit")
    AjaxResult edit(@RequestParam("ids") Long[] ids, @RequestParam("status") String status);

    /**
     * 删除基础数据-硫化机可用信息
     */
    @ApiOperation("删除基础数据-硫化机可用信息")
    @DeleteMapping("/docVulcanizationMachStatus/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/docVulcanizationMachStatus/{id}")
    MdmVulcanizingMachStatus getInfo(@PathVariable("id") Long id);

    /**
     * 校验基础数据-硫化机可用信息唯一性
     */
    @ApiOperation("校验基础数据-硫化机可用信息唯一性")
    @PostMapping("/docVulcanizationMachStatus/checkDocVulcanizingMachStatusUnique")
    String checkDocVulcanizingMachStatusUnique(@RequestBody MdmVulcanizingMachStatusVo docVulcanizingMachStatus);

    /**
     * 复制可用台账信息
     */
    @ApiOperation("复制可用台账信息")
    @PostMapping("/docVulcanizationMachStatus/copyDocVulcanizingMachStatus")
    public AjaxResult copyDocVulcanizingMachStatus(@RequestBody CopyParamVo copyParamVo);

    /**
     * 合并可用台账信息
     */
    @ApiOperation("合并可用台账信息")
    @PostMapping("/docVulcanizationMachStatus/mergeDocVulcanizingMachStatus")
    public AjaxResult mergeDocVulcanizingMachStatus(@RequestBody CopyParamVo copyParamVo);

    /**
     * 生成可用台账信息
     */
    @ApiOperation("生成可用台账信息")
    @PostMapping("/docVulcanizationMachStatus/generateDocVulcanizingMachStatus")
    public AjaxResult generateDocVulcanizingMachStatus(@RequestBody CopyParamVo params);

    /**
     * 拷贝前校验 1.源月份没有数据 2.复制到的月份有数据是否要覆盖
     *
     * @param params 参数
     * @return 结果
     */
    @ApiOperation("拷贝前校验")
    @PostMapping("/docVulcanizationMachStatus/copyValidate")
    public AjaxResult copyValidate(@RequestParam Map<String, Object> params);

    /**
     * 导出基础数据-硫化机可用信息列表
     */
    @ApiOperation("导出基础数据-硫化机可用信息列表")
    @PostMapping("/docVulcanizationMachStatus/getList")
    List<MdmVulcanizingMachStatusVo> getList(@RequestBody MdmVulcanizingMachStatusVo docVulcanizingMachStatusVo);
//

    /**
     * 导入基础数据-硫化机可用信息数据
     */
    @ApiOperation("导入基础数据-硫化机可用信息")
    @PostMapping("/docVulcanizationMachStatus/importData/{updateSupport}/{importLogId}")
    public AjaxResult importData(@RequestBody List<MdmVulcanizingMachStatusVo> list, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("importLogId") Long importLogId);

}
