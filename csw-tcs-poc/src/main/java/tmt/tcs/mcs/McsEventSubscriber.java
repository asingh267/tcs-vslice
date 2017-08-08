package tmt.tcs.mcs;

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
public class McsEventSubscriber extends BaseEventSubscriber {

	private final AssemblyContext assemblyContext;

	private final EventService.EventMonitor subscribeMonitor;

	private McsEventSubscriber(AssemblyContext assemblyContext, Optional<ActorRef> refActor,
			IEventService eventService) {

		System.out.println("Inside McsEventSubscriber");

		subscribeToLocationUpdates();
		this.assemblyContext = assemblyContext;

		subscribeMonitor = startupSubscriptions(eventService);

		receive(subscribeReceive());
	}

	private EventMonitor startupSubscriptions(IEventService eventService) {

		EventMonitor subscribeMonitor = subscribeKeys(eventService, McsConfig.dummyCK);

		System.out.println("Inside McsEventSubscriber actor: " + subscribeMonitor.actorRef());

		subscribeKeys(subscribeMonitor, McsConfig.dummyCK);

		return subscribeMonitor;
	}

	public static Props props(AssemblyContext ac, Optional<ActorRef> refActor, IEventService eventService) {
		return Props.create(new Creator<McsEventSubscriber>() {
			private static final long serialVersionUID = 1L;

			@Override
			public McsEventSubscriber create() throws Exception {
				return new McsEventSubscriber(ac, refActor, eventService);
			}
		});
	}

}
