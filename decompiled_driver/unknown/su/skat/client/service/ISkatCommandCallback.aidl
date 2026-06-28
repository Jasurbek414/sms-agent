package su.skat.client.service;

import su.skat.client.model.SkatCommand;

interface ISkatCommandCallback {
    void onReceive(in SkatCommand command);
}
