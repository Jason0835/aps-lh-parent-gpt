package com.zlt.aps.cx.engine.exception;

/**
 * 成型引擎算法自定义抛出异常
 */
public class CxScheduleEngineException extends RuntimeException {
    public CxScheduleEngineException(){
        super();
    }
    public CxScheduleEngineException(String msg){
        super(msg);
    }
}
