package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.dto.TcLossSettingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-13
 */
@FeignClient(contextId = "ITcLossSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:tc}")
public interface ITcLossSettingService {

    /**
     * 查询胎侧损耗率设定列表
     */
    @PostMapping("/loss/list")
    TableDataInfo list(@RequestBody TcLossSettingDto dto);

    /**
     * 新增胎侧损耗率设定
     */
    @PostMapping("/loss/add")
    AjaxResult add(@RequestBody TcLossSettingDto dto);

    /**
     * 修改胎侧损耗率设定
     */
    @PostMapping("/loss/edit")
    AjaxResult edit(@RequestBody TcLossSettingDto dto);

    /**
     * 删除胎侧损耗率设定
     */
    @DeleteMapping("/loss/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/loss/{id}")
    TcLossSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验胎侧损耗率设定唯一性
     */
    @PostMapping("/loss/checkTcLossSettingUnique")
    String checkTcLossSettingUnique(@RequestBody TcLossSettingDto dto);

    /**
     * 导出胎侧损耗率设定列表
     */
    @PostMapping("/loss/getList")
    List<TcLossSettingDto> getList(@RequestBody TcLossSettingDto dto);

    /**
     * 数据导入
     */
    @PostMapping("/loss/importData")
    AjaxResult importData(@RequestBody List<TcLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);


    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/loss/deleteAll")
    AjaxResult deleteAll();
}
