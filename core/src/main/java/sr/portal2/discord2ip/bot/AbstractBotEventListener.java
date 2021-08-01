package sr.portal2.discord2ip.bot;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.longs.LongSet;

public abstract class AbstractBotEventListener implements BotEventListener {
    @Override
    public void onNewVisibleUser(long userId) {
        // no-op
    }

    @Override
    public void onAvailableChannelsUpdate(LongSet channelIds) {
        // no-op
    }

    @Override
    public void onUserInfoUpdate() {
        // no-op
    }

    @Override
    public void onUserVolumeUpdate(LongList userIds, DoubleList leftVolumes, DoubleList rightVolumes) {
        // no-op
    }
}
