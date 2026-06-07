package com.zlt.aps.cd90.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90SpecifyMachine;
import com.zlt.aps.cd90.mapper.Cd90SpecifyMachineMapper;
import com.zlt.aps.cd90.service.ICd90SpecifyMachineService;
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
import java.util.stream.Collectors;

/**
 * 直裁定点机台控制层。
 */
@Api(tags = "直裁定点机台")
@RestController
@RequestMapping("/specifyMachine")
public class Cd90SpecifyMachineController extends AbstractDocBizController<Cd90SpecifyMachine> {

    @Resource
    private ICd90SpecifyMachineService cd90SpecifyMachineService;

    @Resource
    private Cd90SpecifyMachineMapper cd90SpecifyMachineMapper;

    /** 查询直裁定点机台列表 */
    @ApiOperation("查询直裁定点机台列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd90SpecifyMachine queryVO) {
        return super.list(queryVO);
    }

    /** 新增直裁定点机台 */
    @Log(title = "ui.data.column.cd90SpecifyMachine.modelName", businessType = BusinessType.INSERT)
    @ApiOperation("新增直裁定点机台")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody Cd90SpecifyMachine specifyMachine) {
        return super.save(specifyMachine);
    }

    /** 编辑直裁定点机台 */
    @Log(title = "ui.data.column.cd90SpecifyMachine.modelName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑直裁定点机台")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody Cd90SpecifyMachine specifyMachine) {
        return super.save(specifyMachine);
    }

    /** 删除直裁定点机台 */
    @Log(title = "ui.data.column.cd90SpecifyMachine.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除直裁定点机台")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /** 清空直裁定点机台 */
    @Log(title = "ui.data.column.cd90SpecifyMachine.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("清空直裁定点机台")
    @PostMapping("/removeAll")
    public AjaxResult removeAll(@RequestBody Cd90SpecifyMachine queryVO) {
        QueryWrapper<Cd90SpecifyMachine> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, queryVO);
        List<Long> ids = cd90SpecifyMachineMapper.selectList(wrapper).stream()
                .map(Cd90SpecifyMachine::getId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return AjaxResult.success();
        }
        return super.removeByIds(ids);
    }

    /** 获取直裁定点机台详情 */
    @ApiOperation("获取直裁定点机台详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd90SpecifyMachine getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /** 校验直裁定点机台唯一性 */
    @ApiOperation("校验直裁定点机台唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody Cd90SpecifyMachine specifyMachine) {
        return cd90SpecifyMachineService.checkUnique(specifyMachine);
    }

    /** 导入直裁定点机台 */
    @Log(title = "ui.data.column.cd90SpecifyMachine.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入直裁定点机台")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /** 导出直裁定点机台 */
    @Log(title = "ui.data.column.cd90SpecifyMachine.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出直裁定点机台")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd90SpecifyMachine queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<Cd90SpecifyMachine> listExportData(Cd90SpecifyMachine obj) {
        QueryWrapper<Cd90SpecifyMachine> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<Cd90SpecifyMachine> list = cd90SpecifyMachineMapper.selectList(wrapper);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return cd90SpecifyMachineService;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd90SpecifyMachine> queryWrapper, Cd90SpecifyMachine queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getClothCode()), "CLOTH_CODE", queryVO.getClothCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getJobType()), "JOB_TYPE", queryVO.getJobType());
    }

    @Override
    protected String getTypeCode() {
        return "CD90_SPECIFY_MACHINE";
    }

    @Override
    protected String getOrderBy() {
        return "CLOTH_CODE asc, MACHINE_CODE asc, UPDATE_TIME desc";
    }
}
