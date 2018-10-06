package de.reondo.akka.remoting;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import de.reondo.akka.remoting.actor.ReceiverActor;
import de.reondo.akka.remoting.actor.SenderActor;

import java.util.Scanner;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: App <ActorSystem> (\"Send\" <RemoteActor> <NumMessages> | \"Receive\")");
            System.exit(1);
        }
        ActorSystem actorSystem = ActorSystem.create(args[0]);
        try {
            if (args[1].equals("Send") && args.length == 4) {
                ActorRef senderActor = actorSystem.actorOf(SenderActor.props(), "sender");
                actorSystem.actorOf(ReceiverActor.props(), "receiver");
                int numMessages = Integer.parseInt(args[3]);
                senderActor.tell(new SenderActor.Send(ActorPath.fromString(args[2]), numMessages), ActorRef.noSender());
            } else if (args[1].equals("Receive")) {
                actorSystem.actorOf(ReceiverActor.props(), "receiver");
            } else {
                System.out.println("Unknown command " + args[1]);
            }
            new Scanner(System.in).nextLine();
        } finally {
            actorSystem.terminate();
        }
    }
}
