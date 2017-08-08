package tmt.tcs.common;

import java.util.Objects;
import java.util.Optional;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import csw.services.loc.LocationService;
import csw.services.loc.LocationService.Location;
import csw.services.ts.AbstractTimeServiceScheduler;
import csw.util.config.StateVariable.CurrentState;
import javacsw.services.ccs.JHcdController;
import javacsw.services.pkg.ILocationSubscriberClient;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

/**
 * This is the base Diag Publisher class being extended by all Diag Publisher
 */
public abstract class BaseDiagPublisher extends AbstractTimeServiceScheduler implements ILocationSubscriberClient {

	LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	public PartialFunction<Object, BoxedUnit> operationsReceive(String hcdName, int stateMessageCounter,
			Optional<ActorRef> hcd, Optional<ActorRef> eventPublisher) {
		return ReceiveBuilder.match(CurrentState.class, cs -> {
			publishStateUpdate(cs, eventPublisher);
		}).match(Location.class, location -> {

			if (location instanceof LocationService.ResolvedAkkaLocation) {
				if (Objects.equals(location.connection().name(), hcdName)) {
					LocationService.ResolvedAkkaLocation rloc = (LocationService.ResolvedAkkaLocation) location;
					System.out.println(
							"Inside BaseDiagPublisher operationsReceive updated actorRef: " + rloc.getActorRef());
					Optional<ActorRef> newHcdActorRef = rloc.getActorRef();
					newHcdActorRef.ifPresent(actorRef -> actorRef.tell(JHcdController.Subscribe, self()));
					context().become(operationsReceive(hcdName, stateMessageCounter, newHcdActorRef, eventPublisher));
				}

			} else if (location instanceof LocationService.Unresolved) {
				if (Objects.equals(location.connection().name(), hcdName)) {
					System.out.println("Inside BaseDiagPublisher operationsReceive got unresolve for HCD");
					context().become(operationsReceive(hcdName, stateMessageCounter, Optional.empty(), eventPublisher));
				}
			} else if (location instanceof LocationService.UnTrackedLocation) {
				if (Objects.equals(location.connection().name(), hcdName)) {
					System.out.println("Inside BaseDiagPublisher operationsReceive got untrack for HCD");
					context().become(operationsReceive(hcdName, stateMessageCounter, Optional.empty(), eventPublisher));
				}
			}
		}).matchAny(
				t -> log.warning("Inside BaseDiagPublisher :operationsReceive received an unexpected message: " + t))
				.build();
	}

	public PartialFunction<Object, BoxedUnit> diagnosticReceive(String hcdName, int stateMessageCounter,
			Optional<ActorRef> hcd, Cancellable cancelToken, Optional<ActorRef> eventPublisher) {
		return ReceiveBuilder.match(CurrentState.class, cs -> {
			publishStatsUpdate(cs, eventPublisher);
		}).match(Location.class, location -> {

			if (location instanceof LocationService.ResolvedAkkaLocation) {
				if (Objects.equals(location.connection().name(), hcdName)) {
					LocationService.ResolvedAkkaLocation rloc = (LocationService.ResolvedAkkaLocation) location;
					System.out.println(
							"Inside BaseDiagPublisher diagnosticReceive updated actorRef: " + rloc.getActorRef());
					Optional<ActorRef> newHcdActorRef = rloc.getActorRef();
					newHcdActorRef.ifPresent(actorRef -> actorRef.tell(JHcdController.Subscribe, self()));
					context().become(diagnosticReceive(hcdName, stateMessageCounter, newHcdActorRef, cancelToken,
							eventPublisher));
				}

			} else if (location instanceof LocationService.Unresolved) {
				if (Objects.equals(location.connection().name(), hcdName)) {
					System.out.println("Inside BaseDiagPublisher diagnosticReceive got unresolve for HCD");
					context().become(diagnosticReceive(hcdName, stateMessageCounter, Optional.empty(), cancelToken,
							eventPublisher));
				}

			} else if (location instanceof LocationService.UnTrackedLocation) {
				if (Objects.equals(location.connection().name(), hcdName)) {
					System.out.println("Inside BaseDiagPublisher diagnosticReceive got untrack for HCD");
					context().become(diagnosticReceive(hcdName, stateMessageCounter, Optional.empty(), cancelToken,
							eventPublisher));
				}
			}
		}).matchAny(t -> log.warning("DiagPublisher:diagnosticReceive received an unexpected message: " + t)).build();
	}

	private void publishStateUpdate(CurrentState cs, Optional<ActorRef> eventPublisher) {
		System.out.println("Inside BaseDiagPublisher publish state: " + cs);
		eventPublisher.ifPresent(actorRef -> actorRef.tell(new String(""), self()));
	}

	private void publishStatsUpdate(CurrentState cs, Optional<ActorRef> eventPublisher) {
		System.out.println("Inside BaseDiagPublisher publish stats");
		eventPublisher.ifPresent(actorRef -> actorRef.tell(new String(""), self()));
	}

	/**
	 * Internal messages used by diag publisher
	 */
	public interface DiagPublisherMessages {
	}

	public static class DiagnosticState implements DiagPublisherMessages {
	}

	public static class OperationsState implements DiagPublisherMessages {
	}
}
