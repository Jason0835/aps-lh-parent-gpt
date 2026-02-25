package com.zlt.aps.itf.controller;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.itf.annotation.ItfApi;
import com.zlt.aps.itf.service.IRemoteLhMonthPlanSurplusService;
import com.zlt.aps.mp.api.domain.dto.LhMonthPlanSurplusDetailDto;
import com.zlt.aps.mp.api.domain.entity.LhMonthPlanSurplusDetail;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 月度外胎汇总
 *
 * @author Liam
 * @since 2025/4/10
 */
@Slf4j
@Api(tags = "月度外胎汇总")
@RestController
@RequestMapping("/aps/openApi/monthplan")
public class LhMonthPlanSurplusDetailController {

    @Resource
    private IRemoteLhMonthPlanSurplusService iRemoteLhMonthPlanSurplusService;

    /**
     * 查询月度外胎汇总列表
     */
    @ItfApi
    @ApiOperation("查询月度外胎汇总列表")
    @PostMapping("/scheduleInfo")
    public TableDataInfo scheduleInfo(@RequestBody LhMonthPlanSurplusDetailDto queryVO) {
        // 保证对应年、月、分厂参数需要填写
        if (queryVO.getYear() == null || queryVO.getMonth() == null || StringUtils.isBlank(queryVO.getFactoryCode())) {
            TableDataInfo info = new TableDataInfo();
            info.setCode(HttpStatus.ERROR);
            info.setMsg("请确认对应年、月、分厂");
            return info;
        }
        // 页码和条数不能为空
        if (queryVO.getPageNum() == null || queryVO.getPageSize() == null) {
            TableDataInfo info = new TableDataInfo();
            info.setCode(HttpStatus.ERROR);
            info.setMsg("请确认对应页码和条数");
            return info;
        }

        return iRemoteLhMonthPlanSurplusService.detailList(buildRequestParam(queryVO), queryVO.getPageNum(), queryVO.getPageSize());
    }

    /**
     * 构建查询参数
     */
    private LhMonthPlanSurplusDetail buildRequestParam(LhMonthPlanSurplusDetailDto queryVO) {
        LhMonthPlanSurplusDetail param = new LhMonthPlanSurplusDetail();
        param.setYear(queryVO.getYear());
        param.setMonth(queryVO.getMonth());
        param.setFactoryCode(queryVO.getFactoryCode());
        param.setProductCode(queryVO.getProductCode());
        return param;
    }

}
