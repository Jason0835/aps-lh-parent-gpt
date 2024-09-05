package com.zlt.aps.gsq.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.dto.GsqLossSettingDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 钢丝圈损耗率设定Service接口
 * @author chen
 * @date 2021-07-13
 */
@FeignClient(contextId = "IGsqLossSettingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqLossSettingService {

    /**
     * 查询钢丝圈损耗率设定列表
     */
    @PostMapping("/loss/list")
    TableDataInfo list(@RequestBody GsqLossSettingDto dto);

    /**
    * 新增钢丝圈损耗率设定
    */
    @PostMapping("/loss/add")
    AjaxResult add(@RequestBody GsqLossSettingDto dto);

    /**
     * 修改钢丝圈损耗率设定
     */
    @PostMapping("/loss/edit")
    AjaxResult edit(@RequestBody GsqLossSettingDto dto);

    /**
     * 删除钢丝圈损耗率设定
     */
    @DeleteMapping("/loss/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/loss/{id}")
    GsqLossSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验钢丝圈损耗率设定唯一性
     */
    @PostMapping("/loss/checkGsqLossSettingUnique")
    String checkGsqLossSettingUnique(@RequestBody GsqLossSettingDto dto);

    /**
     * 导出钢丝圈损耗率设定列表
     */
    @PostMapping("/loss/getList")
    List<GsqLossSettingDto> getList(@RequestBody GsqLossSettingDto dto);

    @PostMapping("/loss/importData")
    @ApiOperation("导入钢丝圈损耗率信息")
    public AjaxResult importData(@RequestBody List<GsqLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/loss/deleteAll")
    AjaxResult deleteAll();
}
