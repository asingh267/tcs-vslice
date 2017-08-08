package tmt.tcs;

import static akka.pattern.PatternsCS.ask;
import static javacsw.services.ccs.JCommandStatus.Completed;
import static javacsw.util.config.JConfigDSL.sc;
import static javacsw.util.config.JItems.jadd;
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
import javacsw.services.ccs.JSequentialExecutor;
import tmt.tcs.common.AssemblyContext;
import tmt.tcs.common.AssemblyStateActor.AssemblySetState;
import tmt.tcs.common.AssemblyStateActor.AssemblyState;

/*
 * This is an actor class which receives command specific to Move Operation
 * And after any modifications if required, redirect the same to TPK
 */
public class TcsMoveCommand extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Optional<ActorRef> tcsStateActor;

	public TcsMoveCommand(AssemblyContext ac, SetupConfig sc, ActorRef referedActor, AssemblyState tcsStartState,
			Optional<ActorRef> stateActor) {
		this.tcsStateActor = stateActor;

		receive(ReceiveBuilder.matchEquals(JSequentialExecutor.CommandStart(), t -> {
			if (cmd(tcsStartState).equals(cmdUninitialized) || move(tcsStartState).equals(moveMoving)) {
				String errorMessage = "Tcs Assembly state of " + cmd(tcsStartState) + "/" + move(tcsStartState)
						+ " does not allow move";
				System.out.println("Inside TcsMoveCommand: Error Message is: " + errorMessage);
				sender().tell(new NoLongerValid(new WrongInternalStateIssue(errorMessage)), self());
			} else {
				System.out.println("Inside TcsMoveCommand: Move command -- START: " + t);

				DemandMatcher stateMatcher = TcsCommandHandler.posMatcher();
				SetupConfig scOut = jadd(sc(TcsConfig.movePrefix));

				sendState(new AssemblySetState(cmdItem(cmdBusy), moveItem(moveMoving)));

				referedActor.tell(new HcdController.Submit(scOut), self());

				Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
				TcsCommandHandler.executeMatch(context(), stateMatcher, referedActor, Optional.of(sender()), timeout,
						status -> {
							if (status == Completed) {
								System.out.println("Inside TcsMoveCommand: Move Command Completed");
								sendState(new AssemblySetState(cmdItem(cmdReady), moveItem(moveIndexed)));
							} else if (status instanceof Error) {
								log.error("Inside TcsMoveCommand: Move command match failed with message: "
										+ ((Error) status).message());
							}
						});
			}
		}).matchEquals(JSequentialExecutor.StopCurrentCommand(), t -> {
			System.out.println("Inside TcsMoveCommand: Move command -- STOP: " + t);
			referedActor.tell(new HcdController.Submit(jadd(sc("tcs.stop"))), self());
		}).matchAny(t -> log.warning("Inside TcsMoveCommand: Unknown message received: " + t)).build());
	}

	private void sendState(AssemblySetState tcsSetState) {
		tcsStateActor.ifPresent(actorRef -> {
			try {
				ask(actorRef, tcsSetState, 5000).toCompletableFuture().get();
			} catch (Exception e) {
				log.error(e, "Inside TcsMoveCommand: sendState: Error setting state");
			}
		});
	}

	public static Props props(AssemblyContext ac, SetupConfig sc, ActorRef referedActor, AssemblyState tcsState,
			Optional<ActorRef> stateActor) {
		return Props.create(new Creator<TcsMoveCommand>() {
			private static final long serialVersionUID = 1L;

			@Override
			public TcsMoveCommand create() throws Exception {
				return new TcsMoveCommand(ac, sc, referedActor, tcsState, stateActor);
			}
		});
	}
}
