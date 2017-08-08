package tmt.tcs.common;

import static akka.pattern.PatternsCS.ask;

import java.util.Optional;
import java.util.function.Consumer;

import akka.actor.AbstractActor;
import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.util.Timeout;
import csw.services.ccs.CommandStatus.CommandStatus;
import csw.services.ccs.MultiStateMatcherActor;
import csw.services.ccs.StateMatcher;
import javacsw.services.pkg.ILocationSubscriberClient;

/*
 * This is base class for all command handler classes
 */
public abstract class BaseCommandHandler extends AbstractActor
		implements AssemblyStateClient, ILocationSubscriberClient {

	private AssemblyStateActor.AssemblyState internalState = AssemblyStateActor.defaultAssemblyState;

	public static class CommandDone {
	}

	@Override
	public void setCurrentState(AssemblyStateActor.AssemblyState mcsState) {
		internalState = mcsState;
	}

	public AssemblyStateActor.AssemblyState currentState() {
		return internalState;
	}

	public static void executeMatch(ActorContext context, StateMatcher stateMatcher, ActorRef currentStateSource,
			Optional<ActorRef> replyTo, Timeout timeout, Consumer<CommandStatus> codeBlock) {

		System.out.println("Inside BaseCommandHandler executeMatch: Starts");

		ActorRef matcher = context.actorOf(MultiStateMatcherActor.props(currentStateSource, timeout));

		ask(matcher, MultiStateMatcherActor.createStartMatch(stateMatcher), timeout).thenApply(reply -> {
			CommandStatus cmdStatus = (CommandStatus) reply;
			codeBlock.accept(cmdStatus);
			replyTo.ifPresent(actorRef -> actorRef.tell(cmdStatus, context.self()));
			return null;
		});
	}
}
