package tmt.tcs.ecs;

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
 * This is an actor class that provides the publishing interface specific to ECS
 * to the Event Service and Telemetry Service.
 */
public class EcsEventPublisher extends BaseEventPublisher {
	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	@SuppressWarnings("unused")
	private final AssemblyContext assemblyContext;

	public EcsEventPublisher(AssemblyContext assemblyContext, Optional<IEventService> eventServiceIn,
			Optional<ITelemetryService> telemetryServiceIn) {

		System.out.println("Inside EcsEventPublisher");

		subscribeToLocationUpdates();
		this.assemblyContext = assemblyContext;

		System.out.println("Inside EcsEventPublisher Event Service in: " + eventServiceIn);
		System.out.println("Inside EcsEventPublisher Telemetry Service in: " + telemetryServiceIn);

		receive(publishingEnabled(eventServiceIn, telemetryServiceIn));
	}

	public static Props props(AssemblyContext assemblyContext, Optional<IEventService> eventService,
			Optional<ITelemetryService> telemetryService) {
		return Props.create(new Creator<EcsEventPublisher>() {
			private static final long serialVersionUID = 1L;

			@Override
			public EcsEventPublisher create() throws Exception {
				return new EcsEventPublisher(assemblyContext, eventService, telemetryService);
			}
		});
	}

}
