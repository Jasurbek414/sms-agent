package su.skat.client.service;

import su.skat.client.model.User;

interface ISkatFiscalCallback {
    void fiscalReceiptProgress(int orderId);
    void fiscalReceiptReceived(int orderId, String data, String checkData);
    void fiscalReceiptFailed(int orderId, String errorCode, String checkData);
}