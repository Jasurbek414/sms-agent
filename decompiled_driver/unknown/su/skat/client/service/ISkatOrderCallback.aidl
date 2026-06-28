package su.skat.client.service;

import su.skat.client.model.Order;

interface ISkatOrderCallback {
    void onOrderChanged(in Order order);
    void onOrderIdChanged(int oldId, int newId);
}
