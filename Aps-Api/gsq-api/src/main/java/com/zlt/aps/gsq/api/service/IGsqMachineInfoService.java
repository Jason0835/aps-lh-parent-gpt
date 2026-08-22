package com.zlt.aps.gsq.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 钢丝圈机台信息对外暴露接口
 */
@FeignClient(contextId = "iGsqMachineInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.gsq:gsq}")
public interface IGsqMachineInfoService {

    /**
     * 获取钢丝圈机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/gsq/machine/list")
    TableDataInfo list(@RequestBody GsqMachineInfo machineInfo);

    /**
     * 删除钢丝圈机台信息
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/gsq/machine/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 新增钢丝圈机台信息
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/gsq/machine")
    AjaxResult add(@Validated @RequestBody GsqMachineInfo machineInfo);

    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/gsq/machine/{id}")
    GsqMachineInfo getInfo(@PathVariable("id") Long id);

    /**
     * 修改钢丝圈机台信息
     *
     * @param machineInfo
     * @return
     */
    @PutMapping("/gsq/machine")
    AjaxResult edit(@Validated @RequestBody GsqMachineInfo machineInfo);

    /**
     * 校验钢丝圈机台唯一性
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/gsq/machine/checkMachineCodeUnique")
    String checkMachineCodeUnique(@Validated @RequestBody GsqMachineInfo machineInfo);

    /**
     * 导出钢丝圈机台信息
     * 返回byte[]文件字节流，避免Gateway将List响应包装为统一响应体导致Feign反序列化异常
     *
     * @param entity   查询条件
     * @param fileName 导出文件名
     * @return Excel文件字节流
     */
    @PostMapping("/gsq/machine/exportData/{fileName}")
    @ApiOperation("导出钢丝圈机台信息")
    byte[] exportData(@RequestBody GsqMachineInfo entity, @PathVariable("fileName") String fileName);

    /**
     * 获取钢丝圈机台信息列表
     *
     * @param machineInfo
     * @return
     */
    @PostMapping("/gsq/machine/listMachineInfo")
    List<GsqMachineInfo> listMachineInfo(@RequestBody GsqMachineInfo machineInfo);

    /**
     * 获取所有启用的钢丝圈机台信息（status=0），供下拉框数据源使用
     * 返回AjaxResult，因为Gateway会将响应包装为统一响应体对象，
     * 若声明为List会导致Feign反序列化时MismatchedInputException
     *
     * @return 启用状态的机台列表（包装在AjaxResult中）
     */
    @GetMapping("/gsq/machine/listEnabledMachines")
    AjaxResult listEnabledMachines();

    /**
     * 导入钢丝圈机台信息（基于ImportContext的标准导入接口，供UIController调用）
     *
     * @param importContext 导入上下文（含文件字节、原始文件名等）
     * @param updateSupport 是否支持更新已有数据
     * @return 导入结果
     */
    @PostMapping("/gsq/machine/importData")
    @ApiOperation("导入钢丝圈机台信息")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 导入钢丝圈机台信息（Feign兼容旧接口，基于已解析的实体列表）
     *
     * @param list          待导入的机台信息列表
     * @param updateSupport 是否支持更新已有数据
     * @param importLogId   导入日志ID
     * @return 导入结果
     */
    @PostMapping("/gsq/machine/importDataFeign")
    @ApiOperation("导入钢丝圈机台信息（Feign兼容）")
    AjaxResult importDataFeign(@RequestBody List<GsqMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
