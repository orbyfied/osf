package net.orbyfied.osf.network.handler;

public class HandlerResult {

    ChainAction chain;
    NodeAction nodeAction = NodeAction.KEEP;

    public HandlerResult(ChainAction chain) {
        this.chain = chain;
    }

    public ChainAction chain() {
        return chain;
    }

    public NodeAction nodeAction() {
        return nodeAction;
    }

    public HandlerResult nodeAction(NodeAction action) {
        this.nodeAction = action;
        return this;
    }

}
