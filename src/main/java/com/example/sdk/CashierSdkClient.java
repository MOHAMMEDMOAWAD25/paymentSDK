package com.example.sdk;

import io.qameta.allure.Allure;
import org.json.JSONObject;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

/**
 * Example wrapper that builds and transmits a purchase request to the Cashier SDK backend
 * while decorating the flow with an Allure step and applying RSA encryption to the
 * credential and signature fields.
 */
public class CashierSdkClient {

    private final PurchaseRequestBuilder request;
    private final RsaEncryptionService rsa;
    private final JSONObject active;
    private final RequestTransmitter transmitter;

    public CashierSdkClient(PurchaseRequestBuilder request,
                            RsaEncryptionService rsa,
                            JSONObject active,
                            RequestTransmitter transmitter) {
        this.request = Objects.requireNonNull(request, "request");
        this.rsa = Objects.requireNonNull(rsa, "rsa");
        this.active = Objects.requireNonNull(active, "active");
        this.transmitter = Objects.requireNonNull(transmitter, "transmitter");
    }

    public void transmitPurchaseRequest(String amount,
                                        String paymentOption,
                                        String orderId)
            throws NoSuchPaddingException,
                   IllegalBlockSizeException,
                   NoSuchAlgorithmException,
                   InvalidKeySpecException,
                   BadPaddingException,
                   InvalidKeyException,
                   InterruptedException,
                   IOException {

        Allure.step("Transmit Purchase Request By Amount & Payment Option & Order ID");

        request.generatePurchaseRequest();
        request.setAmount(amount);

        String userName = active.getString("userName");
        String password = active.getString("password");
        String clientTimestamp = request.getRequest().getJSONObject("header").getString("clientTimestamp");
        String requestUuid = request.getRequest().getJSONObject("header").getString("requestUuid");

        request.setPassword(rsa.rsaEncrypt());
        request.setUserName(userName);

        String signature = userName + password + clientTimestamp + requestUuid + amount;
        String encryptedSignature = rsa.rsaEncrypt(signature);
        request.setSignature(encryptedSignature);

        if (paymentOption != null) {
            request.setPaymentOption(paymentOption);
        }

        request.setOrderId(orderId);

        transmitRequest(request.getRequest());
    }

    private void transmitRequest(JSONObject requestJson)
            throws InterruptedException, IOException, NoSuchAlgorithmException, InvalidKeySpecException,
                   InvalidKeyException, BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException {
        transmitter.send(requestJson);
    }

    @FunctionalInterface
    public interface RequestTransmitter {
        void send(JSONObject request) throws InterruptedException, IOException, NoSuchAlgorithmException,
                InvalidKeySpecException, InvalidKeyException, BadPaddingException,
                NoSuchPaddingException, IllegalBlockSizeException;
    }

    public interface PurchaseRequestBuilder {
        void generatePurchaseRequest();

        void setAmount(String amount);

        JSONObject getRequest();

        void setPassword(String password);

        void setUserName(String userName);

        void setSignature(String signature);

        void setPaymentOption(String paymentOption);

        void setOrderId(String orderId);
    }

    public interface RsaEncryptionService {
        String rsaEncrypt() throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException,
                InvalidKeySpecException, BadPaddingException, InvalidKeyException, IOException;

        String rsaEncrypt(String plainText) throws NoSuchPaddingException, IllegalBlockSizeException,
                NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException, IOException;
    }
}
