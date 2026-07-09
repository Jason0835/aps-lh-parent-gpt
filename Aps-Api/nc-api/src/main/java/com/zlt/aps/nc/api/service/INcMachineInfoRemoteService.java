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
import com.zlt.aps.nc.api.domain.entity.NcMachineInfo;

/**
 * 内衬机台信息对外暴露接口
 */
@FeignClient(contextId = "INcMachineInfoRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcMachineInfoRemoteService {

    /**
     * 获取信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/machine/list")
    TableDataInfo list(@RequestBody NcMachineInfo machine);

    /**
     * 保存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/machine/save")
    AjaxResult save(@Validated @RequestBody NcMachineInfo machine);

    /**
     * 删除信息
     *
     * @param ids
     * @return
     */
    @PostMapping("/nc/machine/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/nc/machine/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/nc/machine/checkUnique")
    String checkUnique(@RequestBody NcMachineInfo machine);

    /**
     * 导出信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/nc/machine/exportData/{fileName}")
    byte[] exportData(@RequestBody NcMachineInfo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/nc/machine/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
