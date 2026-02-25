package com.zlt.aps.controller.maindata;


import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.domain.entity.MdmMouldUseStatus;
import com.zlt.aps.mp.api.domain.vo.MdmMouldUseStatusTemplateVo;
import com.zlt.aps.mp.api.domain.vo.MdmMouldUseStatusVo;
import com.zlt.aps.mp.api.domain.vo.PeriodInfo;
import com.zlt.aps.mp.api.service.IMdmMouldUseStatusRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;


/**
 * 模具可用状态Controller
 *
 * @author leo
 * @date 2021-08-27
 */
@Api(tags = "模具可用状态")
@Controller
@RequestMapping("/lean/mouldusestatus")
public class MdmMouldUseStatusUIController extends BaseUIController<MdmMouldUseStatus> {

    @Autowired
    private IMdmMouldUseStatusRemoteService iMdmMouldUseStatusRemoteService;

    /**
     * 根据条件查询模具可用状态列表
     */
    @ApiOperation("根据条件查询模具可用状态列表")
   @RequiresPermissions("lean:mouldusestatus:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmMouldUseStatus entity) {
        return iMdmMouldUseStatusRemoteService.list(entity);
    }

    /**
     * 查询分厂实际生产反馈（调整后汇总）
     */
    @ApiOperation("根据条件查询模具可用状态列表汇总")
    @PostMapping("/listTotal")
    @ResponseBody
    public MdmMouldUseStatusVo listTotal(MdmMouldUseStatus entity) {
        return iMdmMouldUseStatusRemoteService.listTotal(entity);
    }

    /**
     * 修改或新增模具可用状态
     */
    @ApiOperation("修改或新增模具可用状态")
    @RequiresPermissions(value = {"lean:mouldusestatus:edit", "lean:mouldusestatus:add"}, logical = Logical.OR)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MdmMouldUseStatus mdmMouldUseStatus) {
        AjaxResult ajaxResult = null;
        String unique = iMdmMouldUseStatusRemoteService.checkMouldUseStatusUnique(mdmMouldUseStatus);
        if (UserConstants.UNIQUE.equals(unique)) {
            if (mdmMouldUseStatus.getId() != null) {
                ajaxResult = iMdmMouldUseStatusRemoteService.edit(mdmMouldUseStatus);
            } else {
                ajaxResult = iMdmMouldUseStatusRemoteService.add(mdmMouldUseStatus);
            }
        } else {
            ajaxResult = toAjax(false);
            ajaxResult.put("msg", I18nUtil.getMessage("ui.data.alert.mouldUseStatus.notUnique"));//该分厂、年月、模具号的数据在系统中已经存在，不能重复！
        }
        return ajaxResult;
    }

    /**
     * 删除模具可用状态
     */
    @ApiOperation("删除模具可用状态（id不为空）")
    @RequiresPermissions("lean:mouldusestatus:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmMouldUseStatusRemoteService.remove(arr);
    }


    @ApiOperation("校验模具可用状态唯一性")
    @PostMapping("/checkMouldUseStatusUnique")
    @ResponseBody
    public String checkMouldUseStatusUnique(MdmMouldUseStatus mdmMouldUseStatus) {
        return iMdmMouldUseStatusRemoteService.checkMouldUseStatusUnique(mdmMouldUseStatus);
    }

    /**
     * 复制模具可用状态
     */
    @ApiOperation("复制模具可用状态")
    @RequiresPermissions("lean:mouldusestatus:copy")
    @PostMapping("/copy")
    @ResponseBody
    public AjaxResult copy(PeriodInfo vo) {
        return iMdmMouldUseStatusRemoteService.copy(vo);
    }

    /**
     * 合并模具可用状态
     */
    @ApiOperation("合并模具可用状态")
    @RequiresPermissions("lean:mouldusestatus:copy")
    @PostMapping("/merge")
    @ResponseBody
    public AjaxResult merge(PeriodInfo vo) {
        return iMdmMouldUseStatusRemoteService.merge(vo);
    }


    /**
     * 重写导入模板的生成逻辑
     */
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmMouldUseStatusTemplateVo> util = new ExcelUtil<>(MdmMouldUseStatusTemplateVo.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }
    /**
     * 导入
     */
    @Override
    public String getFunctionName() {
        return getExportTemplateFileName();
    }

    @Override
    public List<MdmMouldUseStatus> importDataInit(List<MdmMouldUseStatus> list) {
        return list;
    }

    // @Override
    // public AjaxResult importDataByFeign(List list, boolean updateSupport, Long importLogId) {
    //     return iMdmMouldUseStatusRemoteService.importData(list, false, importLogId);
    // }

    /**
     * 导出
     */
    @Override
    public String getExportTemplateFileName() {
        return I18nUtil.getMessage("ui.data.column.mouldusestatus.modalName");
    }

    @Override
    public String getExportTemplateFileNameForce() {
        return I18nUtil.getMessage("ui.data.column.mouldusestatus.modalName");
    }

    @Override
    public List<MdmMouldUseStatus> exportDataByFeign(MdmMouldUseStatus entity) {
        List<MdmMouldUseStatus> list = iMdmMouldUseStatusRemoteService.getList(entity);
        return list;
    }

    @RequiresPermissions("lean:mouldusestatus:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmMouldUseStatus entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMdmMouldUseStatusRemoteService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @RequiresPermissions("lean:mouldusestatus:import")
    @PostMapping({"/importData"})
    @ResponseBody
    @ApiOperation("数据导入")
    @Override
    public AjaxResult importData(@RequestPart("file") MultipartFile file, boolean updateSupport) throws Exception {
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();

        ImportContext context = new ImportContext();
        context.setImportFilePath(this.importFilePath);
        context.setFunctionName(this.getFunctionName());
        context.setProcedureCode(this.getProcedureCode());
        context.setOriFileName(file.getOriginalFilename());
        context.setFileBytes(data);
        return iMdmMouldUseStatusRemoteService.importData(context, updateSupport);
    }


}
