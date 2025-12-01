package com.zlt.aps.xwyy.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.xwyy.api.domain.dto.XwyyLossSettingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 纤维压延损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-19
 */
@FeignClient(contextId = "IXwyyLossSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.xwyy:xwyy}")
public interface IXwyyLossSettingService {

    /**
     * 查询纤维压延损耗率设定列表
     */
    @PostMapping("/xwyy/loss/list")
    TableDataInfo list(@RequestBody XwyyLossSettingDto dto);

    /**
     * 新增纤维压延损耗率设定
     */
    @PostMapping("/xwyy/loss/add")
    AjaxResult add(@RequestBody XwyyLossSettingDto dto);

    /**
     * 修改纤维压延损耗率设定
     */
    @PostMapping("/xwyy/loss/edit")
    AjaxResult edit(@RequestBody XwyyLossSettingDto dto);

    /**
     * 删除纤维压延损耗率设定
     */
    @DeleteMapping("/xwyy/loss/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/xwyy/loss/{id}")
    XwyyLossSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验纤维压延损耗率设定唯一性
     */
    @PostMapping("/xwyy/loss/checkXwyyLossSettingUnique")
    String checkXwyyLossSettingUnique(@RequestBody XwyyLossSettingDto dto);

    /**
     * 导出纤维压延损耗率设定列表
     */
    @PostMapping("/xwyy/loss/getList")
    List<XwyyLossSettingDto> getList(@RequestBody XwyyLossSettingDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/xwyy/loss/importData")
    public AjaxResult importData(@RequestBody List<XwyyLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/xwyy/loss/deleteAll")
    AjaxResult deleteAll();
}
