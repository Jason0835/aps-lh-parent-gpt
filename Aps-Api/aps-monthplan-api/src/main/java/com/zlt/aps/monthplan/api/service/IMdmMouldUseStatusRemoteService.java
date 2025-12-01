package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMouldUseStatus;
import com.zlt.aps.monthplan.api.domain.vo.MdmMouldUseStatusVo;
import com.zlt.aps.monthplan.api.domain.vo.PeriodInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 模具可用状态Service接口
 *
 * @author leo
 * @date 2021-08-27
 */
@FeignClient(contextId = "IMdmMouldUseStatusRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmMouldUseStatusRemoteService {


    /**
     * 查询模具可用状态列表
     */
    @PostMapping("/mouldusestatus/list")
    TableDataInfo list(@RequestBody MdmMouldUseStatus mdmMouldUseStatus);


    /**
     * 查询模具可用状态列表汇总
     */
    @PostMapping("/mouldusestatus/listTotal")
    MdmMouldUseStatusVo listTotal(@RequestBody MdmMouldUseStatus mdmMouldUseStatus);

    /**
     * 新增模具可用状态
     */
    @PostMapping("/mouldusestatus/add")
    AjaxResult add(@RequestBody MdmMouldUseStatus mdmMouldUseStatus);

    /**
     * 复制模具可用状态
     */
    @PostMapping("/mouldusestatus/copy")
    AjaxResult copy(@RequestBody PeriodInfo periodinfo);

    /**
     * 复制模具可用状态
     */
    @PostMapping("/mouldusestatus/merge")
    AjaxResult merge(@RequestBody PeriodInfo periodinfo);

    /**
     * 修改模具可用状态
     */
    @PostMapping("/mouldusestatus/edit")
    AjaxResult edit(@RequestBody MdmMouldUseStatus mdmMouldUseStatus);


    /**
     * 删除模具可用状态
     */
    @DeleteMapping("/mouldusestatus/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/mouldusestatus/{id}")
    MdmMouldUseStatus getInfo(@PathVariable("id") Long id);


    /**
     * 校验模具可用状态唯一性
     */
    @PostMapping("/mouldusestatus/checkMouldUseStatusUnique")
    String checkMouldUseStatusUnique(@RequestBody MdmMouldUseStatus mdmMouldUseStatus);


    /**
     * 导出模具可用状态列表
     */
    @PostMapping("/mouldusestatus/getList")
    List<MdmMouldUseStatus> getList(@RequestBody MdmMouldUseStatus mdmMouldUseStatus);

    // /**
    //  * 导入excel
    //  */
    // @ApiOperation("导入excel")
    // @PostMapping("/mouldusestatus/importData/{updateSupport}/{importLogId}")
    // AjaxResult importData(@RequestBody List<MdmMouldUseStatus> list, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("importLogId") Long importLogId);

    /**
     * 导出模具可用状态列表
     */
    @ApiOperation("导出模具可用状态列表")
    @PostMapping("/mouldusestatus/exportData/{fileName}")
    byte[] exportData(@RequestBody MdmMouldUseStatus entity, @PathVariable("fileName") String fileName);

    /**
     * 导入模具可用状态列表
     */
    @ApiOperation("导入模具可用状态列表")
    @PostMapping("/mouldusestatus/importData/{updateSupport}")
    AjaxResult importData(@RequestBody ImportContext importContext, @PathVariable("updateSupport") boolean updateSupport);

}
