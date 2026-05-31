package info.pithos.runtime.core.log;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import info.pithos.runtime.model.protocol.http.RequestContextOuterClass.AuthContext;
import info.pithos.runtime.model.protocol.http.RequestContextOuterClass.LogLevelType;
import info.pithos.runtime.model.protocol.http.RequestContextOuterClass.RequestContext;

@ExtendWith(MockitoExtension.class)
class ServiceLoggerImplTest {

    @Mock private Logger mockLogger;

    private ServiceLoggerImpl serviceLogger;

    @BeforeEach
    void setUp() {
        serviceLogger = new ServiceLoggerImpl();
    }

    // --- null guard ---

    @Test
    void logRequest_nullLogger_throws() {
        assertThrows(IllegalArgumentException.class,
            () -> serviceLogger.logRequest(null, null, LogLevelType.ERROR, "msg"));
    }

    // --- message formatting ---
    // Production code calls logger.error(msg, Object[]) — use any(Object[].class) to
    // resolve against error(String, Object...) and avoid ambiguity with error(String, Throwable).

    @Test
    void logRequest_nullRequestContext_logsPlainMessage() {
        serviceLogger.logRequest(null, mockLogger, LogLevelType.ERROR, "plain message");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger).error(captor.capture(), any(Object[].class));
        assertEquals("plain message", captor.getValue());
    }

    @Test
    void logRequest_withRequestContext_prependsRequestId() {
        RequestContext rc = RequestContext.newBuilder()
            .setRequestId("req-123")
            .setLogLevel(LogLevelType.ERROR) // must set; NO_LOG default causes findLogLevel to return NO_LOG
            .build();

        serviceLogger.logRequest(rc, mockLogger, LogLevelType.ERROR, "something happened");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger).error(captor.capture(), any(Object[].class));
        String msg = captor.getValue();
        assertTrue(msg.contains("requestId:[req-123]"), "expected requestId prefix in: " + msg);
        assertTrue(msg.contains("something happened"));
    }

    @Test
    void logRequest_withAuthContext_prependsEnterpriseAndUserId() {
        RequestContext rc = RequestContext.newBuilder()
            .setRequestId("req-456")
            .setLogLevel(LogLevelType.ERROR)
            .setAuthContext(AuthContext.newBuilder()
                .setEnterpriseId("ent-99")
                .setUserId("user-42")
                .build())
            .build();

        serviceLogger.logRequest(rc, mockLogger, LogLevelType.ERROR, "auth event");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger).error(captor.capture(), any(Object[].class));
        String msg = captor.getValue();
        assertTrue(msg.contains("enterpriseId:[ent-99]"), "missing enterpriseId in: " + msg);
        assertTrue(msg.contains("userId:[user-42]"), "missing userId in: " + msg);
    }

    @Test
    void logRequest_withoutAuthContext_doesNotIncludeEnterpriseOrUserId() {
        RequestContext rc = RequestContext.newBuilder()
            .setRequestId("req-789")
            .setLogLevel(LogLevelType.ERROR)
            .build();

        serviceLogger.logRequest(rc, mockLogger, LogLevelType.ERROR, "no auth");

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger).error(captor.capture(), any(Object[].class));
        String msg = captor.getValue();
        assertFalse(msg.contains("enterpriseId"), "unexpected enterpriseId in: " + msg);
        assertFalse(msg.contains("userId"), "unexpected userId in: " + msg);
    }

    // --- log level dispatch ---

    @Test
    void logRequest_errorLevel_callsLoggerError() {
        serviceLogger.logRequest(null, mockLogger, LogLevelType.ERROR, "msg");
        verify(mockLogger).error(any(String.class), any(Object[].class));
    }

    @Test
    void logRequest_warnLevel_callsLoggerWarn() {
        serviceLogger.logRequest(null, mockLogger, LogLevelType.WARN, "msg");
        verify(mockLogger).warn(any(String.class), any(Object[].class));
    }

    @Test
    void logRequest_infoLevel_callsLoggerInfo() {
        // requestContext=null → useLoglevel = loglevel directly; isInfoEnabled() not consulted
        serviceLogger.logRequest(null, mockLogger, LogLevelType.INFO, "msg");
        verify(mockLogger).info(any(String.class), any(Object[].class));
    }

    @Test
    void logRequest_debugLevel_callsLoggerDebug() {
        serviceLogger.logRequest(null, mockLogger, LogLevelType.DEBUG, "msg");
        verify(mockLogger).debug(any(String.class), any(Object[].class));
    }

    @Test
    void logRequest_traceLevel_callsLoggerTrace() {
        serviceLogger.logRequest(null, mockLogger, LogLevelType.TRACE, "msg");
        verify(mockLogger).trace(any(String.class), any(Object[].class));
    }

    // --- findLogLevel fallback ---

    @Test
    void logRequest_requestedInfoButInfoDisabled_fallsBackToError() {
        RequestContext rc = RequestContext.newBuilder()
            .setLogLevel(LogLevelType.INFO)
            .build();
        when(mockLogger.isInfoEnabled()).thenReturn(false);

        serviceLogger.logRequest(rc, mockLogger, LogLevelType.INFO, "msg");

        verify(mockLogger).error(any(String.class), any(Object[].class));
        verify(mockLogger, never()).info(any(String.class), any(Object[].class));
    }

    @Test
    void logRequest_requestedDebugInfoAvailable_fallsBackToInfo() {
        RequestContext rc = RequestContext.newBuilder()
            .setLogLevel(LogLevelType.DEBUG)
            .build();
        when(mockLogger.isDebugEnabled()).thenReturn(false);
        when(mockLogger.isInfoEnabled()).thenReturn(true);

        serviceLogger.logRequest(rc, mockLogger, LogLevelType.DEBUG, "msg");

        verify(mockLogger).info(any(String.class), any(Object[].class));
        verify(mockLogger, never()).debug(any(String.class), any(Object[].class));
    }

    @Test
    void logRequest_requestedDebugNeitherDebugNorInfoEnabled_fallsBackToError() {
        RequestContext rc = RequestContext.newBuilder()
            .setLogLevel(LogLevelType.DEBUG)
            .build();
        when(mockLogger.isDebugEnabled()).thenReturn(false);
        when(mockLogger.isInfoEnabled()).thenReturn(false);

        serviceLogger.logRequest(rc, mockLogger, LogLevelType.DEBUG, "msg");

        verify(mockLogger).error(any(String.class), any(Object[].class));
    }

    // --- exception / throwable overloads ---
    // Production calls logger.error(msg, exception, args) → error(String, Object...) with 2 vararg elements.
    // Using same(ex) typed as Exception gives a 3-arg verify that only matches error(String, Object...).

    @Test
    void logRequest_withException_passesExceptionToLogger() {
        Exception ex = new RuntimeException("test error");
        serviceLogger.logRequest(null, mockLogger, LogLevelType.ERROR, ex, "msg");
        verify(mockLogger).error(any(String.class), same(ex), any(Object[].class));
    }

    @Test
    void logRequest_withThrowable_passesThrowableToLogger() {
        Throwable t = new Error("fatal");
        serviceLogger.logRequest(null, mockLogger, LogLevelType.ERROR, t, "msg");
        verify(mockLogger).error(any(String.class), same(t), any(Object[].class));
    }
}
