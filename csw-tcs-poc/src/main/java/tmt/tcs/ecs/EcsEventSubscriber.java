package tmt.tcs.ecs;

import java.util.Optional;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import csw.services.events.EventService;
import csw.services.events.EventService.EventMonitor;
import javacsw.services.events.IEventService;
import tmt.tcs.common.AssemblyContext;
import tmt.tcs.common.BaseEventSubscriber;

@SuppressWarnings("unused")
public class EcsEventSubscriber extends BaseEventSubscriber {

	private final AssemblyContext assemblyContext;

	private final EventService.EventMonitor subscribeMonitor;

	private EcsEventSubscriber(AssemblyContext assemblyContext, Optional<ActorRef> refActor,
			IEventService eventService) {

		System.out.println("Inside EcsEventPublisher");

		subscribeToLocationUpdates();
		this.assemblyContext = assemblyContext;

		subscribeMonitor = startupSubscriptions(eventService);

		receive(subscribeReceive());
	}

	private EventMonitor startupSubscriptions(IEventService eventService) {

		EventMonitor subscribeMonitor = subscribeKeys(eventService, EcsConfig.dummyCK);

		System.out.println("Inside EcsEventPublisher actor: " + subscribeMonitor.actorRef());

		subscribeKeys(subscribeMonitor, EcsConfig.dummyCK);

		return subscribeMonitor;
	}

	public static Props props(AssemblyContext ac, Optional<ActorRef> refActor, IEventService eventService) {
		return Props.create(new Creator<EcsEventSubscriber>() {
			private static final long serialVersionUID = 1L;

			@Override
			public EcsEventSubscriber create() throws Exception {
				return new EcsEventSubscriber(ac, refActor, eventService);
			}
		});
	}

}
