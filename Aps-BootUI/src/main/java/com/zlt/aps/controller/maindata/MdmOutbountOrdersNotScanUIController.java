package com.zlt.aps.controller.maindata;

import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common4ui.core.controller.BaseUIController;
import com.zlt.aps.mp.api.service.IMdmOutbountOrdersNotScanRemoteService;
import com.zlt.aps.mp.api.domain.entity.MdmOutbountOrdersNotScan;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Api(tags = "出库未扫描订单")
@Controller
@RequestMapping("/monthplan/mdmOutbountOrdersNotScan")
public class MdmOutbountOrdersNotScanUIController extends BaseUIController<MdmOutbountOrdersNotScan> {

    @Autowired
    private IMdmOutbountOrdersNotScanRemoteService iMdmOutbountOrdersNotScanService;

    @ApiOperation("根据条件查询主表数据")
//    @RequiresPermissions("monthplan:mdmOutbountOrdersNotScan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmOutbountOrdersNotScan mdmOutbountOrdersNotScan) {
        return iMdmOutbountOrdersNotScanService.list(mdmOutbountOrdersNotScan);
    }

    @Override
    public String getFunctionName() {
        return "出库未扫描订单";
    }
}
