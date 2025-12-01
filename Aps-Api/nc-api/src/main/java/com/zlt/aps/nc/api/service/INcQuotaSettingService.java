package com.zlt.aps.nc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.entity.NcQuotaSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 内衬定额设定Service接口
 * @author zlt
 * @date 2021-06-29
 */
@FeignClient(contextId = "INcQuotaSettingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcQuotaSettingService {


    /**
     * 查询内衬定额设定列表
     */
    @PostMapping("/nc/quota/list")
    TableDataInfo list(@RequestBody NcQuotaSetting ncQuotaSetting);


    /**
    * 新增内衬定额设定
    */
    @PostMapping("/nc/quota/add")
    AjaxResult add(@RequestBody NcQuotaSetting ncQuotaSetting);


    /**
     * 修改内衬定额设定
     */
    @PostMapping("/nc/quota/edit")
    AjaxResult edit(@RequestBody NcQuotaSetting ncQuotaSetting);


    /**
     * 删除内衬定额设定
     */
    @DeleteMapping("/nc/quota/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/nc/quota/{id}")
    NcQuotaSetting getInfo(@PathVariable("id") Long id);


    /**
     * 校验内衬定额设定唯一性
     */
    @PostMapping("/nc/quota/checkNcQuotaSettingUnique")
    String checkNcQuotaSettingUnique(@RequestBody NcQuotaSetting ncQuotaSetting);


    /**
     * 导出内衬定额设定列表
     */
    @PostMapping("/nc/quota/getList")
    List<NcQuotaSetting> getList(@RequestBody NcQuotaSetting ncQuotaSetting);

    @PostMapping("/nc/quota/importData")
    @ApiOperation("导入内衬定额设定信息")
    public AjaxResult importData(@RequestBody List<NcQuotaSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
