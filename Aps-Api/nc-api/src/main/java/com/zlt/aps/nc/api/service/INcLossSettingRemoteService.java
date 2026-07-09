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
import com.zlt.aps.nc.api.domain.entity.NcLossSetting;


/**
 * 内衬损耗率设定Service接口
 *
 * @author zlt
 * @date 2026-06-10
 */
@FeignClient(contextId = "INcLossSettingRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcLossSettingRemoteService {

    /**
     * 获取信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/lossSetting/list")
    TableDataInfo list(@RequestBody NcLossSetting lossSetting);

    /**
     * 保存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/lossSetting/save")
    AjaxResult save(@Validated @RequestBody NcLossSetting lossSetting);

    /**
     * 删除信息
     *
     * @param ids
     * @return
     */
    @PostMapping("/nc/lossSetting/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/nc/lossSetting/selectStockById/{id}")
    NcLossSetting selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/nc/lossSetting/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/nc/lossSetting/checkUnique")
    String checkUnique(@RequestBody NcLossSetting lossSetting);

    /**
     * 导出信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/nc/lossSetting/exportData/{fileName}")
    byte[] exportData(@RequestBody NcLossSetting queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/nc/lossSetting/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
