package com.zlt.aps.tq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqMachineSpecSpeed;
import com.zlt.aps.tq.api.domain.vo.TqMachineSpecSpeedExportVO;
import com.zlt.aps.tq.mapper.TqMachineSpecSpeedMapper;
import com.zlt.aps.tq.service.ITqMachineSpecSpeedService;
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
@Api(tags = "胎圈机台生产速度")
@RestController
@RequestMapping("/tqMachineSpecSpeed")
public class TqMachineSpecSpeedController extends AbstractDocBizController<TqMachineSpecSpeed> {

    @Autowired
    private ITqMachineSpecSpeedService tqMachineSpecSpeedService;

    @Resource
    private TqMachineSpecSpeedMapper tqMachineSpecSpeedMapper;

    @ApiOperation("查询胎圈机台生产速度列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqMachineSpecSpeed queryVO) {
        startPage();
        List<TqMachineSpecSpeed> list = tqMachineSpecSpeedMapper.listMachineSpecSpeed(queryVO);
        return getDataTable(list);
    }

    @Log(title = "胎圈机台生产速度", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqMachineSpecSpeed billVO) {
        return super.save(billVO);
    }

    @Log(title = "胎圈机台生产速度", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqMachineSpecSpeed getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈机台生产速度", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈机台生产速度", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqMachineSpecSpeed queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        List<TqMachineSpecSpeedExportVO> list = getExportDataList(queryVO);
        ExcelUtil<TqMachineSpecSpeedExportVO> util = new ExcelUtil<>(TqMachineSpecSpeedExportVO.class);
        org.apache.poi.ss.usermodel.Workbook workbook = util.exportExcelFromList(list, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    @ApiOperation("校验机台生产速度唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TqMachineSpecSpeed machineSpecSpeed) {
        return tqMachineSpecSpeedService.checkUnique(machineSpecSpeed);
    }

    @Override
    protected IDocService getDocService() {
        return tqMachineSpecSpeedService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    protected List<TqMachineSpecSpeedExportVO> getExportDataList(TqMachineSpecSpeed obj) {
        List<TqMachineSpecSpeed> list = tqMachineSpecSpeedMapper.listMachineSpecSpeed(obj);

        List<TqMachineSpecSpeedExportVO> voList = new ArrayList<>();
        for (TqMachineSpecSpeed speed : list) {
            TqMachineSpecSpeedExportVO vo = new TqMachineSpecSpeedExportVO();
            vo.setMachineName(speed.getMachineName());
            vo.setBeadCode(speed.getBeadCode());
            vo.setStandardSpeed(speed.getStandardSpeed());
            vo.setQuota(speed.getQuota());
            vo.setQuotaMes(speed.getQuotaMes());
            vo.setRemark(speed.getRemark());
            vo.setUpdateTime(speed.getUpdateTime());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    protected void builderCondition(QueryWrapper<TqMachineSpecSpeed> queryWrapper, TqMachineSpecSpeed queryVO) {
        queryWrapper.eq("IS_DELETE", 0);
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBeadCode()), "BEAD_CODE", queryVO.getBeadCode());
    }
}
