package com.zlt.aps.lh.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.entity.LhRepairCapsule;
import com.zlt.aps.lh.mapper.LhRepairCapsuleMapper;
import com.zlt.aps.lh.service.ILhRepairCapsuleService;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
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

    @Override
    protected List<LhRepairCapsule> listExportData(LhRepairCapsule obj) {
        QueryWrapper<LhRepairCapsule> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return lhRepairCapsuleMapper.selectList(wrapper);
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
        String obtainTimeBegin = queryVO.getObtainTimeBegin();
        String obtainTimeEnd = queryVO.getObtainTimeEnd();
        if (PubUtil.isNotEmpty(obtainTimeBegin)) {
            queryWrapper.ge("OBTAIN_TIME", DateUtil.beginOfDay(DateUtil.parse(obtainTimeBegin, "yyyy-MM-dd")));
        }
        if (PubUtil.isNotEmpty(obtainTimeEnd)) {
            queryWrapper.le("OBTAIN_TIME", DateUtil.endOfDay(DateUtil.parse(obtainTimeEnd, "yyyy-MM-dd")));
        }
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "OBTAIN_TIME desc, id desc";
    }
}
