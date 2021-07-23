package sr.portal2.discord2ip;

import sr.portal2.discord2ip.bot.DiscordBot;

/**
 * A circular buffer that holds 5 opus frames of data.
 * Incoming frames from the {@link DiscordBot} accumulate in these frames,
 * and frames are read, cleared, and reallocated by the {@link NDIAudioSink} whether.
 */
public class AudioBuffer {
}
