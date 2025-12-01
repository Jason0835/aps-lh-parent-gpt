package com.zlt.mix.setting.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.mix.setting.api.domain.entity.MesPmtRecipeWeight;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 配方称量明细Service接口
 *
 * @author chen
 * @date 2022-06-01
 */
@FeignClient(contextId = "IMesPmtRecipeWeightService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.schedule:mixSchedule}")
public interface IMesPmtRecipeWeightService {

    /**
     * 查询配方称量明细列表
     */
    @PostMapping("/MesPmtRecipeWeight/list")
    TableDataInfo listMesPmtRecipeWeight(@RequestBody MesPmtRecipeWeight mesPmtRecipeWeight);

    /**
     * 导出配方称量明细列表
     */
    @PostMapping("/MesPmtRecipeWeight/exportData")
    List<MesPmtRecipeWeight> exportData(@RequestBody MesPmtRecipeWeight mesPmtRecipeWeight);
}
