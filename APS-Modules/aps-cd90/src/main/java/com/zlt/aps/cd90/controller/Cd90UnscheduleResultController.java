package com.zlt.aps.cd90.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.cd90.api.domain.entity.Cd90UnscheduleResult;
import com.zlt.aps.cd90.mapper.Cd90UnscheduleResultMapper;
import com.zlt.aps.cd90.service.ICd90UnscheduleResultService;
import com.zlt.aps.utils.AppUtils;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/** 直裁未排结果Controller。 */
@Api(tags = "直裁未排结果")
@RestController
@RequestMapping("/cd90UnscheduleResult")
public class Cd90UnscheduleResultController extends AbstractDocBizController<Cd90UnscheduleResult> {

    @Resource
    private ICd90UnscheduleResultService service;
    @Resource
    private Cd90UnscheduleResultMapper mapper;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody Cd90UnscheduleResult query) {
        return super.list(query);
    }

    @ApiOperation("获取详情")
    @GetMapping("/getInfo/{id}")
    @Override
    public Cd90UnscheduleResult getInfo(@PathVariable("id") Long id) {
        return super.getInfo(id);
    }

    @Log(title = "ui.data.column.cd90UnscheduleResult.modelName", businessType = BusinessType.EXPORT)
    @ApiOperation("导出")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody Cd90UnscheduleResult query, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(query, fileName, response);
    }

    @Override
    protected List<Cd90UnscheduleResult> listExportData(Cd90UnscheduleResult obj) {
        QueryWrapper<Cd90UnscheduleResult> w = new QueryWrapper<>();
        builderCondition(w, obj);
        List<Cd90UnscheduleResult> list = mapper.selectList(w);
        AppUtils.formatData(list, getQueryFormulas());
        return list;
    }

    @Override
    protected IDocService getDocService() {
        return service;
    }

    @Override
    protected void builderCondition(QueryWrapper<Cd90UnscheduleResult> qw, Cd90UnscheduleResult vo) {
        qw.eq(PubUtil.isNotEmpty(vo.getFactoryCode()), "FACTORY_CODE", vo.getFactoryCode());
        qw.eq(vo.getScheduleDate() != null, "SCHEDULE_DATE", vo.getScheduleDate());
        qw.like(PubUtil.isNotEmpty(vo.getClothCode()), "CLOTH_CODE", vo.getClothCode());
        qw.eq(PubUtil.isNotEmpty(vo.getBatchNo()), "BATCH_NO", vo.getBatchNo());
        qw.eq(PubUtil.isNotEmpty(vo.getReasonCode()), "REASON_CODE", vo.getReasonCode());
    }

    @Override
    protected String getTypeCode() {
        return "CD90_UNSCHEDULE_RESULT";
    }

    @Override
    protected String getOrderBy() {
        return "CREATE_TIME desc";
    }
}
