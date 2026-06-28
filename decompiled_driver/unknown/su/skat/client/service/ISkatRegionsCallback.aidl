package su.skat.client.service;

import su.skat.client.model.Region;

interface ISkatRegionsCallback {
    // sends immediatly when subscribed
    void onRegionsChanged(in List<Region> regions);

    void onNewOrder(int regionId);
    void onRegionStateChanged(int regionId, int carCount, int freeOrders);
    void onFreeOrdersCountChange(int regionId, int newCount);
    void onDriverCountChange(int regionId, int newCount);

    void onUnregistered();
    void onPositionChanged(int regionId, int position);
}
