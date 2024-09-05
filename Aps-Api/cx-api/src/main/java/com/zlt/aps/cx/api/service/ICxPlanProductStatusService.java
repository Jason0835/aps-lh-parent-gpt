package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxPlanProductStatus;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 成型计划投产状态Service接口
 *
 * @author zlt
 * @date 2021-07-21
 */
@FeignClient(contextId = "ICxPlanProductStatusService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxPlanProductStatusService {


    /**
     * 查询成型计划投产状态列表
     */
    @PostMapping("/productStatus/list")
    TableDataInfo list(@RequestBody CxPlanProductStatus cxPlanProductStatus);


    /**
     * 新增成型计划投产状态
     */
    @PostMapping("/productStatus/add")
    AjaxResult add(@RequestBody CxPlanProductStatus cxPlanProductStatus);


    /**
     * 修改成型计划投产状态
     */
    @PostMapping("/productStatus/edit")
    AjaxResult edit(@RequestBody CxPlanProductStatus cxPlanProductStatus);

    /**
     * 修改计划总量
     */
    @PostMapping("/productStatus/modifyQty")
    AjaxResult modifyQty(@RequestBody CxPlanProductStatus cxPlanProductStatus);

    /**
     * 删除成型计划投产状态
     */
    @DeleteMapping("/productStatus/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 标记不投产
     */
    @GetMapping("/productStatus/markUnProduct/{ids}")
    AjaxResult markUnProduct(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/productStatus/{id}")
    CxPlanProductStatus getInfo(@PathVariable("id") Long id);

    /**
     * 根据ID获取详细信息
     */
    @PostMapping(value = "/productStatus/getInfo2")
    CxPlanProductStatus getInfo2(@RequestBody CxPlanProductStatus cxPlanProductStatus);


    /**
     * 校验成型计划投产状态唯一性
     */
    @PostMapping("/productStatus/checkCxPlanProductStatusUnique")
    String checkCxPlanProductStatusUnique(@RequestBody CxPlanProductStatus cxPlanProductStatus);


    /**
     * 导出成型计划投产状态列表
     */
    @PostMapping("/productStatus/getList")
    List<CxPlanProductStatus> getList(@RequestBody CxPlanProductStatus cxPlanProductStatus);

    /**
     * 投产校验
     */
    @PostMapping("/productStatus/validateProduction")
    AjaxResult validateProduction(@RequestBody CxPlanProductStatus cxPlanProductStatus);

    /**
     * 修改投产表备注信息
     */
    @PostMapping("/productStatus/editRemark")
    AjaxResult editRemark(@RequestBody CxPlanProductStatus cxPlanProductStatus);

}
