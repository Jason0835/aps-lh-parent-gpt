package com.zlt.aps.utils;

import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.domain.BaseEntity;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.billcode.api.service.IRemoteSysBillCodeService;
import com.zlt.billcode.vo.BillCodeInfoVO;
import com.zlt.common.utils.MapUtils;
import com.zlt.common.utils.PubUtil;
import com.zlt.common.utils.UtilReflect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 *
 * 旧框架编码规则
 */
@Slf4j
@Component
public class BilllNoService {
//    @Autowired
//    private ISysBillCodeService sysBillCodeService;
    @Autowired
    private IRemoteSysBillCodeService sysBillCodeService;


    //生成单据号
    public String  getBillNo(BaseEntity billVO, String billTypeCode, Date billDate, String billNo, String orgIdField, Long orgId){
        AjaxResult ajaxResult = sysBillCodeService.getBillCode(getBillCodeInfoVO(billVO, billTypeCode,billDate,billNo,orgIdField,orgId));
        if (HttpStatus.SUCCESS == (Integer) ajaxResult.get(AjaxResult.CODE_TAG)) {
            billNo=(String) ajaxResult.get(AjaxResult.DATA_TAG);
        } else {
            String msg="调用框架编码规则错误："+ ajaxResult.get(AjaxResult.MSG_TAG);
            log.error(msg);
            throw new ServiceException(msg);
        }
       return billNo;
    }

    //回退单据号
    public  void returnBillNo(BaseEntity billVO, String billTypeCode, Date billDate,String billNo,String orgIdField,Long orgId) {
        sysBillCodeService.returnBillCode(getBillCodeInfoVO(billVO, billTypeCode,billDate,billNo,orgIdField,orgId));
    }

    /**
     * 编码规则对象
     *
     * @param billVO
     * @param billTypeCode 单据类型编码
     * @param billDate 业务日期
     * @param billNo  单据号
     * @param orgIdField  组织id字段
     * @param orgId  组织id
     * @return
     */
    protected  BillCodeInfoVO getBillCodeInfoVO(BaseEntity billVO, String billTypeCode, Date billDate, String billNo, String orgIdField, Long orgId){
        BillCodeInfoVO billCodeInfoVO = new BillCodeInfoVO<>();
        billCodeInfoVO.setBillTypeCode(billTypeCode);//
        billCodeInfoVO.setBillDate(billDate);
        billCodeInfoVO.setBillNo(billNo);
        billCodeInfoVO.setOrgIdField(StringUtils.isEmpty(orgIdField)?"orgId":orgIdField);
        billCodeInfoVO.setOrgId(orgId);
        Map<String, Object> billheadDataMap = MapUtils.beanToMap(billCodeInfoVO);
        billCodeInfoVO.setBillHeadDataMap(billheadDataMap);
        //单据号对象
        List<Map<String, Object>> billCodeFieldMaps = sysBillCodeService.getBillCodeFields(billTypeCode);
        if (PubUtil.isNotEmpty(billCodeFieldMaps)){
            String billCodeField;
            for (Map<String, Object> billCodeFieldMap : billCodeFieldMaps) {
                billCodeField = (String) billCodeFieldMap.get("billCodeField");
                if (UtilReflect.isFieldExist(billVO.getClass(), billCodeField)){
                    billheadDataMap.put(billCodeField, billVO.getFieldValueByFieldName(billCodeField));
                }
            }
        }

        return billCodeInfoVO;
    }

    /**
     * 批量生成单据编号
     * 
     * @param billNumber   单据数，对用需要生成的编号数
     * @param billVO       单据对象，可以传new对象
     * @param billTypeCode
     * @param billDate
     * @param orgIdField
     * @param orgId
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public List<String> batchCreateBillNo(int billNumber, BaseEntity billVO, String billTypeCode, Date billDate,
            String orgIdField, Long orgId) {
        if (billNumber == 0) {
            return new ArrayList<>(0);
        }
        BillCodeInfoVO billCodeInfoVO = this.getBillCodeInfoVO(billVO, billTypeCode, billDate, null, orgIdField, orgId);

        AjaxResult ajaxResult = sysBillCodeService.getBillCodeBatch(billCodeInfoVO, billNumber);
        if (HttpStatus.SUCCESS == (Integer) ajaxResult.get(AjaxResult.CODE_TAG)) {
            return (List<String>) ajaxResult.get(AjaxResult.DATA_TAG);
        } else {
            String msg = "调用框架编码规则错误：" + ajaxResult.get(AjaxResult.MSG_TAG);
            log.error(msg);
            throw new ServiceException(msg);
        }
    }

}
