package com.zlt.aps.tq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.mapper.TqMachineInfoMapper;
import com.zlt.aps.tq.service.ITqMachineInfoService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@Slf4j
@Api(tags = "胎圈机台信息")
@RestController
@RequestMapping("/tqMachineInfo")
public class TqMachineInfoController extends AbstractDocBizController<TqMachineInfo> {

    @Autowired
    private ITqMachineInfoService tqMachineInfoService;

    @Resource
    private TqMachineInfoMapper tqMachineInfoMapper;

    @ApiOperation("查询胎圈机台信息列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqMachineInfo queryVO) {
        return super.list(queryVO);
    }

    @Log(title = "胎圈机台信息", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqMachineInfo billVO) {
        return super.save(billVO);
    }

    @Log(title = "胎圈机台信息", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqMachineInfo getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈机台信息", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈机台信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqMachineInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TqMachineInfo machineInfo) {
        return tqMachineInfoService.checkMachineCodeUnique(machineInfo);
    }

    @ApiOperation("获取机台信息列表")
    @PostMapping("/listMachineInfo")
    public List<TqMachineInfo> listMachineInfo(@RequestBody TqMachineInfo machineInfo) {
        return tqMachineInfoService.listMachineInfo(machineInfo);
    }

    @ApiOperation("查询未删除且启用的机台列表")
    @PostMapping("/listEnabledMachines")
    public List<TqMachineInfo> listEnabledMachines() {
        QueryWrapper<TqMachineInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("IS_DELETE", 0);
        wrapper.eq("STATUS", "1");
        wrapper.orderByAsc("MACHINE_CODE");
        return tqMachineInfoMapper.selectList(wrapper);
    }

    /**
     * 导出胎圈机台信息
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.EXPORT)
    @ApiOperation("导出胎圈机台信息")
    @PostMapping("/exportList")
    public List<TqMachineInfo> exportList(@RequestBody TqMachineInfo machineInfo) {
        startPage();
        List<TqMachineInfo> list = tqMachineInfoService.selectMachineInfoList(machineInfo);
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return tqMachineInfoService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    @Override
    protected List<TqMachineInfo> listExportData(TqMachineInfo obj) {
        QueryWrapper<TqMachineInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        return tqMachineInfoMapper.selectList(wrapper);
    }

    @Override
    protected void builderCondition(QueryWrapper<TqMachineInfo> queryWrapper, TqMachineInfo queryVO) {
        queryWrapper.eq("IS_DELETE", 0);
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMachineName()), "MACHINE_NAME", queryVO.getMachineName());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getStatus()), "STATUS", queryVO.getStatus());
    }
}
