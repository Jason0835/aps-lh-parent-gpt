package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhSpecifyMachine;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 硫化定点机台信息Service接口
 *
 * @author zlt
 * @date 2021-07-21
 */
@FeignClient(contextId = "ILhSpecifyMachineService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface ILhSpecifyMachineService {


    /**
     * 查询硫化定点机台信息列表
     */
    @PostMapping("/lhSpecifyMachine/list")
    TableDataInfo list(@RequestBody LhSpecifyMachine lhSpecifyMachine);


    /**
     * 新增硫化定点机台信息
     */
    @PostMapping("/lhSpecifyMachine/add")
    AjaxResult add(@RequestBody LhSpecifyMachine lhSpecifyMachine);


    /**
     * 修改硫化定点机台信息
     */
    @PostMapping("/lhSpecifyMachine/edit")
    AjaxResult edit(@RequestBody LhSpecifyMachine lhSpecifyMachine);


    /**
     * 删除硫化定点机台信息
     */
    @DeleteMapping("/lhSpecifyMachine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/lhSpecifyMachine/{id}")
    LhSpecifyMachine getInfo(@PathVariable("id") Long id);


    /**
     * 校验硫化定点机台信息唯一性
     */
    @PostMapping("/lhSpecifyMachine/checkLhSpecifyMachineUnique")
    String checkLhSpecifyMachineUnique(@RequestBody LhSpecifyMachine lhSpecifyMachine);


    /**
     * 导出硫化定点机台信息列表
     */
    @PostMapping("/lhSpecifyMachine/getList")
    List<LhSpecifyMachine> getList(@RequestBody LhSpecifyMachine lhSpecifyMachine);

    /**
     * 导入数据
     */
    @PostMapping("/lhSpecifyMachine/importData")
    public AjaxResult importData(@RequestBody List<LhSpecifyMachine> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);


}
