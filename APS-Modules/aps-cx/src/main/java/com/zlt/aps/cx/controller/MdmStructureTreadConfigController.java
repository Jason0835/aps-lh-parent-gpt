package com.zlt.aps.cx.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.maindata.mapper.MdmStructureTreadConfigEntityMapper;
import com.zlt.aps.maindata.service.IMdmStructureTreadConfigService;
import com.zlt.aps.mp.api.domain.entity.MdmStructureTreadConfig;
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
@Api(tags = "APS结构整车胎面配置")
@RestController
@RequestMapping("/mdmStructureTreadConfig")
public class MdmStructureTreadConfigController extends AbstractDocBizController<MdmStructureTreadConfig> {

    @Autowired
    private IMdmStructureTreadConfigService mdmStructureTreadConfigService;

    @Resource
    private MdmStructureTreadConfigEntityMapper mdmStructureTreadConfigEntityMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmStructureTreadConfig queryVO) {
        return super.list(queryVO);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmStructureTreadConfig getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    @Override
    protected List<MdmStructureTreadConfig> listExportData(MdmStructureTreadConfig obj) {
        QueryWrapper<MdmStructureTreadConfig> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return mdmStructureTreadConfigEntityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return null;
    }

    @Override
    protected void builderCondition(QueryWrapper<MdmStructureTreadConfig> queryWrapper, MdmStructureTreadConfig queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("structureCode")), "STRUCTURE_CODE", queryVO.getFieldValueByFieldName("structureCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("treadCount")), "TREAD_COUNT", queryVO.getFieldValueByFieldName("treadCount"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dataVersion")), "DATA_VERSION", queryVO.getFieldValueByFieldName("dataVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("delFlag")), "DEL_FLAG", queryVO.getFieldValueByFieldName("delFlag"));
        String stockDateBegin = queryVO.getStockDateBegin();
        String stockDateEnd = queryVO.getStockDateEnd();
        if (PubUtil.isNotEmpty(stockDateBegin)) {
            queryWrapper.ge("STOCK_DATE", DateUtil.beginOfDay(DateUtil.parse(stockDateBegin, "yyyy-MM-dd")));
        }
        if (PubUtil.isNotEmpty(stockDateEnd)) {
            queryWrapper.le("STOCK_DATE", DateUtil.endOfDay(DateUtil.parse(stockDateEnd, "yyyy-MM-dd")));
        }
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "STOCK_DATE desc, id desc";
    }
}
