package messages;

import java.io.Serializable;

public class ClientMsgs {

    public abstract static class DataMsg implements Serializable {
        public final int key;
        public final int requestId;

        protected DataMsg(int key, int requestId) {
            this.key = key;
            this.requestId = requestId;
        }
    }

    public static class GetMsg extends DataMsg {
        public final Integer last_version_seen;

        public GetMsg(int requestId, int key, Integer last_version_seen) {
            super(requestId, key);
            this.last_version_seen = last_version_seen;
        }
    }

    public static class UpdateMsg extends DataMsg {
        public final String newValue;
        public final Integer last_version_seen;

        public UpdateMsg(int requestId, int key, String newValue, Integer last_version_seen) {
            super(requestId, key);
            this.newValue = newValue;
            this.last_version_seen = last_version_seen;
        }
    }


    public abstract static class StatusMsg implements Serializable {
        public final int requestId;

        protected StatusMsg(int requestId) {
            this.requestId = requestId;
        }
    }

    public static class JoinMsg extends StatusMsg {
        public final int bootstrappingPear;

        public JoinMsg(int requestId, int bootstrappingPear) {
            super(requestId);
            this.bootstrappingPear = bootstrappingPear;
        }
    }

    public class LeaveMsg extends StatusMsg {
        public LeaveMsg(int requestId) {
            super(requestId);
        }
    }

    public class RecoverMsg extends StatusMsg {
        public final int bootstrappingPear;

        public RecoverMsg(int requestId, int bootstrappingPear) {
            super(requestId);
            this.bootstrappingPear = bootstrappingPear;
        }
    }

    // Answers:
    public class CompletedMsg implements Serializable {
        
    }

    public class TimeoutedMsg implements Serializable {

    }

}
