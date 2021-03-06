package tmt.tcs.m3;

import java.util.Optional;

import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import javacsw.services.events.IEventService;
import javacsw.services.events.ITelemetryService;
import tmt.tcs.common.AssemblyContext;
import tmt.tcs.common.BaseEventPublisher;

/**
 * This is an actor class that provides the publishing interface specific to M3
 * to the Event Service and Telemetry Service.
 */
public class M3EventPublisher extends BaseEventPublisher {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	@SuppressWarnings("unused")
	private final AssemblyContext assemblyContext;

	public M3EventPublisher(AssemblyContext assemblyContext, Optional<IEventService> eventServiceIn,
			Optional<ITelemetryService> telemetryServiceIn) {

		System.out.println("Inside M3EventPublisher");

		subscribeToLocationUpdates();
		this.assemblyContext = assemblyContext;

		System.out.println("Inside M3EventPublisher Event Service in: " + eventServiceIn);
		System.out.println("Inside M3EventPublisher Telemetry Service in: " + telemetryServiceIn);

		receive(publishingEnabled(eventServiceIn, telemetryServiceIn));
	}

	public static Props props(AssemblyContext assemblyContext, Optional<IEventService> eventService,
			Optional<ITelemetryService> telemetryService) {
		return Props.create(new Creator<M3EventPublisher>() {
			private static final long serialVersionUID = 1L;

			@Override
			public M3EventPublisher create() throws Exception {
				return new M3EventPublisher(assemblyContext, eventService, telemetryService);
			}
		});
	}

}
