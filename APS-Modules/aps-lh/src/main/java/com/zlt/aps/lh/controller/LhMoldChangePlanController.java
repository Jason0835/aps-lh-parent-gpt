package com.zlt.aps.lh.controller;

import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.lh.api.domain.entity.LhMoldChangePlan;
import com.zlt.aps.lh.service.LhMoldChangePlanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模具变动单Controller
 *
 * @author zlt
 * @date 2021-06-17
 */
@Api(tags = "模具变动单维护接口")
@RestController
@RequestMapping("/moldChange")
public class LhMoldChangePlanController extends BaseController {
    @Autowired
    private LhMoldChangePlanService lhMoldChangePlanService;

    /**
     * 查询模具变动单列表
     */
    @ApiOperation("根据条件查询模具变动单")
    @PostMapping("/list")
    public TableDataInfo list(@RequestBody LhMoldChangePlan lhMoldChangePlan) {
        startPage();
        lhMoldChangePlan.setOrderStr(orderStr());
        List<LhMoldChangePlan> list = lhMoldChangePlanService.selectLhMoldChangePlanList(lhMoldChangePlan);
        return getDataTable(list);
    }

    /**
     * 获取模具变动单列表
     */
    @Log(title = "ui.data.column.moldChange.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("获取模具变动单列表")
    @PostMapping("/getList")
    public List<LhMoldChangePlan> getList(@RequestBody LhMoldChangePlan lhMoldChangePlan) {
        startPage();
        lhMoldChangePlan.setOrderStr(orderStr());
        return lhMoldChangePlanService.selectLhMoldChangePlanList(lhMoldChangePlan);
    }

    /**
     * 获取模具变动单详细信息
     */
    @ApiOperation("根据id获取模具变动单详细信息")
    @GetMapping(value = "/{id}")
    public LhMoldChangePlan getInfo(@PathVariable("id") Long id) {
        return lhMoldChangePlanService.selectLhMoldChangePlanById(id);
    }

    /**
     * 新增模具变动单
     */
    @Log(title = "ui.data.column.moldChange.modalName", businessType = BusinessType.INSERT)
    @ApiOperation("新增模具变动单")
    @PostMapping
    public AjaxResult add(@RequestBody LhMoldChangePlan lhMoldChangePlan) {
        return lhMoldChangePlanService.insertLhMoldChangePlan(lhMoldChangePlan);
    }

    /**
     * 修改模具变动单
     */
    @Log(title = "ui.data.column.moldChange.modalName", businessType = BusinessType.INSERT)
    @ApiOperation("修改模具变动单")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody LhMoldChangePlan lhMoldChangePlan) {
        return toAjax(lhMoldChangePlanService.updateLhMoldChangePlan(lhMoldChangePlan));
    }

    /**
     * 删除模具变动单
     */
    @Log(title = "ui.data.column.moldChange.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除模具变动单")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(lhMoldChangePlanService.deleteLhMoldChangePlanByIds(ids));
    }

    @Log(title = "ui.data.column.moldChange.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    public AjaxResult importData(@RequestBody List<LhMoldChangePlan> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.import.nodata"));
        }
        return lhMoldChangePlanService.importData(list, updateSupport, importLogId);
    }

	/**
	 * 发布变动单
	 * 
	 * @param lhMoldChangePlan
	 * @return
	 */
	@Log(title = "ui.lh.moldChange.publish", businessType = BusinessType.PUBLISH)
	@ApiOperation("发布变动单")
	@PostMapping("/publish")
	public AjaxResult publish(@RequestBody LhMoldChangePlan lhMoldChangePlan) {
		// 取出当日的待发布变动单
		lhMoldChangePlan.setIsRelease(ApsConstant.NO_RELEASE);
		List<LhMoldChangePlan> planList = lhMoldChangePlanService.selectLhMoldChangePlanList(lhMoldChangePlan);
        if (CollectionUtils.isEmpty(planList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.lh.moldChange.publish.notExistsData"));
        }
		// 取出ID
		long[] ids = planList.stream().mapToLong(item -> item.getId()).toArray();
		// 调用发布服务
		return lhMoldChangePlanService.publish(ids, lhMoldChangePlan.getScheduleDate());
	}
}
