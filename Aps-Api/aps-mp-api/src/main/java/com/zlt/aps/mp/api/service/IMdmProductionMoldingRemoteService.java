package com.zlt.aps.mp.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.mp.api.domain.entity.MdmProductionMolding;
import com.zlt.aps.mp.api.domain.vo.MdmProductionMoldingPageVo;
import com.zlt.aps.mp.api.domain.vo.MdmProductionMoldingVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分厂成型正在生产的品种Service接口
 *
 * @author hsc
 * @date 2021-08-30
 */
@FeignClient(contextId = "IMdmProductionMoldingRemoteService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.monthplan:/monthplan}")
public interface IMdmProductionMoldingRemoteService {

    /**
     * 查询分厂成型正在生产的品种列表
     */
    @ApiOperation("查询分厂成型正在生产的品种列表")
    @PostMapping("/factoryProductionProduct/list")
    TableDataInfo list(@RequestBody MdmProductionMolding mdmProductionMolding);

    /**
     * 新增分厂成型正在生产的品种
     */
    @ApiOperation("新增分厂成型正在生产的品种")
    @PostMapping("/factoryProductionProduct/add")
    AjaxResult add(@RequestBody MdmProductionMolding mdmProductionMolding);

    /**
     * 修改分厂成型正在生产的品种
     */
    @ApiOperation("修改分厂成型正在生产的品种")
    @PostMapping("/factoryProductionProduct/edit")
    AjaxResult edit(@RequestBody MdmProductionMolding mdmProductionMolding);

    /**
     * 删除分厂成型正在生产的品种
     */
    @ApiOperation("删除分厂成型正在生产的品种")
    @DeleteMapping("/factoryProductionProduct/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/factoryProductionProduct/{id}")
    MdmProductionMolding getInfo(@PathVariable("id") Long id);

    /**
     * 校验分厂成型正在生产的品种唯一性
     */
    @ApiOperation("校验分厂成型正在生产的品种唯一性")
    @PostMapping("/factoryProductionProduct/checkFactoryProductionProductUnique")
    String checkFactoryProductionProductUnique(@RequestBody MdmProductionMolding mdmProductionMolding);

    /**
     * 导出分厂成型正在生产的品种列表
     */
    @ApiOperation("导出分厂成型正在生产的品种列表")
    @PostMapping("/factoryProductionProduct/getList")
    List<MdmProductionMoldingVo> getList(@RequestBody MdmProductionMolding mdmProductionMolding);

    /**
     * 导入分厂成型正在生产的品种数据
     */
    @ApiOperation("导入分厂成型正在生产的品种")
    @PostMapping("/factoryProductionProduct/importData/{updateSupport}/{importLogId}")
    public AjaxResult importData(@RequestBody List<MdmProductionMolding> list, @PathVariable("updateSupport") boolean updateSupport, @PathVariable("importLogId") Long importLogId);

    // /**
    //  * 抓取数据
    //  *
    //  * @param year
    //  * @param month
    //  * @return
    //  */
    // @ApiOperation("抓取成型在产品种")
    // @PostMapping("/factoryProductionProduct/grabData")
    // public AjaxResult grabData(@RequestParam("year") Long year, @RequestParam("month") Long month);

    @ApiOperation("获取成型法")
    @PostMapping("/factoryProductionProduct/getMachineMethod")
    AjaxResult getMachineMethod(@RequestBody MdmProductionMoldingPageVo vo);
}
