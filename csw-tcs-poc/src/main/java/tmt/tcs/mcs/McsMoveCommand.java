package tmt.tcs.mcs;

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
 * And after any modifications if required, redirect the same to MCS HCD
 */
public class McsMoveCommand extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Optional<ActorRef> mcsStateActor;

	public McsMoveCommand(AssemblyContext ac, SetupConfig sc, ActorRef mcsHcd, AssemblyState mcsStartState,
			Optional<ActorRef> stateActor) {
		this.mcsStateActor = stateActor;

		receive(ReceiveBuilder.matchEquals(JSequentialExecutor.CommandStart(), t -> {
			if (cmd(mcsStartState).equals(cmdUninitialized)
					|| move(mcsStartState).equals(moveMoving)) {
				String errorMessage = "Mcs Assembly state of " + cmd(mcsStartState) + "/" + move(mcsStartState)
						+ " does not allow move";
				System.out.println("Inside McsMoveCommand: Error Message is: " + errorMessage);
				sender().tell(new NoLongerValid(new WrongInternalStateIssue(errorMessage)), self());
			} else {
				System.out.println("Inside McsMoveCommand: Move command -- START: " + t);

				DoubleItem azItem = jitem(sc, McsConfig.azDemandKey);
				DoubleItem elItem = jitem(sc, McsConfig.elDemandKey);
				DoubleItem timeItem = jitem(sc, McsConfig.timeDemandKey);

				Double az = jvalue(azItem);
				Double el = jvalue(elItem);
				Double time = jvalue(timeItem);

				System.out.println("Inside McsMoveCommand: Move command: sc is: " + sc + ": az is: " + az + ": el is: "
						+ el + ": time is: " + time);

				DemandMatcher stateMatcher = McsCommandHandler.posMatcher(az, el, time);
				SetupConfig scOut = jadd(sc(McsConfig.movePrefix), jset(McsConfig.az, az),
						jset(McsConfig.el, el), jset(McsConfig.time, time));

				sendState(new AssemblySetState(cmdItem(cmdBusy), moveItem(moveMoving)));

				mcsHcd.tell(new HcdController.Submit(scOut), self());

				Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
				McsCommandHandler.executeMatch(context(), stateMatcher, mcsHcd, Optional.of(sender()), timeout,
						status -> {
							if (status == Completed) {
								System.out.println("Inside McsMoveCommand: Move Command Completed");
								sendState(new AssemblySetState(cmdItem(cmdReady), moveItem(moveIndexed)));
							} else if (status instanceof Error) {
								log.error("Inside McsMoveCommand: Move command match failed with message: "
										+ ((Error) status).message());
							}
						});
			}
		}).matchEquals(JSequentialExecutor.StopCurrentCommand(), t -> {
			System.out.println("Inside McsMoveCommand: Move command -- STOP: " + t);
			mcsHcd.tell(new HcdController.Submit(jadd(sc("tcs.mcs.stop"))), self());
		}).matchAny(t -> log.warning("Inside McsMoveCommand: Unknown message received: " + t)).build());
	}

	private void sendState(AssemblySetState mcsSetState) {
		mcsStateActor.ifPresent(actorRef -> {
			try {
				ask(actorRef, mcsSetState, 5000).toCompletableFuture().get();
			} catch (Exception e) {
				log.error(e, "Inside McsMoveCommand: sendState: Error setting state");
			}
		});
	}

	public static Props props(AssemblyContext ac, SetupConfig sc, ActorRef mcsHcd, AssemblyState mcsState,
			Optional<ActorRef> stateActor) {
		return Props.create(new Creator<McsMoveCommand>() {
			private static final long serialVersionUID = 1L;

			@Override
			public McsMoveCommand create() throws Exception {
				return new McsMoveCommand(ac, sc, mcsHcd, mcsState, stateActor);
			}
		});
	}
}
