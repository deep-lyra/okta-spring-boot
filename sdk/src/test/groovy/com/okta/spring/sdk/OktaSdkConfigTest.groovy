/*
 * Copyright 2017 Okta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.okta.spring.sdk

import com.okta.sdk.client.Client
import com.okta.sdk.client.Proxy
import com.okta.sdk.impl.cache.DefaultCacheManager
import com.okta.spring.config.OktaClientProperties
import com.okta.spring.sdk.cache.SpringCacheManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests
import org.testng.annotations.Test

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.instanceOf
import static org.hamcrest.Matchers.notNullValue
import static org.hamcrest.Matchers.nullValue

@SpringBootTest(classes    = [MockSdkApp],
                properties = ["okta.client.orgUrl=https://okta.example.com",
                              "okta.client.token=my-secret-api-token",
                              "okta.oauth2.discoveryDisabled=true"])
class OktaSdkConfigTest extends AbstractTestNGSpringContextTests {

    @Autowired
    Client client

    @Test
    void basicConfigTest() {
        assertThat client, notNullValue()

        // check if client properties were set correctly
        assertThat client.dataStore.baseUrlResolver.getBaseUrl(), equalTo("https://okta.example.com")
        assertThat client.dataStore.clientCredentialsResolver.getClientCredentials().getCredentials(), equalTo("my-secret-api-token")

        // no spring cache manager enabled the default is expected
        assertThat client.dataStore.cacheManager, instanceOf(DefaultCacheManager)
    }

    @SpringBootTest(classes    = [MockSdkAppWithCache],
                    properties = ["okta.client.orgUrl=https://okta.example.com",
                                  "okta.client.token=my-secret-api-token",
                                  "okta.oauth2.discoveryDisabled=true"])
    class WithSpringCacheManagerTest extends AbstractTestNGSpringContextTests {

        @Autowired
        Client client

        @Test
        void correctCacheImpl() {
            assertThat client.dataStore.cacheManager, instanceOf(SpringCacheManager)
        }
    }

    class ProxyConfigTest {

        @Test
        void proxyConfig() {
            OktaClientProperties clientProperties = new OktaClientProperties()
            clientProperties.setOrgUrl("https://okta.example.com")
            OktaSdkConfig config = new OktaSdkConfig(clientProperties, null)
            assertThat config.oktaSdkProxy(), nullValue()

            // just host
            clientProperties.proxy.hostname = "http://proxy.example.com"
            Proxy proxy = config.oktaSdkProxy()
            assertThat proxy, notNullValue()
            assertThat proxy.host, equalTo("http://proxy.example.com")
            assertThat proxy.port, equalTo(0)
            assertThat proxy.username, nullValue()
            assertThat proxy.password, nullValue()

            // host and port
            clientProperties.proxy.port = 9999
            proxy = config.oktaSdkProxy()
            assertThat proxy, notNullValue()
            assertThat proxy.host, equalTo("http://proxy.example.com")
            assertThat proxy.port, equalTo(9999)
            assertThat proxy.username, nullValue()
            assertThat proxy.password, nullValue()

            // just port
            clientProperties.proxy.hostname = null
            clientProperties.proxy.port = 9999
            proxy = config.oktaSdkProxy()
            assertThat config.oktaSdkProxy(), nullValue()

            // host, port, username
            clientProperties.proxy.hostname = "http://proxy.example.com"
            clientProperties.proxy.port = 9999
            clientProperties.proxy.username = "proxy-user"
            proxy = config.oktaSdkProxy()
            assertThat proxy, notNullValue()
            assertThat proxy.host, equalTo("http://proxy.example.com")
            assertThat proxy.port, equalTo(9999)
            assertThat proxy.username, equalTo("proxy-user")
            assertThat proxy.password, nullValue()

            // host, port, username, password
            clientProperties.proxy.password = "proxy-pass"
            proxy = config.oktaSdkProxy()
            assertThat proxy, notNullValue()
            assertThat proxy.host, equalTo("http://proxy.example.com")
            assertThat proxy.port, equalTo(9999)
            assertThat proxy.username, equalTo("proxy-user")
            assertThat proxy.password, equalTo("proxy-pass")

            // host, port, password
            clientProperties.proxy.username = null
            proxy = config.oktaSdkProxy()
            assertThat proxy, notNullValue()
            assertThat proxy.host, equalTo("http://proxy.example.com")
            assertThat proxy.port, equalTo(9999)
            assertThat proxy.username, nullValue()
            assertThat proxy.password, equalTo("proxy-pass")
        }
    }
}