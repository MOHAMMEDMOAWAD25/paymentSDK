package com.example.tests;

import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end UI automation that validates the Cashier SDK maps the paymentExtraDetails
 * parameter into the customProperties header field for purchase transactions and that the
 * terminal response still contains the same properties.
 *
 * <p>The test spins up a {@link MockWebServer} to capture the outgoing purchase request and
 * respond with a canned payload that mimics the terminal.</p>
 *
 * <p><strong>Environment prerequisites</strong></p>
 * <ul>
 *   <li>Appium server running and reachable via {@code -Dappium.server.url} (default http://127.0.0.1:4723/wd/hub)</li>
 *   <li>The AUT (application under test) is configured to read its Cashier SDK base URL from the
 *       {@code cashierSdkBaseUrl} capability so requests are routed to the mock server.</li>
 *   <li>Supply package / activity or app path through system properties when starting the tests.</li>
 * </ul>
 */
class CashierSdkPurchaseTest {

    private static final Map<String, String> EXPECTED_CUSTOM_PROPERTIES;
    private static final String PURCHASE_RESPONSE_JSON;

    private static MockWebServer mockWebServer;
    private static URL appiumServerUrl;

    private AndroidDriver<MobileElement> driver;

    private static final By AMOUNT_INPUT = By.id("com.example.cashier:id/input_amount");
    private static final By CARD_PAYMENT_OPTION = By.id("com.example.cashier:id/option_card");
    private static final By EXTRA_DETAILS_BUTTON = By.id("com.example.cashier:id/button_custom_properties");
    private static final By EXTRA_DETAILS_KEY_FIELD = By.id("com.example.cashier:id/input_custom_key");
    private static final By EXTRA_DETAILS_VALUE_FIELD = By.id("com.example.cashier:id/input_custom_value");
    private static final By EXTRA_DETAILS_ADD_BUTTON = By.id("com.example.cashier:id/button_add_custom_property");
    private static final By PURCHASE_SUBMIT_BUTTON = By.id("com.example.cashier:id/button_purchase_submit");
    private static final By RECEIPT_STATUS_LABEL = By.id("com.example.cashier:id/text_receipt_status");

    static {
        Map<String, String> properties = new HashMap<>();
        properties.put("PAYMENT_SDK_DOT_NET_VERSION", "1.0.46.3");
        properties.put("omar", "909090");
        EXPECTED_CUSTOM_PROPERTIES = Collections.unmodifiableMap(properties);

        PURCHASE_RESPONSE_JSON = """
                {
                    "header": {
                        "requestUuid": "a2581722-37b7-4a88-9a78-3b183a94e88f",
                        "messageCode": "purchase",
                        "serverTimestamp": "20251106111050",
                        "status": {
                            "statusCode": 1,
                            "statusDesc": "Payment.VALUE_PAYMENT_STATUS_SUCCESS",
                            "hostStatusCode": 200,
                            "hostStatusDesc": "SUCCESS"
                        },
                        "terminalCode": "1171780338",
                        "customProperties": {
                            "omar": "909090",
                            "PAYMENT_SDK_DOT_NET_VERSION": "1.0.46.3"
                        }
                    },
                    "body": {
                        "fawryReference": "1491361",
                        "amount": 199.0,
                        "currency": "EGP,2",
                        "fees": 0.0,
                        "tips": 0.0,
                        "serviceProvider": null,
                        "btc": "999",
                        "paymentOption": "VISA",
                        "clientTerminalSequenceID": "06112511103800000025",
                        "signature": "20251106111050a2581722-37b7-4a88-9a78-3b183a94e88f1200149136119906112511103800000025",
                        "transactionType": "Sale",
                        "printReceipt": true,
                        "balance": 1383456.28,
                        "groupReferenceNumber": null,
                        "receiptInfo": {
                            "authId": "638377",
                            "effDt": "17624202372391762420237239",
                            "receiptNumber": "000006",
                            "rrn": "729820005487",
                            "merchantId": "111229300",
                            "terminalId": "22930005",
                            "acquirerBankId": "CIB",
                            "pinMode": null,
                            "authMethod": "071",
                            "transactionType": null,
                            "cardInfo": {
                                "cardHolderName": null,
                                "cardAcctId": "0891",
                                "cardScheme": "VISA",
                                "maskedPAN": null,
                                "issuerBankId": "458832",
                                "appID": "A0000000031010",
                                "appName": "Visa Debit",
                                "cardNumber": "458832******0891",
                                "expiryDate": "1l6nesL03zQI6v8XB+qEZP7t1I7ammqJT8H+Jr7Gc4S2yjbkKSEHNk37UfAlQZv1XixackTq5EoUER+gDVzEh65Oa+EaIVJvyCGpHlu7cbcqWGhknmIfXUiKdFC+qgekBCcpOAoN0HF3jYAti2e7y0pBhDg5+w516u9IzSyzs2w="
                            },
                            "installmentPlan": null,
                            "convenienceFees": 0.0,
                            "paymentNetwork": null,
                            "senderNumber": null,
                            "tips": 0.0,
                            "cardVerificationMethod": null,
                            "cardVerificationMethodDescription": null,
                            "panEntryMode": "07",
                            "panEntryModeDescription": "Chip contactless"
                        },
                        "bnplInfo": null,
                        "extraBillInfo": null,
                        "discount": null,
                        "promo": null
                    }
                }
                """;
    }

    @BeforeAll
    static void globalSetUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String appiumServerProperty = System.getProperty("appium.server.url", "http://127.0.0.1:4723/wd/hub");
        try {
            appiumServerUrl = new URL(appiumServerProperty);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid Appium server URL: " + appiumServerProperty, e);
        }
    }

    @AfterAll
    static void globalTearDown() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    @BeforeEach
    void setUp() throws MalformedURLException {
        enqueueSuccessfulPurchaseResponse();

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, System.getProperty("platformName", "Android"));
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, System.getProperty("deviceName", "Android Emulator"));
        capabilities.setCapability(MobileCapabilityType.NEW_COMMAND_TIMEOUT, 120);
        capabilities.setCapability("appium:automationName", System.getProperty("automationName", "UiAutomator2"));

        setCapabilityIfPresent(capabilities, MobileCapabilityType.APP, "app.path");
        setCapabilityIfPresent(capabilities, "appium:appPackage", "app.package");
        setCapabilityIfPresent(capabilities, "appium:appActivity", "app.activity");

        capabilities.setCapability("cashierSdkBaseUrl", mockWebServer.url("/").toString());

        driver = new AndroidDriver<>(appiumServerUrl, capabilities);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
        mockWebServer.clearRecordedRequests();
    }

    @Test
    @DisplayName("Verify Cashier SDK supports paymentExtraDetails parameter for purchase transactions")
    @EnabledIfEnvironmentVariable(named = "RUN_E2E", matches = "true")
    void verifyPaymentExtraDetailsAreMappedToCustomProperties() throws Exception {
        triggerPurchaseTransactionFromApp();

        RecordedRequest request = mockWebServer.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request, "No purchase request received by the mock terminal endpoint");

        JSONObject requestJson = new JSONObject(request.getBody().readUtf8());
        JSONObject header = requestJson.getJSONObject("header");

        assertEquals("purchase", header.getString("messageCode"), "messageCode should be purchase");

        JSONObject customProperties = header.getJSONObject("customProperties");
        EXPECTED_CUSTOM_PROPERTIES.forEach((key, value) ->
                assertEquals(value, customProperties.getString(key), "Custom property mismatch for " + key)
        );

        assertTerminalResponseHandledByApp();
    }

    private void triggerPurchaseTransactionFromApp() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        // Wait for the purchase screen to be ready
        wait.until(ExpectedConditions.presenceOfElementLocated(AMOUNT_INPUT));

        MobileElement amountInput = driver.findElement(AMOUNT_INPUT);
        amountInput.clear();
        amountInput.sendKeys("199.0");

        driver.findElement(CARD_PAYMENT_OPTION).click();

        // Populate custom properties (paymentExtraDetails)
        driver.findElement(EXTRA_DETAILS_BUTTON).click();
        EXPECTED_CUSTOM_PROPERTIES.forEach((key, value) -> addCustomProperty(key, value));

        driver.findElement(PURCHASE_SUBMIT_BUTTON).click();
    }

    private void addCustomProperty(String key, String value) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        wait.until(ExpectedConditions.visibilityOfElementLocated(EXTRA_DETAILS_KEY_FIELD));

        MobileElement keyField = driver.findElement(EXTRA_DETAILS_KEY_FIELD);
        MobileElement valueField = driver.findElement(EXTRA_DETAILS_VALUE_FIELD);

        keyField.clear();
        keyField.sendKeys(key);
        valueField.clear();
        valueField.sendKeys(value);

        driver.findElement(EXTRA_DETAILS_ADD_BUTTON).click();
    }

    private void assertTerminalResponseHandledByApp() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        MobileElement statusLabel = (MobileElement) wait.until(ExpectedConditions.visibilityOfElementLocated(RECEIPT_STATUS_LABEL));

        assertNotNull(statusLabel, "Receipt status label is missing after purchase completion");
        String statusText = statusLabel.getText();
        assertTrue(statusText.toUpperCase().contains("SUCCESS"), "Unexpected terminal response status: " + statusText);
    }

    private void enqueueSuccessfulPurchaseResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(PURCHASE_RESPONSE_JSON));
    }

    private void setCapabilityIfPresent(DesiredCapabilities capabilities, String capabilityName, String systemPropertyKey) {
        String value = System.getProperty(systemPropertyKey);
        if (value != null && !value.isBlank()) {
            capabilities.setCapability(capabilityName, value);
        }
    }
}
