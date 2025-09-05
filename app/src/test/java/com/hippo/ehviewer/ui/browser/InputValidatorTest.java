package com.hippo.ehviewer.ui.browser;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class InputValidatorTest {

    private InputValidator inputValidator;

    @Before
    public void setUp() {
        inputValidator = new InputValidator();
    }

    @Test
    public void testValidUrl_CompleteHttpUrl() {
        assertTrue(inputValidator.isValidUrl("https://www.google.com"));
        assertTrue(inputValidator.isValidUrl("http://example.com"));
        assertTrue(inputValidator.isValidUrl("https://github.com/user/repo"));
    }

    @Test
    public void testValidUrl_DomainOnly() {
        assertTrue(inputValidator.isValidUrl("google.com"));
        assertTrue(inputValidator.isValidUrl("www.example.com"));
        assertTrue(inputValidator.isValidUrl("sub.domain.com"));
    }

    @Test
    public void testValidUrl_IPAddress() {
        assertTrue(inputValidator.isValidUrl("192.168.1.1"));
        assertTrue(inputValidator.isValidUrl("127.0.0.1"));
        assertTrue(inputValidator.isValidUrl("255.255.255.255"));
    }

    @Test
    public void testInvalidUrl_SearchQueries() {
        assertFalse(inputValidator.isValidUrl("how to cook"));
        assertFalse(inputValidator.isValidUrl("weather today"));
        assertFalse(inputValidator.isValidUrl("java tutorial"));
        assertFalse(inputValidator.isValidUrl("android development guide"));
    }

    @Test
    public void testInvalidUrl_EmptyOrNull() {
        assertFalse(inputValidator.isValidUrl(""));
        assertFalse(inputValidator.isValidUrl("   "));
        assertFalse(inputValidator.isValidUrl(null));
    }

    @Test
    public void testInvalidUrl_SpecialCases() {
        assertFalse(inputValidator.isValidUrl("just text"));
        assertFalse(inputValidator.isValidUrl("search for something"));
        assertFalse(inputValidator.isValidUrl("123 456"));
    }

    @Test
    public void testNormalizeUrl_AddHttpsPrefix() {
        assertEquals("https://google.com", inputValidator.normalizeUrl("google.com"));
        assertEquals("https://www.example.com", inputValidator.normalizeUrl("www.example.com"));
        assertEquals("https://192.168.1.1", inputValidator.normalizeUrl("192.168.1.1"));
    }

    @Test
    public void testNormalizeUrl_KeepExistingProtocol() {
        assertEquals("http://example.com", inputValidator.normalizeUrl("http://example.com"));
        assertEquals("https://secure.com", inputValidator.normalizeUrl("https://secure.com"));
    }

    @Test
    public void testIsSearchQuery() {
        assertTrue(inputValidator.isSearchQuery("search term"));
        assertTrue(inputValidator.isSearchQuery("how to program"));
        assertTrue(inputValidator.isSearchQuery("android tutorial"));
        
        assertFalse(inputValidator.isSearchQuery("google.com"));
        assertFalse(inputValidator.isSearchQuery("https://example.com"));
        assertFalse(inputValidator.isSearchQuery("192.168.1.1"));
    }

    @Test
    public void testEdgeCases() {
        // 测试常见的搜索关键词模式
        assertFalse(inputValidator.isValidUrl("goo")); // 不完整的域名
        assertTrue(inputValidator.isValidUrl("goo.gl")); // 短域名
        
        // 测试带端口的URL
        assertTrue(inputValidator.isValidUrl("localhost:8080"));
        assertTrue(inputValidator.isValidUrl("192.168.1.1:3000"));
    }
}