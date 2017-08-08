package tmt.tcs.common;

import java.util.Arrays;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import csw.services.events.EventService.EventMonitor;
import csw.util.config.Configurations.ConfigKey;
import csw.util.config.Events.SystemEvent;
import javacsw.services.events.IEventService;
import javacsw.services.pkg.ILocationSubscriberClient;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;
import tmt.tcs.mcs.McsConfig;

/**
 * This is the base Event Subscriber class being extended by all Event
 * Subscribers
 */
public abstract class BaseEventSubscriber extends AbstractActor implements ILocationSubscriberClient {

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public PartialFunction<Object, BoxedUnit> subscribeReceive() {
		return ReceiveBuilder.

				match(SystemEvent.class, event -> {
					if (event.info().source().equals(McsConfig.dummyCK)) {

					} else {
						System.out
								.println("Inside BaseEventSubscriber subscribeReceive received an unknown SystemEvent: "
										+ event.info().source());
					}
				}).

				matchAny(t -> log
						.warning("Inside BaseEventSubscriber Unexpected message received:subscribeReceive: " + t))
				.build();
	}

	public void unsubscribeKeys(EventMonitor monitor, ConfigKey... configKeys) {
		System.out.println("Inside BaseEventSubscriber Unsubscribing to: " + Arrays.toString(configKeys));
		for (ConfigKey configKey : configKeys) {
			monitor.unsubscribeFrom(configKey.prefix());
		}
	}

	public EventMonitor subscribeKeys(IEventService eventService, ConfigKey... configKeys) {
		System.out.println("Inside BaseEventSubscriber Subscribing to: " + Arrays.toString(configKeys));
		String[] prefixes = new String[configKeys.length];
		for (int i = 0; i < configKeys.length; i++) {
			prefixes[i] = configKeys[i].prefix();
		}
		return eventService.subscribe(self(), false, prefixes);
	}

	public void subscribeKeys(EventMonitor monitor, ConfigKey... configKeys) {
		System.out.println("Inside BaseEventSubscriber Subscribing to: " + Arrays.toString(configKeys));
		for (ConfigKey configKey : configKeys) {
			monitor.subscribeTo(configKey.prefix());
		}
	}
}
