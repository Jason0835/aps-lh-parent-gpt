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
import com.zlt.aps.dj.api.domain.entity.DjStock;

/**
 * 垫胶库存信息对外暴露接口
 */
@FeignClient(contextId = "iDjStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:dj}")
public interface IDjStockService {

    /**
     * 获取垫胶库存信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/dj/stock/list")
    TableDataInfo list(@RequestBody DjStock stock);

    /**
     * 保存垫胶库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/dj/stock/save")
    AjaxResult save(@Validated @RequestBody DjStock stock);

    /**
     * 删除垫胶库存信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/dj/stock/{ids}")
    AjaxResult removeByIds(@RequestBody List<Long> ids);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/dj/stock/selectStockById/{id}")
    DjStock selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/dj/stock/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 校验唯一性
     */
    @PostMapping("/dj/stock/checkUnique")
    String checkUnique(@RequestBody DjStock cxStock);

    /**
     * 导出垫胶库存信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/dj/stock/exportData/{fileName}")
    byte[] exportData(@RequestBody DjStock queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导如垫胶库存信息
     * 
     * @param stock
     * @return
     */
    @PostMapping("/dj/stock/importData")
    AjaxResult importData(@RequestBody ImportContext importContext,
            @RequestParam("updateSupport") boolean updateSupport);
}
