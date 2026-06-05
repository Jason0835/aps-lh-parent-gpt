package com.zlt.aps.cd15.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd15.api.domain.dto.Cd15LossSettingDto;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 15度裁断损耗率设定Service接口
 * @author chen
 * @date 2021-07-19
 */
@FeignClient(contextId = "ICd15LossSettingService", value =ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd15:cd15}")
public interface ICd15LossSettingService {

    /**
     * 查询15度裁断损耗率设定列表
     */
    @PostMapping("/cd15/loss/list")
    TableDataInfo list(@RequestBody Cd15LossSettingDto dto);

    /**
    * 新增15度裁断损耗率设定
    */
    @PostMapping("/cd15/loss/add")
    AjaxResult add(@RequestBody Cd15LossSettingDto dto);

    /**
     * 修改15度裁断损耗率设定
     */
    @PostMapping("/cd15/loss/edit")
    AjaxResult edit(@RequestBody Cd15LossSettingDto dto);

    /**
     * 删除15度裁断损耗率设定
     */
    @DeleteMapping("/cd15/loss/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/cd15/loss/{id}")
    Cd15LossSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验15度裁断损耗率设定唯一性
     */
    @PostMapping("/cd15/loss/checkCd15LossSettingUnique")
    String checkCd15LossSettingUnique(@RequestBody Cd15LossSettingDto dto);

    /**
     * 导出15度裁断损耗率设定列表
     */
    @PostMapping("/cd15/loss/getList")
    List<Cd15LossSettingDto> getList(@RequestBody Cd15LossSettingDto dto);

    @PostMapping("/cd15/loss/importData")
    @ApiOperation("导入15度裁断损耗率信息")
    public AjaxResult importData(@RequestBody List<Cd15LossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/cd15/loss/deleteAll")
    AjaxResult deleteAll();

}
