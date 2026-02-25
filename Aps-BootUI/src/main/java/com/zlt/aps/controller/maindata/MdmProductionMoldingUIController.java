package com.zlt.aps.controller.maindata;

import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.MdmProductionMolding;
import com.zlt.aps.mp.api.domain.vo.MdmProductionMoldingPageVo;
import com.zlt.aps.mp.api.domain.vo.MdmProductionMoldingTemplateVo;
import com.zlt.aps.mp.api.domain.vo.MdmProductionMoldingVo;
import com.zlt.aps.mp.api.service.IMdmProductionMoldingRemoteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 分厂成型正在生产的品种Controller
 *
 * @author hsc
 * @date 2021-08-30
 */
@Api(tags = "分厂成型正在生产的品种")
@Controller
@RequestMapping("/lean/factoryProductionProduct")
public class MdmProductionMoldingUIController extends BaseUIController<MdmProductionMoldingVo> {

    @Autowired
    private IMdmProductionMoldingRemoteService iMdmProductionMoldingRemoteService;

    // @ApiOperation("抓取")
    // @RequiresPermissions("lean:factoryProductionProduct:catchData")
    // @PostMapping("/catchData")
    // @ResponseBody
    // public AjaxResult catchData(MdmProductionMoldingPageVo mainPlanVersionInfoVo) {
    //     return iMdmProductionMoldingRemoteService.grabData(mainPlanVersionInfoVo.getYear(), mainPlanVersionInfoVo.getMonth());
    // }

    @ApiOperation("获取成型法接口")
    @PostMapping("/getmachineMethod")
    @ResponseBody
    public AjaxResult getmachineMethod(@RequestParam Map<String, String> params) {
        String productCode = params.get("productCode");
        String factoryCode = params.get("factoryCode");
        String machineCode = params.get("machineCode");
        MdmProductionMoldingPageVo vo = new MdmProductionMoldingPageVo();
        vo.setProductCode(productCode);
        vo.setFactoryCode(factoryCode);
        vo.setMachineCode(machineCode);

        return iMdmProductionMoldingRemoteService.getMachineMethod(vo);
    }

    /**
     * 根据条件查询分厂成型正在生产的品种列表
     */
    @ApiOperation("根据条件查询分厂成型正在生产的品种列表")
//    @RequiresPermissions("lean:factoryProductionProduct:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmProductionMolding entity) {
        return iMdmProductionMoldingRemoteService.list(entity);
    }

    /**
     * 修改或新增分厂成型正在生产的品种
     */
    @ApiOperation("修改或新增分厂成型正在生产的品种")
    @RequiresPermissions("lean:factoryProductionProduct:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MdmProductionMolding mdmProductionMolding) {
        AjaxResult ajaxResult = null;
        if (mdmProductionMolding.getId() != null) {
            ajaxResult = iMdmProductionMoldingRemoteService.edit(mdmProductionMolding);
        } else {
            ajaxResult = iMdmProductionMoldingRemoteService.add(mdmProductionMolding);
        }
        return ajaxResult;
    }

    /**
     * 删除分厂成型正在生产的品种
     */
    @ApiOperation("删除分厂成型正在生产的品种（id不为空）")
    @RequiresPermissions("lean:factoryProductionProduct:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmProductionMoldingRemoteService.remove(arr);
    }

    /**
     * 校验分厂成型正在生产的品种唯一性
     */
    @ApiOperation("校验分厂成型正在生产的品种唯一性")
    @PostMapping("/checkFactoryProductionProductUnique")
    @ResponseBody
    public String checkFactoryProductionProductUnique(MdmProductionMolding mdmProductionMolding) {
        return iMdmProductionMoldingRemoteService.checkFactoryProductionProductUnique(mdmProductionMolding);
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmProductionMoldingTemplateVo> util = new ExcelUtil<>(MdmProductionMoldingTemplateVo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @Override
    public String getFunctionName() {
        return getExportTemplateFileName();
    }

    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.factoryProductionProduct.modelName");
    }

    @Override
    public String getExportTemplateFileNameForce() {
        return I18nUtil.getMessage("ui.data.column.factoryProductionProduct.modelName", Locale.SIMPLIFIED_CHINESE);
    }

    @Override
    public AjaxResult importDataByFeign(List list, boolean updateSupport, Long importLogId) {
        return iMdmProductionMoldingRemoteService.importData(list, true, importLogId);
    }

    @Override
    public List<MdmProductionMoldingVo> exportDataByFeign(MdmProductionMoldingVo entity) {
        return iMdmProductionMoldingRemoteService.getList(entity);
    }
}

