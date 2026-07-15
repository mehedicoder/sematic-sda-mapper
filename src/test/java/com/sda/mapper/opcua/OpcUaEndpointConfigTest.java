package com.sda.mapper.opcua;

import com.sda.mapper.opcua.model.OpcUaEndpointConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OPC UA Endpoint Config")
class OpcUaEndpointConfigTest {

    @Test
    @DisplayName("Should store host, port, resource path, security policy, auth policy, and SSL flag")
    void shouldStoreAllFields() {
        OpcUaEndpointConfig config = new OpcUaEndpointConfig(
            "192.168.1.100",
            4840,
            "/UA/MyServer",
            OpcUaEndpointConfig.SecurityPolicy.Basic256Sha256,
            OpcUaEndpointConfig.AuthPolicy.UserName,
            true
        );

        assertEquals("192.168.1.100", config.host());
        assertEquals(4840, config.port());
        assertEquals("/UA/MyServer", config.resourcePath());
        assertEquals(OpcUaEndpointConfig.SecurityPolicy.Basic256Sha256, config.securityPolicy());
        assertEquals(OpcUaEndpointConfig.AuthPolicy.UserName, config.authPolicy());
        assertTrue(config.useSsl());
    }

    @Test
    @DisplayName("Should generate correct endpoint URL for local config")
    void shouldGenerateLocalEndpointUrl() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local("localhost", 4334, "/UA/FlowMeterServer");

        assertEquals("opc.tcp://localhost:4334/UA/FlowMeterServer", config.getEndpointUrl());
    }

    @Test
    @DisplayName("Should generate endpoint URL with custom host and port")
    void shouldGenerateEndpointUrlWithCustomHostPort() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local("10.0.0.5", 4840, "/");

        assertEquals("opc.tcp://10.0.0.5:4840/", config.getEndpointUrl());
    }

    @Test
    @DisplayName("Should create local config with None security and Anonymous auth")
    void shouldCreateLocalConfigWithDefaults() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.local("localhost", 4840, "/UA/Test");

        assertEquals(OpcUaEndpointConfig.SecurityPolicy.None, config.securityPolicy());
        assertEquals(OpcUaEndpointConfig.AuthPolicy.Anonymous, config.authPolicy());
        assertFalse(config.useSsl());
    }

    @Test
    @DisplayName("Should create secure config with Basic256Sha256 security and SSL enabled")
    void shouldCreateSecureConfig() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.secure("192.168.1.10", 4840, "/UA/SecureServer");

        assertEquals(OpcUaEndpointConfig.SecurityPolicy.Basic256Sha256, config.securityPolicy());
        assertEquals(OpcUaEndpointConfig.AuthPolicy.Anonymous, config.authPolicy());
        assertTrue(config.useSsl());
    }

    @Test
    @DisplayName("Should generate endpoint URL for secure config")
    void shouldGenerateSecureEndpointUrl() {
        OpcUaEndpointConfig config = OpcUaEndpointConfig.secure("server.local", 4840, "/UA/Secure");

        assertEquals("opc.tcp://server.local:4840/UA/Secure", config.getEndpointUrl());
    }

    @Test
    @DisplayName("Should support equality via record semantics")
    void shouldSupportEquality() {
        OpcUaEndpointConfig config1 = new OpcUaEndpointConfig(
            "localhost", 4840, "/UA/Test",
            OpcUaEndpointConfig.SecurityPolicy.None,
            OpcUaEndpointConfig.AuthPolicy.Anonymous,
            false
        );
        OpcUaEndpointConfig config2 = new OpcUaEndpointConfig(
            "localhost", 4840, "/UA/Test",
            OpcUaEndpointConfig.SecurityPolicy.None,
            OpcUaEndpointConfig.AuthPolicy.Anonymous,
            false
        );

        assertEquals(config1, config2);
    }

    @Test
    @DisplayName("Should not be equal when hosts differ")
    void shouldNotBeEqualWhenHostDiffers() {
        OpcUaEndpointConfig config1 = OpcUaEndpointConfig.local("localhost", 4840, "/UA/Test");
        OpcUaEndpointConfig config2 = OpcUaEndpointConfig.local("192.168.1.1", 4840, "/UA/Test");

        assertNotEquals(config1, config2);
    }

    @Test
    @DisplayName("Should list all security policy values")
    void shouldListAllSecurityPolicies() {
        OpcUaEndpointConfig.SecurityPolicy[] policies = OpcUaEndpointConfig.SecurityPolicy.values();

        assertEquals(4, policies.length);
        assertArrayEquals(
            new OpcUaEndpointConfig.SecurityPolicy[]{
                OpcUaEndpointConfig.SecurityPolicy.None,
                OpcUaEndpointConfig.SecurityPolicy.Basic128Rsa15,
                OpcUaEndpointConfig.SecurityPolicy.Basic256,
                OpcUaEndpointConfig.SecurityPolicy.Basic256Sha256
            },
            policies
        );
    }

    @Test
    @DisplayName("Should list all auth policy values")
    void shouldListAllAuthPolicies() {
        OpcUaEndpointConfig.AuthPolicy[] policies = OpcUaEndpointConfig.AuthPolicy.values();

        assertEquals(3, policies.length);
        assertArrayEquals(
            new OpcUaEndpointConfig.AuthPolicy[]{
                OpcUaEndpointConfig.AuthPolicy.Anonymous,
                OpcUaEndpointConfig.AuthPolicy.UserName,
                OpcUaEndpointConfig.AuthPolicy.Certificate
            },
            policies
        );
    }
}
