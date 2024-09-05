package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqQuotaSetting;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 钢丝圈定额设定Service接口
 * @author zlt
 * @date 2021-06-29
 */
@FeignClient(contextId = "IGsqQuotaSettingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqQuotaSettingService {


    /**
     * 查询钢丝圈定额设定列表
     */
    @PostMapping("/quota/list")
    TableDataInfo list(@RequestBody GsqQuotaSetting gsqQuotaSetting);


    /**
    * 新增钢丝圈定额设定
    */
    @PostMapping("/quota/add")
    AjaxResult add(@RequestBody GsqQuotaSetting gsqQuotaSetting);


    /**
     * 修改钢丝圈定额设定
     */
    @PostMapping("/quota/edit")
    AjaxResult edit(@RequestBody GsqQuotaSetting gsqQuotaSetting);


    /**
     * 删除钢丝圈定额设定
     */
    @DeleteMapping("/quota/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/quota/{id}")
    GsqQuotaSetting getInfo(@PathVariable("id") Long id);


    /**
     * 校验钢丝圈定额设定唯一性
     */
    @PostMapping("/quota/checkGsqQuotaSettingUnique")
    String checkGsqQuotaSettingUnique(@RequestBody GsqQuotaSetting gsqQuotaSetting);


    /**
     * 导出钢丝圈定额设定列表
     */
    @PostMapping("/quota/getList")
    List<GsqQuotaSetting> getList(@RequestBody GsqQuotaSetting gsqQuotaSetting);

    @PostMapping("/quota/importData")
    @ApiOperation("导入钢丝圈定额设定信息")
    public AjaxResult importData(@RequestBody List<GsqQuotaSetting> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
