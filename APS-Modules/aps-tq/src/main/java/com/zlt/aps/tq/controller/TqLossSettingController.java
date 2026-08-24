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
import com.zlt.aps.tq.api.domain.entity.TqLossSetting;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.vo.TqLossSettingExportVO;
import com.zlt.aps.tq.mapper.TqLossSettingMapper;
import com.zlt.aps.tq.service.ITqLossSettingService;
import com.zlt.aps.tq.service.ITqMachineInfoService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Api(tags = "胎圈损耗率设定")
@RestController
@RequestMapping("/tqLossSetting")
public class TqLossSettingController extends AbstractDocBizController<TqLossSetting> {

    @Autowired
    private ITqLossSettingService tqLossSettingService;

    @Autowired
    private ITqMachineInfoService tqMachineInfoService;

    @Resource
    private TqLossSettingMapper tqLossSettingMapper;

    @ApiOperation("查询胎圈损耗率设定列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqLossSetting queryVO) {
        startPage();
        List<TqLossSetting> list = tqLossSettingMapper.selectLossSettingList(queryVO);
        return getDataTable(list);
    }

    @Log(title = "胎圈损耗率设定", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqLossSetting billVO) {
        // 双保险校验：系统判断"胎圈代码+生产线"是否存在，存在则提示"损耗率记录已存在"
        if (UserConstants.NOT_UNIQUE.equals(tqLossSettingService.checkUnique(billVO))) {
            return AjaxResult.error(I18nUtil.getMessage("ui.error.message.loss.unique"));
        }
        return super.save(billVO);
    }

    @Log(title = "胎圈损耗率设定", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqLossSetting getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈损耗率设定", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈损耗率设定", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqLossSetting queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        List<TqLossSettingExportVO> list = getExportDataList(queryVO);
        ExcelUtil<TqLossSettingExportVO> util = new ExcelUtil<>(TqLossSettingExportVO.class);
        Workbook workbook = util.exportExcelFromList(list, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    @ApiOperation("校验唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TqLossSetting lossSetting) {
        return tqLossSettingService.checkUnique(lossSetting);
    }

    @Log(title = "胎圈损耗率设定", businessType = BusinessType.EXPORT)
    @ApiOperation("导出胎圈损耗率列表")
    @PostMapping("/exportList")
    public List<TqLossSetting> exportList(@RequestBody TqLossSetting lossSetting) {
        startPage();
        return tqLossSettingService.listLossSetting(lossSetting);
    }

    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tqLossSettingService.deleteAll();
        return AjaxResult.success();
    }

    @Override
    protected IDocService getDocService() {
        return tqLossSettingService;
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
    protected List<TqLossSetting> listExportData(TqLossSetting obj) {
        QueryWrapper<TqLossSetting> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        return tqLossSettingMapper.selectList(wrapper);
    }

    private List<TqLossSettingExportVO> getExportDataList(TqLossSetting obj) {
        QueryWrapper<TqLossSetting> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        List<TqLossSetting> list = tqLossSettingMapper.selectList(wrapper);

        // 查询机台信息
        Map<String, String> machineMap = new HashMap<>();
        if (!list.isEmpty()) {
            List<TqMachineInfo> machineList = tqMachineInfoService.selectMachineInfoList(new TqMachineInfo());
            machineMap = machineList.stream()
                    .collect(Collectors.toMap(TqMachineInfo::getMachineCode, TqMachineInfo::getMachineName, (v1, v2) -> v1));
        }

        // 转换为VO
        List<TqLossSettingExportVO> voList = new ArrayList<>();
        for (TqLossSetting setting : list) {
            TqLossSettingExportVO vo = new TqLossSettingExportVO();
            vo.setBeadCode(setting.getBeadCode());
            vo.setMachineName(machineMap.getOrDefault(setting.getMachineCode(), ""));
            vo.setLossRate(setting.getLossRate() != null ? setting.getLossRate() * 100 : null);
            vo.setRemark(setting.getRemark());
            vo.setUpdateTime(setting.getUpdateTime());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    protected void builderCondition(QueryWrapper<TqLossSetting> queryWrapper, TqLossSetting queryVO) {
        queryWrapper.eq("IS_DELETE", 0);
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBeadCode()), "BEAD_CODE", queryVO.getBeadCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
    }
}
