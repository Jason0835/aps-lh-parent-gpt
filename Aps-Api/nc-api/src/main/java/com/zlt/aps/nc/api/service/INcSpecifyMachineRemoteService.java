package com.zlt.aps.nc.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.entity.NcSpecifyMachine;

/**
 * 内衬定点机台对外暴露接口
 */
@FeignClient(contextId = "INcSpecifyMachineRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.nc:nc}")
public interface INcSpecifyMachineRemoteService {

    /**
     * 获取信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/specifyMachine/list")
    TableDataInfo list(@RequestBody NcSpecifyMachine machine);

    /**
     * 保存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/specifyMachine/save")
    AjaxResult save(@Validated @RequestBody NcSpecifyMachine machine);

    /**
     * 删除信息
     *
     * @param ids
     * @return
     */
    @PostMapping("/nc/specifyMachine/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/nc/specifyMachine/selectStockById/{id}")
    NcSpecifyMachine selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/nc/specifyMachine/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/nc/specifyMachine/checkUnique")
    String checkUnique(@RequestBody NcSpecifyMachine machine);

    /**
     * 导出信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/nc/specifyMachine/exportData/{fileName}")
    byte[] exportData(@RequestBody NcSpecifyMachine queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/nc/specifyMachine/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
