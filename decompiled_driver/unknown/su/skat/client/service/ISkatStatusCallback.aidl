package su.skat.client.service;

import su.skat.client.model.User;
import su.skat.client.model.Article;
import su.skat.client.model.Order;

interface ISkatStatusCallback {
    // BaseStatusActivity panels data
    void onSettingsChanged();
    void onConnectionStateChanged(int state);
    void onGpsStatusChanged(boolean isGPSActive, int inUse, int allSatellites);
    void onPrepaidDateChanged(int timestamp);
    void onPreOrdersCountChanged(int count);
    void onQueuedOrdersCountChanged(int queuedCount, int reservedCount, boolean taximeterIsActive, String firstSrc);

    // ask for actions from Service
    void askProposeProfile(String cityName, int profileId);
    void askLogin();
    void askAppPermissions();

    // incoming orders
    void incomingOrder(in Order order, int mode, long timeoutTimestamp);
    void onNextOrderNotify(in Order nextOrder, long timeoutTimestamp);

    // ask for session init dialog from Service
    void requestSessionInit(String groupName, double price);
    void requestPrepaySessionInit(String groupName, double price, int duration);
    void requestPostpaySessionInit(String groupName, double price, String billingTime);
}