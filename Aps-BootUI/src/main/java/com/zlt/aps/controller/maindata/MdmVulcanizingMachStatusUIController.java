package com.zlt.aps.controller.maindata;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.vo.CopyParamVo;
import com.zlt.aps.monthplan.api.domain.vo.MdmMoldingMachineStatusTemplateVo;
import com.zlt.aps.monthplan.api.domain.vo.MdmVulcanizingMachStatusVo;
import com.zlt.aps.monthplan.api.service.IMdmVulcanizingMachStatusRemoteService;
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
 * 基础数据-硫化机可用信息Controller
 *
 * @author chen
 * @date 2021-09-04
 */
@Api(tags = "基础数据-硫化机可用信息")
@Controller
@RequestMapping("/fac/docVulcanizationMachStatus")
public class MdmVulcanizingMachStatusUIController extends BaseUIController<MdmVulcanizingMachStatusVo> {

    @Autowired
    private IMdmVulcanizingMachStatusRemoteService iDocVulcanizingMachStatusService;

    /**
     * 根据条件查询基础数据-硫化机可用信息列表
     */
    @ApiOperation("根据条件查询基础数据-硫化机可用信息列表")
//    @RequiresPermissions("fac:docVulcanizationMachStatus:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmVulcanizingMachStatusVo entity) {
        return iDocVulcanizingMachStatusService.list(entity);
    }

    /**
     * 修改基础数据-硫化机可用信息
     */
    @ApiOperation("修改基础数据-硫化机可用信息")
    @RequiresPermissions("biz:docVulcanizationMachStatus:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(String ids, String status) {
        Long[] idsArr = Convert.toLongArray(ids);
        return iDocVulcanizingMachStatusService.edit(idsArr, status);
    }

    /**
     * 新增基础数据-硫化机可用信息
     */
    @ApiOperation("新增基础数据-硫化机可用信息")
    @RequiresPermissions("biz:docVulcanizationMachStatus:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(MdmVulcanizingMachStatusVo docVulcanizingMachStatus) {
        if (UserConstants.NOT_UNIQUE.equals(checkDocVulcanizingMachStatusUnique(docVulcanizingMachStatus))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.docVulcanizingMachStatus.unique"));
        }
        return iDocVulcanizingMachStatusService.add(docVulcanizingMachStatus);
    }

    /**
     * 删除基础数据-硫化机可用信息
     */
    @ApiOperation("删除基础数据-硫化机可用信息（id不为空）")
    @RequiresPermissions("biz:docVulcanizationMachStatus:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iDocVulcanizingMachStatusService.remove(arr);
    }

    /**
     * 校验基础数据-硫化机可用信息唯一性
     */
    @ApiOperation("校验基础数据-硫化机可用信息唯一性")
    @PostMapping("/checkDocVulcanizingMachStatusUnique")
    @ResponseBody
    public String checkDocVulcanizingMachStatusUnique(MdmVulcanizingMachStatusVo docVulcanizingMachStatus) {
        return iDocVulcanizingMachStatusService.checkDocVulcanizingMachStatusUnique(docVulcanizingMachStatus);
    }

    /**
     * 复制可用台账信息
     *
     * @param copyParamVo 复制参数
     * @return 结果
     */
    @ApiOperation("复制可用台账信息")
    @PostMapping("/copyDocVulcanizingMachStatus")
    @ResponseBody
    public AjaxResult copyDocVulcanizingMachStatus(CopyParamVo copyParamVo) {
        return iDocVulcanizingMachStatusService.copyDocVulcanizingMachStatus(copyParamVo);
    }

    /**
     * 合并可用台账信息
     *
     * @param copyParamVo 复制参数
     * @return 结果
     */
    @ApiOperation("合并可用台账信息")
    @PostMapping("/mergeDocVulcanizingMachStatus")
    @ResponseBody
    public AjaxResult mergeDocVulcanizingMachStatus(CopyParamVo copyParamVo) {
        return iDocVulcanizingMachStatusService.mergeDocVulcanizingMachStatus(copyParamVo);
    }


    /**
     * 生成可用台账信息
     */
    @ApiOperation("生成可用台账信息")
    @PostMapping("/generateDocVulcanizingMachStatus")
    @ResponseBody
    public AjaxResult generateDocVulcanizingMachStatus(CopyParamVo copyParamVo) {
        return iDocVulcanizingMachStatusService.generateDocVulcanizingMachStatus(copyParamVo);
    }

    /**
     * 拷贝前校验 1.源月份没有数据 2.复制到的月份有数据是否要覆盖
     *
     * @param params 参数
     * @return 结果
     */
    @ApiOperation("拷贝前校验")
    @PostMapping("/copyValidate")
    @ResponseBody
    public AjaxResult copyValidate(@RequestParam Map<String, Object> params) {
        AjaxResult ajaxResult = iDocVulcanizingMachStatusService.copyValidate(params);
        if (AjaxResult.Type.ERROR.value() == Integer.parseInt(ajaxResult.get(AjaxResult.CODE_TAG).toString())) {
            String msg = ajaxResult.get(AjaxResult.MSG_TAG).toString();
            return "ui.data.column.docVulcanizationMachStatus.sourceDataNotExist".equals(msg) ? AjaxResult.error(I18nUtil.getMessage(msg)) : AjaxResult.success(I18nUtil.getMessage(msg));
        }
        return AjaxResult.success();
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmMoldingMachineStatusTemplateVo> util = new ExcelUtil<>(MdmMoldingMachineStatusTemplateVo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @Override
    public AjaxResult importDataByFeign(List list, boolean updateSupport, Long importLogId) {
        return iDocVulcanizingMachStatusService.importData(list, updateSupport, importLogId);
    }

    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.docVulcanizationMachStatus.modelName");
    }
    @Override
    public String getExportTemplateFileNameForce() {
        return I18nUtil.getMessage("ui.data.column.docVulcanizationMachStatus.modelName", Locale.SIMPLIFIED_CHINESE);
    }

    @Override
    public String getFunctionName() {
        return getExportTemplateFileName();
    }

    @Override
    public List<MdmVulcanizingMachStatusVo> exportDataByFeign(MdmVulcanizingMachStatusVo entity) {
        return iDocVulcanizingMachStatusService.getList(entity);
    }
}
