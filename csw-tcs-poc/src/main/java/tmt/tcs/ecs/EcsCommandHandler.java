package tmt.tcs.ecs;

import static akka.pattern.PatternsCS.ask;
import static javacsw.util.config.JItems.jadd;
import static javacsw.util.config.JItems.jset;
import static scala.compat.java8.OptionConverters.toJava;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.japi.pf.ReceiveBuilder;
import akka.util.Timeout;
import csw.services.ccs.CommandStatus.CommandStatus;
import csw.services.ccs.DemandMatcher;
import csw.services.ccs.SequentialExecutor.ExecuteOne;
import csw.services.loc.LocationService.Location;
import csw.services.loc.LocationService.ResolvedAkkaLocation;
import csw.services.loc.LocationService.ResolvedTcpLocation;
import csw.services.loc.LocationService.Unresolved;
import csw.util.config.Configurations.ConfigKey;
import csw.util.config.Configurations.SetupConfig;
import csw.util.config.StateVariable.DemandState;
import javacsw.services.ccs.JSequentialExecutor;
import javacsw.services.events.IEventService;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;
import tmt.tcs.common.AssemblyContext;
import tmt.tcs.common.AssemblyStateActor;
import tmt.tcs.common.BaseCommandHandler;

/*
 * This is an actor class which receives commands forwarded by ECS Assembly
 * And based upon the command config key send to specific command actor class
 */
@SuppressWarnings("unused")
public class EcsCommandHandler extends BaseCommandHandler {

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final AssemblyContext assemblyContext;
	private final Optional<ActorRef> allEventPublisher;

	private final ActorRef ecsStateActor;

	private final ActorRef badHcdReference;

	private ActorRef ecsHcd;

	private Optional<IEventService> badEventService = Optional.empty();
	private Optional<IEventService> eventService = badEventService;

	public EcsCommandHandler(AssemblyContext ac, Optional<ActorRef> ecsHcd, Optional<ActorRef> allEventPublisher) {

		System.out.println("Inside EcsCommandHandler");

		this.assemblyContext = ac;
		badHcdReference = context().system().deadLetters();
		this.ecsHcd = ecsHcd.orElse(badHcdReference);
		this.allEventPublisher = allEventPublisher;
		ecsStateActor = context().actorOf(AssemblyStateActor.props());

		subscribeToLocationUpdates();

		receive(initReceive());
	}

	private void handleLocations(Location location) {
		if (location instanceof ResolvedAkkaLocation) {
			ResolvedAkkaLocation l = (ResolvedAkkaLocation) location;
			System.out.println("Inside EcsCommandHandler: CommandHandler receive an actorRef: " + l.getActorRef());
			ecsHcd = l.getActorRef().orElse(badHcdReference);

		} else if (location instanceof ResolvedTcpLocation) {
			ResolvedTcpLocation t = (ResolvedTcpLocation) location;
			System.out.println("Inside EcsCommandHandler: Received TCP Location: " + t.connection());
			if (location.connection().equals(IEventService.eventServiceConnection())) {
				System.out.println("Inside EcsCommandHandler: Assembly received ES connection: " + t);
				eventService = Optional.of(IEventService.getEventService(t.host(), t.port(), context().system()));
				System.out.println("Inside EcsCommandHandler: Event Service at: " + eventService);
			}

		} else if (location instanceof Unresolved) {
			System.out.println("Inside EcsCommandHandler: Unresolved: " + location.connection());
			if (location.connection().equals(IEventService.eventServiceConnection()))
				eventService = badEventService;
			if (location.connection().componentId().equals(assemblyContext.hcdComponentId))
				ecsHcd = badHcdReference;

		} else {
			System.out.println("Inside EcsCommandHandler: CommandHandler received some other location: " + location);
		}
	}

	private PartialFunction<Object, BoxedUnit> initReceive() {
		return ReceiveBuilder.match(Location.class, this::handleLocations).match(ExecuteOne.class, t -> {

			SetupConfig sc = t.sc();
			Optional<ActorRef> commandOriginator = toJava(t.commandOriginator());
			ConfigKey configKey = sc.configKey();

			System.out.println("Inside EcsCommandHandler initReceive: ExecuteOne: SetupConfig is: " + sc
					+ ": configKey is: " + configKey);

			if (configKey.equals(EcsConfig.positionDemandCK)) {
				System.out.println("Inside EcsCommandHandler initReceive: ExecuteOne: moveCK Command ");
				ActorRef moveActorRef = context().actorOf(
						EcsMoveCommand.props(assemblyContext, sc, ecsHcd, currentState(), Optional.of(ecsStateActor)));
				context().become(actorExecutingReceive(moveActorRef, commandOriginator));
			} else if (configKey.equals(EcsConfig.offsetDemandCK)) {
				System.out.println("Inside EcsCommandHandler initReceive: ExecuteOne: offsetCK Command ");
				ActorRef offsetActorRef = context().actorOf(EcsOffsetCommand.props(assemblyContext, sc, ecsHcd,
						currentState(), Optional.of(ecsStateActor)));
				context().become(actorExecutingReceive(offsetActorRef, commandOriginator));
			}

			self().tell(JSequentialExecutor.CommandStart(), self());
		}).build();
	}

	private PartialFunction<Object, BoxedUnit> actorExecutingReceive(ActorRef currentCommand,
			Optional<ActorRef> commandOriginator) {
		Timeout timeout = new Timeout(5, TimeUnit.SECONDS);

		return ReceiveBuilder.matchEquals(JSequentialExecutor.CommandStart(), t -> {
			System.out.println("Inside EcsCommandHandler actorExecutingReceive: JSequentialExecutor.CommandStart");

			ask(currentCommand, JSequentialExecutor.CommandStart(), timeout.duration().toMillis()).thenApply(reply -> {
				CommandStatus cs = (CommandStatus) reply;
				System.out.println("Inside EcsCommandHandler actorExecutingReceive: CommandStatus is: " + cs);
				commandOriginator.ifPresent(actorRef -> actorRef.tell(cs, self()));
				currentCommand.tell(PoisonPill.getInstance(), self());
				return null;
			});
		}).

				match(CommandDone.class, t -> {
					System.out.println("Inside EcsCommandHandler actorExecutingReceive: CommandDone");
					context().become(initReceive());
				}).

				match(SetupConfig.class, t -> {
					System.out.println("Inside EcsCommandHandler actorExecutingReceive: SetupConfig");
				}).

				match(ExecuteOne.class, t -> {
					System.out.println("Inside EcsCommandHandler actorExecutingReceive: ExecuteOne");
				})
				.matchAny(t -> log
						.warning("Inside EcsCommandHandler actorExecutingReceive: received an unknown message: " + t))
				.build();
	}

	public static Props props(AssemblyContext ac, Optional<ActorRef> ecsHcd, Optional<ActorRef> allEventPublisher) {
		return Props.create(new Creator<EcsCommandHandler>() {
			private static final long serialVersionUID = 1L;

			@Override
			public EcsCommandHandler create() throws Exception {
				return new EcsCommandHandler(ac, ecsHcd, allEventPublisher);
			}
		});
	}

	public static DemandMatcher posMatcher(double az, double el, double time) {
		System.out.println("Inside EcsCommandHandler posMatcher Move: Starts");

		DemandState ds = jadd(new DemandState(EcsConfig.ecsStateCK.prefix()), jset(EcsConfig.az, az),
				jset(EcsConfig.el, el), jset(EcsConfig.time, time));
		return new DemandMatcher(ds, false);
	}

	public static DemandMatcher posMatcher(double x, double y) {
		System.out.println("Inside EcsCommandHandler posMatcher Offset: Starts");

		DemandState ds = jadd(new DemandState(EcsConfig.ecsStateCK.prefix()), jset(EcsConfig.x, x),
				jset(EcsConfig.y, y));
		return new DemandMatcher(ds, false);
	}

}
