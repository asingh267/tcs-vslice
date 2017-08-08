package tmt.tcs.mcs;

import java.util.Optional;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import javacsw.util.config.JPublisherActor;
import tmt.tcs.common.AssemblyContext;
import tmt.tcs.common.BaseDiagPublisher;

@SuppressWarnings({ "unused" })
public class McsDiagPublisher extends BaseDiagPublisher {

	private final Optional<ActorRef> eventPublisher;
	private final String hcdName;

	private McsDiagPublisher(AssemblyContext assemblyContext, Optional<ActorRef> mcsHcd,
			Optional<ActorRef> eventPublisher) {

		System.out.println("Inside McsDiagPublisher");
		this.eventPublisher = eventPublisher;

		subscribeToLocationUpdates();

		mcsHcd.ifPresent(actorRef -> actorRef.tell(JPublisherActor.Subscribe, self()));
		this.hcdName = assemblyContext.info.getConnections().get(0).name();

		receive(operationsReceive(hcdName, 0, mcsHcd, eventPublisher));
	}

	public static Props props(AssemblyContext assemblyContext, Optional<ActorRef> mcsHcd,
			Optional<ActorRef> eventPublisher) {
		return Props.create(new Creator<McsDiagPublisher>() {
			private static final long serialVersionUID = 1L;

			@Override
			public McsDiagPublisher create() throws Exception {
				return new McsDiagPublisher(assemblyContext, mcsHcd, eventPublisher);
			}
		});
	}
}
