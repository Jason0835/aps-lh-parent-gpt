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
import com.zlt.aps.dj.api.domain.entity.DjSpecifyMachine;

/**
 * 垫胶定点机台对外暴露接口
 */
@FeignClient(contextId = "iDjSpecifyMachineRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path="${api.path.dj:dj}")
public interface IDjSpecifyMachineRemoteService {

    /**
     * 获取信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/dj/specifyMachine/list")
    TableDataInfo list(@RequestBody DjSpecifyMachine machine);

    /**
     * 保存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/dj/specifyMachine/save")
    AjaxResult save(@Validated @RequestBody DjSpecifyMachine machine);

    /**
     * 删除信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/dj/specifyMachine/{ids}")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/dj/specifyMachine/selectStockById/{id}")
    DjSpecifyMachine selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/dj/specifyMachine/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/dj/specifyMachine/checkUnique")
    String checkUnique(@RequestBody DjSpecifyMachine machine);

    /**
     * 导出信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/dj/specifyMachine/exportData/{fileName}")
    byte[] exportData(@RequestBody DjSpecifyMachine queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/dj/specifyMachine/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
