package com.zlt.aps.tq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.tq.api.domain.entity.TqToolingCartCapacity;
import com.zlt.aps.tq.api.domain.vo.TqToolingCartCapacityExportVO;
import com.zlt.aps.tq.mapper.TqToolingCartCapacityMapper;
import com.zlt.aps.tq.service.ITqToolingCartCapacityService;
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
@Api(tags = "胎圈工装车容量管理")
@RestController
@RequestMapping("/tqToolingCartCapacity")
public class TqToolingCartCapacityController extends AbstractDocBizController<TqToolingCartCapacity> {

    @Autowired
    private ITqToolingCartCapacityService tqToolingCartCapacityService;

    @Resource
    private TqToolingCartCapacityMapper tqToolingCartCapacityMapper;

    @ApiOperation("查询胎圈工装车容量管理列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody TqToolingCartCapacity queryVO) {
        startPage();
        List<TqToolingCartCapacity> list = tqToolingCartCapacityMapper.listToolingCartCapacity(queryVO);
        return getDataTable(list);
    }

    @Log(title = "胎圈工装车容量管理", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody TqToolingCartCapacity billVO) {
        return super.save(billVO);
    }

    @Log(title = "胎圈工装车容量管理", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public TqToolingCartCapacity getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "胎圈工装车容量管理", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "胎圈工装车容量管理", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody TqToolingCartCapacity queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        List<TqToolingCartCapacityExportVO> list = getExportDataList(queryVO);
        ExcelUtil<TqToolingCartCapacityExportVO> util = new ExcelUtil<>(TqToolingCartCapacityExportVO.class);
        Workbook workbook = util.exportExcelFromList(list, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    @ApiOperation("校验工装车编码+胎圈编码唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody TqToolingCartCapacity entity) {
        return tqToolingCartCapacityService.checkUnique(entity);
    }

    @Log(title = "胎圈工装车容量管理", businessType = BusinessType.DELETE)
    @ApiOperation("删除全部(逻辑删)")
    @PostMapping("/deleteAll")
    public AjaxResult deleteAll() {
        tqToolingCartCapacityService.deleteAllToolingCartCapacity();
        return AjaxResult.success();
    }

    @Override
    protected IDocService getDocService() {
        return tqToolingCartCapacityService;
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    protected List<TqToolingCartCapacityExportVO> getExportDataList(TqToolingCartCapacity obj) {
        QueryWrapper<TqToolingCartCapacity> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        List<TqToolingCartCapacity> list = tqToolingCartCapacityMapper.selectList(wrapper);

        List<TqToolingCartCapacityExportVO> voList = new ArrayList<>();
        for (TqToolingCartCapacity entity : list) {
            TqToolingCartCapacityExportVO vo = new TqToolingCartCapacityExportVO();
            vo.setCartCode(entity.getCartCode());
            vo.setMaterialCode(entity.getMaterialCode());
            vo.setCartCapacity(entity.getCartCapacity());
            vo.setRemark(entity.getRemark());
            vo.setUpdateTime(entity.getUpdateTime());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    protected void builderCondition(QueryWrapper<TqToolingCartCapacity> queryWrapper, TqToolingCartCapacity queryVO) {
        queryWrapper.eq("IS_DELETE", 0);
        queryWrapper.like(queryVO.getCartCode() != null && !queryVO.getCartCode().isEmpty(),
                "CART_CODE", queryVO.getCartCode());
        queryWrapper.like(queryVO.getMaterialCode() != null && !queryVO.getMaterialCode().isEmpty(),
                "MATERIAL_CODE", queryVO.getMaterialCode());
    }
}
