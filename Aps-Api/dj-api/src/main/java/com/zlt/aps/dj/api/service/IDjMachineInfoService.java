package com.zlt.aps.dj.api.service;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.entity.DjMachineInfo;

/**
 * 垫胶机台信息对外暴露接口
 */
@FeignClient(contextId = "iDjMachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:dj}")
public interface IDjMachineInfoService {

    /**
     * 获取信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/dj/machine/list")
    TableDataInfo list(@RequestBody DjMachineInfo machine);

    /**
     * 保存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/dj/machine/save")
    AjaxResult save(@Validated @RequestBody DjMachineInfo machine);

    /**
     * 删除信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/dj/machine/{ids}")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/dj/machine/selectStockById/{id}")
    DjMachineInfo selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/dj/machine/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/dj/machine/checkUnique")
    String checkUnique(@RequestBody DjMachineInfo machine);

    /**
     * 导出信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/dj/machine/exportData/{fileName}")
    byte[] exportData(@RequestBody DjMachineInfo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/dj/machine/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
