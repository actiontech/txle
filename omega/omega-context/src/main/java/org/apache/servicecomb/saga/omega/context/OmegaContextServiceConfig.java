package org.apache.servicecomb.saga.omega.context;

/**
 * OmegaContext holds the globalTxId and localTxId which are used to build the invocation map
 */
public class OmegaContextServiceConfig extends OmegaContext {
    // Do not need think about concurrency situation, due to they're one-to-one with current application.
    private String serviceName;
    private String instanceId;

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

    public OmegaContextServiceConfig(IdGenerator<String> idGenerator) {
        super(idGenerator);
    }

    public OmegaContextServiceConfig(OmegaContext context) {
        super(null);
        setGlobalTxId(context.globalTxId());
        setLocalTxId(context.localTxId());
        setCategory(context.category());
    }

}
