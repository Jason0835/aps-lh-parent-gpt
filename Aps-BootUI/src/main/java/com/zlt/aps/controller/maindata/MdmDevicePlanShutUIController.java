package com.zlt.aps.controller.maindata;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.bean.BeanUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.constant.UserConstants;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.common.core.enums.DeviceShutMachineTypeEnums;
import com.zlt.aps.lh.api.service.ILhMachineInfoRemoteService;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mp.api.domain.entity.MdmDevicePlanShut;
import com.zlt.aps.mp.api.domain.entity.MdmMoldingMachine;
import com.zlt.aps.mp.api.domain.vo.MdmDevicePlanShutQueryMachineParamVo;
import com.zlt.aps.mp.api.service.IMdmDevicePlanShutRemoteService;
import com.zlt.aps.mp.api.service.IMdmMoldingMachineRemoteService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmDevicePlanShutUIController.java
 * 描    述：0106基础数据_设备计划停机 UI控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-04
 */
@Slf4j
@Api(tags = "设备计划停机")
@Controller
@RequestMapping("/monthplan/mdmDevicePlanShut")
public class MdmDevicePlanShutUIController extends BaseUIController<MdmDevicePlanShut> {

    private final String prefix = "aps/monthplan/mdmDevicePlanShut";
    @Autowired
    private IMdmDevicePlanShutRemoteService iMdmDevicePlanShutService;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("monthplan:mdmDevicePlanShut:view")
    @GetMapping()
    public String toIndex() {
        return prefix + "/mdmDevicePlanShut";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("mdmDevicePlanShut", new MdmDevicePlanShut());
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mdmDevicePlanShut", iMdmDevicePlanShutService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主表数据
     */
    @ApiOperation("根据条件查询主表数据")
    @RequiresPermissions("monthplan:mdmDevicePlanShut:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmDevicePlanShut mdmDevicePlanShut) {
        return iMdmDevicePlanShutService.list(mdmDevicePlanShut);
    }

    /**
     * 修改或新增
     */
    @ApiOperation("修改或新增")
    @RequiresPermissions("monthplan:mdmDevicePlanShut:edit")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult save(MdmDevicePlanShut mdmDevicePlanShut) {
        if (UserConstants.NOT_UNIQUE.equals(iMdmDevicePlanShutService.checkUnique(mdmDevicePlanShut))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mdmDevicePlanShut.checkUnique"));
        }
        return iMdmDevicePlanShutService.save(mdmDevicePlanShut);
    }

    /**
     * 删除0106基础数据_设备计划停机
     */
    @ApiOperation("删除,id不为空")
    @RequiresPermissions("monthplan:mdmDevicePlanShut:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmDevicePlanShutService.removeByIds(Arrays.asList(arr));
    }

    /**
     * 校验0106基础数据_设备计划停机唯一性
     */
    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    @ResponseBody
    public String checkUnique(MdmDevicePlanShut mdmDevicePlanShut) {
        return iMdmDevicePlanShutService.checkUnique(mdmDevicePlanShut);
    }

    /**
     * 导出模板文件的文件名，派生类重写名称。
     * 示例：支持多语言写法： String fileName = I18nUtil.getMessage("ui.cd90.machine.export.fileName");
     *
     * @return
     */
    @Override
    public String getExportTemplateFileName() {
        return this.getFunctionName();
    }


    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getProcedureCode() {
        return "0";
    }

    /**
     * 继承时重写方法。
     *
     * @return
     */
    @Override
    public String getFunctionName() {
        return I18nUtil.getMessage("ui.data.column.mdmDevicePlanShut.modelName");
    }

    /**
     * 重写导入模板的生成逻辑
     */
    @ApiOperation("下载导入模板")
    @Override
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = this.getExportTemplateFileName();
        ExcelUtil<MdmDevicePlanShut> util = new ExcelUtil<>(MdmDevicePlanShut.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    @RequiresPermissions("monthplan:mdmDevicePlanShut:export")
    @ApiOperation("数据导出")
    @GetMapping({"/export"})
    @ResponseBody
    @Override
    public void export(HttpServletResponse response, MdmDevicePlanShut entity) throws IOException {
        String fileName = this.getExportTemplateFileName();
        byte[] excelBytes = iMdmDevicePlanShutService.exportData(entity, fileName);
        ByteArrayInputStream in = new ByteArrayInputStream(excelBytes);
        ExcelUtil.setResponseHeader(response, fileName, ".xlsx");
        IOUtils.copy(in, response.getOutputStream());
        response.flushBuffer();
    }

    @RequiresPermissions("monthplan:mdmDevicePlanShut:import")
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
        AjaxResult ajaxResult = iMdmDevicePlanShutService.importData(context, updateSupport);
        return ajaxResult;
    }

    @Autowired
    private IMdmMoldingMachineRemoteService iMdmMoldingMachineService;
    @Autowired
    private ILhMachineInfoRemoteService iLhMachineInfoService;

    /**
     * 获取机台列表
     *
     * @param mdmDevicePlanShutQueryMachineParamVo 参数
     * @return 结果
     */
    @ApiOperation("获取机台列表")
    @PostMapping("/getMachineList")
    @ResponseBody
    public TableDataInfo getMachineList(MdmDevicePlanShutQueryMachineParamVo mdmDevicePlanShutQueryMachineParamVo) {
        String machineType = mdmDevicePlanShutQueryMachineParamVo.getMachineType();
        DeviceShutMachineTypeEnums deviceShutMachineTypeEnums = DeviceShutMachineTypeEnums.getNameByCode(machineType);
        switch (deviceShutMachineTypeEnums) {
            case LH:
                LhMachineInfo lhMachineInfo = new LhMachineInfo();
                BeanUtils.copyProperties(mdmDevicePlanShutQueryMachineParamVo, lhMachineInfo);
                return iLhMachineInfoService.list(lhMachineInfo);
            case CX:
                MdmMoldingMachine mdmMoldingMachine = new MdmMoldingMachine();
                BeanUtils.copyProperties(mdmDevicePlanShutQueryMachineParamVo, mdmMoldingMachine);
                TableDataInfo tableDataInfo = iMdmMoldingMachineService.list(mdmMoldingMachine);
                List<MdmMoldingMachine> list = JSON.parseArray(JSON.toJSONString(tableDataInfo.getRows()), MdmMoldingMachine.class);
                for (MdmMoldingMachine moldingMachine : list) {
                    moldingMachine.setMachineCode(moldingMachine.getCxMachineCode());
                    moldingMachine.setMachineName(moldingMachine.getCxMachineCode());
                }
                tableDataInfo.setRows(list);
                return tableDataInfo;
            default:
                return null;
        }
    }
}
