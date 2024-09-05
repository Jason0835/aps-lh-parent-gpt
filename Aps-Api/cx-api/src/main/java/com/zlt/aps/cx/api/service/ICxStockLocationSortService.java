package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.CxStockLocationSortDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 库存地点生产顺序Service接口
 *
 * @author chen
 * @date 2021-07-22
 */
@FeignClient(contextId = "ICxStockLocationSortService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxStockLocationSortService {

    /**
     * 查询库存地点生产顺序列表
     */
    @PostMapping("/stockLocationSort/list")
    TableDataInfo list(@RequestBody CxStockLocationSortDto dto);

    /**
     * 新增库存地点生产顺序
     */
    @PostMapping("/stockLocationSort/add")
    AjaxResult add(@RequestBody CxStockLocationSortDto dto);

    /**
     * 修改库存地点生产顺序
     */
    @PostMapping("/stockLocationSort/edit")
    AjaxResult edit(@RequestBody CxStockLocationSortDto dto);

    /**
     * 删除库存地点生产顺序
     */
    @DeleteMapping("/stockLocationSort/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/stockLocationSort/{id}")
    CxStockLocationSortDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验库存地点生产顺序唯一性
     */
    @PostMapping("/stockLocationSort/checkCxStockLocationSortUnique")
    String checkCxStockLocationSortUnique(@RequestBody CxStockLocationSortDto dto);

    /**
     * 导出库存地点生产顺序列表
     */
    @PostMapping("/stockLocationSort/getList")
    List<CxStockLocationSortDto> getList(@RequestBody CxStockLocationSortDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/stockLocationSort/importData")
    public AjaxResult importData(@RequestBody List<CxStockLocationSortDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
