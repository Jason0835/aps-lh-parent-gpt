package com.zlt.aps.cx.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cx.api.domain.dto.CxLossSettingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 成型损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-19
 */
@FeignClient(contextId = "ICxLossSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cx:cx}")
public interface ICxLossSettingService {

    /**
     * 查询成型损耗率设定列表
     */
    @PostMapping("/loss/list")
    TableDataInfo list(@RequestBody CxLossSettingDto dto);

    /**
     * 新增成型损耗率设定
     */
    @PostMapping("/loss/add")
    AjaxResult add(@RequestBody CxLossSettingDto dto);

    /**
     * 修改成型损耗率设定
     */
    @PostMapping("/loss/edit")
    AjaxResult edit(@RequestBody CxLossSettingDto dto);

    /**
     * 删除成型损耗率设定
     */
    @DeleteMapping("/loss/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/loss/{id}")
    CxLossSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验成型损耗率设定唯一性
     */
    @PostMapping("/loss/checkCxLossSettingUnique")
    String checkCxLossSettingUnique(@RequestBody CxLossSettingDto dto);

    /**
     * 导出成型损耗率设定列表
     */
    @PostMapping("/loss/getList")
    List<CxLossSettingDto> getList(@RequestBody CxLossSettingDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/loss/importData")
    public AjaxResult importData(@RequestBody List<CxLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
