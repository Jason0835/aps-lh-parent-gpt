package com.zlt.aps.cd90.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.cd90.api.domain.dto.Cd90LossSettingDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 90度裁断损耗率设定Service接口
 *
 * @author chen
 * @date 2021-07-19
 */
@FeignClient(contextId = "ICd90LossSettingService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.cd90:cd90}")
public interface ICd90LossSettingService {

    /**
     * 查询90度裁断损耗率设定列表
     */
    @PostMapping("/loss/list")
    TableDataInfo list(@RequestBody Cd90LossSettingDto dto);

    /**
     * 新增90度裁断损耗率设定
     */
    @PostMapping("/loss/add")
    AjaxResult add(@RequestBody Cd90LossSettingDto dto);

    /**
     * 修改90度裁断损耗率设定
     */
    @PostMapping("/loss/edit")
    AjaxResult edit(@RequestBody Cd90LossSettingDto dto);

    /**
     * 删除90度裁断损耗率设定
     */
    @DeleteMapping("/loss/{ids}")
    AjaxResult remove(@PathVariable("ids") Long[] ids);

    /**
     * 根据ID获取详细信息
     */
    @GetMapping(value = "/loss/{id}")
    Cd90LossSettingDto getInfo(@PathVariable("id") Long id);

    /**
     * 校验90度裁断损耗率设定唯一性
     */
    @PostMapping("/loss/checkCd90LossSettingUnique")
    String checkCd90LossSettingUnique(@RequestBody Cd90LossSettingDto dto);

    /**
     * 导出90度裁断损耗率设定列表
     */
    @PostMapping("/loss/getList")
    List<Cd90LossSettingDto> getList(@RequestBody Cd90LossSettingDto dto);

    /**
     * 导入数据
     */
    @PostMapping("/loss/importData")
    public AjaxResult importData(@RequestBody List<Cd90LossSettingDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);
    /**
     * 删除全部(逻辑删)
     */
    @PostMapping("/loss/deleteAll")
    AjaxResult deleteAll();

}
