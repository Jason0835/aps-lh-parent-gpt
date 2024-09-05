package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import io.swagger.annotations.ApiOperation;

import com.zlt.aps.cx.api.domain.entity.CxMachineGroupList;


/**
 * 组别机台列Service接口
 * @author zlt
 * @date 2021-12-16
 */
@FeignClient(contextId = "ICxMachineGroupListService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxMachineGroupListService {

    /**
     * 查询组别机台列列表
     */
    @ApiOperation("查询组别机台列列表")
    @PostMapping("/groupMachineList/list")
    TableDataInfo list(@RequestBody CxMachineGroupList cxMachineGroupList);

    /**
    * 新增组别机台列
    */
    @ApiOperation("新增组别机台列")
    @PostMapping("/groupMachineList/add")
    AjaxResult add(@RequestBody CxMachineGroupList cxMachineGroupList);

    /**
     * 修改组别机台列
     */
    @ApiOperation("修改组别机台列")
    @PostMapping("/groupMachineList/edit")
    AjaxResult edit(@RequestBody CxMachineGroupList cxMachineGroupList);

    /**
     * 删除组别机台列
     */
    @ApiOperation("删除组别机台列")
    @DeleteMapping("/groupMachineList/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/groupMachineList/{id}")
    CxMachineGroupList getInfo(@PathVariable("id") Long id);

    /**
     * 校验组别机台列唯一性
     */
    @ApiOperation("校验组别机台列唯一性")
    @PostMapping("/groupMachineList/checkCxMachineGroupListUnique")
    List<CxMachineGroupList> checkCxMachineGroupListUnique(@RequestBody CxMachineGroupList cxMachineGroupList);

    /**
     * 导出组别机台列列表
     */
    @ApiOperation("导出组别机台列列表")
    @PostMapping("/groupMachineList/getList")
    List<CxMachineGroupList> getList(@RequestBody CxMachineGroupList cxMachineGroupList);

    /**
     * 导入组别机台列数据
     */
    @ApiOperation("导入组别机台列")
    @PostMapping("/groupMachineList/importData")
    public AjaxResult importData(@RequestBody List<CxMachineGroupList> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
