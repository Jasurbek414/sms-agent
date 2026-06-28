package su.skat.client.service;

import su.skat.client.model.GlobalExtra;
import su.skat.client.model.GpsSatellite;

interface ISkatLocationCallback {
    void onLocationChanged(double latitude, double longitude, float bearing, float speed, float accuracy);
    void onSatellitesChanged(in List<GpsSatellite> satellites);
    void onGpsStatusChanged(boolean isGPSActive, int inUse, int allSatellites);
}
