package com.zlt.aps.mdm.mes;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;

import com.zlt.aps.mdm.api.domain.entity.MdmModelInfo;
import com.zlt.aps.mdm.api.domain.entity.MdmProductStock;
import com.zlt.aps.mdm.api.domain.entity.MdmSkuMouldRel;
import com.zlt.aps.mdm.api.domain.entity.MdmUnqualifiedStock;
import com.zlt.aps.mdm.api.domain.vo.AuxReqSyncDataLogs;
import com.zlt.aps.mp.api.domain.entity.MdmCxMachineOnlineInfo;
import com.zlt.aps.mp.api.domain.entity.MdmLhMachineOnlineInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.text.ParseException;
import java.util.List;

/**
 * MES接口业务
 *
 * @author Chen
 * @since 2025/12/16
 */
@FeignClient(contextId = "IMesItfService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.itf:/itf}")
public interface IMesItfService {

    /**
     * 同步SKU与模具关系
     *
     * @param mdmSkuMouldRel SKU与模具关系
     * @return 结果
     */
    @ApiOperation("同步SKU与模具关系")
    @PostMapping("/mesItf/syncProductModRelation")
    public AjaxResult syncProductModRelation(@RequestBody MdmSkuMouldRel mdmSkuMouldRel);

    /**
     * 同步模具台账
     *
     * @param modelInfo 模具台账
     * @return 结果
     */
    @ApiOperation("同步模具台账")
    @PostMapping("/mesItf/syncModelInfo")
    public AjaxResult syncModelInfo(@RequestBody MdmModelInfo modelInfo);

    /**
     * 同步成品库存
     *
     * @param productStock 参数
     * @return 结果
     */
    @ApiOperation("同步成品库存")
    @PostMapping("/mesItf/syncProductStock")
    public AjaxResult syncProductStock(@RequestBody MdmProductStock productStock);

    /**
     * 同步不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @return 结果
     */
    @ApiOperation("同步不合格库存")
    @PostMapping("/mesItf/syncUnqualifiedStock")
    public AjaxResult syncUnqualifiedStock(@RequestBody MdmUnqualifiedStock mdmUnqualifiedStock);

    /**
     * 查询实时成品库存
     *
     * @param productStock 参数
     * @return 结果
     */
    @ApiOperation("查询实时成品库存")
    @PostMapping("/mesItf/getProductStock")
    public List<MdmProductStock> getProductStock(@RequestBody MdmProductStock productStock);

    /**
     * 获取不合格库存
     *
     * @param mdmUnqualifiedStock 参数
     * @return 结果
     */
    @ApiOperation("获取不合格库存")
    @PostMapping("/mesItf/getUnqualifiedStock")
    public List<MdmUnqualifiedStock> getUnqualifiedStock(@RequestBody MdmUnqualifiedStock mdmUnqualifiedStock);
    /**
     * 生成超期SKU
     * @param mdmProductStock 参数
     * @return 结果
     */
    @ApiOperation("生成超期SKU")
    @PostMapping("/mesItf/genOverDueSkuByStock")
    public AjaxResult genOverDueSkuByStock(@RequestBody MdmProductStock mdmProductStock) throws ParseException;
    /**
     * 同步模壳台账信息
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步模壳台账信息")
    @PostMapping("/mesItf/syncMoldShell")
    public AjaxResult syncMoldShell(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步成型在机数据
     * @param mdmCxMachineOnlineInfo 参数
     * @return 结果
     */
    @ApiOperation("同步成型在机数据")
    @PostMapping("/mesItf/syncMachineOnlineInfo")
    public AjaxResult syncMachineOnlineInfo(@RequestBody MdmCxMachineOnlineInfo mdmCxMachineOnlineInfo);

    /**
     * 同步硫化在机数据
     * @param mdmLhMachineOnlineInfo 参数
     * @return 结果
     */
    @ApiOperation("同步硫化在机数据")
    @PostMapping("/mesItf/syncLhMachineOnlineInfo")
    public AjaxResult syncLhMachineOnlineInfo(@RequestBody MdmLhMachineOnlineInfo mdmLhMachineOnlineInfo);

    /**
     * 同步设备保养计划
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步设备保养计划")
    @PostMapping("/mesItf/syncDevMaintenancePlan")
    public AjaxResult syncDevMaintenancePlan(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步模具清洗预警计划
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步模具清洗预警计划")
    @PostMapping("/mesItf/syncMouldCleanPlan")
    public AjaxResult syncMouldCleanPlan(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步成型排程完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步成型排程完成量")
    @PostMapping("/mesItf/syncCxClassShiftFinishQty")
    public AjaxResult syncCxClassShiftFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步硫化排程完成量
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步硫化排程完成量")
    @PostMapping("/mesItf/syncLhClassShiftFinishQty")
    public AjaxResult syncLhClassShiftFinishQty(@RequestBody AuxReqSyncDataLogs syncDataLogs);
}
