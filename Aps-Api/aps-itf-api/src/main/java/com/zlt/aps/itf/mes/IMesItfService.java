package com.zlt.aps.itf.mes;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.itf.vo.AuxReqSyncDataLogs;
import com.zlt.aps.itf.vo.MesBrandDict;
import com.zlt.aps.mp.api.domain.entity.*;
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
     * 同步特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     */
    @ApiOperation("同步特殊材料库存")
    @PostMapping("/mesItf/syncRawSpecialMaterialStock")
    public AjaxResult syncRawSpecialMaterialStock(@RequestBody RawSpecialMaterialStock rawSpecialMaterialStock);

    /**
     * 同步原材料出库
     *
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步原材料出库")
    @PostMapping("/mesItf/syncRawMaterialOutboundRecord")
    public AjaxResult syncRawMaterialOutboundRecord(@RequestBody AuxReqSyncDataLogs syncDataLogs);

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
     * 查询特殊材料库存
     *
     * @param rawSpecialMaterialStock 参数
     * @return 结果
     */
    @ApiOperation("同步特殊材料库存")
    @PostMapping("/mesItf/getRawSpecialMaterialStock")
    public List<RawSpecialMaterialStock> getRawSpecialMaterialStock(@RequestBody RawSpecialMaterialStock rawSpecialMaterialStock);

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
     * 下发月计划
     *
     * @param finalResultList 参数
     * @return 结果
     */
    @ApiOperation("下发月计划")
    @PostMapping("/mesItf/issueMonthPlan")
    public AjaxResult issueMonthPlan(@RequestBody List<FactoryMonthPlanProductionFinalResult> finalResultList);

    /**
     * 查询MES品牌字典
     *
     * @return 结果
     */
    @ApiOperation("查询MES品牌字典")
    @PostMapping("/mesItf/selectMesBrandDict")
    public List<MesBrandDict> selectMesBrandDict();

    /**
     * 生成超期SKU
     * @param mdmProductStock 参数
     * @return 结果
     */
    @ApiOperation("生成超期SKU")
    @PostMapping("/mesItf/genOverDueSkuByStock")
    public AjaxResult genOverDueSkuByStock(@RequestBody MdmProductStock mdmProductStock) throws ParseException;

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
     * 同步胶囊已使用次数
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步胶囊已使用次数")
    @PostMapping("/mesItf/syncLhRepairCapsule")
    public AjaxResult syncLhRepairCapsule(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步模具清洗预警计划
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步模具清洗预警计划")
    @PostMapping("/mesItf/syncMouldCleanPlan")
    public AjaxResult syncMouldCleanPlan(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步结构整车胎面配置
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步结构整车胎面配置")
    @PostMapping("/mesItf/syncStructureTreadConfig")
    public AjaxResult syncStructureTreadConfig(@RequestBody AuxReqSyncDataLogs syncDataLogs);

    /**
     * 同步生胎库存
     * @param syncDataLogs 参数
     * @return 结果
     */
    @ApiOperation("同步生胎库存")
    @PostMapping("/mesItf/syncMesCxStock")
    public AjaxResult syncMesCxStock(@RequestBody AuxReqSyncDataLogs syncDataLogs);
}
