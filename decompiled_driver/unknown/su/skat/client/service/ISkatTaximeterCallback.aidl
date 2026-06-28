package su.skat.client.service;

import su.skat.client.model.Rate;
import su.skat.client.model.Order;
import su.skat.client.model.TaximeterData;
import su.skat.client.taxometr.TaxometrResult;

interface ISkatTaximeterCallback {
    void onRateChanged(int orderId, in Rate rate);
    void onTaximeterUpdated(int orderId, in TaximeterData data);
    void onTaximeterStarted(in Order order);
    void onTaximeterStopped(in Order order, in TaxometrResult result);
}
