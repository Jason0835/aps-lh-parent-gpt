package com.zlt.aps.cx.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.service.CxMatchingSpecifyMachineService;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxMatchingSpecifyMachineList;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 定点机台Controller
 *
 * @author zlt
 * @date 2021-06-11
 */
@Api(tags = "成型定点机台接口")
@RestController
@RequestMapping("/specifyMachine")
public class CxMatchingSpecifyMachineController {

    @Autowired
    private CxMatchingSpecifyMachineService tSpecifyMachineService;

    @PostMapping("/detail/viewList")
    public AjaxResult viewList(@RequestBody CxMatchingSpecifyMachineList cxMatchingSpecifyMachineList) {
        List<CxMatchingSpecifyMachineList> list = tSpecifyMachineService.viewList(cxMatchingSpecifyMachineList);
        return AjaxResult.success(list);
    }


}
