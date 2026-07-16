package com.zlt.aps.gsq.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.gsq.api.domain.entity.GsqLossRate;
import com.zlt.aps.gsq.api.domain.entity.GsqMachineInfo;
import com.zlt.aps.gsq.api.domain.vo.GsqLossRateExportVO;
import com.zlt.aps.gsq.mapper.GsqLossRateMapper;
import com.zlt.aps.gsq.service.GsqMachineInfoService;
import com.zlt.aps.gsq.service.IGsqLossRateService;
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
 * 钢丝圈损耗率管理控制层
 * 路径：/gsq/lossRate
 * 业务规则：
 *   1. 钢丝圈编码与机台编码至少一个有值
 *   2. 损耗率必填
 *   3. "钢丝圈编码+机台编码"组合唯一
 *
 * @author zlt
 * @date 2026-07-08
 */
@Slf4j
@Api(tags = "钢丝圈损耗率管理")
@RestController
@RequestMapping("/gsq/lossRate")
public class GsqLossRateController extends AbstractDocBizController<GsqLossRate> {

    @Autowired
    private IGsqLossRateService gsqLossRateService;

    @Autowired
    private GsqMachineInfoService gsqMachineInfoService;

    @Resource
    private GsqLossRateMapper gsqLossRateMapper;

    /**
     * 查询钢丝圈损耗率列表（左联机台信息表反显机台名称）
     */
    @ApiOperation("查询钢丝圈损耗率列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody GsqLossRate queryVO) {
        startPage();
        List<GsqLossRate> list = gsqLossRateMapper.listLossRate(queryVO);
        return getDataTable(list);
    }

    /**
     * 新增钢丝圈损耗率
     * 父类内部会调用 Service 的 checkUnique 进行唯一性校验，
     * 同时 checkUnique 内置了"钢丝圈编码与机台编码至少一个有值"和"损耗率必填"的前置校验
     */
    @Log(title = "ui.data.column.gsq.lossRate.modalName", businessType = BusinessType.INSERT)
    @ApiOperation("新增钢丝圈损耗率")
    @PostMapping("/add")
    public AjaxResult add(@RequestBody GsqLossRate entity) {
        return super.save(entity);
    }

    /**
     * 编辑钢丝圈损耗率
     */
    @Log(title = "ui.data.column.gsq.lossRate.modalName", businessType = BusinessType.UPDATE)
    @ApiOperation("编辑钢丝圈损耗率")
    @PostMapping("/edit")
    public AjaxResult edit(@RequestBody GsqLossRate entity) {
        return super.save(entity);
    }

    /**
     * 删除钢丝圈损耗率（逻辑删除）
     */
    @Log(title = "ui.data.column.gsq.lossRate.modalName", businessType = BusinessType.DELETE)
    @ApiOperation("删除钢丝圈损耗率")
    @PostMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

    /**
     * 获取钢丝圈损耗率详细信息
     */
    @ApiOperation("获取钢丝圈损耗率详细信息")
    @GetMapping("/getInfo/{id}")
    @Override
    public GsqLossRate getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    /**
     * 校验钢丝圈损耗率唯一性
     * 同时承担"钢丝圈编码与机台编码至少一个有值"和"损耗率必填"的前置校验
     */
    @ApiOperation("校验钢丝圈损耗率唯一性")
    @PostMapping("/checkUnique")
    public String checkUnique(@RequestBody GsqLossRate entity) {
        return gsqLossRateService.checkUnique(entity);
    }

    /**
     * 导入钢丝圈损耗率
     */
    @Log(title = "ui.data.column.gsq.lossRate.modalName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入钢丝圈损耗率")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext,
                                 @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出钢丝圈损耗率
     */
    @Log(title = "ui.data.column.gsq.lossRate.modalName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出钢丝圈损耗率")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody GsqLossRate queryVO,
                             @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        List<GsqLossRateExportVO> list = getExportDataList(queryVO);
        ExcelUtil<GsqLossRateExportVO> util = new ExcelUtil<>(GsqLossRateExportVO.class);
        Workbook workbook = util.exportExcelFromList(list, fileName);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    @Override
    protected IDocService getDocService() {
        return gsqLossRateService;
    }

    @Override
    protected String getTypeCode() {
        return "GSQ_LOSS_RATE";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }

    /**
     * 构建查询条件
     * 钢丝圈编码按模糊匹配 %xxx%，机台编码精确匹配
     * 框架已自动过滤逻辑删除数据，无需手动追加 IS_DELETE 条件
     */
    @Override
    protected void builderCondition(QueryWrapper<GsqLossRate> queryWrapper, GsqLossRate queryVO) {
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSteelRingCode()), "STEEL_RING_CODE", queryVO.getSteelRingCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getMachineCode()), "MACHINE_CODE", queryVO.getMachineCode());
    }

    /**
     * 获取导出数据列表，并补反显机台名称字段
     */
    protected List<GsqLossRateExportVO> getExportDataList(GsqLossRate obj) {
        QueryWrapper<GsqLossRate> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        List<GsqLossRate> list = gsqLossRateMapper.selectList(wrapper);

        Map<String, String> machineMap = new java.util.HashMap<>();
        if (!list.isEmpty()) {
            List<GsqMachineInfo> machineList = gsqMachineInfoService.selectMachineInfoList(new GsqMachineInfo());
            machineMap = machineList.stream()
                    .collect(Collectors.toMap(GsqMachineInfo::getMachineCode, GsqMachineInfo::getMachineName, (v1, v2) -> v1));
        }

        List<GsqLossRateExportVO> voList = new ArrayList<>();
        for (GsqLossRate entity : list) {
            GsqLossRateExportVO vo = new GsqLossRateExportVO();
            vo.setSteelRingCode(entity.getSteelRingCode());
            vo.setMachineName(machineMap.getOrDefault(entity.getMachineCode(), ""));
            vo.setLossRate(entity.getLossRate());
            vo.setRemark(entity.getRemark());
            vo.setUpdateTime(entity.getUpdateTime());
            voList.add(vo);
        }
        return voList;
    }
}
