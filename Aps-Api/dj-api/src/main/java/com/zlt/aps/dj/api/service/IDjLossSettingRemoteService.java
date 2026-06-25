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
import com.zlt.aps.dj.api.domain.entity.DjLossSetting;


/**
 * 垫胶损耗率设定Service接口
 *
 * @author zlt
 * @date 2026-06-10
 */
@FeignClient(contextId = "IDjLossSettingRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:dj}")
public interface IDjLossSettingRemoteService {

    /**
     * 获取信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/dj/lossSetting/list")
    TableDataInfo list(@RequestBody DjLossSetting lossSetting);

    /**
     * 保存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/dj/lossSetting/save")
    AjaxResult save(@Validated @RequestBody DjLossSetting lossSetting);

    /**
     * 删除信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/dj/lossSetting/{ids}")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/dj/lossSetting/selectStockById/{id}")
    DjLossSetting selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/dj/lossSetting/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/dj/lossSetting/checkUnique")
    String checkUnique(@RequestBody DjLossSetting lossSetting);

    /**
     * 导出信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/dj/lossSetting/exportData/{fileName}")
    byte[] exportData(@RequestBody DjLossSetting queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/dj/lossSetting/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
