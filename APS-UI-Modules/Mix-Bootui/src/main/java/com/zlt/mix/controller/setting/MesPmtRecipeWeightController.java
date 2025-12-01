package com.zlt.mix.controller.setting;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipeWeight;
import com.zlt.mix.setting.api.service.IMesPmtRecipeWeightService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 配方称量明细Controller
 * @author chen
 * @date 2022-06-01
 */
@Api(tags = "配方称量明细")
@Controller
@RequestMapping("/setting/MesPmtRecipeWeight")
public class MesPmtRecipeWeightController extends BaseController {

    @Resource
    private IMesPmtRecipeWeightService iMesPmtRecipeWeightService;
    @Resource
    private IExportLogService iExportLogService;

    private final String prefix = "setting/MesPmtRecipeWeight";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("setting:MesPmtRecipe:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/MesPmtRecipeWeight";
    }

    @ApiOperation("根据条件查询配方称量明细列表")
    @RequiresPermissions("setting:MesPmtRecipe:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listMesPmtRecipeWeight(MesPmtRecipeWeight entity) {
        return iMesPmtRecipeWeightService.listMesPmtRecipeWeight(entity);
    }

    /**
     * 导出配方称量明细
     */
    @ApiOperation("导出配方称量明细")
    @RequiresPermissions("setting:MesPmtRecipe:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,MesPmtRecipeWeight mesPmtRecipeWeight) throws IOException {
        String fileName = I18nUtil.getMessage("setting.MesPmtRecipeWeight.modelName");
        List<MesPmtRecipeWeight> list = iMesPmtRecipeWeightService.exportData(mesPmtRecipeWeight);
        ExcelUtil<MesPmtRecipeWeight> util = new ExcelUtil<>(MesPmtRecipeWeight. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, mesPmtRecipeWeight.toString(),ZltConstant.PROCEDURE_CODE_SETTING);
        iExportLogService.add(exportLog);
    }

}
