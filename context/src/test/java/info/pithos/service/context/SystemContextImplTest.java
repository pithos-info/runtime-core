package info.pithos.runtime.core.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import info.pithos.runtime.model.config.Config.BootstrapConfigs;
import info.pithos.runtime.model.config.Config.ConfigMap;

class SystemContextImplTest {

    private SystemContextImpl ctx;

    @AfterEach
    void tearDown() {
        if (ctx != null) {
            ctx.shutdown(100);
        }
    }

    private ContextCreator creatorFor(ConfigMap cm) {
        return new ContextCreator() {
            @Override public ConfigMap getConfigMap() { return cm; }
            @Override public Iterable<ServiceModule> getInjectionModules(ApplicationContext appCtx) {
                return List.of();
            }
        };
    }

    private ConfigMap configMap(String name) {
        return ConfigMap.newBuilder()
            .setBootstrapConfigs(BootstrapConfigs.newBuilder()
                .setServiceName(name).setMultiplier(1).build())
            .build();
    }

    @Test
    void constructor_nullCreator_throws() {
        assertThrows(IllegalArgumentException.class, () -> new SystemContextImpl(null));
    }

    @Test
    void constructor_nullConfigMap_throws() {
        assertThrows(IllegalArgumentException.class, () -> new SystemContextImpl(creatorFor(null)));
    }

    @Test
    void constructor_nullServiceName_throws() {
        // proto string default is "" so omitting setServiceName gives empty string
        ConfigMap cm = ConfigMap.newBuilder()
            .setBootstrapConfigs(BootstrapConfigs.newBuilder().build())
            .build();
        assertThrows(IllegalArgumentException.class, () -> new SystemContextImpl(creatorFor(cm)));
    }

    @Test
    void constructor_emptyServiceName_throws() {
        ConfigMap cm = ConfigMap.newBuilder()
            .setBootstrapConfigs(BootstrapConfigs.newBuilder().setServiceName("").build())
            .build();
        assertThrows(IllegalArgumentException.class, () -> new SystemContextImpl(creatorFor(cm)));
    }

    @Test
    void getServiceName_returnsBootstrapName() {
        ctx = new SystemContextImpl(creatorFor(configMap("pithos-svc")));
        assertEquals("pithos-svc", ctx.getServiceName());
    }

    @Test
    void getForkJoinExecutor_isNonNull() {
        ctx = new SystemContextImpl(creatorFor(configMap("svc")));
        assertNotNull(ctx.getForkJoinExecutor());
    }

    @Test
    void getScheduledExecutor_isNonNull() {
        ctx = new SystemContextImpl(creatorFor(configMap("svc")));
        assertNotNull(ctx.getScheduledExecutor());
    }

    @Test
    void getLogger_isNonNull() {
        ctx = new SystemContextImpl(creatorFor(configMap("svc")));
        assertNotNull(ctx.getLogger());
    }

    @Test
    void getConfigMap_returnsSameInstance() {
        ConfigMap cm = configMap("svc");
        ctx = new SystemContextImpl(creatorFor(cm));
        assertSame(cm, ctx.getConfigMap());
    }

    @Test
    void getServiceConfigs_isNonNull() {
        ctx = new SystemContextImpl(creatorFor(configMap("svc")));
        assertNotNull(ctx.getServiceConfigs());
    }

    @Test
    void shutdown_returnsTrue() {
        ctx = new SystemContextImpl(creatorFor(configMap("svc")));
        assertTrue(ctx.shutdown(200));
    }

    @Test
    void multiplierZero_throws() {
        // poolSize = cores * 0 = 0; ForkJoinPool(0) rejects parallelism < 1
        ConfigMap cm = ConfigMap.newBuilder()
            .setBootstrapConfigs(BootstrapConfigs.newBuilder()
                .setServiceName("svc").setMultiplier(0).build())
            .build();
        assertThrows(IllegalArgumentException.class, () -> new SystemContextImpl(creatorFor(cm)));
    }
}
