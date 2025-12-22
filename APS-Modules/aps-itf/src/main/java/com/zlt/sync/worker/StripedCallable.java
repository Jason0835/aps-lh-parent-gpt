package com.zlt.sync.worker;

import java.util.concurrent.Callable;

public interface StripedCallable<V> extends Callable<V>, StripedObject {

}
