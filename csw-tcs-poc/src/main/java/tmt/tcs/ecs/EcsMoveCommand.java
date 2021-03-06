package tmt.tcs.ecs;

import static akka.pattern.PatternsCS.ask;
import static javacsw.services.ccs.JCommandStatus.Completed;
import static javacsw.util.config.JConfigDSL.sc;
import static javacsw.util.config.JItems.jadd;
import static javacsw.util.config.JItems.jitem;
import static javacsw.util.config.JItems.jset;
import static javacsw.util.config.JItems.jvalue;
import static tmt.tcs.common.AssemblyStateActor.cmd;
import static tmt.tcs.common.AssemblyStateActor.cmdBusy;
import static tmt.tcs.common.AssemblyStateActor.cmdItem;
import static tmt.tcs.common.AssemblyStateActor.cmdReady;
import static tmt.tcs.common.AssemblyStateActor.cmdUninitialized;
import static tmt.tcs.common.AssemblyStateActor.move;
import static tmt.tcs.common.AssemblyStateActor.moveIndexed;
import static tmt.tcs.common.AssemblyStateActor.moveItem;
import static tmt.tcs.common.AssemblyStateActor.moveMoving;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.japi.pf.ReceiveBuilder;
import akka.util.Timeout;
import csw.services.ccs.CommandStatus.Error;
import csw.services.ccs.CommandStatus.NoLongerValid;
import csw.services.ccs.DemandMatcher;
import csw.services.ccs.HcdController;
import csw.services.ccs.Validation.WrongInternalStateIssue;
import csw.util.config.Configurations.SetupConfig;
import csw.util.config.DoubleItem;
import javacsw.services.ccs.JSequentialExecutor;
import tmt.tcs.common.AssemblyContext;
import tmt.tcs.common.AssemblyStateActor.AssemblySetState;
import tmt.tcs.common.AssemblyStateActor.AssemblyState;

/*
 * This is an actor class which receives command specific to Move Operation
 * And after any modifications if required, redirect the same to ECS HCD
 */
public class EcsMoveCommand extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Optional<ActorRef> ecsStateActor;

	public EcsMoveCommand(AssemblyContext ac, SetupConfig sc, ActorRef ecsHcd, AssemblyState ecsStartState,
			Optional<ActorRef> stateActor) {
		this.ecsStateActor = stateActor;

		receive(ReceiveBuilder.matchEquals(JSequentialExecutor.CommandStart(), t -> {
			if (cmd(ecsStartState).equals(cmdUninitialized) || move(ecsStartState).equals(moveMoving)) {
				String errorMessage = "Ecs Assembly state of " + cmd(ecsStartState) + "/" + move(ecsStartState)
						+ " does not allow move";
				System.out.println("Inside EcsMoveCommand: Error Message is: " + errorMessage);
				sender().tell(new NoLongerValid(new WrongInternalStateIssue(errorMessage)), self());
			} else {
				System.out.println("Inside EcsMoveCommand: Move command -- START: " + t);

				DoubleItem azItem = jitem(sc, EcsConfig.azDemandKey);
				DoubleItem elItem = jitem(sc, EcsConfig.elDemandKey);
				DoubleItem timeItem = jitem(sc, EcsConfig.timeDemandKey);

				Double az = jvalue(azItem);
				Double el = jvalue(elItem);
				Double time = jvalue(timeItem);

				System.out.println("Inside EcsMoveCommand: Move command: sc is: " + sc + ": az is: " + az + ": el is: "
						+ el + ": time is: " + time);

				DemandMatcher stateMatcher = EcsCommandHandler.posMatcher(az, el, time);
				SetupConfig scOut = jadd(sc(EcsConfig.movePrefix), jset(EcsConfig.az, az), jset(EcsConfig.el, el),
						jset(EcsConfig.time, time));

				sendState(new AssemblySetState(cmdItem(cmdBusy), moveItem(moveMoving)));

				ecsHcd.tell(new HcdController.Submit(scOut), self());

				Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
				EcsCommandHandler.executeMatch(context(), stateMatcher, ecsHcd, Optional.of(sender()), timeout,
						status -> {
							if (status == Completed) {
								System.out.println("Inside EcsMoveCommand: Move Command Completed");
								sendState(new AssemblySetState(cmdItem(cmdReady), moveItem(moveIndexed)));
							} else if (status instanceof Error) {
								log.error("Inside EcsMoveCommand: Move command match failed with message: "
										+ ((Error) status).message());
							}
						});
			}
		}).matchEquals(JSequentialExecutor.StopCurrentCommand(), t -> {
			System.out.println("Inside EcsMoveCommand: Move command -- STOP: " + t);
			ecsHcd.tell(new HcdController.Submit(jadd(sc("tcs.ecs.stop"))), self());
		}).matchAny(t -> log.warning("Inside EcsMoveCommand: Unknown message received: " + t)).build());
	}

	private void sendState(AssemblySetState ecsSetState) {
		ecsStateActor.ifPresent(actorRef -> {
			try {
				ask(actorRef, ecsSetState, 5000).toCompletableFuture().get();
			} catch (Exception e) {
				log.error(e, "Inside EcsMoveCommand: sendState: Error setting state");
			}
		});
	}

	public static Props props(AssemblyContext ac, SetupConfig sc, ActorRef ecsHcd, AssemblyState ecsState,
			Optional<ActorRef> stateActor) {
		return Props.create(new Creator<EcsMoveCommand>() {
			private static final long serialVersionUID = 1L;

			@Override
			public EcsMoveCommand create() throws Exception {
				return new EcsMoveCommand(ac, sc, ecsHcd, ecsState, stateActor);
			}
		});
	}
}
