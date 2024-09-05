package com.zlt.aps.tq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tq.api.domain.entity.TqQuotaSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 胎圈定额设定Service接口
 * @author zlt
 * @date 2021-06-29
 */
@FeignClient(contextId = "ITqQuotaSettingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tq:tq}")
public interface ITqQuotaSettingService {


    /**
     * 查询胎圈定额设定列表
     */
    @PostMapping("/quota/list")
    TableDataInfo list(@RequestBody TqQuotaSetting tqQuotaSetting);


    /**
    * 新增胎圈定额设定
    */
    @PostMapping("/quota/add")
    AjaxResult add(@RequestBody TqQuotaSetting tqQuotaSetting);


    /**
     * 修改胎圈定额设定
     */
    @PostMapping("/quota/edit")
    AjaxResult edit(@RequestBody TqQuotaSetting tqQuotaSetting);


    /**
     * 删除胎圈定额设定
     */
    @DeleteMapping("/quota/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/quota/{id}")
    TqQuotaSetting getInfo(@PathVariable("id") Long id);


    /**
     * 校验胎圈定额设定唯一性
     */
    @PostMapping("/quota/checkTqQuotaSettingUnique")
    String checkTqQuotaSettingUnique(@RequestBody TqQuotaSetting tqQuotaSetting);


    /**
     * 导出胎圈定额设定列表
     */
    @PostMapping("/quota/getList")
    List<TqQuotaSetting> getList(@RequestBody TqQuotaSetting tqQuotaSetting);

    @PostMapping("/quota/importData")
    @ApiOperation("导入胎圈定额设定信息")
    public AjaxResult importData(@RequestBody List<TqQuotaSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
