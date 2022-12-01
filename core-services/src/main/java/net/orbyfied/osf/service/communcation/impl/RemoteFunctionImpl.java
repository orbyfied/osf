package net.orbyfied.osf.service.communcation.impl;

import net.orbyfied.osf.service.communcation.CommunicationAdapter;

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
