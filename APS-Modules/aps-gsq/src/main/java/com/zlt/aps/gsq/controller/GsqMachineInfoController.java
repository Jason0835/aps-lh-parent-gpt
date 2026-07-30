package com.zlt.aps.gsq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.mapper.GsqMachineInfoMapper;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.IGsqMachineInfoDocService;
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

/**
 * 钢丝圈机台信息Controller
 * 继承AbstractDocBizController，路径保持/gsq/machine以兼容Feign接口
 *
 * @author zlt
 * @date 2021-05-28
 */
@Slf4j
@Api(tags = "钢丝圈机台信息维护接口")
@RestController
@RequestMapping("/gsq/machine")
public class GsqMachineInfoController extends AbstractDocBizController<GsqMachineInfo> {

    @Autowired
    private IGsqMachineInfoDocService gsqMachineInfoDocService;

    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    @Resource
    private GsqMachineInfoMapper machineInfoMapper;

    /**
     * 查询钢丝圈机台信息列表
     */
    @ApiOperation("查询钢丝圈机台信息列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GsqMachineInfo queryVO) {
        return super.list(queryVO);
    }

    /**
     * 保存（新增或修改）
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody GsqMachineInfo billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public GsqMachineInfo getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 导入数据
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出数据
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody GsqMachineInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    /**
     * 校验机台编号唯一性
     */
    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody GsqMachineInfo machineInfo) {
        return gsqMachineInfoDocService.checkUnique(machineInfo);
    }

    /**
     * 校验机台编号唯一性（兼容旧Feign接口路径）
     */
    @ApiOperation("校验机台编号唯一性")
    @PostMapping("/checkMachineCodeUnique")
    public String checkMachineCodeUnique(@RequestBody GsqMachineInfo machineInfo) {
        return gsqMachineInfoDocService.checkMachineCodeUnique(machineInfo);
    }

    /**
     * 根据钢丝圈代码获取对应机台信息
     */
    @ApiOperation("根据钢丝圈代码获取对应机台信息")
    @PostMapping("/listMachineInfo")
    public List<GsqMachineInfo> listMachineInfo(@RequestBody GsqMachineInfo machineInfo) {
        return gsqMachineInfoDocService.listMachineInfo(machineInfo);
    }

    /**
     * 获取所有启用的钢丝圈机台信息（status=1），供下拉框数据源使用
     * 返回AjaxResult，与Feign接口声明一致，避免经过Gateway后反序列化异常
     */
    @ApiOperation("获取所有启用的钢丝圈机台信息")
    @GetMapping("/listEnabledMachines")
    public AjaxResult listEnabledMachines() {
        QueryWrapper<GsqMachineInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("IS_DELETE", "0");
        wrapper.eq("STATUS", "1");
        wrapper.orderByAsc("MACHINE_CODE");
        List<GsqMachineInfo> list = machineInfoMapper.selectList(wrapper);
        return AjaxResult.success(list);
    }

    /**
     * 导出钢丝圈机台信息列表
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.EXPORT)
    @ApiOperation("查询钢丝圈机台信息列表")
    @PostMapping("/exportList")
    public List<GsqMachineInfo> exportList(@RequestBody GsqMachineInfo machineInfo) {
        startPage();
        return gsqMachineInfoDocService.selectMachineInfoList(machineInfo);
    }

    /**
     * 新增钢丝圈机台信息（兼容旧Feign接口）
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.INSERT)
    @ApiOperation("新增钢丝圈机台信息（id不为空）")
    @PostMapping
    public AjaxResult add(@RequestBody GsqMachineInfo machineInfo) {
        return toAjax(gsqMachineInfoService.insertMachineInfo(machineInfo));
    }

    /**
     * 修改钢丝圈机台信息（兼容旧Feign接口）
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.UPDATE)
    @ApiOperation("修改钢丝圈机台信息（id不为空）")
    @PutMapping
    public AjaxResult edit(@RequestBody GsqMachineInfo machineInfo) {
        return toAjax(gsqMachineInfoService.updateMachineInfo(machineInfo));
    }

    /**
     * 删除钢丝圈机台信息（兼容旧Feign接口）
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.DELETE)
    @ApiOperation("删除钢丝圈机台信息（id不为空）")
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(gsqMachineInfoService.deleteMachineInfoByIds(ids));
    }

    /**
     * 兼容旧Feign接口的importData方法
     */
    @Log(title = "ui.data.column.machine.info", businessType = BusinessType.IMPORT)
    @PostMapping("/importDataFeign")
    @ApiOperation("导入钢丝圈机台信息（Feign兼容）")
    public AjaxResult importDataFeign(@RequestBody List<GsqMachineInfo> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId) {
        if (CollectionUtil.isEmpty(list)) {
            return AjaxResult.error("ui.data.column.import.nodata");
        }
        return gsqMachineInfoService.importData(list, updateSupport, importLogId);
    }

    @Override
    protected IDocService getDocService() {
        return gsqMachineInfoDocService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    /**
     * 构建查询条件（使用IS_DELETE与数据库列名一致）
     */
    @Override
    protected void builderCondition(QueryWrapper<GsqMachineInfo> queryWrapper, GsqMachineInfo queryVO) {
        queryWrapper.eq("IS_DELETE", "0");
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMachineName()), "MACHINE_NAME", queryVO.getMachineName());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getStatus()), "STATUS", queryVO.getStatus());
    }

    /**
     * 导出数据查询
     */
    @Override
    protected List<GsqMachineInfo> listExportData(GsqMachineInfo obj) {
        QueryWrapper<GsqMachineInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        return machineInfoMapper.selectList(wrapper);
    }
}
