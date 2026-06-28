package su.skat.client.service;

interface ISkatRegionCarsCallback {
    void onCarsListFetched(int regionId, in List<String> cars);
}
