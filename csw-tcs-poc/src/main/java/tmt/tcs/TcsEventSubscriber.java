package tmt.tcs;

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
public class TcsEventSubscriber extends BaseEventSubscriber {

	private final AssemblyContext assemblyContext;

	private final EventService.EventMonitor subscribeMonitor;

	private TcsEventSubscriber(AssemblyContext assemblyContext, Optional<ActorRef> refActor,
			IEventService eventService) {

		System.out.println("Inside TcsEventSubscriber");

		subscribeToLocationUpdates();
		this.assemblyContext = assemblyContext;

		subscribeMonitor = startupSubscriptions(eventService);

		receive(subscribeReceive());
	}

	private EventMonitor startupSubscriptions(IEventService eventService) {

		EventMonitor subscribeMonitor = subscribeKeys(eventService, TcsConfig.dummyCK);

		System.out.println("Inside TcsEventSubscriber actor: " + subscribeMonitor.actorRef());

		subscribeKeys(subscribeMonitor, TcsConfig.dummyCK);

		return subscribeMonitor;
	}

	public static Props props(AssemblyContext ac, Optional<ActorRef> refActor, IEventService eventService) {
		return Props.create(new Creator<TcsEventSubscriber>() {
			private static final long serialVersionUID = 1L;

			@Override
			public TcsEventSubscriber create() throws Exception {
				return new TcsEventSubscriber(ac, refActor, eventService);
			}
		});
	}

}
