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
import com.zlt.aps.dj.api.domain.entity.DjCurlRoll;


/**
 * 垫胶卷曲信息维护Service接口
 *
 * @author zlt
 * @date 2026-06-10
 */
@FeignClient(contextId = "IDjCurlRollRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:dj}")
public interface IDjCurlRollRemoteService {

    /**
     * 获取信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/dj/curlRoll/list")
    TableDataInfo list(@RequestBody DjCurlRoll curlRoll);

    /**
     * 保存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/dj/curlRoll/save")
    AjaxResult save(@Validated @RequestBody DjCurlRoll curlRoll);

    /**
     * 删除信息
     *
     * @param ids
     * @return
     */
    @PostMapping("/dj/curlRoll/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/dj/curlRoll/selectStockById/{id}")
    DjCurlRoll selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/dj/curlRoll/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/dj/curlRoll/checkUnique")
    String checkUnique(@RequestBody DjCurlRoll curlRoll);

    /**
     * 导出信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/dj/curlRoll/exportData/{fileName}")
    byte[] exportData(@RequestBody DjCurlRoll queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/dj/curlRoll/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}