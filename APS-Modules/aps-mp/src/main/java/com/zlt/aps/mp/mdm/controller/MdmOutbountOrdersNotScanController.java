package com.zlt.aps.mp.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.maindata.service.IMdmOutbountOrdersNotScanService;
import com.zlt.aps.mp.api.domain.entity.MdmOutbountOrdersNotScan;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Api(tags = "出库未扫描订单")
@RestController
@RequestMapping("/mdmOutbountOrdersNotScan")
public class MdmOutbountOrdersNotScanController extends AbstractDocBizController<MdmOutbountOrdersNotScan> {

    @Autowired
    private IMdmOutbountOrdersNotScanService mdmOutbountOrdersNotScanService;

    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmOutbountOrdersNotScan queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    @Override
    protected IDocService getDocService() {
        return mdmOutbountOrdersNotScanService;
    }

    @Override
    protected void builderCondition(QueryWrapper<MdmOutbountOrdersNotScan> queryWrapper, MdmOutbountOrdersNotScan queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFactoryCode()), "FACTORY_CODE", queryVO.getFactoryCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSaleBillNo()), "SALE_BILL_NO", queryVO.getSaleBillNo());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSaleOrderNo()), "SALE_ORDER_NO", queryVO.getSaleOrderNo());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getBillId()), "BILL_ID", queryVO.getBillId());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMaterialCode()), "MATERIAL_CODE", queryVO.getMaterialCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSapCode()), "SAP_CODE", queryVO.getSapCode());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getMaterialName()), "MATERIAL_NAME", queryVO.getMaterialName());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSaleOrg()), "SALE_ORG", queryVO.getSaleOrg());
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getSellTo()), "SELL_TO", queryVO.getSellTo());
    }

    @Override
    protected String getTypeCode() {
        return "MDM0217";
    }
}
