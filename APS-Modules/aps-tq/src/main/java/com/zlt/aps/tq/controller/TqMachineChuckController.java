package com.zlt.aps.tq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqMachineChuck;
import com.zlt.aps.tq.api.domain.entity.TqMachineInfo;
import com.zlt.aps.tq.api.domain.vo.TqMachineChuckExportVO;
import com.zlt.aps.tq.mapper.TqMachineChuckMapper;
import com.zlt.aps.tq.service.ITqMachineChuckService;
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
@Api(tags = "胎圈机台寸口对应")
@RestController
@RequestMapping("/tqMachineChuck")
public class TqMachineChuckController extends AbstractDocBizController<TqMachineChuck> {

    @Autowired
    private ITqMachineChuckService tqMachineChuckService;

    @Autowired
    private ITqMachineInfoService tqMachineInfoService;

    @Resource
    private TqMachineChuckMapper tqMachineChuckMapper;

    @ApiOperation("查询胎圈机台寸口对应列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqMachineChuck queryVO) {
        startPage();
        List<TqMachineChuck> list = tqMachineChuckMapper.listMachineChuck(queryVO);
        return getDataTable(list);
    }

    @Log(title = "胎圈机台寸口对应", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqMachineChuck billVO) {
        return super.save(billVO);
    }

    @Log(title = "胎圈机台寸口对应", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqMachineChuck getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈机台寸口对应", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈机台寸口对应", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqMachineChuck queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        List<TqMachineChuckExportVO> list = getExportDataList(queryVO);
        ExcelUtil<TqMachineChuckExportVO> util = new ExcelUtil<>(TqMachineChuckExportVO.class);
        Workbook workbook = util.exportExcelFromList(list, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    @ApiOperation("校验机台寸口唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TqMachineChuck machineChuck) {
        return tqMachineChuckService.checkUnique(machineChuck);
    }

    @Log(title = "胎圈机台寸口对应", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tqMachineChuckService.deleteAllMachineChuck();
        return AjaxResult.success();
    }

    @Override
    protected IDocService getDocService() {
        return tqMachineChuckService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    protected List<TqMachineChuckExportVO> getExportDataList(TqMachineChuck obj) {
        QueryWrapper<TqMachineChuck> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        List<TqMachineChuck> list = tqMachineChuckMapper.selectList(wrapper);

        Map<Long, String> machineMap = new HashMap<>();
        if (!list.isEmpty()) {
            List<TqMachineInfo> machineList = tqMachineInfoService.selectMachineInfoList(new TqMachineInfo());
            machineMap = machineList.stream()
                    .collect(Collectors.toMap(TqMachineInfo::getId, TqMachineInfo::getMachineName, (v1, v2) -> v1));
        }

        List<TqMachineChuckExportVO> voList = new ArrayList<>();
        for (TqMachineChuck chuck : list) {
            TqMachineChuckExportVO vo = new TqMachineChuckExportVO();
            vo.setMachineName(machineMap.getOrDefault(chuck.getMachineId(), ""));
            vo.setChuckCode(chuck.getChuckCode());
            vo.setChuckName(chuck.getChuckName());
            vo.setInchSize(chuck.getInchSize());
            vo.setRemark(chuck.getRemark());
            vo.setUpdateTime(chuck.getUpdateTime());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    protected void builderCondition(QueryWrapper<TqMachineChuck> queryWrapper, TqMachineChuck queryVO) {
        queryWrapper.eq("IS_DELETE", 0);
        queryWrapper.eq(queryVO.getMachineId() != null, "MACHINE_ID", queryVO.getMachineId());
    }
}
