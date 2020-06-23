package com.shh.miaosha.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

//当spring容器内没有TomcatEmbededServletContainerFactory这个bean时，会把此bean加载进来
@Component
public class WebServerConfiguration implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {
    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        ( (TomcatServletWebServerFactory)factory).addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                Http11NioProtocol protocol= (Http11NioProtocol) connector.getProtocolHandler();
                //定制化keepAliveTimeOut
                protocol.setSelectorTimeout(30000);//30s,30s没有请求服务，自动断开
                //当请求超过10000条就自动断开keepAlive链接
                protocol.setMaxKeepAliveRequests(10000);

            }
        });
    }
}
