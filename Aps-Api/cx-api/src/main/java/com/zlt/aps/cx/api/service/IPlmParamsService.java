package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.CxParamsDto;
import com.zlt.aps.cx.api.domain.entity.PlmConstructionInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * PLM参数对外暴露接口
 *
 */
@FeignClient(contextId = "IPlmParamsService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface IPlmParamsService {

    /**
     * 查询PLM参数信息列表
     */
    @PostMapping("/cx/plm/list")
    TableDataInfo list(@RequestBody PlmConstructionInfo dto);

    /**
     * 获取PLM参数信息详细信息
     */
    @GetMapping("/cx/plm/{id}")
    PlmConstructionInfo getInfo(@PathVariable("id") Long id);

    /**
     * 导出接口
     *
     * @param plm
     */
    @GetMapping("/cx/plm/exportData")
    List<PlmConstructionInfo> exportData(@SpringQueryMap PlmConstructionInfo plm);
}
