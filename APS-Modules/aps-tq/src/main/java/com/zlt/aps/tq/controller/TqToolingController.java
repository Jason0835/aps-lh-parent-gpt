package com.zlt.aps.tq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqTooling;
import com.zlt.aps.tq.api.domain.vo.TqToolingExportVO;
import com.zlt.aps.tq.mapper.TqToolingMapper;
import com.zlt.aps.tq.service.ITqToolingService;
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

@Slf4j
@Api(tags = "胎圈工装管理")
@RestController
@RequestMapping("/tqTooling")
public class TqToolingController extends AbstractDocBizController<TqTooling> {

    @Autowired
    private ITqToolingService tqToolingService;

    @Resource
    private TqToolingMapper tqToolingMapper;

    @ApiOperation("查询胎圈工装管理列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqTooling queryVO) {
        startPage();
        List<TqTooling> list = tqToolingMapper.listTooling(queryVO);
        return getDataTable(list);
    }

    @Log(title = "胎圈工装管理", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqTooling billVO) {
        return super.save(billVO);
    }

    @Log(title = "胎圈工装管理", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqTooling getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈工装管理", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈工装管理", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqTooling queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        List<TqToolingExportVO> list = getExportDataList(queryVO);
        ExcelUtil<TqToolingExportVO> util = new ExcelUtil<>(TqToolingExportVO.class);
        Workbook workbook = util.exportExcelFromList(list, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    @ApiOperation("查询所有未删除的工装列表")
    @PostMapping("/listAllTooling")
    public AjaxResult listAllTooling() {
        QueryWrapper<TqTooling> wrapper = new QueryWrapper<>();
        wrapper.eq("IS_DELETE", 0);
        wrapper.orderByAsc("TOOLING_CODE");
        return AjaxResult.success(tqToolingMapper.selectList(wrapper));
    }

    @ApiOperation("校验工装编码唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TqTooling tooling) {
        return tqToolingService.checkUnique(tooling);
    }

    @Log(title = "胎圈工装管理", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tqToolingService.deleteAllTooling();
        return AjaxResult.success();
    }

    @Override
    protected IDocService getDocService() {
        return tqToolingService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    protected List<TqToolingExportVO> getExportDataList(TqTooling obj) {
        QueryWrapper<TqTooling> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        List<TqTooling> list = tqToolingMapper.selectList(wrapper);

        List<TqToolingExportVO> voList = new ArrayList<>();
        for (TqTooling tooling : list) {
            TqToolingExportVO vo = new TqToolingExportVO();
            vo.setToolingCode(tooling.getToolingCode());
            vo.setToolingName(tooling.getToolingName());
            vo.setTotalQty(tooling.getTotalQty());
            vo.setRemark(tooling.getRemark());
            vo.setUpdateTime(tooling.getUpdateTime());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    protected void builderCondition(QueryWrapper<TqTooling> queryWrapper, TqTooling queryVO) {
        queryWrapper.eq("IS_DELETE", 0);
        queryWrapper.like(queryVO.getToolingCode() != null && !queryVO.getToolingCode().isEmpty(),
                "TOOLING_CODE", queryVO.getToolingCode());
    }
}
