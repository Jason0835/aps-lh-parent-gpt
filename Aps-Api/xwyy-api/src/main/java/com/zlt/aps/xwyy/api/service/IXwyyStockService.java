package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.entity.XwyyStock;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 纤维压延库存信息对外暴露接口
 */
@FeignClient(contextId = "iXwyyStockService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyStockService {

    /**
     * 获取纤维压延库存信息列表
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock/list")
    TableDataInfo list(@RequestBody XwyyStock stock);

    /**
     * 删除纤维压延库存信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/stock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增纤维压延库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock")
    AjaxResult add(@Validated @RequestBody XwyyStock stock);


    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/selectStockById/{id}")
    XwyyStock selectStockById(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/stock/{id}")
    AjaxResult getInfo(@PathVariable("id") Long id);

    /**
     * 修改纤维压延库存信息
     *
     * @param stock
     * @return
     */
    @PutMapping("/stock")
    AjaxResult edit(@Validated @RequestBody XwyyStock stock);

    /**
     * 导出纤维压延库存信息
     *
     * @param stock
     * @return
     */
    @PostMapping("/stock/exportList")
    List<XwyyStock> exportList(@RequestBody XwyyStock stock);

    /**
     * 导入数据
     */
    @PostMapping("/stock/importData")
    public AjaxResult importData(@RequestBody List<XwyyStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);


}
