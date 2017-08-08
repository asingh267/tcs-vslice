package tmt.tcs.mcs;

import java.util.Optional;

import akka.actor.Props;
import akka.japi.Creator;
import javacsw.services.events.IEventService;
import javacsw.services.events.ITelemetryService;
import tmt.tcs.common.AssemblyContext;
import tmt.tcs.common.BaseEventPublisher;

/**
 * This is an actor class that provides the publishing interface specific to MCS
 * to the Event Service and Telemetry Service.
 */
public class McsEventPublisher extends BaseEventPublisher {

	@SuppressWarnings("unused")
	private final AssemblyContext assemblyContext;

	public McsEventPublisher(AssemblyContext assemblyContext, Optional<IEventService> eventServiceIn,
			Optional<ITelemetryService> telemetryServiceIn) {

		System.out.println("Inside McsEventPublisher");

		subscribeToLocationUpdates();
		this.assemblyContext = assemblyContext;

		System.out.println("Inside McsEventPublisher Event Service in: " + eventServiceIn);
		System.out.println("Inside McsEventPublisher Telemetry Service in: " + telemetryServiceIn);

		receive(publishingEnabled(eventServiceIn, telemetryServiceIn));
	}

	public static Props props(AssemblyContext assemblyContext, Optional<IEventService> eventService,
			Optional<ITelemetryService> telemetryService) {
		return Props.create(new Creator<McsEventPublisher>() {
			private static final long serialVersionUID = 1L;

			@Override
			public McsEventPublisher create() throws Exception {
				return new McsEventPublisher(assemblyContext, eventService, telemetryService);
			}
		});
	}

}
