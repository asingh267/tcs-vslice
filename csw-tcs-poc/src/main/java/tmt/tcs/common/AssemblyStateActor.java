package tmt.tcs.common;

import static javacsw.util.config.JItems.jset;
import static javacsw.util.config.JItems.jvalue;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.japi.Creator;
import akka.japi.pf.ReceiveBuilder;
import csw.util.config.Choice;
import csw.util.config.ChoiceItem;
import csw.util.config.ChoiceKey;
import csw.util.config.Choices;
import scala.PartialFunction;
import scala.runtime.BoxedUnit;

public class AssemblyStateActor extends AbstractActor {

	private AssemblyStateActor() {
		receive(stateReceive(new AssemblyState(cmdDefault, moveDefault)));
	}

	private PartialFunction<Object, BoxedUnit> stateReceive(AssemblyState assemblyCurrentState) {
		return ReceiveBuilder.match(AssemblySetState.class, t -> {
			System.out.println("Inside AssemblyStateActor stateReceive : AssemblySetState: " + assemblyCurrentState);
			AssemblyState assemblyState = t.assemblyState;
			if (!assemblyState.equals(assemblyCurrentState)) {
				context().system().eventStream().publish(assemblyState);
				context().become(stateReceive(assemblyState));
				sender().tell(new AssemblyStateWasSet(true), self());
			} else {
				sender().tell(new AssemblyStateWasSet(false), self());
			}
		}).match(AssemblyGetState.class, t -> {
			System.out.println("Inside AssemblyStateActor stateReceive : GetState");
			sender().tell(assemblyCurrentState, self());
		}).matchAny(t -> System.out.println("Inside AssemblyStateActor stateReceive message is: " + t)).build();
	}

	public static Props props() {
		return Props.create(new Creator<AssemblyStateActor>() {
			private static final long serialVersionUID = 1L;

			@Override
			public AssemblyStateActor create() throws Exception {
				return new AssemblyStateActor();
			}
		});
	}

	// Keys for state telemetry item
	public static final Choice cmdUninitialized = new Choice("uninitialized");
	public static final Choice cmdReady = new Choice("ready");
	public static final Choice cmdBusy = new Choice("busy");
	public static final Choice cmdContinuous = new Choice("continuous");
	public static final Choice cmdError = new Choice("error");
	public static final ChoiceKey cmdKey = new ChoiceKey("cmd",
			Choices.fromChoices(cmdUninitialized, cmdReady, cmdBusy, cmdContinuous, cmdError));
	public static final ChoiceItem cmdDefault = cmdItem(cmdReady);

	public static Choice cmd(AssemblyState assemblyState) {
		return jvalue(assemblyState.cmd);
	}

	/**
	 * A convenience method to set the cmdItem choice
	 *
	 * @param ch
	 *            one of the cmd choices
	 * @return a ChoiceItem with the choice value
	 */
	public static ChoiceItem cmdItem(Choice ch) {
		return jset(cmdKey, ch);
	}

	public static final Choice moveUnindexed = new Choice("unindexed");
	public static final Choice moveIndexing = new Choice("indexing");
	public static final Choice moveIndexed = new Choice("indexed");
	public static final Choice moveMoving = new Choice("moving");
	public static final ChoiceKey moveKey = new ChoiceKey("move",
			Choices.fromChoices(moveUnindexed, moveIndexing, moveIndexed, moveMoving));
	public static final ChoiceItem moveDefault = moveItem(moveIndexed);

	public static Choice move(AssemblyState assemblyState) {
		return jvalue(assemblyState.move);
	}

	/**
	 * A convenience method to set the moveItem choice
	 *
	 * @param ch
	 *            one of the move choices
	 * @return a ChoiceItem with the choice value
	 */
	public static ChoiceItem moveItem(Choice ch) {
		return jset(moveKey, ch);
	}

	public static final AssemblyState defaultAssemblyState = new AssemblyState(cmdDefault, moveDefault);

	/**
	 * This class is sent to the publisher for publishing when any state value
	 * changes
	 */
	public static class AssemblyState {
		public final ChoiceItem cmd;
		public final ChoiceItem move;

		/**
		 * Constructor
		 *
		 * @param cmd
		 *            the current cmd state
		 * @param move
		 *            the current move state
		 */
		public AssemblyState(ChoiceItem cmd, ChoiceItem move) {
			this.cmd = cmd;
			this.move = move;
		}
	}

	/**
	 * Update the current state with a AssemblyState
	 */
	public static class AssemblySetState {
		public final AssemblyState assemblyState;

		/**
		 * Constructor
		 *
		 * @param assemblyState
		 *            the new assembly state value
		 */
		public AssemblySetState(AssemblyState assemblyState) {
			this.assemblyState = assemblyState;
		}

		/**
		 * Alternate way to create the SetState message using items
		 *
		 * @param cmd
		 *            a ChoiceItem created with cmdItem
		 * @param move
		 *            a ChoiceItem created with moveItem
		 */
		public AssemblySetState(ChoiceItem cmd, ChoiceItem move) {
			this(new AssemblyState(cmd, move));
		}

		/**
		 * Alternate way to create the SetState message using primitives
		 *
		 * @param cmd
		 *            a Choice for the cmd value (i.e. cmdReady, cmdBusy, etc.)
		 * @param move
		 *            a Choice for the mvoe value (i.e. moveUnindexed,
		 *            moveIndexing, etc.)
		 */
		public AssemblySetState(Choice cmd, Choice move) {
			this(new AssemblyState(cmdItem(cmd), moveItem(move)));
		}
	}

	/**
	 * A message that causes the current state to be sent back to the sender
	 */
	public static class AssemblyGetState {
	}

	/**
	 * Reply to SetState message that indicates if the state was actually set
	 */
	public static class AssemblyStateWasSet {
		final boolean wasSet;

		public AssemblyStateWasSet(boolean wasSet) {
			this.wasSet = wasSet;
		}
	}

}
