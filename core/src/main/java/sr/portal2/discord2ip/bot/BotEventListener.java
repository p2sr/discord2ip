package sr.portal2.discord2ip.bot;

import it.unimi.dsi.fastutil.longs.LongSet;

public interface BotEventListener {
    /**
     * Fired when a new user becomes visible and should be tracked by the WS.
     *
     * A string of these events will be followed by a call to {@link #onUserInfoUpdate()}.
     *
     * @param userId The ID of the new visible user.
     */
    void onNewVisibleUser(long userId);

    /**
     * Fired with a new set of channels that the bot can join.
     *
     * @param channelIds The new set of channels.
     */
    void onAvailableChannelsUpdate(LongSet channelIds);

    /**
     * Fired when some event happens that causes user information to change.
     */
    void onUserInfoUpdate();
}
