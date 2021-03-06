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
 * This is an actor class which receives command specific to Offset Operation
 * And after any modifications if required, redirect the same to ECS HCD
 */
public class EcsOffsetCommand extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Optional<ActorRef> ecsStateActor;

	public EcsOffsetCommand(AssemblyContext ac, SetupConfig sc, ActorRef ecsHcd, AssemblyState ecsStartState, Optional<ActorRef> stateActor) {
		this.ecsStateActor = stateActor;

		receive(ReceiveBuilder.matchEquals(JSequentialExecutor.CommandStart(), t -> {
			if (cmd(ecsStartState).equals(cmdUninitialized)
					|| move(ecsStartState).equals(moveMoving)) {
				String errorMessage = "Ecs Assembly state of " + cmd(ecsStartState) + "/" + move(ecsStartState)
						+ " does not allow move";
				System.out.println("Inside EcsOffsetCommand: Error Message is: " + errorMessage);
				sender().tell(new NoLongerValid(new WrongInternalStateIssue(errorMessage)), self());
			} else {
				System.out.println("Inside EcsOffsetCommand: Move command -- START: " + t);

				DoubleItem xItem = jitem(sc, EcsConfig.xDemandKey);
				DoubleItem yItem = jitem(sc, EcsConfig.yDemandKey);

				Double x = jvalue(xItem);
				Double y = jvalue(yItem);

				System.out.println("Inside EcsOffsetCommand: Move command: sc is: " + sc + ": x is: " + x + ": y is: "
						+ y);
				
				DemandMatcher stateMatcher = EcsCommandHandler.posMatcher(x, y);
				SetupConfig scOut = jadd(sc(EcsConfig.offsetPrefix), jset(EcsConfig.x, x),
						jset(EcsConfig.y, y));

				sendState(new AssemblySetState(cmdItem(cmdBusy), moveItem(moveMoving)));

				ecsHcd.tell(new HcdController.Submit(scOut), self());

				Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
				EcsCommandHandler.executeMatch(context(), stateMatcher, ecsHcd, Optional.of(sender()), timeout,
						status -> {
							if (status == Completed) {
								System.out.println("Inside EcsOffsetCommand: Move Command Completed");
								sendState(new AssemblySetState(cmdItem(cmdReady), moveItem(moveIndexed)));
							} else if (status instanceof Error) {
								log.error("Inside EcsOffsetCommand: Offset command match failed with message: "
										+ ((Error) status).message());
							}
						});
			}
		}).matchEquals(JSequentialExecutor.StopCurrentCommand(), t -> {
			System.out.println("Inside EcsOffsetCommand: Offset command -- STOP: " + t);
			ecsHcd.tell(new HcdController.Submit(jadd(sc("tcs.ecs.stop"))), self());
		}).matchAny(t -> log.warning("Inside EcsOffsetCommand: Unknown message received: " + t)).build());
	}
	
	private void sendState(AssemblySetState ecsSetState) {
		ecsStateActor.ifPresent(actorRef -> {
			try {
				ask(actorRef, ecsSetState, 5000).toCompletableFuture().get();
			} catch (Exception e) {
				log.error(e, "Inside EcsOffsetCommand: sendState: Error setting state");
			}
		});
	}

	public static Props props(AssemblyContext ac, SetupConfig sc, ActorRef ecsHcd, AssemblyState ecsState, Optional<ActorRef> stateActor) {
		return Props.create(new Creator<EcsOffsetCommand>() {
			private static final long serialVersionUID = 1L;

			@Override
			public EcsOffsetCommand create() throws Exception {
				return new EcsOffsetCommand(ac, sc, ecsHcd, ecsState, stateActor);
			}
		});
	}
}
