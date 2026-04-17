package com.zlt.aps.lh.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhMachineOnlineInfo;
import com.zlt.aps.lh.mapper.LhMachineOnlineInfoMapper;
import com.zlt.aps.lh.service.ILhMachineOnlineInfoService;
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
import java.io.IOException;
import java.util.List;

/**
 * 硫化在机信息 Controller
 *
 * @author APS Team
 * @date 2026-04-17
 */
@Slf4j
@Api(tags = "硫化在机信息")
@RestController
@RequestMapping("/lhMachineOnlineInfo")
public class LhMachineOnlineInfoController extends AbstractDocBizController<LhMachineOnlineInfo> {

    @Autowired
    private ILhMachineOnlineInfoService lhMachineOnlineInfoService;

    @Resource
    private LhMachineOnlineInfoMapper lhMachineOnlineInfoMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhMachineOnlineInfo queryVO) {
        return super.list(queryVO);
    }

    @ApiOperation("获取详情信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhMachineOnlineInfo getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    @Log(title = "ui.data.column.lhMachineOnlineInfo.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhMachineOnlineInfo queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<LhMachineOnlineInfo> listExportData(LhMachineOnlineInfo obj) {
        QueryWrapper<LhMachineOnlineInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return lhMachineOnlineInfoMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return lhMachineOnlineInfoService;
    }

    @Override
    protected void builderCondition(QueryWrapper<LhMachineOnlineInfo> queryWrapper, LhMachineOnlineInfo queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getOnlineDate()), "ONLINE_DATE", queryVO.getOnlineDate());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getLhCode()), "LH_CODE", queryVO.getLhCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMaterialCode()), "MATERIAL_CODE", queryVO.getMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMesMaterialCode()), "MES_MATERIAL_CODE", queryVO.getMesMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSpecDesc()), "SPEC_DESC", queryVO.getSpecDesc());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getLrMolds()), "LR_MOLDS", queryVO.getLrMolds());
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getDataVersion()), "DATA_VERSION", queryVO.getDataVersion());
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "update_time desc, id desc";
    }
}

