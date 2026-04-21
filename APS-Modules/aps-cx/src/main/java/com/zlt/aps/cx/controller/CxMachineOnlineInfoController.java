package com.zlt.aps.cx.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import com.zlt.aps.cx.api.domain.entity.CxMachineOnlineInfo;
import com.zlt.aps.cx.mapper.CxMachineOnlineInfoMapper;
import com.zlt.aps.cx.service.ICxMachineOnlineInfoService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 成型在机信息业务控制器
 */
@Api(tags = "成型在机信息")
@RestController
@RequestMapping("/cxMachineOnlineInfo")
public class CxMachineOnlineInfoController extends AbstractDocBizController<CxMachineOnlineInfo> {

    @Autowired
    private ICxMachineOnlineInfoService cxMachineOnlineInfoService;

    @Resource
    private CxMachineOnlineInfoMapper cxMachineOnlineInfoMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody CxMachineOnlineInfo queryVO) {
        return super.list(queryVO);
    }

    @ApiOperation("获取详情")
    @GetMapping(value = "/{billId}")
    @Override
    public CxMachineOnlineInfo getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    @Log(title = "ui.data.column.cxMachineOnlineInfo.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody CxMachineOnlineInfo entity) {
        return super.save(entity);
    }

    @Log(title = "ui.data.column.cxMachineOnlineInfo.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }

//    @ApiOperation("\u6821\u9A8C\u552F\u4E00\u6027")
//    @PostMapping("/checkUnique")
//    public String checkUnique(@RequestBody CxMachineOnlineInfo entity) {
//        return UserConstants.UNIQUE;
//    }

    @Log(title = "ui.data.column.cxMachineOnlineInfo.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    @Log(title = "ui.data.column.cxMachineOnlineInfo.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody CxMachineOnlineInfo queryVO, @PathVariable("fileName") String fileName, HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<CxMachineOnlineInfo> listExportData(CxMachineOnlineInfo obj) {
        QueryWrapper<CxMachineOnlineInfo> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return cxMachineOnlineInfoMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return cxMachineOnlineInfoService;
    }

    @Override
    protected void builderCondition(QueryWrapper<CxMachineOnlineInfo> queryWrapper, CxMachineOnlineInfo queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("onlineDate")), "ONLINE_DATE", queryVO.getFieldValueByFieldName("onlineDate"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("cxCode")), "CX_CODE", queryVO.getFieldValueByFieldName("cxCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mesMaterialCode")), "MES_MATERIAL_CODE", queryVO.getFieldValueByFieldName("mesMaterialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specDesc")), "SPEC_DESC", queryVO.getFieldValueByFieldName("specDesc"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoSpec")), "EMBRYO_SPEC", queryVO.getFieldValueByFieldName("embryoSpec"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dataVersion")), "DATA_VERSION", queryVO.getFieldValueByFieldName("dataVersion"));
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }
}

