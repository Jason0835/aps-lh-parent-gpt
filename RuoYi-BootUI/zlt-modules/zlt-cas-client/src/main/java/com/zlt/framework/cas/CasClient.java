package com.zlt.framework.cas;
import org.pac4j.cas.config.CasConfiguration;
public class CasClient extends org.pac4j.cas.client.CasClient {

    public CasClient(CasConfiguration configuration){
        super(configuration);
    }

    @Override
    protected void clientInit() {
        this.defaultAuthenticator(
                new com.zlt.framework.cas.CasAuthenticator(getConfiguration(),
                        getName(),
                        getUrlResolver(),
                        getCallbackUrlResolver(),
                        callbackUrl));
        super.clientInit();
    }

}
