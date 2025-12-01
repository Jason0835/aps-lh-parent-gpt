package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmQuotaSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 胎面定额设定Service接口
 * @author zlt
 * @date 2021-06-28
 */
@FeignClient(contextId = "ITmQuotaSettingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:tm}")
public interface ITmQuotaSettingService {


    /**
     * 查询胎面定额设定列表
     */
    @PostMapping("/tm/quota/list")
    TableDataInfo list(@RequestBody TmQuotaSetting tmQuotaSetting);


    /**
    * 新增胎面定额设定
    */
    @PostMapping("/tm/quota/add")
    AjaxResult add(@RequestBody TmQuotaSetting tmQuotaSetting);


    /**
     * 修改胎面定额设定
     */
    @PostMapping("/tm/quota/edit")
    AjaxResult edit(@RequestBody TmQuotaSetting tmQuotaSetting);


    /**
     * 删除胎面定额设定
     */
    @DeleteMapping("/tm/quota/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/tm/quota/{id}")
    TmQuotaSetting getInfo(@PathVariable("id") Long id);


    /**
     * 校验胎面定额设定唯一性
     */
    @PostMapping("/tm/quota/checkTmQuotaSettingUnique")
    String checkTmQuotaSettingUnique(@RequestBody TmQuotaSetting tmQuotaSetting);


    /**
     * 导出胎面定额设定列表
     */
    @PostMapping("/tm/quota/getList")
    List<TmQuotaSetting> getList(@RequestBody TmQuotaSetting tmQuotaSetting);

    @PostMapping("/tm/quota/importData")
    @ApiOperation("导入胎面定额设定信息")
    public AjaxResult importData(@RequestBody List<TmQuotaSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
