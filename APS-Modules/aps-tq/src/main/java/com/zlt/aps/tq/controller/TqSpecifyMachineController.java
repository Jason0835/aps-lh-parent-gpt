package com.zlt.aps.tq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqSpecifyMachine;
import com.zlt.aps.tq.api.domain.vo.TqSpecifyMachineExportVO;
import com.zlt.aps.tq.mapper.TqSpecifyMachineMapper;
import com.zlt.aps.tq.service.ITqSpecifyMachineService;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Api(tags = "胎圈定点机台信息")
@RestController
@RequestMapping("/tqSpecifyMachine")
public class TqSpecifyMachineController extends AbstractDocBizController<TqSpecifyMachine> {

    @Autowired
    private ITqSpecifyMachineService tqSpecifyMachineService;

    @Resource
    private TqSpecifyMachineMapper tqSpecifyMachineMapper;

    @ApiOperation("查询胎圈定点机台信息列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqSpecifyMachine queryVO) {
        startPage();
        List<TqSpecifyMachine> list = tqSpecifyMachineMapper.listSpecifyMachine(queryVO);
        return getDataTable(list);
    }

    @Log(title = "胎圈定点机台信息", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqSpecifyMachine billVO) {
        return super.save(billVO);
    }

    @Log(title = "胎圈定点机台信息", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqSpecifyMachine getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈定点机台信息", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈定点机台信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqSpecifyMachine queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        List<TqSpecifyMachineExportVO> list = getExportDataList(queryVO);
        ExcelUtil<TqSpecifyMachineExportVO> util = new ExcelUtil<>(TqSpecifyMachineExportVO.class);
        org.apache.poi.ss.usermodel.Workbook workbook = util.exportExcelFromList(list, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    @ApiOperation("校验定点机台唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TqSpecifyMachine specifyMachine) {
        return tqSpecifyMachineService.checkUnique(specifyMachine);
    }

    @Log(title = "胎圈定点机台信息", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tqSpecifyMachineService.deleteAllSpecifyMachine();
        return AjaxResult.success();
    }

    @Override
    protected IDocService getDocService() {
        return tqSpecifyMachineService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    protected List<TqSpecifyMachineExportVO> getExportDataList(TqSpecifyMachine obj) {
        List<TqSpecifyMachine> list = tqSpecifyMachineMapper.listSpecifyMachine(obj);
        
        // 转换为VO
        List<TqSpecifyMachineExportVO> voList = new ArrayList<>();
        for (TqSpecifyMachine machine : list) {
            TqSpecifyMachineExportVO vo = new TqSpecifyMachineExportVO();
            vo.setBeadCode(machine.getBeadCode());
            vo.setMachineName(machine.getMachineName());
            vo.setLineType(machine.getLineType());
            vo.setJobType(machine.getJobType());
            vo.setRemark(machine.getRemark());
            vo.setUpdateTime(machine.getUpdateTime());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    protected void builderCondition(QueryWrapper<TqSpecifyMachine> queryWrapper, TqSpecifyMachine queryVO) {
        queryWrapper.eq("is_delete", 0);
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBeadCode()), "bead_code", queryVO.getBeadCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "machine_code", queryVO.getMachineCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getLineType()), "line_type", queryVO.getLineType());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getJobType()), "job_type", queryVO.getJobType());
    }
}
