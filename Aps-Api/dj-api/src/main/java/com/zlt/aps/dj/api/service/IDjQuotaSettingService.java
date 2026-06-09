package com.zlt.aps.dj.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.dj.api.domain.entity.DjQuotaSetting;

import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 垫胶定额设定Service接口
 * @author zlt
 * @date 2021-06-29
 */
@FeignClient(contextId = "INcQuotaSettingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.dj:nc}")
public interface IDjQuotaSettingService {


    /**
     * 查询垫胶定额设定列表
     */
    @PostMapping("/dj/quota/list")
    TableDataInfo list(@RequestBody DjQuotaSetting ncQuotaSetting);


    /**
    * 新增垫胶定额设定
    */
    @PostMapping("/dj/quota/add")
    AjaxResult add(@RequestBody DjQuotaSetting ncQuotaSetting);


    /**
     * 修改垫胶定额设定
     */
    @PostMapping("/dj/quota/edit")
    AjaxResult edit(@RequestBody DjQuotaSetting ncQuotaSetting);


    /**
     * 删除垫胶定额设定
     */
    @DeleteMapping("/dj/quota/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/dj/quota/{id}")
    DjQuotaSetting getInfo(@PathVariable("id") Long id);


    /**
     * 校验垫胶定额设定唯一性
     */
    @PostMapping("/dj/quota/checkNcQuotaSettingUnique")
    String checkNcQuotaSettingUnique(@RequestBody DjQuotaSetting ncQuotaSetting);


    /**
     * 导出垫胶定额设定列表
     */
    @PostMapping("/dj/quota/getList")
    List<DjQuotaSetting> getList(@RequestBody DjQuotaSetting ncQuotaSetting);

    @PostMapping("/dj/quota/importData")
    @ApiOperation("导入垫胶定额设定信息")
    public AjaxResult importData(@RequestBody List<DjQuotaSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
}
