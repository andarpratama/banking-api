package com.company.banking.notification.application;

import com.company.banking.notification.domain.NotificationMessage;

/**
 * Outbound notification port. Implementations must not fail the calling business flow
 * (log/no-op stubs swallow errors; real vendors should isolate delivery failures).
 *
 * <p>Wired today from {@code TransferService} after a successful transfer (T-052).
 * Auth and other money paths can inject the same port later without vendor lock-in.
 */
public interface NotificationPublisher {

    void publish(NotificationMessage message);
}
