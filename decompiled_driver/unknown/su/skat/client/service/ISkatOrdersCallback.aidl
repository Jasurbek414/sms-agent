package su.skat.client.service;

import su.skat.client.model.Order;
import su.skat.client.model.FreeOrder;

interface ISkatOrdersCallback {
    void onFreeOrdersFetched(in List<FreeOrder> orders);
    void onAttachedOrdersFetched(in List<Order> orders);
    void onReservOrdersFetched(in List<Order> orders);
}
