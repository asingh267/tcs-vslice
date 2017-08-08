package tmt.tcs.ecs;

import java.util.Objects;
import java.util.Optional;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.japi.pf.ReceiveBuilder;
import csw.services.loc.LocationService;
import csw.services.loc.LocationService.Location;
import csw.services.ts.AbstractTimeServiceScheduler;
import csw.util.config.StateVariable.CurrentState;
import javacsw.services.ccs.JHcdController;
import javacsw.services.pkg.ILocationSubscriberClient;
import javacsw.util.config.JPublisherActor;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;
import tmt.tcs.common.AssemblyContext;
import tmt.tcs.common.BaseDiagPublisher;

@SuppressWarnings({ "unused" })
public class EcsDiagPublisher extends BaseDiagPublisher {

	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Optional<ActorRef> eventPublisher;
	private final String hcdName;

	private EcsDiagPublisher(AssemblyContext assemblyContext, Optional<ActorRef> ecsHcd,
			Optional<ActorRef> eventPublisher) {

		System.out.println("Inside EcsDiagPublisher");
		this.eventPublisher = eventPublisher;

		subscribeToLocationUpdates();

		ecsHcd.ifPresent(actorRef -> actorRef.tell(JPublisherActor.Subscribe, self()));
		this.hcdName = assemblyContext.info.getConnections().get(0).name();

		receive(operationsReceive(hcdName, 0, ecsHcd, eventPublisher));
	}
	
	public static Props props(AssemblyContext assemblyContext, Optional<ActorRef> ecsHcd,
			Optional<ActorRef> eventPublisher) {
		return Props.create(new Creator<EcsDiagPublisher>() {
			private static final long serialVersionUID = 1L;

			@Override
			public EcsDiagPublisher create() throws Exception {
				return new EcsDiagPublisher(assemblyContext, ecsHcd, eventPublisher);
			}
		});
	}
}
