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
import csw.services.ccs.DemandMatcher;
import csw.services.ccs.HcdController;
import csw.services.ccs.CommandStatus.Error;
import csw.services.ccs.CommandStatus.NoLongerValid;
import csw.services.ccs.Validation.WrongInternalStateIssue;
import csw.util.config.DoubleItem;
import csw.util.config.Configurations.SetupConfig;
import javacsw.services.ccs.JSequentialExecutor;
import tmt.tcs.common.AssemblyContext;
import tmt.tcs.common.AssemblyStateActor.AssemblySetState;
import tmt.tcs.common.AssemblyStateActor.AssemblyState;

/*
 * This is an actor class which receives command specific to Offset Operation
 * And after any modifications if required, redirect the same to MCS HCD
 */
public class McsOffsetCommand extends AbstractActor {

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Optional<ActorRef> mcsStateActor;

	public McsOffsetCommand(AssemblyContext ac, SetupConfig sc, ActorRef mcsHcd, AssemblyState mcsStartState, Optional<ActorRef> stateActor) {
		this.mcsStateActor = stateActor;

		receive(ReceiveBuilder.matchEquals(JSequentialExecutor.CommandStart(), t -> {
			if (cmd(mcsStartState).equals(cmdUninitialized)
					|| move(mcsStartState).equals(moveMoving)) {
				String errorMessage = "Mcs Assembly state of " + cmd(mcsStartState) + "/" + move(mcsStartState)
						+ " does not allow move";
				System.out.println("Inside McsOffsetCommand: Error Message is: " + errorMessage);
				sender().tell(new NoLongerValid(new WrongInternalStateIssue(errorMessage)), self());
			} else {
				System.out.println("Inside McsOffsetCommand: Move command -- START: " + t);

				DoubleItem xItem = jitem(sc, McsConfig.xDemandKey);
				DoubleItem yItem = jitem(sc, McsConfig.yDemandKey);

				Double x = jvalue(xItem);
				Double y = jvalue(yItem);

				System.out.println("Inside McsOffsetCommand: Move command: sc is: " + sc + ": x is: " + x + ": y is: "
						+ y);
				
				DemandMatcher stateMatcher = McsCommandHandler.posMatcher(x, y);
				SetupConfig scOut = jadd(sc(McsConfig.offsetPrefix), jset(McsConfig.x, x),
						jset(McsConfig.y, y));

				sendState(new AssemblySetState(cmdItem(cmdBusy), moveItem(moveMoving)));

				mcsHcd.tell(new HcdController.Submit(scOut), self());

				Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
				McsCommandHandler.executeMatch(context(), stateMatcher, mcsHcd, Optional.of(sender()), timeout,
						status -> {
							if (status == Completed) {
								System.out.println("Inside McsOffsetCommand: Move Command Completed");
								sendState(new AssemblySetState(cmdItem(cmdReady), moveItem(moveIndexed)));
							} else if (status instanceof Error) {
								log.error("Inside McsOffsetCommand: Offset command match failed with message: "
										+ ((Error) status).message());
							}
						});
			}
		}).matchEquals(JSequentialExecutor.StopCurrentCommand(), t -> {
			System.out.println("Inside McsOffsetCommand: Offset command -- STOP: " + t);
			mcsHcd.tell(new HcdController.Submit(jadd(sc("tcs.mcs.stop"))), self());
		}).matchAny(t -> log.warning("Inside McsOffsetCommand: Unknown message received: " + t)).build());
	}
	
	private void sendState(AssemblySetState mcsSetState) {
		mcsStateActor.ifPresent(actorRef -> {
			try {
				ask(actorRef, mcsSetState, 5000).toCompletableFuture().get();
			} catch (Exception e) {
				log.error(e, "Inside McsOffsetCommand: sendState: Error setting state");
			}
		});
	}

	public static Props props(AssemblyContext ac, SetupConfig sc, ActorRef mcsHcd, AssemblyState mcsState, Optional<ActorRef> stateActor) {
		return Props.create(new Creator<McsOffsetCommand>() {
			private static final long serialVersionUID = 1L;

			@Override
			public McsOffsetCommand create() throws Exception {
				return new McsOffsetCommand(ac, sc, mcsHcd, mcsState, stateActor);
			}
		});
	}
}
