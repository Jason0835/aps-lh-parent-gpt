package com.zlt.aps.lh.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import com.zlt.aps.lh.mapper.LhRepairCapsuleMapper;
import com.zlt.aps.lh.service.ILhRepairCapsuleService;
import com.zlt.aps.utils.AppUtils;
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

@Slf4j
@Api(tags = "APS胶囊已使用次数")
@RestController
@RequestMapping("/lhRepairCapsule")
public class LhRepairCapsuleController extends AbstractDocBizController<LhRepairCapsule> {

    @Autowired
    private ILhRepairCapsuleService lhRepairCapsuleService;

    @Resource
    private LhRepairCapsuleMapper lhRepairCapsuleMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhRepairCapsule queryVO) {
        return super.list(queryVO);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhRepairCapsule getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    /**
     * 导出列表
     */
    @Log(title = "ui.data.column.lhRepairCapsule.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhRepairCapsule queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<LhRepairCapsule> listExportData(LhRepairCapsule obj) {
        QueryWrapper<LhRepairCapsule> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        List<LhRepairCapsule> list = lhRepairCapsuleMapper.selectList(wrapper);
        for (LhRepairCapsule lhRepairCapsule : list){
            lhRepairCapsule.setUpdateDate(lhRepairCapsule.getUpdateTime());
        }
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return lhRepairCapsuleService;
    }

    @Override
    protected void builderCondition(QueryWrapper<LhRepairCapsule> queryWrapper, LhRepairCapsule queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("brand")), "BRAND", queryVO.getFieldValueByFieldName("brand"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhCode")), "LH_CODE", queryVO.getFieldValueByFieldName("lhCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.apply(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")),
                "EXISTS (SELECT 1 FROM T_MDM_MATERIAL_INFO m WHERE m.MATERIAL_CODE = t_lh_repair_capsule.MATERIAL_CODE AND m.MATERIAL_DESC LIKE CONCAT('%',{0},'%'))",
                queryVO.getFieldValueByFieldName("materialDesc"));
        String obtainTimeBegin = queryVO.getObtainTimeBegin();
        String obtainTimeEnd = queryVO.getObtainTimeEnd();
        if (PubUtil.isNotEmpty(obtainTimeBegin)) {
            queryWrapper.ge("OBTAIN_TIME", DateUtil.beginOfDay(DateUtil.parse(obtainTimeBegin, "yyyy-MM-dd")));
        }
        if (PubUtil.isNotEmpty(obtainTimeEnd)) {
            queryWrapper.le("OBTAIN_TIME", DateUtil.endOfDay(DateUtil.parse(obtainTimeEnd, "yyyy-MM-dd")));
        }
        queryWrapper.orderByDesc("UPDATE_TIME");
        queryWrapper.orderByAsc("LH_CODE");
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String[] getQueryFormulas() {
        return lhRepairCapsuleService.getQueryFormulas();
    }

    @Override
    protected String getOrderBy() {
        return "UPDATE_TIME desc, LH_CODE asc";
    }
}
