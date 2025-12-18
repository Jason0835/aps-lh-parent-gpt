package com.zlt.aps.itf.mes;

import com.ruoyi.common.constant.ServiceNameConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.monthplan.api.domain.entity.MdmModelInfo;
import com.zlt.aps.monthplan.api.domain.entity.MdmSkuMouldRel;
import io.swagger.annotations.ApiOperation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * MES接口业务
 *
 * @author Chen
 * @since 2025/12/16
 */
@FeignClient(contextId = "IMesItfService", value = ServiceNameConstants.GATEWAY_SERVICE, path = "${api.path.itf:/itf}")
public interface IMesItfService {

    /**
     * 同步SAP与模具关系
     *
     * @param mdmSkuMouldRel SAP与模具关系
     * @return 结果
     */
    @ApiOperation("同步SAP与模具关系")
    @PostMapping("/mesItf/syncProductModRelation")
    public AjaxResult syncProductModRelation(@RequestBody MdmSkuMouldRel mdmSkuMouldRel);

    /**
     * 同步模具台账
     *
     * @param modelInfo 模具台账
     * @return 结果
     */
    @ApiOperation("同步模具台账")
    @PostMapping("/mesItf/syncModelInfo")
    public AjaxResult syncModelInfo(@RequestBody MdmModelInfo modelInfo);
}
