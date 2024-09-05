package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxSpecifyMachine;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 定点机台Service接口
 *
 * @author zlt
 * @date 2021-07-21
 */
@FeignClient(contextId = "ICxSpecifyMachine1Service", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxSpecifyMachineService {


    /**
     * 查询定点机台列表
     */
    @PostMapping("/cxSpecifyMachine/list")
    TableDataInfo list(@RequestBody CxSpecifyMachine cxSpecifyMachine);


    /**
     * 新增定点机台
     */
    @PostMapping("/cxSpecifyMachine/add")
    AjaxResult add(@RequestBody CxSpecifyMachine cxSpecifyMachine);


    /**
     * 修改定点机台
     */
    @PostMapping("/cxSpecifyMachine/edit")
    AjaxResult edit(@RequestBody CxSpecifyMachine cxSpecifyMachine);


    /**
     * 删除定点机台
     */
    @DeleteMapping("/cxSpecifyMachine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/cxSpecifyMachine/{id}")
    CxSpecifyMachine getInfo(@PathVariable("id") Long id);


    /**
     * 校验定点机台唯一性
     */
    @PostMapping("/cxSpecifyMachine/checkCxSpecifyMachine1Unique")
    String checkCxSpecifyMachine1Unique(@RequestBody CxSpecifyMachine cxSpecifyMachine);


    /**
     * 导出定点机台列表
     */
    @PostMapping("/cxSpecifyMachine/getList")
    List<CxSpecifyMachine> getList(@RequestBody CxSpecifyMachine cxSpecifyMachine);

    /**
     * 导入数据
     */
    @PostMapping("/cxSpecifyMachine/importData")
    public AjaxResult importData(@RequestBody List<CxSpecifyMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);


}
