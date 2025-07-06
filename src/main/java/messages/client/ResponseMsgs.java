package messages.client;

import java.io.Serializable;

// TODO move out
public interface ResponseMsgs extends Serializable {
    // Answers:
    public class Completed implements ResponseMsgs {

    }

    public class Timeouted implements ResponseMsgs {

    }

    public class Invalid implements ResponseMsgs {

    }
}
