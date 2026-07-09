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
import com.zlt.aps.nc.api.domain.entity.NcCurlRoll;


/**
 * 内衬卷曲信息维护Service接口
 *
 * @author zlt
 * @date 2026-06-10
 */
@FeignClient(contextId = "INcCurlRollRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcCurlRollRemoteService {

    /**
     * 获取信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/curlRoll/list")
    TableDataInfo list(@RequestBody NcCurlRoll curlRoll);

    /**
     * 保存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/curlRoll/save")
    AjaxResult save(@Validated @RequestBody NcCurlRoll curlRoll);

    /**
     * 删除信息
     *
     * @param ids
     * @return
     */
    @PostMapping("/nc/curlRoll/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/nc/curlRoll/selectStockById/{id}")
    NcCurlRoll selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/nc/curlRoll/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/nc/curlRoll/checkUnique")
    String checkUnique(@RequestBody NcCurlRoll curlRoll);

    /**
     * 导出信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/curlRoll/exportData/{fileName}")
    byte[] exportData(@RequestBody NcCurlRoll queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/curlRoll/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
