package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.LhMachineInfoDto;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 成型机台对外暴露接口
 *
 * @author Joran.Zhang
 */
@FeignClient(contextId = "cxMachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxMachineInfoService {
    /**
     * 查询成型机台信息列表
     */
    @PostMapping("/cx/machine/list")
    public TableDataInfo list(@RequestBody CxMachineInfo cxMachineInfo);

    @PostMapping("/cx/machine/listOrderByName")
    public List<CxMachineInfo> listOrderByName(@RequestBody CxMachineInfo cxMachineInfo);

    /**
     * 获取成型机台信息详细信息
     */
    @GetMapping("/cx/machine/{id}")
    public CxMachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 修改成型机台信息
     */
    @PostMapping("/cx/machine/edit")
    public AjaxResult edit(@RequestBody CxMachineInfo cxMachineInfo);

    /**
     * 新增成型机台信息
     */
    @PostMapping("/cx/machine/add")
    public AjaxResult add(@Validated @RequestBody CxMachineInfo cxMachineInfo);

    /**
     * 更新机台状态
     *
     * @param cxMachineInfo
     * @return
     */
    @PostMapping("/cx/machine/updateStatus")
    public AjaxResult updateStatus(@RequestBody CxMachineInfo cxMachineInfo);

    /**
     * 删除胎面机台信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/cx/machine/remove/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 校验胎面机台唯一性
     *
     * @param cxMachineInfo
     * @return
     */
    @PostMapping("/cx/machine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody CxMachineInfo cxMachineInfo);

    /**
     * 导出内衬机台列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/cx/machine/exportList")
    List<CxMachineInfo> exportList(@RequestBody CxMachineInfo machineInfo);

    /**
     * 获取其他半部件机台列表
     */
    @PostMapping("/cx/machine/getOrtherMachineInfo")
    public List<CxMachineInfo> getOrtherMachineInfo(@RequestBody CxMachineInfo cxMachineInfo);

    /**
     * 硫化机台下拉列表
     */
    @PostMapping("/cx/machine/getLhMachineForQty")
    public TableDataInfo getLhMachineForQty(@RequestBody LhMachineInfoDto lhMachineInfoDto);


    @PostMapping("/cx/machine/list2")
    public List<CxMachineInfo> list2(@RequestBody CxMachineInfo cxMachineInfo);

    /**
     * 导入数据
     */
    @PostMapping("/cx/machine/importData")
    public AjaxResult importData(@RequestBody List<CxMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
