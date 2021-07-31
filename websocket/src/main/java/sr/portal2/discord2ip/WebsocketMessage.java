package sr.portal2.discord2ip;

import com.dslplatform.json.CompiledJson;
import com.dslplatform.json.DslJson;
import com.dslplatform.json.JsonAttribute;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;

@CompiledJson
public class WebsocketMessage {
    public static final int UPDATE_IDENTITY_TYPE = 0;
    public static final int UPDATE_CHANNELS_TYPE = 1;
    public static final int UPDATE_CURRENT_CHANNEL_TYPE = 2;
    public static final int UPDATE_USERS_TYPE = 3;
    public static final int UPDATE_CURRENT_SOLO_TYPE = 4;
    public static final int UPDATE_VOLUMES = 5;

    @CompiledJson
    public static class Identity {
        public Identity(String name, String avatar, String state) {
            this.name = name;
            this.avatar = avatar;
            this.state = state;
        }

        @JsonAttribute
        public String name;

        @JsonAttribute
        public String avatar;

        @JsonAttribute
        public String state;
    }

    @CompiledJson
    public static class Channel {
        public Channel(String id, String name, String server) {
            this.id = id;
            this.name = name;
            this.server = server;
        }

        @JsonAttribute
        public String id;

        @JsonAttribute
        public String name;

        @JsonAttribute
        public String server;
    }

    @CompiledJson
    public static class User {

        public User() {
        }

        public User(String id) {
            this.id = id;
        }

        @JsonAttribute
        public String id;

        @JsonAttribute
        public String name;

        @JsonAttribute
        public String avatar;

        @JsonAttribute
        public boolean muted = false;

        @JsonAttribute
        public String channel;

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof User)) {
                return false;
            }
            return ((User)obj).id.equals(this.id);
        }
    }

    @CompiledJson
    public static class VolumeUpdate {
        public VolumeUpdate(String id, double volumeLeft, double volumeRight) {
            this.id = id;
            this.volumeLeft = volumeLeft;
            this.volumeRight = volumeRight;
        }

        @JsonAttribute
        public String id;

        @JsonAttribute
        public double volumeLeft;

        @JsonAttribute
        public double volumeRight;
    }

    @JsonAttribute(name = "m", mandatory = true)
    public int type;

    @JsonAttribute(name = "identity")
    public Identity identity;

    @JsonAttribute(name = "channels")
    public Collection<Channel> channels;

    @JsonAttribute(name = "currentChannel")
    public String currentChannel;

    @JsonAttribute(name = "users")
    public Collection<User> users;

    @JsonAttribute(name = "volumeUpdates")
    public Collection<VolumeUpdate> volumeUpdates;

    @JsonAttribute(name = "soloUser")
    public String soloUser;

    public String serialize(DslJson<?> dslJson) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            dslJson.serialize(this, baos);
            String serializationResult = baos.toString();
            return baos.toString();
        }
    }
}
