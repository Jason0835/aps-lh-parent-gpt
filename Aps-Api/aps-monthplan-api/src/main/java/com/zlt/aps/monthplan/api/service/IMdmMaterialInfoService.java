package com.zlt.aps.monthplan.api.service;

import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmMaterialInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.monthplan.api.domain.vo.ConfigConstructionVo;
import com.zlt.aps.monthplan.api.domain.vo.TableProductInfoVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 物料信息表Service接口
 *
 * @author leo
 * @date 2021-08-24
 */
@FeignClient(contextId = "IMaterialInfoService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmMaterialInfoService {
    /**
     * 查询物料信息表列表
     *
     * @param productInfo
     * @return
     */
    @PostMapping("/productinfo/list")
    TableDataInfo list(@RequestBody MdmMaterialInfo productInfo);

    /**
     * 查询物料信息表列表
     *
     * @param productInfo
     * @return
     */
    @PostMapping("/productinfo/getTableList")
    TableDataInfo getTableList(@RequestBody TableProductInfoVo productInfo);


    /**
     * 新增物料信息表
     *
     * @param productInfo
     * @return
     */
    @PostMapping("/productinfo/add")
    AjaxResult add(@RequestBody MdmMaterialInfo productInfo);


    /**
     * 修改物料信息表
     *
     * @param productInfo
     * @return
     */
    @PostMapping("/productinfo/edit")
    AjaxResult edit(@RequestBody MdmMaterialInfo productInfo);


    /**
     * 删除物料信息表
     *
     * @param ids
     * @return
     */
    @DeleteMapping("/productinfo/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);


    /**
     * 根据ID获取详细信息
     *
     * @param id
     * @return
     */
    @GetMapping(value = "/productinfo/{id}")
    MdmMaterialInfo getInfo(@PathVariable("id") Long id);


    /**
     * 校验物料信息表唯一性
     *
     * @param productInfo
     * @return
     */
    @PostMapping("/productinfo/checkMaterialInfoUnique")
    String checkMaterialInfoUnique(@RequestBody MdmMaterialInfo productInfo);


    /**
     * 导出物料信息表列表
     *
     * @param productInfo
     * @return
     */
    @PostMapping("/productinfo/getList")
    List<MdmMaterialInfo> getList(@RequestBody MdmMaterialInfo productInfo);

    /**
     * 根据物料编码获取物料信息
     *
     * @param materialCode
     * @return
     */
    @PostMapping("/productinfo/getMaterialInfo")
    AjaxResult getMaterialInfo(@RequestParam("materialCode") String materialCode);

    /**
     * 物料数据导出
     *
     * @param queryVO
     * @param fileName
     * @return
     */
    @ApiOperation("物料数据导出")
    @PostMapping("/productinfo/exportData2/{fileName}")
    byte[] exportData(@RequestBody TableProductInfoVo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 物料数据导入
     *
     * @param importContext
     * @param updateSupport
     * @return
     */
    @ApiOperation("物料数据导入")
    @PostMapping("/productinfo/importData")
    AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 物料毛利率数据导入
     *
     * @param importContext
     * @param updateSupport
     * @return
     */
    @ApiOperation("物料毛利率数据导入")
    @PostMapping("/productinfo/importGrossRate")
    AjaxResult importGrossRate(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport);

    /**
     * 物料毛利率数据导出
     *
     * @param queryVO
     * @param fileName
     * @return
     */
    @ApiOperation("物料毛利率数据导出")
    @PostMapping("/productinfo/exportGrossRate/{fileName}")
    byte[] exportGrossRate(@RequestBody TableProductInfoVo queryVO, @PathVariable("fileName") String fileName);

    /**
     * 施工配置校验物料是否相同
     *
     * @param productConstruction 施工数据
     * @return 结果
     */
    @ApiOperation("施工配置校验物料是否相同")
    @PostMapping("/productinfo/configurationConstructionCheck")
    AjaxResult configurationConstructionCheck(@RequestBody MdmProductConstruction productConstruction);

    /**
     * 施工配置
     *
     * @param productConstruction 施工数据
     * @return 结果
     */
    @ApiOperation("施工配置")
    @PostMapping("/productinfo/configurationConstruction")
    AjaxResult configurationConstruction(@RequestBody MdmProductConstruction productConstruction);

    /**
     * 查询物料对应施工列表
     *
     * @param productConstruction 施工数据
     * @return 结果
     */
    @ApiOperation("查询物料对应施工列表")
    @PostMapping("/productinfo/selectConstructionCheckList")
    public AjaxResult selectConstructionCheckList(@RequestBody MdmProductConstruction productConstruction);

    /**
     * 施工配置-新
     * @param configConstructionVo 配置数据
     * @return 结果
     */
    @ApiOperation("施工配置-新")
    @PostMapping("/productinfo/configConstruction")
    public AjaxResult configConstruction(@RequestBody ConfigConstructionVo configConstructionVo);
}
