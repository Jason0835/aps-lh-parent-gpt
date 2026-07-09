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
import com.zlt.aps.nc.api.domain.entity.NcStock;

/**
 * 内衬库存信息对外暴露接口
 */
@FeignClient(contextId = "iNcStockRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcStockRemoteService {

    /**
     * 获取信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/stock/list")
    TableDataInfo list(@RequestBody NcStock stock);

    /**
     * 保存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/nc/stock/save")
    AjaxResult save(@Validated @RequestBody NcStock stock);

    /**
     * 删除信息
     *
     * @param ids
     * @return
     */
    @PostMapping("/nc/stock/remove")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/nc/stock/selectStockById/{id}")
    NcStock selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/nc/stock/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/nc/stock/checkUnique")
    String checkUnique(@RequestBody NcStock cxStock);

    /**
     * 导出信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/nc/stock/exportData/{fileName}")
    byte[] exportData(@RequestBody NcStock queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导如信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/nc/stock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
