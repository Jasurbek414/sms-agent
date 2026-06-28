package su.skat.client.service;

import su.skat.client.model.Article;

interface ISkatPromisedPaymentsCallback {
    void onPromisedPaymentSuccess();
    void onPromisedPaymentFailed(String errorText);
    void onPromisedPaymentDisabled(String text);
    void onPromisedPaymentEnabled(double maxAmount, double maxLength, boolean inHours, boolean commissionEnabled, double commissionAmount, boolean commissionIsPercent, boolean commissionIsPerDay);
}
