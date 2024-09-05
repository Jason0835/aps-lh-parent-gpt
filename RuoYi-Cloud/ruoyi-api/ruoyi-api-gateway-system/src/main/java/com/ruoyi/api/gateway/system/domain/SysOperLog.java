package com.ruoyi.api.gateway.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.annotation.Excel;
import com.ruoyi.common.core.annotation.Excel.ColumnType;
import com.ruoyi.common.core.web.domain.BaseEntity;

/**
 * 操作日志记录表 oper_log
 * 
 * @author ruoyi
 */
public class SysOperLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志主键 */
    @Excel(name = "ui.data.column.operlog.operId", cellType = ColumnType.NUMERIC)
    private Long operId;

    /** 操作模块 */
    @Excel(name = "ui.data.column.operlog.title")
    private String title;

    /** 业务类型（0其它 1新增 2修改 3删除） */
    @Excel(name = "ui.data.column.operlog.businessType", readConverterExp = "api.log.desc.business.type")
    private Integer businessType;

    /** 业务类型数组 */
    private Integer[] businessTypes;

    /** 请求方法 */
    @Excel(name = "api.log.columnname.request.method")
    private String method;

    /** 请求方式 */
    @Excel(name = "api.log.columnname.request.type")
    private String requestMethod;

    /** 操作类别（0其它 1后台用户 2手机端用户） */
    @Excel(name = "api.log.columnname.operation.type", readConverterExp = "api.log.operation.type.desc")
    private Integer operatorType;

    /** 操作人员 */
    @Excel(name = "api.log.columnname.operator")
    private String operName;

    /** 部门名称 */
    @Excel(name = "api.log.columnname.deptname")
    private String deptName;

    /** 请求url */
    @Excel(name = "api.log.columnname.requesturl")
    private String operUrl;

    /** 操作地址 */
    @Excel(name = "api.log.columnname.operationurl")
    private String operIp;

    /** 请求参数 */
    @Excel(name = "api.log.columnname.requestparameter")
    private String operParam;

    /** 返回参数 */
    @Excel(name = "api.log.columnname.responseparameter")
    private String jsonResult;

    /** 操作状态（0正常 1异常） */
    @Excel(name = "api.log.columnname.status", readConverterExp = "api.log.status.desc")
    private Integer status;

    /** 错误消息 */
    @Excel(name = "api.log.columnname.errormsg")
    private String errorMsg;

    /**
     * 操作位置
     */
    private String operLocation;

    //查询条件
    private String  beginTime;
    private String  endTime;

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    /** 操作时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "api.log.columnname.operation.time", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date operTime;

    public Long getOperId()
    {
        return operId;
    }

    public void setOperId(Long operId)
    {
        this.operId = operId;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public Integer getBusinessType()
    {
        return businessType;
    }

    public void setBusinessType(Integer businessType)
    {
        this.businessType = businessType;
    }

    public Integer[] getBusinessTypes()
    {
        return businessTypes;
    }

    public void setBusinessTypes(Integer[] businessTypes)
    {
        this.businessTypes = businessTypes;
    }

    public String getMethod()
    {
        return method;
    }

    public void setMethod(String method)
    {
        this.method = method;
    }

    public String getRequestMethod()
    {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod)
    {
        this.requestMethod = requestMethod;
    }

    public Integer getOperatorType()
    {
        return operatorType;
    }

    public void setOperatorType(Integer operatorType)
    {
        this.operatorType = operatorType;
    }

    public String getOperName()
    {
        return operName;
    }

    public void setOperName(String operName)
    {
        this.operName = operName;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getOperUrl()
    {
        return operUrl;
    }

    public void setOperUrl(String operUrl)
    {
        this.operUrl = operUrl;
    }

    public String getOperIp()
    {
        return operIp;
    }

    public void setOperIp(String operIp)
    {
        this.operIp = operIp;
    }

    public String getOperParam()
    {
        return operParam;
    }

    public void setOperParam(String operParam)
    {
        this.operParam = operParam;
    }

    public String getJsonResult()
    {
        return jsonResult;
    }

    public void setJsonResult(String jsonResult)
    {
        this.jsonResult = jsonResult;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public String getErrorMsg()
    {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg)
    {
        this.errorMsg = errorMsg;
    }

    public Date getOperTime()
    {
        return operTime;
    }

    public void setOperTime(Date operTime)
    {
        this.operTime = operTime;
    }

    public String getOperLocation() {
        return operLocation;
    }

    public void setOperLocation(String operLocation) {
        this.operLocation = operLocation;
    }
}
