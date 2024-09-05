package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.entity.CxMdmMonthProdPlan1;
import com.zlt.aps.cx.api.domain.entity.CxMdmMonthProdPlan2;
import com.zlt.aps.cx.api.domain.entity.Gante;
import com.zlt.aps.cx.api.domain.entity.MdmMonthProdPlan;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.List;
import java.util.Map;


/**
 * 主计划月度生产计划Service接口
 *
 * @author zlt
 * @date 2021-09-15
 */
@FeignClient(contextId = "IMdmMonthProdPlanService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface IMdmMonthProdPlanService {

    /**
     * 查询主计划月度生产计划列表
     */
    @ApiOperation("查询主计划月度生产计划列表")
    @PostMapping("/mdmMonthProdPlan/list")
    TableDataInfo list(@RequestBody MdmMonthProdPlan mdmMonthProdPlan);

    /**
     * 新增主计划月度生产计划
     */
    @ApiOperation("新增主计划月度生产计划")
    @PostMapping("/mdmMonthProdPlan/add")
    AjaxResult add(@RequestBody MdmMonthProdPlan mdmMonthProdPlan);

    /**
     * 修改主计划月度生产计划
     */
    @ApiOperation("修改主计划月度生产计划")
    @PostMapping("/mdmMonthProdPlan/edit")
    AjaxResult edit(@RequestBody MdmMonthProdPlan mdmMonthProdPlan);


    @PostMapping("/mdmMonthProdPlan/updateExpectedExcessArrears")
    AjaxResult updateExpectedExcessArrears(@RequestBody MdmMonthProdPlan mdmMonthProdPlan);


    /**
     * 删除主计划月度生产计划
     */
    @ApiOperation("删除主计划月度生产计划")
    @DeleteMapping("/mdmMonthProdPlan/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @ApiOperation("根据ID获取详细信息")
    @GetMapping(value = "/mdmMonthProdPlan/{id}")
    MdmMonthProdPlan getInfo(@PathVariable("id") Long id);

    /**
     * 校验主计划月度生产计划唯一性
     */
    @ApiOperation("校验主计划月度生产计划唯一性")
    @PostMapping("/mdmMonthProdPlan/checkMdmMonthProdPlanUnique")
    String checkMdmMonthProdPlanUnique(@RequestBody MdmMonthProdPlan mdmMonthProdPlan);

    /**
     * 导出主计划月度生产计划列表
     */
    @ApiOperation("导出主计划月度生产计划列表")
    @PostMapping("/mdmMonthProdPlan/getList")
    List<MdmMonthProdPlan> getList(@RequestBody MdmMonthProdPlan mdmMonthProdPlan);

    /**
     * 预计超欠产导出
     */
    @ApiOperation("预计超欠产导出")
    @PostMapping("/mdmMonthProdPlan/expectedExport")
    List<CxMdmMonthProdPlan1> expectedExport(@RequestBody MdmMonthProdPlan mdmMonthProdPlan);

    /**
     * 超欠产导出
     */
    @ApiOperation("超欠产导出")
    @PostMapping("/mdmMonthProdPlan/overProdExport")
    List<CxMdmMonthProdPlan2> overProdExport(@RequestBody MdmMonthProdPlan mdmMonthProdPlan);

    /**
     * 导入主计划月度生产计划数据
     */
    @ApiOperation("导入主计划月度生产计划")
    @PostMapping("/mdmMonthProdPlan/importData")
    public AjaxResult importData(@RequestBody byte[] data, @RequestParam("mainPlanMonth") String mainPlanMonth, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId, @RequestParam("isFinamized") boolean isFinamized,@RequestParam Map<String, String> map)throws Exception ;

    /**
     * 下发主计划
     */
    @ApiOperation("下发主计划")
    @PostMapping("/mdmMonthProdPlan/issuePlan")
    public AjaxResult issuePlan(@RequestBody MdmMonthProdPlan mdmMonthProdPlan, @RequestParam Map<String, String> map);



    /**
     * 查询月计划甘特图数据
     */
    @PostMapping("/mdmMonthProdPlan/getMonthPlanGanteData")
    public List<Gante> getMonthPlanGanteData(@RequestBody Gante gante);


    /**
     * 查询月计划柱状图数据
     */
    @GetMapping("/mdmMonthProdPlan/dailyChart/{scheduleDate}")
    public Map<String,List<Integer>> dailyChart(@PathVariable("scheduleDate") String scheduleDate);

}
