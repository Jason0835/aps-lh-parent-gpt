package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachine;
import com.zlt.aps.cx.api.domain.entity.CxMatchingSpecifyMachineList;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 定点机台信息对外暴露接口
 */
@FeignClient(contextId = "iCxSpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxMatchingSpecifyMachineService {

    /**
     * 获取定点机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/specifyMachine/list")
    TableDataInfo list(@RequestBody CxMatchingSpecifyMachine machineInfo);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/specifyMachine/{id}")
    CxMatchingSpecifyMachine getInfo(@PathVariable("id") Long id);

    /**
     * 新增定点机台信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/specifyMachine")
    AjaxResult add(@Validated @RequestBody CxMatchingSpecifyMachine machineInfo);

    /**
     * 修改定点机台信息
     *
     * @param machineInfo
     * @return
     */
    @PutMapping("/specifyMachine")
    AjaxResult edit(@Validated @RequestBody CxMatchingSpecifyMachine machineInfo);

    /**
     * 删除定点机台信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/specifyMachine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 校验定点机台唯一性
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/specifyMachine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody CxMatchingSpecifyMachine machineInfo);

    /**
     * 导出定点机台列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/specifyMachine/exportList")
    List<CxMatchingSpecifyMachine> exportList(@RequestBody CxMatchingSpecifyMachine machineInfo);

    /**
     * 导入数据
     */
    @PostMapping("/specifyMachine/importData")
    public AjaxResult importData(@RequestBody List<CxMatchingSpecifyMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);


    @PostMapping("/specifyMachine/getDetailById")
    TableDataInfo getDetailById(@RequestBody CxMatchingSpecifyMachine machineInfo);

    @PostMapping("/specifyMachine/detailList")
    TableDataInfo detailList(@RequestBody CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList);

    @GetMapping(value = "/specifyMachine/detail/{id}")
    CxMatchingSpecifyMachineList getDetailInfo(@PathVariable("id") Long id);

    @PostMapping("/specifyMachine/detail/add")
    AjaxResult detailAdd(@RequestBody CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList);

    @PostMapping("/specifyMachine/detail/edit")
    AjaxResult detailEdit(@RequestBody CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList);

    @DeleteMapping("/specifyMachine/detail/{ids}")
    AjaxResult detailRemove(@PathVariable("ids") Long[] ids);

    @PostMapping("/specifyMachine/detail/exportList")
    List<CxMatchingSpecifyMachineList> detailExport(@RequestBody CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList);

    /**
     * 导入数据
     */
    @PostMapping("/specifyMachine/detail/detailImportData")
    public AjaxResult detailImportData(@RequestBody List<CxMatchingSpecifyMachineList> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    @PostMapping("/specifyMachine/detail/viewList")
    List<CxMatchingSpecifyMachineList> viewList(@RequestBody CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList);


}
