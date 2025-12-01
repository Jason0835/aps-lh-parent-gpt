package com.zlt.aps.tc.api.service;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.zlt.aps.tc.api.domain.dto.TcCurlRollDto;
import com.zlt.aps.tc.api.domain.entity.TcCurlRoll;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 胎侧卷曲信息对外暴露接口
 */
@FeignClient(contextId = "iTcCurlRollService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.tc:tc}")
public interface ITcCurlRollService {

    /**
     * 根据条件查询帘布大卷信息列表
     */
    @PostMapping("/tc/curlRoll/listCurlRoll")
    TableDataInfo listCurlRoll(@RequestBody TcCurlRoll dto);

    /**
     * 根据id查询帘布大卷信息信息
     */
    @GetMapping("/tc/curlRoll/getCurlRoll/{id}")
    TcCurlRoll getCurlRoll(@PathVariable("id") Long id);

    /**
     * 保存帘布大卷信息信息（id为空则新增，id不为空则修改）
     */
    @PostMapping("/tc/curlRoll/saveCurlRoll")
    AjaxResult saveCurlRoll(@RequestBody TcCurlRoll dto);

    /**
     * 根据code判断纤维大卷代号是否已经存在
     */
    @PostMapping("/tc/curlRoll/checkCurlRollCodeUnique")
    String checkCurlRollCodeUnique(@RequestBody TcCurlRoll dto);

    /**
     * 批量删除帘布大卷信息信息(逻辑删)
     *
     * @param ids 多个id逗号分割
     */
    @PostMapping("/tc/curlRoll/deleteCurlRoll/{ids}")
    AjaxResult deleteCurlRoll(@PathVariable("ids") Long[] ids);

    /**
     * 导出接口
     *
     * @param dto
     */
    @PostMapping("/tc/curlRoll/exportData")
    List<TcCurlRoll> exportData(@RequestBody TcCurlRoll dto);

    /**
     * 导入数据
     */
    @PostMapping("/tc/curlRoll/importData")
    public AjaxResult importData(@RequestBody List<TcCurlRollDto> list, @RequestParam("updateSupport") boolean updateSupport, @RequestParam("importLogId") Long importLogId);

    /**
     * 根据编号查询卷曲长度
     *
     * @param curlRoll 查询条件
     * @return 结果
     */
    @ApiOperation("根据编号查询卷曲长度")
    @PostMapping("/tc/curlRoll/selectCurlLengthByCode")
    public AjaxResult selectCurlLengthByCode(@RequestBody TcCurlRoll curlRoll);
}
