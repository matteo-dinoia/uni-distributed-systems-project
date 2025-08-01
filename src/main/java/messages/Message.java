package messages;

import akka.actor.typed.ActorRef;

import java.io.Serializable;

public record Message(ActorRef<Message> sender, Serializable content) {

//    public Message clone() {
//        Message.Content copyContent = switch (this.content) {
//            case StatusMsg.Join(var a) -> new StatusMsg.Join(a);
//            case StatusMsg.InitialMembers(var a) -> new StatusMsg.InitialMembers(a);
//            case NodeMsg.BootstrapRequest(var a) -> new NodeMsg.BootstrapRequest(a);
//            case NodeMsg.BootstrapResponse(var a, var b) -> new NodeMsg.BootstrapResponse(a, b);
//            case NodeMsg.ResponsabilityRequest(var a, var b) -> new NodeMsg.ResponsabilityRequest(a, b);
//            case NodeMsg.ResponsabilityResponse(var a, var b, var c) -> new NodeMsg.ResponsabilityResponse(a, b, c);
//            // ───────────── Leave ─────────────
//            case NodeMsg.PassResponsabilityRequest(var a, var b) -> new NodeMsg.PassResponsabilityRequest(a, b);
//            case NodeMsg.PassResponsabilityResponse(var a, var b) -> new NodeMsg.PassResponsabilityResponse(a, b);
//            case NodeMsg.RollbackPassResponsability(var a) -> new NodeMsg.RollbackPassResponsability(a);
//            // ───────────── Recover ─────────────
//            case StatusMsg.Recover(var a) -> new StatusMsg.Recover(a);
//            // ───────────── Timeout ─────────────
//            case NodeMsg.Timeout(var a) -> new NodeMsg.Timeout(a);
//            // ───────────── Notifications ─────────────
//            case NotifyMsg.NodeJoined(var a) -> new NotifyMsg.NodeJoined(a);
//            case NotifyMsg.NodeLeft(var a) -> new NotifyMsg.NodeLeft(a);
//            // ───────────── Crash / Leave ─────────────
//            case StatusMsg.Crash() -> new StatusMsg.Crash();
//            case StatusMsg.Leave() -> new StatusMsg.Leave();
//            // ───────────── Read (Get) ─────────────
//            case DataMsg.Get(var a, var b) -> new DataMsg.Get(a, b);
//            case NodeDataMsg.ReadRequest(var a, var b) -> new NodeDataMsg.ReadRequest(a, b);
//            case NodeDataMsg.ReadResponse(var a, var b) -> new NodeDataMsg.ReadResponse(a, b);
//            case NodeDataMsg.ReadImpossibleForLock(var a) -> new NodeDataMsg.ReadImpossibleForLock(a);
//            // ───────────── Write (Update) ─────────────
//            case DataMsg.Update(var a, var b) -> new DataMsg.Update(a, b);
//            case NodeDataMsg.WriteRequest(var a, var b, var c, var d) -> new NodeDataMsg.WriteRequest(a, b, c, d);
//            case NodeDataMsg.WriteAck(var a) -> new NodeDataMsg.WriteAck(a);
//            // ───────────── Locking ─────────────
//            case NodeDataMsg.WriteLockRequest(var a, var b) -> new NodeDataMsg.WriteLockRequest(a, b);
//            case NodeDataMsg.WriteLockGranted(var a, var b, var c) -> new NodeDataMsg.WriteLockGranted(a, b, c);
//            case NodeDataMsg.WriteLockDenied(var a) -> new NodeDataMsg.WriteLockDenied(a);
//            case NodeDataMsg.LocksRelease(var a, var b) -> new NodeDataMsg.LocksRelease(a, b);
//            case NodeDataMsg.ReadLockAcked(var a) -> new NodeDataMsg.ReadLockAcked(a);
//            case NodeDataMsg.ReadLockRequest(var a, var b) -> new NodeDataMsg.ReadLockRequest(a, b);
//            // OTHER
//            case ControlMsg.LeaveAck(var a) -> new ControlMsg.LeaveAck(a);
//            case ControlMsg.RecoverAck(var a) -> new ControlMsg.RecoverAck(a);
//            case ControlMsg.CrashAck() -> new ControlMsg.CrashAck();
//            case ControlMsg.JoinAck(var a) -> new ControlMsg.JoinAck(a);
//            case ControlMsg.WriteFullyCompleted() -> new ControlMsg.WriteFullyCompleted();
//            case ControlMsg.InitialMembersAck() -> new ControlMsg.InitialMembersAck();
//            case ControlMsg.DebugCurrentStateRequest() -> new ControlMsg.DebugCurrentStateRequest();
//            case ControlMsg.DebugCurrentStateResponse(var a) -> new ControlMsg.DebugCurrentStateResponse(a);
//            case ControlMsg.DebugCurrentStorageRequest() -> new ControlMsg.DebugCurrentStorageRequest();
//            case ControlMsg.DebugCurrentStorageResponse(var a, var b) ->
//                    new ControlMsg.DebugCurrentStorageResponse(a, b);
//            case ResponseMsgs.ReadSucceeded(var a, var b, var c) -> new ResponseMsgs.ReadSucceeded(a, b, c);
//            case ResponseMsgs.ReadResultFailed(var a) -> new ResponseMsgs.ReadResultFailed(a);
//            case ResponseMsgs.ReadResultInexistentValue(var a) -> new ResponseMsgs.ReadResultInexistentValue(a);
//            case ResponseMsgs.ReadTimeout(var a) -> new ResponseMsgs.ReadTimeout(a);
//            case ResponseMsgs.WriteSucceeded(var a, var b, var c) -> new ResponseMsgs.WriteSucceeded(a, b, c);
//            case ResponseMsgs.WriteTimeout(var a) -> new ResponseMsgs.WriteTimeout(a);
//            default -> {
//                System.err.println("Unexpected type of message to copy " + this.content());
//                throw new IllegalStateException("Unexpected type of message to copy");
//            }
//        };
//        return new Message(this.sender(), copyContent);
//    }
}
