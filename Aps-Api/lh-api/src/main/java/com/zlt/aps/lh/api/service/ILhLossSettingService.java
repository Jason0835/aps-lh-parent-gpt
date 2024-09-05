package com.zlt.aps.lh.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.lh.api.domain.dto.LhLossSettingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 硫化损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-19
 */
@FeignClient(contextId = "ILhLossSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.lh:lh}")
public interface ILhLossSettingService {

    /**
     * 查询硫化损耗率设定列表
     */
    @PostMapping("/loss/list")
    TableDataInfo list(@RequestBody LhLossSettingDto dto);

    /**
     * 新增硫化损耗率设定
     */
    @PostMapping("/loss/add")
    AjaxResult add(@RequestBody LhLossSettingDto dto);

    /**
     * 修改硫化损耗率设定
     */
    @PostMapping("/loss/edit")
    AjaxResult edit(@RequestBody LhLossSettingDto dto);

    /**
     * 删除硫化损耗率设定
     */
    @DeleteMapping("/loss/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/loss/{id}")
    LhLossSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验硫化损耗率设定唯一性
     */
    @PostMapping("/loss/checkLhLossSettingUnique")
    String checkLhLossSettingUnique(@RequestBody LhLossSettingDto dto);

    /**
     * 导出硫化损耗率设定列表
     */
    @PostMapping("/loss/getList")
    List<LhLossSettingDto> getList(@RequestBody LhLossSettingDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/loss/importData")
    public AjaxResult importData(@RequestBody List<LhLossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

}
