package com.zlt.aps.gdyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gdyy.api.domain.dto.GdyyLossSettingDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 钢带压延损耗率设定Service接口
 * @author chen
 * @date 2021-07-19
 */
@FeignClient(contextId = "IGdyyLossSettingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gdyy:gdyy}")
public interface IGdyyLossSettingService {

    /**
     * 查询钢带压延损耗率设定列表
     */
    @PostMapping("/gdyy/loss/list")
    TableDataInfo list(@RequestBody GdyyLossSettingDto dto);

    /**
    * 新增钢带压延损耗率设定
    */
    @PostMapping("/gdyy/loss/add")
    AjaxResult add(@RequestBody GdyyLossSettingDto dto);

    /**
     * 修改钢带压延损耗率设定
     */
    @PostMapping("/gdyy/loss/edit")
    AjaxResult edit(@RequestBody GdyyLossSettingDto dto);

    /**
     * 删除钢带压延损耗率设定
     */
    @DeleteMapping("/gdyy/loss/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/gdyy/loss/{id}")
    GdyyLossSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验钢带压延损耗率设定唯一性
     */
    @PostMapping("/gdyy/loss/checkGdyyLossSettingUnique")
    String checkGdyyLossSettingUnique(@RequestBody GdyyLossSettingDto dto);

    /**
     * 导出钢带压延损耗率设定列表
     */
    @PostMapping("/gdyy/loss/getList")
    List<GdyyLossSettingDto> getList(@RequestBody GdyyLossSettingDto dto);

    @PostMapping("/gdyy/loss/importData")
    @ApiOperation("导入钢带压延损耗率设定")
    public AjaxResult importData(@RequestBody List<GdyyLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/gdyy/loss/deleteAll")
    AjaxResult deleteAll();

}
