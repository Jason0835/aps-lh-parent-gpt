package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import com.zlt.aps.lh.api.domain.entity.MixBadRubberStock;


/**
 * 不合格胶库存Service接口
 * @author zlt
 * @date 2021-11-08
 */
@FeignClient(contextId = "IMixBadRubberStockService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface IMixBadRubberStockService {

    /**
     * 查询不合格胶库存列表
     */
    @ApiOperation("查询不合格胶库存列表")
    @PostMapping("/badStock/list")
    TableDataInfo list(@RequestBody MixBadRubberStock mixBadRubberStock);

    /**
    * 新增不合格胶库存
    */
    @ApiOperation("新增不合格胶库存")
    @PostMapping("/badStock/add")
    AjaxResult add(@RequestBody MixBadRubberStock mixBadRubberStock);

    /**
     * 修改不合格胶库存
     */
    @ApiOperation("修改不合格胶库存")
    @PostMapping("/badStock/edit")
    AjaxResult edit(@RequestBody MixBadRubberStock mixBadRubberStock);

    /**
     * 删除不合格胶库存
     */
    @ApiOperation("删除不合格胶库存")
    @DeleteMapping("/badStock/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/badStock/{id}")
    MixBadRubberStock getInfo(@PathVariable("id") Long id);

    /**
     * 校验不合格胶库存唯一性
     */
    @ApiOperation("校验不合格胶库存唯一性")
    @PostMapping("/badStock/checkMixBadRubberStockUnique")
    String checkMixBadRubberStockUnique(@RequestBody MixBadRubberStock mixBadRubberStock);

    /**
     * 导出不合格胶库存列表
     */
    @ApiOperation("导出不合格胶库存列表")
    @PostMapping("/badStock/getList")
    List<MixBadRubberStock> getList(@RequestBody MixBadRubberStock mixBadRubberStock);

    /**
     * 导入不合格胶库存数据
     */
    @ApiOperation("导入不合格胶库存")
    @PostMapping("/badStock/importData")
    public AjaxResult importData(@RequestBody List<MixBadRubberStock> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
