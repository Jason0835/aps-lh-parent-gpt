package com.zlt.aps.nc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.nc.api.domain.dto.NcCurlRollDto;
import com.zlt.aps.nc.api.domain.entity.NcCurlRoll;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 内衬卷曲信息对外暴露接口
 */
@FeignClient(contextId = "iNcCurlRollService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.nc:nc}")
public interface INcCurlRollService {

    /**
     * 根据条件查询帘布大卷信息列表
     */
    @PostMapping("/nc/curlRoll/listCurlRoll")
    TableDataInfo listCurlRoll(@RequestBody NcCurlRoll dto);

    /**
     * 根据id查询帘布大卷信息信息
     */
    @GetMapping("/nc/curlRoll/getCurlRoll/{id}")
    NcCurlRoll getCurlRoll(@PathVariable("id") Long id);

    /**
     * 保存帘布大卷信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/nc/curlRoll/saveCurlRoll")
    AjaxResult saveCurlRoll(@RequestBody NcCurlRoll dto);

    /**
     * 根据code判断纤维大卷代号是否已经存在
     */
    @PostMapping("/nc/curlRoll/checkCurlRollCodeUnique")
    String checkCurlRollCodeUnique(@RequestBody NcCurlRoll dto);

    /**
     * 批量删除帘布大卷信息信息(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/nc/curlRoll/deleteCurlRoll/{ids}")
    AjaxResult deleteCurlRoll(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     *
     * @param dto
     */
    @PostMapping("/nc/curlRoll/exportData")
    List<NcCurlRoll> exportData(@RequestBody NcCurlRoll dto);

    /**
     * 导入数据
     */
    @PostMapping("/nc/curlRoll/importData")
    public AjaxResult importData(@RequestBody List<NcCurlRollDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 根据编号查询卷曲长度
     *
     * @param curlRoll 查询条件
     * @return 结果
     */
    @ApiOperation("根据编号查询卷曲长度")
    @PostMapping("/nc/curlRoll/selectCurlLengthByCode")
    public AjaxResult selectCurlLengthByCode(@RequestBody NcCurlRoll curlRoll);
}
