package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.dto.Cd90MachineRollMappingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 90度裁断帘布大卷与机台的映射表对外暴露接口
 * </p>
 *
 * @author duanjuntao
 * @since 2021-06-16
 */
@FeignClient(contextId = "Cd90MachineRollMappingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:cd90}")
public interface ICd90MachineRollMappingService {
    /**
     * 根据条件查询90度裁断帘布大卷与机台的映射表
     */
    @PostMapping("/cd90/MachineRollMapping/listMachineRollMapping")
    TableDataInfo listMachineRollMapping(@RequestBody Cd90MachineRollMappingDto dto);

    /**
     * 根据id查询90度裁断帘布大卷与机台的映射表
     */
    @GetMapping("/cd90/MachineRollMapping/getMachineRollMapping/{id}")
    Cd90MachineRollMappingDto getBigRollColor(@PathVariable("id") Long id);

    /**
     * 保存90度裁断帘布大卷与机台的映射表（id为空则新增，id不为空则修改）
     */
    @PostMapping("/cd90/MachineRollMapping/saveMachineRollMapping")
    AjaxResult saveMachineRollMapping(@RequestBody Cd90MachineRollMappingDto dto);

    /**
     * 保存帘90度裁断帘布大卷与机台的映射表（id为空则新增，id不为空则修改）
     */
    @PostMapping("/cd90/MachineRollMapping/checkMachineRollMapping")
    String checkMachineRollMapping(@RequestBody Cd90MachineRollMappingDto dto);

    /**
     * 批量删除帘布大卷与机台的映射表(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/cd90/MachineRollMapping/deleteMachineRollMapping/{ids}")
    AjaxResult deleteMachineRollMapping(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     *
     * @param dto
     */
    @PostMapping("/cd90/MachineRollMapping/exportData")
    List<Cd90MachineRollMappingDto> exportData(@RequestBody Cd90MachineRollMappingDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/cd90/MachineRollMapping/importData")
    public AjaxResult importData(@RequestBody List<Cd90MachineRollMappingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/cd90/MachineRollMapping/deleteAll")
    AjaxResult deleteAll();

}
