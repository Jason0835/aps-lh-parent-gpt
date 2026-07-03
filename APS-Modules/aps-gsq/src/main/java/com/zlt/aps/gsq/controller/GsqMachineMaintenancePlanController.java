package com.zlt.aps.gsq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineMaintenancePlan;
import com.zlt.aps.gsq.api.domain.vo.GsqMachineMaintenancePlanExportVO;
import com.zlt.aps.gsq.mapper.GsqMachineMaintenancePlanMapper;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.IGsqMachineMaintenancePlanService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
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

/**
 * 钢丝圈机台维修计划Controller
 * 路径：/gsq/machineMaintenancePlan
 *
 * @author zlt
 * @date 2026-07-01
 */
@Slf4j
@Api(tags = "钢丝圈机台维修计划")
@RestController
@RequestMapping("/gsq/machineMaintenancePlan")
public class GsqMachineMaintenancePlanController extends AbstractDocBizController<GsqMachineMaintenancePlan> {

    @Autowired
    private IGsqMachineMaintenancePlanService gsqMachineMaintenancePlanService;

    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    @Resource
    private GsqMachineMaintenancePlanMapper gsqMachineMaintenancePlanMapper;

    /**
     * 查询钢丝圈机台维修计划列表
     */
    @ApiOperation("查询钢丝圈机台维修计划列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GsqMachineMaintenancePlan queryVO) {
        startPage();
        List<GsqMachineMaintenancePlan> list = gsqMachineMaintenancePlanMapper.listMachineMaintenancePlan(queryVO);
        return getDataTable(list);
    }

    /**
     * 保存钢丝圈机台维修计划（id为空新增，id不为空修改）
     * 父类内部会调用 Service 的 checkUnique 进行"停机日期+机台编码+停机班次"唯一性校验
     */
    @Log(title = "钢丝圈机台维修计划", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody GsqMachineMaintenancePlan billVO) {
        return super.save(billVO);
    }

    /**
     * 删除钢丝圈机台维修计划（逻辑删除）
     */
    @Log(title = "钢丝圈机台维修计划", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取钢丝圈机台维修计划详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public GsqMachineMaintenancePlan getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 导入钢丝圈机台维修计划
     */
    @Log(title = "钢丝圈机台维修计划", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext,
                                 @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出钢丝圈机台维修计划
     */
    @Log(title = "钢丝圈机台维修计划", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody GsqMachineMaintenancePlan queryVO,
                             @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        List<GsqMachineMaintenancePlanExportVO> list = getExportDataList(queryVO);
        ExcelUtil<GsqMachineMaintenancePlanExportVO> util = new ExcelUtil<>(GsqMachineMaintenancePlanExportVO.class);
        Workbook workbook = util.exportExcelFromList(list, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    /**
     * 校验钢丝圈机台维修计划唯一性
     */
    @ApiOperation("校验钢丝圈机台维修计划唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody GsqMachineMaintenancePlan entity) {
        return gsqMachineMaintenancePlanService.checkUnique(entity);
    }

    @Override
    protected IDocService getDocService() {
        return gsqMachineMaintenancePlanService;
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
     * 构建查询条件（手动追加 IS_DELETE=0 过滤逻辑删除数据）
     */
    @Override
    protected void builderCondition(QueryWrapper<GsqMachineMaintenancePlan> queryWrapper, GsqMachineMaintenancePlan queryVO) {
        queryWrapper.eq("IS_DELETE", "0");
        queryWrapper.ge(queryVO.getDowntimeDateBegin() != null, "DOWNTIME_DATE", queryVO.getDowntimeDateBegin());
        queryWrapper.le(queryVO.getDowntimeDateEnd() != null, "DOWNTIME_DATE", queryVO.getDowntimeDateEnd());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
    }

    /**
     * 获取导出数据列表，并补反显机台名称字段
     */
    protected List<GsqMachineMaintenancePlanExportVO> getExportDataList(GsqMachineMaintenancePlan obj) {
        QueryWrapper<GsqMachineMaintenancePlan> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        List<GsqMachineMaintenancePlan> list = gsqMachineMaintenancePlanMapper.selectList(wrapper);

        Map<String, String> machineMap = new java.util.HashMap<>();
        if (!list.isEmpty()) {
            List<GsqMachineInfo> machineList = gsqMachineInfoService.selectMachineInfoList(new GsqMachineInfo());
            machineMap = machineList.stream()
                    .collect(Collectors.toMap(GsqMachineInfo::getMachineCode, GsqMachineInfo::getMachineName, (v1, v2) -> v1));
        }

        List<GsqMachineMaintenancePlanExportVO> voList = new ArrayList<>();
        for (GsqMachineMaintenancePlan entity : list) {
            GsqMachineMaintenancePlanExportVO vo = new GsqMachineMaintenancePlanExportVO();
            vo.setMachineName(machineMap.getOrDefault(entity.getMachineCode(), ""));
            vo.setDowntimeDate(entity.getDowntimeDate());
            vo.setDowntimeShift(entity.getDowntimeShift());
            vo.setDowntimeHours(entity.getDowntimeHours());
            vo.setRemark(entity.getRemark());
            vo.setUpdateTime(entity.getUpdateTime());
            voList.add(vo);
        }
        return voList;
    }
}
