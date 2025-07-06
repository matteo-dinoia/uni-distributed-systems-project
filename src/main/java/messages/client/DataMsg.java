package messages.client;

import java.io.Serializable;

public abstract class DataMsg implements Serializable {
    public final int key;
    public final int requestId;

    protected DataMsg(int key, int requestId) {
        this.key = key;
        this.requestId = requestId;
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
}
