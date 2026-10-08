package com.learney.contentaudit.auditcli.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

import com.learney.contentaudit.revisiondomain.batch.BatchService;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class MainLazyBatchServiceTest {
    /** A new default method on the port must not disappear at the real CLI's lazy boundary. */
    @TestFactory
    Stream<DynamicTest> delegatesEveryBatchPortMethodWithoutEagerInitialization() {
        return Arrays.stream(BatchService.class.getDeclaredMethods()).map(method -> DynamicTest.dynamicTest(
                method.toGenericString(), () -> {
                    var delegate = mock(BatchService.class);
                    var creations = new AtomicInteger();
                    Supplier<BatchService> factory = () -> {
                        creations.incrementAndGet();
                        return delegate;
                    };
                    Method lazy = Main.class.getDeclaredMethod("lazyBatchService", Supplier.class);
                    lazy.setAccessible(true);
                    BatchService wrapper = (BatchService) lazy.invoke(null, factory);
                    assertEquals(0, creations.get());
                    method.invoke(wrapper, new Object[method.getParameterCount()]);
                    assertEquals(1, creations.get(), "only the requested operation initializes the service");
                    var invocations = mockingDetails(delegate).getInvocations();
                    assertEquals(1, invocations.size(), "the operation must reach the actual service");
                    assertEquals(method, invocations.iterator().next().getMethod());
                }));
    }
}
