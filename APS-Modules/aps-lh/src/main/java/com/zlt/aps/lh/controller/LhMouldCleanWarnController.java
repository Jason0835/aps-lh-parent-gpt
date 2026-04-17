package com.zlt.aps.lh.controller;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.lh.api.domain.entity.LhMouldCleanWarn;
import com.zlt.aps.lh.mapper.LhMouldCleanWarnMapper;
import com.zlt.aps.lh.service.ILhMouldCleanWarnService;
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
 * 模具清洗预警Controller
 *
 * @author APS Team
 * @since 2026/04/10
 */
@Slf4j
@Api(tags = "模具清洗预警")
@RestController
@RequestMapping("/mouldCleanWarn")
public class LhMouldCleanWarnController extends AbstractDocBizController<LhMouldCleanWarn> {

    @Autowired
    private ILhMouldCleanWarnService lhMouldCleanWarnService;

    @Resource
    private LhMouldCleanWarnMapper lhMouldCleanWarnMapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody LhMouldCleanWarn queryVO) {
        return super.list(queryVO);
    }

    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public LhMouldCleanWarn getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }

    @Log(title = "模具清洗预警", businessType = BusinessType.EXPORT)
    @ApiOperation("导出数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody LhMouldCleanWarn queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<LhMouldCleanWarn> listExportData(LhMouldCleanWarn obj) {
        QueryWrapper<LhMouldCleanWarn> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        wrapper.last("ORDER BY " + getOrderBy());
        return lhMouldCleanWarnMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return lhMouldCleanWarnService;
    }

    @Override
    protected void builderCondition(QueryWrapper<LhMouldCleanWarn> queryWrapper, LhMouldCleanWarn queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getLhCode()), "LH_CODE", queryVO.getLhCode());

        if (queryVO.getOperTimeBegin() != null) {
            queryWrapper.ge("OPER_TIME", DateUtil.parse(queryVO.getOperTimeBegin()));
        }
        if (queryVO.getOperTimeEnd() != null) {
            queryWrapper.le("OPER_TIME", DateUtil.endOfDay(DateUtil.parse(queryVO.getOperTimeEnd())));
        }

        if (queryVO.getFirstWashTimeBegin() != null) {
            queryWrapper.ge("FIRST_WASH_TIME", DateUtil.parse(queryVO.getFirstWashTimeBegin()));
        }
        if (queryVO.getFirstWashTimeEnd() != null) {
            queryWrapper.le("FIRST_WASH_TIME", DateUtil.endOfDay(DateUtil.parse(queryVO.getFirstWashTimeEnd())));
        }

        if (queryVO.getSecondWashTimeBegin() != null) {
            queryWrapper.ge("SECOND_WASH_TIME", DateUtil.parse(queryVO.getSecondWashTimeBegin()));
        }
        if (queryVO.getSecondWashTimeEnd() != null) {
            queryWrapper.le("SECOND_WASH_TIME", DateUtil.endOfDay(DateUtil.parse(queryVO.getSecondWashTimeEnd())));
        }
    }

    @Override
    protected String getTypeCode() {
        return "0";
    }

    @Override
    protected String getOrderBy() {
        return "OPER_TIME desc, id desc";
    }
}
