package com.zlt.aps.tq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.entity.TqMouthPlate;
import com.zlt.aps.tq.api.domain.vo.TqMouthPlateExportVO;
import com.zlt.aps.tq.mapper.TqMouthPlateMapper;
import com.zlt.aps.tq.service.ITqMachineInfoService;
import com.zlt.aps.tq.service.ITqMouthPlateService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = "胎圈口型板信息")
@RestController
@RequestMapping("/tqMouthPlate")
public class TqMouthPlateController extends AbstractDocBizController<TqMouthPlate> {

    @Autowired
    private ITqMouthPlateService tqMouthPlateService;

    @Autowired
    private ITqMachineInfoService tqMachineInfoService;

    @Resource
    private TqMouthPlateMapper tqMouthPlateMapper;

    @ApiOperation("查询胎圈口型板信息列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqMouthPlate queryVO) {
        startPage();
        List<TqMouthPlate> list = tqMouthPlateMapper.selectMouthPlateWithMachineInfo(queryVO);
        return getDataTable(list);
    }

    @Log(title = "胎圈口型板信息", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqMouthPlate billVO) {
        // 双保险校验：系统判断"口型板编号+生产线"是否存在，存在则拒绝保存
        if (UserConstants.NOT_UNIQUE.equals(tqMouthPlateService.checkUnique(billVO))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.mouthPlate.message.unique"));
        }
        return super.save(billVO);
    }

    @Log(title = "胎圈口型板信息", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqMouthPlate getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈口型板信息", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈口型板信息", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqMouthPlate queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        List<TqMouthPlateExportVO> list = getExportDataList(queryVO);
        ExcelUtil<TqMouthPlateExportVO> util = new ExcelUtil<>(TqMouthPlateExportVO.class);
        org.apache.poi.ss.usermodel.Workbook workbook = util.exportExcelFromList(list, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    @ApiOperation("校验口型板唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TqMouthPlate mouthPlate) {
        return tqMouthPlateService.checkUnique(mouthPlate);
    }

    @Log(title = "胎圈口型板信息", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tqMouthPlateService.deleteAll();
        return AjaxResult.success();
    }

    @Override
    protected IDocService getDocService() {
        return tqMouthPlateService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    protected List<TqMouthPlateExportVO> getExportDataList(TqMouthPlate obj) {
        QueryWrapper<TqMouthPlate> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        List<TqMouthPlate> list = tqMouthPlateMapper.selectList(wrapper);

        // 查询机台信息
        Map<String, String> machineMap = new HashMap<>();
        if (!list.isEmpty()) {
            List<TqMachineInfo> machineList = tqMachineInfoService.selectMachineInfoList(new TqMachineInfo());
            machineMap = machineList.stream()
                    .collect(Collectors.toMap(TqMachineInfo::getMachineCode, TqMachineInfo::getMachineName, (v1, v2) -> v1));
        }

        // 转换为VO
        List<TqMouthPlateExportVO> voList = new ArrayList<>();
        for (TqMouthPlate plate : list) {
            TqMouthPlateExportVO vo = new TqMouthPlateExportVO();
            vo.setMouthPlateCode(plate.getMouthPlateCode());
            vo.setMachineName(machineMap.getOrDefault(plate.getMachineCode(), ""));
            vo.setStatus(plate.getStatus());
            vo.setRemark(plate.getRemark());
            vo.setUpdateTime(plate.getUpdateTime());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    protected void builderCondition(QueryWrapper<TqMouthPlate> queryWrapper, TqMouthPlate queryVO) {
        queryWrapper.eq("IS_DELETE", 0);
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMouthPlateCode()), "MOUTH_PLATE_CODE", queryVO.getMouthPlateCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getStatus()), "STATUS", queryVO.getStatus());
    }
}
