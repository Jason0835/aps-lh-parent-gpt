package com.zlt.aps.mp.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmDeviceMaintenancePlan;
import com.zlt.aps.mp.api.domain.vo.MdmDeviceMaintenancePlanVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "IMdmDeviceMaintenancePlanRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmDeviceMaintenancePlanRemoteService {

    /**
     * 新增基础数据-设备维护计划
     */
    @ApiOperation("新增基础数据-设备维护计划")
    @PostMapping("/docDeviceMaintenancePlan/add")
    AjaxResult add(@RequestBody MdmDeviceMaintenancePlan docDeviceMaintenancePlan);

    /**
     * 修改基础数据-设备维护计划
     */
    @ApiOperation("修改基础数据-设备维护计划")
    @PostMapping("/docDeviceMaintenancePlan/edit")
    AjaxResult edit(@RequestBody MdmDeviceMaintenancePlan docDeviceMaintenancePlan);

    /**
     * 查询基础数据-设备维护计划列表
     */
    @ApiOperation("查询基础数据-设备维护计划列表")
    @PostMapping("/docDeviceMaintenancePlan/list")
    public TableDataInfo list(@RequestBody MdmDeviceMaintenancePlanVo docDeviceMaintenancePlan);

    @GetMapping("/docDeviceMaintenancePlan/getById/{id}")
    MdmDeviceMaintenancePlan getById(@PathVariable("id") Long id);

    /**
     * 删除基础数据-设备维护计划
     */
    @ApiOperation("删除基础数据-设备维护计划")
    @DeleteMapping("/docDeviceMaintenancePlan/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 导出查询设备维护计划数据
     *
     * @param entity
     * @return
     */
    @ApiOperation("导出查询设备维护计划")
    @PostMapping("/docDeviceMaintenancePlan/getList")
    List<MdmDeviceMaintenancePlanVo> getList(@RequestBody MdmDeviceMaintenancePlanVo entity);

    // /**
    //  * 导入设备维护计划
    //  *
    //  * @param list
    //  * @param updateSupport
    //  * @param importLogId
    //  * @return
    //  */
    // @ApiOperation("导入设备维护计划")
    // @PostMapping("/docDeviceMaintenancePlan/importData/{updateSupport}/{importLogId}")
    // public AjaxResult importData(@RequestBody List<MdmDeviceMaintenancePlanVo> list, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("importLogId") Long importLogId);

    /**
     * 导出设备维护计划
     */
    @ApiOperation("导出设备维护计划")
    @PostMapping("/docDeviceMaintenancePlan/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmDeviceMaintenancePlanVo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 导入设备维护计划
     */
    @ApiOperation("导入设备维护计划")
    @PostMapping("/docDeviceMaintenancePlan/importData/{updateSupport}/{planType}")
    AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("planType") Integer planType);

}
