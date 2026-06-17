package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15MachineInfo;
import com.zlt.aps.cd15.mapper.Cd15MachineInfoMapper;
import com.zlt.aps.cd15.service.ICd15MachineInfoService;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 斜裁机台基础信息控制层。
 */
@Api(tags = "斜裁机台基础信息")
@RestController
@RequestMapping("/cd15MachineInfo")
public class Cd15MachineInfoController extends AbstractDocBizController<Cd15MachineInfo> {

    @Resource
    private ICd15MachineInfoService cd15MachineInfoService;

    @Resource
    private Cd15MachineInfoMapper cd15MachineInfoMapper;

    /** 查询斜裁机台列表 */
    @ApiOperation("查询斜裁机台列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15MachineInfo queryVO) {
        return super.list(queryVO);
    }

    /** 新增斜裁机台 */
    @Log(title = "ui.data.column.cd15MachineInfo.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增斜裁机台")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd15MachineInfo machineInfo) {
        return super.save(machineInfo);
    }

    /** 编辑斜裁机台 */
    @Log(title = "ui.data.column.cd15MachineInfo.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑斜裁机台")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd15MachineInfo machineInfo) {
        return super.save(machineInfo);
    }

    /** 删除斜裁机台 */
    @Log(title = "ui.data.column.cd15MachineInfo.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除斜裁机台")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /** 获取斜裁机台详情 */
    @ApiOperation("获取斜裁机台详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15MachineInfo getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /** 校验斜裁机台唯一性 */
    @ApiOperation("校验斜裁机台唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd15MachineInfo machineInfo) {
        return cd15MachineInfoService.checkUnique(machineInfo);
    }

    /** 启用机台下拉 */
    @ApiOperation("启用机台下拉")
    @PostMapping("/enableOptions")
    public AjaxResult enableOptions(@RequestBody Cd15MachineInfo queryVO) {
        queryVO.setStatus(ApsConstant.APS_STRING_1);
        return AjaxResult.success(getList(queryVO));
    }

    /** 修改机台状态 */
    @Log(title = "ui.data.column.cd15MachineInfo.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("修改机台状态")
    @PostMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody Cd15MachineInfo machineInfo) {
        LambdaUpdateWrapper<Cd15MachineInfo> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Cd15MachineInfo::getId, machineInfo.getId())
               .set(Cd15MachineInfo::getStatus, machineInfo.getStatus());
        cd15MachineInfoMapper.update(null, wrapper);
        return AjaxResult.success();
    }

    /** 导入斜裁机台 */
    @Log(title = "ui.data.column.cd15MachineInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入斜裁机台")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /** 导出斜裁机台 */
    @Log(title = "ui.data.column.cd15MachineInfo.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出斜裁机台")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15MachineInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd15MachineInfo> listExportData(Cd15MachineInfo obj) {
        QueryWrapper<Cd15MachineInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd15MachineInfo> list = cd15MachineInfoMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd15MachineInfoService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15MachineInfo> queryWrapper, Cd15MachineInfo queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMachineName()), "MACHINE_NAME", queryVO.getMachineName());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getStatus()), "STATUS", queryVO.getStatus());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getClassShift()), "CLASS_SHIFT", queryVO.getClassShift());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_MACHINE_INFO";
    }

    @Override
    protected String getOrderBy() {
        return "MACHINE_CODE asc, UPDATE_TIME desc";
    }
}
