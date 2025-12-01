package com.zlt.mix.controller.schedule;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.common.utils.StringUtil;
import com.zlt.framework.utils.AuthorizationUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.CollectionUtil;
import com.zlt.mix.common.core.utils.MixCommonUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.schedule.api.domain.dto.GlueDecomposePlanExportDictDto;
import com.zlt.mix.schedule.api.domain.dto.GlueSpanReceiveDto;
import com.zlt.mix.schedule.api.domain.dto.GlueSpanSendDto;
import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanSend;
import com.zlt.mix.schedule.api.service.IGlueDecomposePlanService;
import com.zlt.mix.schedule.api.service.IGlueSpanReceiveService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分解胶料需求量Controller
 *
 * @author chen
 * @date 2022-05-04
 */
@Api(tags = "分解胶料需求量")
@Controller
@RequestMapping("/schedule/glueDecomposePlan")
public class GlueDecomposePlanController extends BaseController {

    @Resource
    private IGlueDecomposePlanService iGlueDecomposePlanService;
    @Resource
    private IExportLogService iExportLogService;
    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    @Resource
    private IGlueSpanReceiveService iGlueSpanReceiveService;

    private final String prefix = "schedule/glueDecomposePlan";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("schedule:glueDecomposePlan:view")
    @GetMapping()
    public String toIndex(ModelMap modelMap) {
        modelMap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        GlueSpanReceive glueSpanReceive = new GlueSpanReceive();
        glueSpanReceive.setScheduleDate(DateUtils.addDays(new Date(), 1));
        glueSpanReceive.setSource(ZltConstant.SOURCE_GLUE_DECOMPOSE_PLAN);
        glueSpanReceive.setEntrustedMixArea(ZltConstant.DEFAULT_MIX_AREA);
        modelMap.put("notReceivedQuantity", iGlueSpanReceiveService.selectUnReceiveCount(glueSpanReceive));
        return prefix + "/glueDecomposePlan";
    }

    @ApiOperation("根据条件查询分解胶料需求量列表")
    @RequiresPermissions("schedule:glueDecomposePlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listGlueDecomposePlan(GlueDecomposePlan entity) {
        return iGlueDecomposePlanService.listGlueDecomposePlan(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        GlueDecomposePlan decomposePlan = new GlueDecomposePlan();
        decomposePlan.setPlanDate(DateUtils.addDays(new Date(), 1));
        mmap.put("glueDecomposePlan", decomposePlan);
        return prefix + "/add";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        GlueDecomposePlan glueDecomposePlanInfo = iGlueDecomposePlanService.getGlueDecomposePlanInfo(id);
        mmap.put("glueDecomposePlan", glueDecomposePlanInfo);
        mmap.put("isDisabled", !glueDecomposePlanInfo.getGlue().contains("/"));
        return prefix + "/edit";
    }

    @ApiOperation("跳转至分解计划页面")
    @GetMapping("/toDecompositionPlan")
    public String toDecompositionPlan(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        return prefix + "/decompositionPlan";
    }

    @ApiOperation("跳转至修改母炼胶机台页面")
    @GetMapping("/toModifyMachine/{id}")
    public String toModifyMachine(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("glueDecomposePlan", iGlueDecomposePlanService.getGlueDecomposePlanInfo(id));
        return prefix + "/modifyMachine";
    }

    @ApiOperation("跳转至跨区发送页面")
    @GetMapping("/toSendCrossRegional")
    public String toSendCrossRegional(String mixArea, String ids, ModelMap mmap) {
        mmap.put("mixArea", mixArea);
        mmap.put("sendPerson", AuthorizationUtils.getSysUser().getUserName());
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        // 查询选中的记录回写发送页
        List<GlueDecomposePlan> selectList = new ArrayList<>();
        if (StringUtils.isNotBlank(ids)) {
            Long[] scheduleIds = Convert.toLongArray(ids);
            selectList = iGlueDecomposePlanService.selectSpanSendNeedFieldByIds(scheduleIds);
            
            // 给没有机台的计划自动计算发送数
			if (CollectionUtils.isNotEmpty(selectList)) {
				GlueDecomposePlan plan = CollectionUtil.firstElement(selectList);
				List<Long> scheduleIdList = selectList.stream().filter(p -> p.getMachineCode() == null)
						.map(GlueDecomposePlan::getId).collect(Collectors.toList());
				if (CollectionUtils.isNotEmpty(scheduleIdList)) {
					GlueSpanReceiveDto dto = new GlueSpanReceiveDto();
					dto.setEntrustedMixArea(ZltConstant.DEFAULT_MIX_AREA);
					dto.setEntrustMixArea(mixArea);
					dto.setScheduleDate(plan.getPlanDate());
					dto.setScheduleIdList(scheduleIdList);
					List<GlueSpanReceive> receiveList = iGlueDecomposePlanService.caculateGlueSpanSendQty(dto);
					for (GlueSpanReceive receiveVo: receiveList) {
						Long scheduleId = receiveVo.getScheduleId();
						Long sendQty = receiveVo.getSendQty();
						GlueDecomposePlan selectPlan = selectList.stream().filter(p -> p.getId().equals(scheduleId)).findAny().orElse(null);
						if (selectPlan != null && sendQty != null) {
							selectPlan.setProduceQty((double)sendQty.longValue());
						}
					}
				}
			}
        }
        mmap.put("selectList", selectList);
        return prefix + "/sendCrossRegional";
    }

    @ApiOperation("跳转至跨区接收页面")
    @GetMapping("/toReceiveCrossRegional/{mixArea}")
    public String toReceiveCrossRegional(@PathVariable("mixArea") String mixArea, ModelMap mmap) {
        mmap.put("receivePerson", AuthorizationUtils.getSysUser().getUserName());
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        mmap.put("entrustedMixArea", mixArea);
        return prefix + "/receiveCrossRegional";
    }

    @ApiOperation("跳转至跨区接收页面")
    @GetMapping("/toChooseMachine")
    public String toChooseMachine(GlueSpanReceive param, ModelMap mmap) {
        GlueSpanReceive receiveInfo = iGlueDecomposePlanService.getGlueSpanReceiveInfo(param);
        String machineCode = param.getMachineCode();
        String machineName = param.getMachineName();
        String recipeTypeName = param.getRecipeTypeName();
        String recipeType = param.getRecipeType();
        String recipeVersionId = param.getRecipeVersionId();
        String recipeStage = param.getRecipeStage();
        if (StringUtils.isNotBlank(machineCode)) {
            receiveInfo.setMachineCode(machineCode);
        }
        if (StringUtils.isNotBlank(machineName)) {
            receiveInfo.setMachineName(machineName);
        }
        if (StringUtils.isNotBlank(recipeTypeName)) {
            receiveInfo.setRecipeTypeName(recipeTypeName);
        }
        if (StringUtils.isNotBlank(recipeType)) {
            receiveInfo.setRecipeType(recipeType);
        }
        if (StringUtils.isNotBlank(recipeVersionId)) {
            receiveInfo.setRecipeVersionId(recipeVersionId);
        }
        if (StringUtils.isNotBlank(recipeStage)) {
            receiveInfo.setRecipeStage(recipeStage);
        }
        mmap.put("glueSpanReceive", receiveInfo);
        return prefix + "/chooseMachine";
    }

    @ApiOperation("修改或新增分解胶料需求量")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueDecomposePlan(GlueDecomposePlan glueDecomposePlan) {
        AjaxResult result = iGlueDecomposePlanService.saveGlueDecomposePlan(glueDecomposePlan);
        return result;
    }

    @ApiOperation("删除分解胶料需求量（id不为空）")
    @RequiresPermissions("schedule:glueDecomposePlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeGlueDecomposePlan(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGlueDecomposePlanService.deleteGlueDecomposePlan(arr);
    }

    @ApiOperation("校验分解胶料需求量唯一性")
    @PostMapping("/checkGlueDecomposePlanUnique")
    @ResponseBody
    public String checkGlueDecomposePlanUnique(GlueDecomposePlan glueDecomposePlan) {
        return iGlueDecomposePlanService.checkGlueDecomposePlanUnique(glueDecomposePlan);
    }

    /**
     * 导出分解胶料需求量
     */
    @ApiOperation("导出分解胶料需求量")
    @RequiresPermissions("schedule:glueDecomposePlan:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GlueDecomposePlan glueDecomposePlan) throws IOException {
        String fileName = I18nUtil.getMessage("schedule.glueDecomposePlan.modelName");
        GlueDecomposePlanExportDictDto dictDto = new GlueDecomposePlanExportDictDto();
        HashMap<String, String> mixAreaDictMap = iSysDictDataCacheService.getType("MIX_AREA").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        HashMap<String, String> isFinishingDictMap = iSysDictDataCacheService.getType("IS_HAVE").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        dictDto.setMixAreaDictMap(mixAreaDictMap);
        dictDto.setIsFinishingDictMap(isFinishingDictMap);
        BeanUtils.copyProperties(glueDecomposePlan, dictDto);
        byte[] data = iGlueDecomposePlanService.exportData(dictDto);
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, dictDto.toString(), ZltConstant.PROCEDURE_CODE_MIX);
        iExportLogService.add(exportLog);
    }

    @ApiOperation("分解计划")
    @RequiresPermissions("schedule:glueDecomposePlan:decompositionPlan")
    @PostMapping("/decompositionPlan")
    @ResponseBody
    public AjaxResult decompositionPlan(GlueDecomposePlan glueDecomposePlan) {
        if (glueDecomposePlan.getPlanDate() == null) {
            glueDecomposePlan.setPlanDate(DateUtils.addDays(new Date(), 1));
        }
        if (StringUtils.isBlank(glueDecomposePlan.getMixArea())) {
            return AjaxResult.error(I18nUtil.getMessage("ui.frame.btn.choose") + I18nUtil.getMessage("schedule.glueDecomposePlan.mixArea"));
        }
        return iGlueDecomposePlanService.decompositionPlan(glueDecomposePlan);
    }

    /**
     * 更新安全库存
     * @param glueDecomposePlan 要更新的数据
     * @return 结果
     */
    @ApiOperation("更改安全库存")
    @RequiresPermissions("schedule:glueDecomposePlan:edit")
    @PostMapping("/updateSafeStock")
    @ResponseBody
    public AjaxResult updateSafeStock(GlueDecomposePlan glueDecomposePlan) {
        return iGlueDecomposePlanService.updateSafeStock(glueDecomposePlan);
    }

    @ApiOperation("检测对应日期和密炼区的数据是否存在")
    @PostMapping("/checkPlanDateAndMixAreaExist")
    @ResponseBody
    public AjaxResult checkPlanDateAndMixAreaExist(GlueDecomposePlan glueDecomposePlan) {
        String unique = iGlueDecomposePlanService.checkPlanDateAndMixAreaExist(glueDecomposePlan);

        //避免ZltConstant是否唯一的常量值修改，在此处定义0为唯一，1为不唯一
        if (ZltConstant.UNIQUE.equals(unique)) {
            return AjaxResult.success("0");
        }
        if (ZltConstant.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.success("1");
        }
        return AjaxResult.error();
    }

    @ApiOperation("根据条件查询分解胶料需求量跨区发送列表")
    @RequiresPermissions("schedule:glueDecomposePlan:glueSpanSend")
    @PostMapping("/listGlueSpanSend")
    @ResponseBody
    public TableDataInfo listGlueSpanSend(GlueSpanSend entity) {
        return iGlueDecomposePlanService.listGlueSpanSend(entity);
    }

    @ApiOperation("发送跨区请求")
    @RequiresPermissions("schedule:glueDecomposePlan:glueSpanSend")
    @PostMapping("/sendGlueSpan")
    @ResponseBody
    public AjaxResult sendGlueSpan(GlueSpanSendDto dto) {
        return iGlueDecomposePlanService.sendGlueSpan(dto);
    }

    @ApiOperation("根据条件查询分解胶料需求量跨区接收列表")
    @RequiresPermissions("schedule:glueDecomposePlan:glueSpanReceive")
    @PostMapping("/listGlueSpanReceive")
    @ResponseBody
    public TableDataInfo listGlueSpanReceive(GlueSpanReceive entity) {
        return iGlueDecomposePlanService.listGlueSpanReceive(entity);
    }

    @ApiOperation("接收跨区请求")
    @RequiresPermissions("schedule:glueDecomposePlan:glueSpanReceive")
    @PostMapping("/receiveGlueSpanReceive")
    @ResponseBody
    public AjaxResult receiveGlueSpanReceive(GlueSpanReceiveDto dto) {
        return iGlueDecomposePlanService.receiveGlueSpanReceive(dto);
    }

    /**
     * 删除跨区发送请求
     * @param ids 要删除的跨区发送请求id
     * @return 结果
     */
    @ApiOperation("删除跨区发送请求")
    @RequiresPermissions("glueDecomposePlan:glueSpanSend:remove")
    @PostMapping("/deleteGlueSpanSend")
    @ResponseBody
    public AjaxResult deleteGlueSpanSend(Long[] ids) {
        return iGlueDecomposePlanService.deleteGlueSpanSend(ids);
    }
    


    /**
     * 计算跨区后计划量（单个）
     * @param dto 要计算跨区发送量的
     * @return 结果
     */
    @ApiOperation("计算跨区后计划量")
    @PostMapping("/caculateGlueSpanSendQty")
    @ResponseBody
    public AjaxResult caculateGlueSpanSendQty(GlueSpanReceiveDto dto) {
    	if (dto == null || CollectionUtil.isEmpty(dto.getScheduleIdList()) || StringUtil.isEmpty(dto.getEntrustedMixArea())) {
        	return AjaxResult.success(); // 校验不合格的直接返回即可
    	}
    	List<GlueSpanReceive> receiveList = iGlueDecomposePlanService.caculateGlueSpanSendQty(dto);
    	if (CollectionUtil.isEmpty(receiveList)) {
        	return AjaxResult.success(); // 校验不合格的直接返回即可
    	}
    	return AjaxResult.success(receiveList);
    }
}
