package tmt.tcs.m3;

import java.util.Optional;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import javacsw.util.config.JPublisherActor;
import tmt.tcs.common.AssemblyContext;
import tmt.tcs.common.BaseDiagPublisher;

@SuppressWarnings({ "unused" })
public class M3DiagPublisher extends BaseDiagPublisher {

	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Optional<ActorRef> eventPublisher;
	private final String hcdName;

	private M3DiagPublisher(AssemblyContext assemblyContext, Optional<ActorRef> m3Hcd,
			Optional<ActorRef> eventPublisher) {

		System.out.println("Inside M3DiagPublisher");
		this.eventPublisher = eventPublisher;

		subscribeToLocationUpdates();

		m3Hcd.ifPresent(actorRef -> actorRef.tell(JPublisherActor.Subscribe, self()));
		this.hcdName = assemblyContext.info.getConnections().get(0).name();

		receive(operationsReceive(hcdName, 0, m3Hcd, eventPublisher));
	}

	public static Props props(AssemblyContext assemblyContext, Optional<ActorRef> m3Hcd,
			Optional<ActorRef> eventPublisher) {
		return Props.create(new Creator<M3DiagPublisher>() {
			private static final long serialVersionUID = 1L;

			@Override
			public M3DiagPublisher create() throws Exception {
				return new M3DiagPublisher(assemblyContext, m3Hcd, eventPublisher);
			}
		});
	}
}
