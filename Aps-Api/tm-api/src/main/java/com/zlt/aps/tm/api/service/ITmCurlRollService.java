package com.zlt.aps.tm.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tm.api.domain.entity.TmCurlRoll;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎面卷曲信息对外暴露接口
 */
@FeignClient(contextId = "iTmCurlRollService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tm:tm}")
public interface ITmCurlRollService {

    /**
     * 根据条件查询帘布大卷信息列表
     */
    @PostMapping("/tm/curlRoll/listCurlRoll")
    TableDataInfo listCurlRoll(@RequestBody TmCurlRoll dto);

    /**
     * 根据id查询帘布大卷信息信息
     */
    @GetMapping("/tm/curlRoll/getCurlRoll/{id}")
    TmCurlRoll getCurlRoll(@PathVariable("id") Long id);

    /**
     * 保存帘布大卷信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/tm/curlRoll/saveCurlRoll")
    AjaxResult saveCurlRoll(@RequestBody TmCurlRoll dto);

    /**
     * 根据code判断纤维大卷代号是否已经存在
     */
    @PostMapping("/tm/curlRoll/checkCurlRollCodeUnique")
    String checkCurlRollCodeUnique(@RequestBody TmCurlRoll dto);

    /**
     * 批量删除帘布大卷信息信息(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/tm/curlRoll/deleteCurlRoll/{ids}")
    AjaxResult deleteCurlRoll(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     *
     * @param dto
     */
    @PostMapping("/tm/curlRoll/exportData")
    List<TmCurlRoll> exportData(@RequestBody TmCurlRoll dto);

    /**
     * 导入数据
     */
    @PostMapping("/tm/curlRoll/importData")
    public AjaxResult importData(@RequestBody List<TmCurlRoll> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 根据编号查询卷曲长度
     *
     * @param curlRoll 查询条件
     * @return 结果
     */
    @ApiOperation("根据编号查询卷曲长度")
    @PostMapping("/tm/curlRoll/selectCurlLengthByCode")
    public AjaxResult selectCurlLengthByCode(@RequestBody TmCurlRoll curlRoll);
}
