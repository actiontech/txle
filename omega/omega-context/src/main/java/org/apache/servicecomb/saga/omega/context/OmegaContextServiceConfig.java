package org.apache.servicecomb.saga.omega.context;

/**
 * OmegaContext holds the globalTxId and localTxId which are used to build the invocation map
 */
public class OmegaContextServiceConfig extends OmegaContext {
    // Do not need think about concurrency situation, due to they're one-to-one with current application.
    private String serviceName;
    private String instanceId;
    private boolean isAutoCompensate;// compensation or auto-compensation
    private boolean isEnabledAutoCompensateTx;// true: record undo_log, otherwise do nothing, just for auto-compensation.

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String serviceName() {
        return serviceName;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String instanceId() {
        return instanceId;
    }

    public boolean isAutoCompensate() {
        return isAutoCompensate;
    }

    public void setAutoCompensate(boolean autoCompensate) {
        this.isAutoCompensate = autoCompensate;
    }

    public boolean isEnabledAutoCompensateTx() {
        return this.isEnabledAutoCompensateTx;
    }

    public void setIsEnabledAutoCompensateTx(boolean isEnabledAutoCompensateTx) {
        this.isEnabledAutoCompensateTx = isEnabledAutoCompensateTx;
    }

    public OmegaContextServiceConfig(IdGenerator<String> idGenerator) {
        super(idGenerator);
    }

    public OmegaContextServiceConfig(OmegaContext context) {
        super(null);
        this.setGlobalTxId(context.globalTxId());
        this.setLocalTxId(context.localTxId());
        this.setCategory(context.category());
    }

    public OmegaContextServiceConfig(OmegaContext context, boolean isAutoCompensate, boolean isEnabledAutoCompensateTx) {
        super(null);
        this.setGlobalTxId(context.globalTxId());
        this.setLocalTxId(context.localTxId());
        this.setCategory(context.category());
        this.setAutoCompensate(isAutoCompensate);
        this.setIsEnabledAutoCompensateTx(isEnabledAutoCompensateTx);
    }

}
