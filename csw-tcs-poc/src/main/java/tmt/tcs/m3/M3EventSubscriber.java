package tmt.tcs.m3;

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
public class M3EventSubscriber extends BaseEventSubscriber {

	private final AssemblyContext assemblyContext;

	private final EventService.EventMonitor subscribeMonitor;

	private M3EventSubscriber(AssemblyContext assemblyContext, Optional<ActorRef> refActor,
			IEventService eventService) {

		System.out.println("Inside M3EventSubscriber");

		subscribeToLocationUpdates();
		this.assemblyContext = assemblyContext;

		subscribeMonitor = startupSubscriptions(eventService);

		receive(subscribeReceive());
	}

	private EventMonitor startupSubscriptions(IEventService eventService) {

		EventMonitor subscribeMonitor = subscribeKeys(eventService, M3Config.dummyCK);

		System.out.println("Inside M3EventSubscriber actor: " + subscribeMonitor.actorRef());

		subscribeKeys(subscribeMonitor, M3Config.dummyCK);

		return subscribeMonitor;
	}

	public static Props props(AssemblyContext ac, Optional<ActorRef> refActor, IEventService eventService) {
		return Props.create(new Creator<M3EventSubscriber>() {
			private static final long serialVersionUID = 1L;

			@Override
			public M3EventSubscriber create() throws Exception {
				return new M3EventSubscriber(ac, refActor, eventService);
			}
		});
	}

}
