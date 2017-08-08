package tmt.tcs.common;

import java.util.Optional;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import csw.services.loc.LocationService;
import csw.services.loc.LocationService.ResolvedTcpLocation;
import csw.util.config.DoubleItem;
import javacsw.services.events.IEventService;
import javacsw.services.events.ITelemetryService;
import javacsw.services.pkg.ILocationSubscriberClient;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

/**
 * This is the base Event Publisher class being extended by all Event Publisher
 */
public abstract class BaseEventPublisher extends AbstractActor implements ILocationSubscriberClient {

	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public PartialFunction<Object, BoxedUnit> publishingEnabled(Optional<IEventService> eventService,
			Optional<ITelemetryService> telemetryService) {
		return ReceiveBuilder.match(EngrUpdate.class, t -> publishEngr(telemetryService, t.az, t.el, t.time)).

				match(LocationService.Location.class,
						location -> handleLocations(location, eventService, telemetryService))
				.

				matchAny(t -> log.warning("Inside BaseEventPublisher Unexpected message in publishingEnabled: " + t)).

				build();
	}

	private void handleLocations(LocationService.Location location, Optional<IEventService> currentEventService,
			Optional<ITelemetryService> currentTelemetryService) {
		if (location instanceof ResolvedTcpLocation) {
			ResolvedTcpLocation t = (ResolvedTcpLocation) location;
			System.out.println("Inside BaseEventPublisher Received TCP Location: " + t.connection());
			// Verify that it is the event service
			if (location.connection().equals(IEventService.eventServiceConnection())) {
				System.out.println("Inside BaseEventPublisher received connection: " + t);
				Optional<IEventService> newEventService = Optional
						.of(IEventService.getEventService(t.host(), t.port(), context().system()));
				System.out.println("Inside BaseEventPublisherEvent Service at: " + newEventService);
				context().become(publishingEnabled(newEventService, currentTelemetryService));
			}

			if (location.connection().equals(ITelemetryService.telemetryServiceConnection())) {
				System.out.println("Inside BaseEventPublisher received connection: " + t);
				Optional<ITelemetryService> newTelemetryService = Optional
						.of(ITelemetryService.getTelemetryService(t.host(), t.port(), context().system()));
				System.out.println("Inside BaseEventPublisher Telemetry Service at: " + newTelemetryService);
				context().become(publishingEnabled(currentEventService, newTelemetryService));
			}

		} else if (location instanceof LocationService.Unresolved) {
			log.debug("Unresolved: " + location.connection());
			if (location.connection().equals(IEventService.eventServiceConnection()))
				context().become(publishingEnabled(Optional.empty(), currentTelemetryService));
			else if (location.connection().equals(ITelemetryService.telemetryServiceConnection()))
				context().become(publishingEnabled(currentEventService, Optional.empty()));

		} else {
			System.out.println("Inside BaseEventPublisher received some other location: " + location);
		}
	}

	private void publishEngr(Optional<ITelemetryService> telemetryService, DoubleItem az, DoubleItem el,
			DoubleItem time) {

	}

	public static class EngrUpdate {
		public final DoubleItem az;
		public final DoubleItem el;
		public final DoubleItem time;

		public EngrUpdate(DoubleItem az, DoubleItem el, DoubleItem time) {
			this.az = az;
			this.el = el;
			this.time = time;
		}
	}
}
