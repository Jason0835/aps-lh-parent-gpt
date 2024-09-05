package com.zlt.aps.lh.engine.exception;

/**
 * 硫化引擎自定义抛出异常
 */
public class LhEngineException extends RuntimeException {

    public LhEngineException(){
        super();
    }

    public LhEngineException(String msg){
        super(msg);
    }
}
