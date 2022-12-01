package net.orbyfied.osf.service.communication.impl;

import net.orbyfied.osf.service.communication.CommunicationAdapter;

public class RemoteFunctionImpl {

    // the communication adapter
    final CommunicationAdapter adapter;

    public RemoteFunctionImpl(CommunicationAdapter adapter) {
        this.adapter = adapter;
    }

    public CommunicationAdapter getAdapter() {
        return adapter;
    }

}
