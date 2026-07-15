package com.zlt.aps.cd15.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd15.api.domain.entity.Cd15ScheduleLaneAllocation;
import com.zlt.aps.cd15.mapper.Cd15ScheduleLaneAllocationMapper;
import com.zlt.aps.cd15.service.ICd15ScheduleLaneAllocationService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/** CD15斜裁排程库排分配明细Controller。 */
@Api(tags = "CD15斜裁排程库排分配明细")
@RestController
@RequestMapping("/cd15ScheduleLaneAllocation")
public class Cd15ScheduleLaneAllocationController extends AbstractDocBizController<Cd15ScheduleLaneAllocation> {

    @Resource
    private ICd15ScheduleLaneAllocationService service;

    @Resource
    private Cd15ScheduleLaneAllocationMapper mapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd15ScheduleLaneAllocation query) {
        return super.list(query);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd15ScheduleLaneAllocation getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "ui.data.column.cd15ScheduleLaneAllocation.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd15ScheduleLaneAllocation query, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(query, fileName, response);
    }

    @Override
    protected List<Cd15ScheduleLaneAllocation> listExportData(Cd15ScheduleLaneAllocation obj) {
        QueryWrapper<Cd15ScheduleLaneAllocation> queryWrapper = new QueryWrapper<>();
        this.builderCondition(queryWrapper, obj);
        List<Cd15ScheduleLaneAllocation> list = mapper.selectList(queryWrapper);
        AppUtils.formatData(list, this.getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return service;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd15ScheduleLaneAllocation> queryWrapper, Cd15ScheduleLaneAllocation vo) {
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        queryWrapper.eq(vo.getScheduleDate() != null, "SCHEDULE_DATE", vo.getScheduleDate());
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getBatchNo()), "BATCH_NO", vo.getBatchNo());
        queryWrapper.eq(vo.getScheduleResultId() != null, "SCHEDULE_RESULT_ID", vo.getScheduleResultId());
        queryWrapper.like(PubUtil.isNotEmpty(vo.getOrderNo()), "ORDER_NO", vo.getOrderNo());
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getGroupNo()), "GROUP_NO", vo.getGroupNo());
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getClassField()), "CLASS_FIELD", vo.getClassField());
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getStorageLaneCode()), "STORAGE_LANE_CODE", vo.getStorageLaneCode());
        queryWrapper.like(PubUtil.isNotEmpty(vo.getSteelStripCode()), "STEEL_STRIP_CODE", vo.getSteelStripCode());
        queryWrapper.like(PubUtil.isNotEmpty(vo.getBigRollCode()), "BIG_ROLL_CODE", vo.getBigRollCode());
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getCuttingAngle()), "CUTTING_ANGLE", vo.getCuttingAngle());
        queryWrapper.eq(PubUtil.isNotEmpty(vo.getMachineCode()), "MACHINE_CODE", vo.getMachineCode());
    }

    @Override
    protected String getTypeCode() {
        return "CD15_SCHEDULE_LANE_ALLOCATION";
    }

    @Override
    protected String getOrderBy() {
        return "SCHEDULE_DATE desc, MACHINE_CODE asc, CLASS_FIELD asc, ALLOCATION_ORDER asc, CREATE_TIME desc";
    }
}