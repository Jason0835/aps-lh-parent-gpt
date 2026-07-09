package com.zlt.aps.gsq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.domain.entity.GsqSpecifyMachine;
import com.zlt.aps.gsq.api.domain.vo.GsqSpecifyMachineExportVO;
import com.zlt.aps.gsq.mapper.GsqSpecifyMachineMapper;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.IGsqSpecifyMachineService;
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
 * 钢丝圈定点机台Controller
 * 路径：/gsq/specifyMachine
 *
 * @author zlt
 * @date 2026-07-08
 */
@Slf4j
@Api(tags = "钢丝圈定点机台")
@RestController
@RequestMapping("/gsq/specifyMachine")
public class GsqSpecifyMachineController extends AbstractDocBizController<GsqSpecifyMachine> {

    @Autowired
    private IGsqSpecifyMachineService gsqSpecifyMachineService;

    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    @Resource
    private GsqSpecifyMachineMapper gsqSpecifyMachineMapper;

    /**
     * 查询钢丝圈定点机台列表
     */
    @ApiOperation("查询钢丝圈定点机台列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GsqSpecifyMachine queryVO) {
        startPage();
        List<GsqSpecifyMachine> list = gsqSpecifyMachineMapper.listSpecifyMachine(queryVO);
        return getDataTable(list);
    }

    /**
     * 保存钢丝圈定点机台（id为空新增，id不为空修改）
     * 父类内部会调用 Service 的 checkUnique 进行"钢丝圈代码+生产线"唯一性校验
     */
    @Log(title = "钢丝圈定点机台", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody GsqSpecifyMachine billVO) {
        return super.save(billVO);
    }

    /**
     * 删除钢丝圈定点机台（逻辑删除）
     */
    @Log(title = "钢丝圈定点机台", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @PostMapping("/delete/{ids}")
    public AjaxResult deleteByIds(@PathVariable("ids") List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取钢丝圈定点机台详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping("/{id}")
    @Override
    public GsqSpecifyMachine getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 导入钢丝圈定点机台
     */
    @Log(title = "钢丝圈定点机台", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext,
                                 @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出钢丝圈定点机台
     */
    @Log(title = "钢丝圈定点机台", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody GsqSpecifyMachine queryVO,
                             @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        List<GsqSpecifyMachineExportVO> list = getExportDataList(queryVO);
        ExcelUtil<GsqSpecifyMachineExportVO> util = new ExcelUtil<>(GsqSpecifyMachineExportVO.class);
        Workbook workbook = util.exportExcelFromList(list, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    /**
     * 校验钢丝圈定点机台唯一性
     */
    @ApiOperation("校验钢丝圈定点机台唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody GsqSpecifyMachine entity) {
        return gsqSpecifyMachineService.checkUnique(entity);
    }

    @Override
    protected IDocService getDocService() {
        return gsqSpecifyMachineService;
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
    protected void builderCondition(QueryWrapper<GsqSpecifyMachine> queryWrapper, GsqSpecifyMachine queryVO) {
        queryWrapper.eq("IS_DELETE", "0");
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getSteelRingCode()), "STEEL_RING_CODE", queryVO.getSteelRingCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getLineType()), "LINE_TYPE", queryVO.getLineType());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getJobType()), "JOB_TYPE", queryVO.getJobType());
    }

    /**
     * 获取导出数据列表，并补反显生产线名称字段
     */
    protected List<GsqSpecifyMachineExportVO> getExportDataList(GsqSpecifyMachine obj) {
        QueryWrapper<GsqSpecifyMachine> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        List<GsqSpecifyMachine> list = gsqSpecifyMachineMapper.selectList(wrapper);

        Map<String, String> machineMap = new java.util.HashMap<>();
        if (!list.isEmpty()) {
            List<GsqMachineInfo> machineList = gsqMachineInfoService.selectMachineInfoList(new GsqMachineInfo());
            machineMap = machineList.stream()
                    .collect(Collectors.toMap(GsqMachineInfo::getMachineCode, GsqMachineInfo::getMachineName, (v1, v2) -> v1));
        }

        List<GsqSpecifyMachineExportVO> voList = new ArrayList<>();
        for (GsqSpecifyMachine entity : list) {
            GsqSpecifyMachineExportVO vo = new GsqSpecifyMachineExportVO();
            vo.setSteelRingCode(entity.getSteelRingCode());
            vo.setMachineCode(entity.getMachineCode());
            vo.setMachineName(machineMap.getOrDefault(entity.getMachineCode(), ""));
            vo.setLineType(entity.getLineType());
            vo.setJobType(entity.getJobType());
            vo.setRemark(entity.getRemark());
            vo.setUpdateTime(entity.getUpdateTime());
            voList.add(vo);
        }
        return voList;
    }
}
