package tmt.tcs.m3;

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
 * And after any modifications if required, redirect the same to M3 HCD
 */
public class M3OffsetCommand extends AbstractActor{

	private LoggingAdapter log = Logging.getLogger(getContext().system(), this);

	private final Optional<ActorRef> m3StateActor;

	public M3OffsetCommand(AssemblyContext ac, SetupConfig sc, ActorRef m3Hcd, AssemblyState m3StartState, Optional<ActorRef> stateActor) {
		this.m3StateActor = stateActor;

		receive(ReceiveBuilder.matchEquals(JSequentialExecutor.CommandStart(), t -> {
			if (cmd(m3StartState).equals(cmdUninitialized)
					|| move(m3StartState).equals(moveMoving)) {
				String errorMessage = "M3 Assembly state of " + cmd(m3StartState) + "/" + move(m3StartState)
						+ " does not allow move";
				System.out.println("Inside M3OffsetCommand: Error Message is: " + errorMessage);
				sender().tell(new NoLongerValid(new WrongInternalStateIssue(errorMessage)), self());
			} else {
				System.out.println("Inside M3OffsetCommand: Move command -- START: " + t);

				DoubleItem xItem = jitem(sc, M3Config.xDemandKey);
				DoubleItem yItem = jitem(sc, M3Config.yDemandKey);

				Double x = jvalue(xItem);
				Double y = jvalue(yItem);

				System.out.println("Inside M3OffsetCommand: Move command: sc is: " + sc + ": x is: " + x + ": y is: "
						+ y);
				
				DemandMatcher stateMatcher = M3CommandHandler.posMatcher(x, y);
				SetupConfig scOut = jadd(sc(M3Config.offsetPrefix), jset(M3Config.x, x),
						jset(M3Config.y, y));

				sendState(new AssemblySetState(cmdItem(cmdBusy), moveItem(moveMoving)));

				m3Hcd.tell(new HcdController.Submit(scOut), self());

				Timeout timeout = new Timeout(5, TimeUnit.SECONDS);
				M3CommandHandler.executeMatch(context(), stateMatcher, m3Hcd, Optional.of(sender()), timeout,
						status -> {
							if (status == Completed) {
								System.out.println("Inside M3OffsetCommand: Move Command Completed");
								sendState(new AssemblySetState(cmdItem(cmdReady), moveItem(moveIndexed)));
							} else if (status instanceof Error) {
								log.error("Inside M3OffsetCommand: Offset command match failed with message: "
										+ ((Error) status).message());
							}
						});
			}
		}).matchEquals(JSequentialExecutor.StopCurrentCommand(), t -> {
			System.out.println("Inside M3OffsetCommand: Offset command -- STOP: " + t);
			m3Hcd.tell(new HcdController.Submit(jadd(sc("tcs.m3.stop"))), self());
		}).matchAny(t -> log.warning("Inside M3OffsetCommand: Unknown message received: " + t)).build());
	}
	
	private void sendState(AssemblySetState m3SetState) {
		m3StateActor.ifPresent(actorRef -> {
			try {
				ask(actorRef, m3SetState, 5000).toCompletableFuture().get();
			} catch (Exception e) {
				log.error(e, "Inside M3OffsetCommand: sendState: Error setting state");
			}
		});
	}

	public static Props props(AssemblyContext ac, SetupConfig sc, ActorRef m3Hcd, AssemblyState m3State, Optional<ActorRef> stateActor) {
		return Props.create(new Creator<M3OffsetCommand>() {
			private static final long serialVersionUID = 1L;

			@Override
			public M3OffsetCommand create() throws Exception {
				return new M3OffsetCommand(ac, sc, m3Hcd, m3State, stateActor);
			}
		});
	}
}
