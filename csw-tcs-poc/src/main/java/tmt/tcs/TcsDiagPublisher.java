package tmt.tcs;

import java.util.Optional;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import javacsw.util.config.JPublisherActor;
import tmt.tcs.common.AssemblyContext;
import tmt.tcs.common.BaseDiagPublisher;

@SuppressWarnings({ "unused" })
public class TcsDiagPublisher extends BaseDiagPublisher {

	private final Optional<ActorRef> eventPublisher;
	private final String actorName;

	private TcsDiagPublisher(AssemblyContext assemblyContext, Optional<ActorRef> referedActor,
			Optional<ActorRef> eventPublisher) {

		System.out.println("Inside TcsDiagPublisher");
		this.eventPublisher = eventPublisher;

		subscribeToLocationUpdates();

		referedActor.ifPresent(actorRef -> actorRef.tell(JPublisherActor.Subscribe, self()));
		this.actorName = assemblyContext.info.getConnections().get(0).name();

		receive(operationsReceive(actorName, 0, referedActor, eventPublisher));
	}

	public static Props props(AssemblyContext assemblyContext, Optional<ActorRef> referedActor,
			Optional<ActorRef> eventPublisher) {
		return Props.create(new Creator<TcsDiagPublisher>() {
			private static final long serialVersionUID = 1L;

			@Override
			public TcsDiagPublisher create() throws Exception {
				return new TcsDiagPublisher(assemblyContext, referedActor, eventPublisher);
			}
		});
	}
}
