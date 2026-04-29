package com.zlt.aps.tq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.entity.TqMachineMaintenancePlan;
import com.zlt.aps.tq.api.domain.vo.TqMachineMaintenancePlanExportVO;
import com.zlt.aps.tq.mapper.TqMachineMaintenancePlanMapper;
import com.zlt.aps.tq.service.ITqMachineInfoService;
import com.zlt.aps.tq.service.ITqMachineMaintenancePlanService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = "胎圈机台维修计划")
@RestController
@RequestMapping("/tqMachineMaintenancePlan")
public class TqMachineMaintenancePlanController extends AbstractDocBizController<TqMachineMaintenancePlan> {

    @Autowired
    private ITqMachineMaintenancePlanService tqMachineMaintenancePlanService;

    @Autowired
    private ITqMachineInfoService tqMachineInfoService;

    @Resource
    private TqMachineMaintenancePlanMapper tqMachineMaintenancePlanMapper;

    @ApiOperation("查询胎圈机台维修计划列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqMachineMaintenancePlan queryVO) {
        startPage();
        List<TqMachineMaintenancePlan> list = tqMachineMaintenancePlanMapper.listMachineMaintenancePlan(queryVO);
        return getDataTable(list);
    }

    @Log(title = "胎圈机台维修计划", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqMachineMaintenancePlan billVO) {
        return super.save(billVO);
    }

    @Log(title = "胎圈机台维修计划", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqMachineMaintenancePlan getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈机台维修计划", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈机台维修计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqMachineMaintenancePlan queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        List<TqMachineMaintenancePlanExportVO> list = getExportDataList(queryVO);
        ExcelUtil<TqMachineMaintenancePlanExportVO> util = new ExcelUtil<>(TqMachineMaintenancePlanExportVO.class);
        Workbook workbook = util.exportExcelFromList(list, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    @ApiOperation("校验机台维修计划唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TqMachineMaintenancePlan entity) {
        return tqMachineMaintenancePlanService.checkUnique(entity);
    }

    @Log(title = "胎圈机台维修计划", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tqMachineMaintenancePlanService.deleteAllMachineMaintenancePlan();
        return AjaxResult.success();
    }

    @Override
    protected IDocService getDocService() {
        return tqMachineMaintenancePlanService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    protected List<TqMachineMaintenancePlanExportVO> getExportDataList(TqMachineMaintenancePlan obj) {
        QueryWrapper<TqMachineMaintenancePlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        List<TqMachineMaintenancePlan> list = tqMachineMaintenancePlanMapper.selectList(wrapper);

        Map<Long, String> machineMap = new java.util.HashMap<>();
        if (!list.isEmpty()) {
            List<TqMachineInfo> machineList = tqMachineInfoService.selectMachineInfoList(new TqMachineInfo());
            machineMap = machineList.stream()
                    .collect(Collectors.toMap(TqMachineInfo::getId, TqMachineInfo::getMachineName, (v1, v2) -> v1));
        }

        List<TqMachineMaintenancePlanExportVO> voList = new ArrayList<>();
        for (TqMachineMaintenancePlan entity : list) {
            TqMachineMaintenancePlanExportVO vo = new TqMachineMaintenancePlanExportVO();
            vo.setMachineName(machineMap.getOrDefault(entity.getMachineId(), ""));
            vo.setDowntimeDate(entity.getDowntimeDate());
            vo.setDowntimeShift(entity.getDowntimeShift());
            vo.setDowntimeHours(entity.getDowntimeHours());
            vo.setRemark(entity.getRemark());
            vo.setUpdateTime(entity.getUpdateTime());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    protected void builderCondition(QueryWrapper<TqMachineMaintenancePlan> queryWrapper, TqMachineMaintenancePlan queryVO) {
        queryWrapper.eq("IS_DELETE", 0);
        queryWrapper.ge(queryVO.getDowntimeDateBegin() != null, "DOWNTIME_DATE", queryVO.getDowntimeDateBegin());
        queryWrapper.le(queryVO.getDowntimeDateEnd() != null, "DOWNTIME_DATE", queryVO.getDowntimeDateEnd());
        queryWrapper.eq(queryVO.getMachineId() != null, "MACHINE_ID", queryVO.getMachineId());
    }
}
