package com.zlt.aps.controller.maindata;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachineStatus;
import com.zlt.aps.monthplan.api.domain.vo.CopyParamVo;
import com.zlt.aps.monthplan.api.domain.vo.MdmMoldingMachineStatusTemplateVo;
import com.zlt.aps.monthplan.api.domain.vo.MdmMoldingMachineStatusVo;
import com.zlt.aps.monthplan.api.service.IMdmMoldingMachineStatusRemoteService;
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
 * 基础数据-成型机可用信息Controller
 *
 * @author chen
 * @date 2021-09-08
 */
@Api(tags = "基础数据-成型机可用信息")
@Controller
@RequestMapping("/fac/docMoldingMachineStatus")
public class MdmMoldingMachineStatusUIController extends BaseUIController<MdmMoldingMachineStatusVo> {

    @Autowired
    private IMdmMoldingMachineStatusRemoteService iMdmMoldingMachineStatusRemoteService;

    /**
     * 根据条件查询基础数据-成型机可用信息列表
     */
    @ApiOperation("根据条件查询基础数据-成型机可用信息列表")
//    @RequiresPermissions("fac:docMoldingMachineStatus:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmMoldingMachineStatus entity) {
        return iMdmMoldingMachineStatusRemoteService.list(entity);
    }

    /**
     * 修改或新增基础数据-成型机可用信息
     */
    @ApiOperation("修改或新增基础数据-成型机可用信息")
    @RequiresPermissions("fac:docMoldingMachineStatus:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult edit(String ids, String status) {
        Long[] idsArr = Convert.toLongArray(ids);
        return iMdmMoldingMachineStatusRemoteService.edit(idsArr, status);
    }

    /**
     * 新增基础数据-成型机可用信息
     */
    @ApiOperation("新增基础数据-成型机可用信息")
    @RequiresPermissions("fac:docMoldingMachineStatus:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(MdmMoldingMachineStatus docMoldingMachineStatus) {
        if (UserConstants.NOT_UNIQUE.equals(checkDocMoldingMachineStatusUnique(docMoldingMachineStatus))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.alert.docMoldingMachineStatus.unique"));
        }
        return iMdmMoldingMachineStatusRemoteService.add(docMoldingMachineStatus);
    }

    /**
     * 删除基础数据-成型机可用信息
     */
    @ApiOperation("删除基础数据-成型机可用信息（id不为空）")
    @RequiresPermissions("fac:docMoldingMachineStatus:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmMoldingMachineStatusRemoteService.remove(arr);
    }

    /**
     * 校验基础数据-成型机可用信息唯一性
     */
    @ApiOperation("校验基础数据-成型机可用信息唯一性")
    @PostMapping("/checkDocMoldingMachineStatusUnique")
    @ResponseBody
    public String checkDocMoldingMachineStatusUnique(MdmMoldingMachineStatus docMoldingMachineStatus) {
        return iMdmMoldingMachineStatusRemoteService.checkDocMoldingMachineStatusUnique(docMoldingMachineStatus);
    }

    /**
     * 复制可用台账信息
     *
     * @return 结果
     */
    @ApiOperation("复制可用台账信息")
    @PostMapping("/copyDocMoldingMachineStatus")
    @ResponseBody
    public AjaxResult copyDocMoldingMachineStatusEntity(CopyParamVo params) {
        return iMdmMoldingMachineStatusRemoteService.copyMoldingMachineStatus(params);
    }


    /**
     * 合并可用台账信息
     *
     * @return 结果
     */
    @ApiOperation("合并可用台账信息")
    @PostMapping("/mergeDocMoldingMachineStatus")
    @ResponseBody
    public AjaxResult mergeDocMoldingMachineStatus(CopyParamVo params) {
        return iMdmMoldingMachineStatusRemoteService.mergeMoldingMachineStatus(params);
    }

    /**
     * 生成可用台账信息
     */
    @ApiOperation("生成可用台账信息")
    @PostMapping("/generateDocMoldingMachineStatus")
    @ResponseBody
    public AjaxResult generateDocMoldingMachineStatusEntity(CopyParamVo params) {
        //Integer generateYear, Integer generateMonth
        return iMdmMoldingMachineStatusRemoteService.generateMoldingMachineStatus(params);
    }

    /**
     * 拷贝前校验 1.源月份没有数据 2.复制到的月份有数据是否要覆盖
     *
     * @return 结果
     */
    @ApiOperation("拷贝前校验")
    @PostMapping("/copyValidate")
    @ResponseBody
    public AjaxResult copyValidate(@RequestParam Map<String, Object> params) {
        AjaxResult ajaxResult = iMdmMoldingMachineStatusRemoteService.copyValidate(params);
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
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.docMoldingMachineStatus.modelName");
    }

    @Override
    public AjaxResult importDataByFeign(List list, boolean updateSupport, Long importLogId) {
        return iMdmMoldingMachineStatusRemoteService.importData(list, false, importLogId);
    }

    @Override
    public String getExportTemplateFileName() {
        return getFunctionName();
    }

    @Override
    public String getExportTemplateFileNameForce() {
        return I18nUtil.getMessage("ui.data.column.docMoldingMachineStatus.modelName", Locale.SIMPLIFIED_CHINESE);
    }

    @Override
    public List<MdmMoldingMachineStatusVo> exportDataByFeign(MdmMoldingMachineStatusVo entity) {
        return iMdmMoldingMachineStatusRemoteService.getList(entity);
    }

    @Override
    protected Long getUserId() {
        return SecurityUtils.getUserId();
    }
}
